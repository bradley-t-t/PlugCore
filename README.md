# PlugCore

A Minecraft plugin licensing and authentication system that integrates with plugcore.io via Supabase Edge Functions.

## Features

- Server linking system with verification codes
- Plugin dependency validation
- Automatic disabling of unauthorized plugins
- Cached validation to reduce API calls
- Async API communication
- Public API for other plugins to use

## Setup

1. Configure your Supabase credentials in `SupabaseConfig.java` before building:
```java
private static final String BASE_URL = "https://your-project.supabase.co/functions/v1";
private static final String ANON_KEY = "your-anon-key-here";
```

2. Build the plugin with Maven

3. Install on your server

4. Link your server as an operator:
```
/plugcore link <verification-code>
```

Get your verification code from plugcore.io

## Commands

All commands require operator permission.

- `/plugcore link <code>` - Link your server to PlugCore
- `/plugcore unlink` - Unlink your server from PlugCore
- `/plugcore status` - Check current link status
- `/plugcore validate` - Manually validate server with PlugCore
- `/plugcore plugins` - List dependent plugins and their authorization status
- `/plugcore reload` - Rescan and revalidate plugins

Alias: `/pc`

## How It Works

### Server Linking & Ownership Verification

**Step 1: Generate Token on Website**
- User logs into plugcore.io and navigates to Account > Your Servers
- Clicks "Generate Linking Token" button
- Website creates a unique UUID token in `server_linking_tokens` table
- Token expires after 15 minutes
- User copies the token or the full command: `/plugcore link <token>`

**Step 2: Link Server In-Game**
- Server operator (must have OP status) runs `/plugcore link <token>` in-game
- Plugin collects server information:
  - Server Name (from Bukkit)
  - Server IP (from Bukkit)
  - Minecraft Version (from Bukkit)
  - Server UUID (generated once per server, stored in config)
  
**Step 3: Verification Process**
- Plugin sends POST request to `/link-server` edge function with:
  - `token`: The linking token from website
  - `serverName`: Server's name
  - `serverIp`: Server's IP address  
  - `minecraftVersion`: Minecraft version (e.g., "1.21")
  - `serverUuid`: Unique identifier for this server
  
**Step 4: Database Validation**
- Edge function validates the token:
  - Checks if token exists in `server_linking_tokens`
  - Verifies token hasn't been used (`used = false`)
  - Confirms token hasn't expired (`expires_at > NOW()`)
- If valid, creates/updates record in `linked_servers` table:
  - Links `server_uuid` to `user_id` (server owner)
  - Stores server details (name, IP, Minecraft version)
  - Sets `linked_at` and `last_seen` timestamps
- Marks token as used to prevent reuse

**Step 5: Success Response**
- Plugin receives confirmation
- Stores `server-uuid` and `linked: true` in config
- Server is now linked to the user's plugcore.io account

### Plugin Purchase Validation

**When Does Validation Happen?**
1. Server startup (after 1 second delay)
2. When `/plugcore validate` is run manually
3. Every 5 minutes automatically (background task)
4. When `/plugcore reload` is executed

**Validation Process:**

1. **Plugin Detection**: PlugCore scans all installed plugins looking for:
   ```yaml
   # In other plugin's plugin.yml:
   depend: [PlugCore]
   # or
   softdepend: [PlugCore]
   ```

2. **Retrieve Purchased Plugins**: 
   - Plugin calls `/validate-server` edge function with `serverUuid`
   - Edge function queries `linked_servers` to get `user_id`
   - Queries `plugin_purchases` table matching the user
   - Filters results:
     - **One-time purchases**: Always valid
     - **Subscriptions**: Only if `subscription_status = 'active'` AND `subscription_end_date > NOW()`
   - Returns list of authorized plugin names

3. **Authorization Check**: For each dependent plugin:
   - Check if plugin name is in the authorized list from database
   - Check local cache (1-hour TTL) to reduce API calls
   - If not found, calls `/check-plugin` edge function for specific validation

4. **Enforcement**:
   - **Authorized**: Plugin remains enabled
   - **Unauthorized**: `Bukkit.getPluginManager().disablePlugin(plugin)` is called
   - Console logs which plugins were disabled

5. **Caching Strategy**:
   - Validation results cached for 1 hour (configurable via `cache.validation-ttl`)
   - Individual plugin authorization cached in memory
   - Reduces load on Supabase database

### Database Flow Diagram

```
┌─────────────────┐
│  User visits    │
│  plugcore.io    │
│  /account       │
└────────┬────────┘
         │
         ▼
┌─────────────────────────┐
│ Clicks "Generate Token" │
└────────┬────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ INSERT INTO server_linking_tokens│
│ (token, user_id, expires_at)     │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────┐
│ User copies token        │
│ /plugcore link <token>   │
└────────┬─────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│ Plugin → POST /link-server      │
│ {token, serverName, serverIp,   │
│  minecraftVersion, serverUuid}  │
└────────┬────────────────────────┘
         │
         ▼
┌────────────────────────────────────┐
│ Edge Function validates token:     │
│ SELECT * FROM server_linking_tokens│
│ WHERE token = ? AND used = false   │
│ AND expires_at > NOW()             │
└────────┬───────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│ INSERT/UPDATE linked_servers        │
│ (user_id, server_uuid, server_name, │
│  server_ip, minecraft_version)      │
└────────┬────────────────────────────┘
         │
         ▼
┌───────────────────────────────┐
│ UPDATE server_linking_tokens  │
│ SET used = true               │
│ WHERE token = ?               │
└────────┬──────────────────────┘
         │
         ▼
┌────────────────────────┐
│ Server linked! ✓       │
│ stored in plugin config│
└────────────────────────┘

═══════════════════════════════════

On Startup / Every 5 min:
         │
         ▼
┌──────────────────────────────┐
│ Plugin → POST /validate-server│
│ {serverUuid}                  │
└────────┬─────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│ SELECT user_id FROM linked_servers│
│ WHERE server_uuid = ?            │
└────────┬────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────┐
│ SELECT * FROM plugin_purchases           │
│ WHERE user_id = ?                        │
│ AND (is_subscription = false OR          │
│      (subscription_status = 'active' AND │
│       subscription_end_date > NOW()))    │
└────────┬─────────────────────────────────┘
         │
         ▼
┌────────────────────────────────┐
│ Return list of plugin names    │
│ ["PluginA", "PluginB"]         │
└────────┬───────────────────────┘
         │
         ▼
┌────────────────────────────────┐
│ For each installed plugin:     │
│ - Check if in authorized list  │
│ - If NO → Disable plugin       │
│ - If YES → Keep enabled        │
└────────────────────────────────┘
```

## API Usage

Other plugins can integrate with PlugCore:

```java
import io.plugcore.plugCore.api.PlugCoreAPI;

public class YourPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        if (!PlugCoreAPI.isServerLinked()) {
            getLogger().warning("Server not linked to PlugCore!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        PlugCoreAPI.isPluginAuthorized("YourPluginName").thenAccept(authorized -> {
            if (!authorized) {
                getLogger().warning("Not authorized to use this plugin!");
                getServer().getPluginManager().disablePlugin(this);
            }
        });
    }
}
```

### Adding PlugCore as a Dependency

In your plugin.yml:
```yaml
name: YourPlugin
version: 1.0
main: com.example.YourPlugin
depend: [PlugCore]
```

## Supabase Database Schema

The plugin requires the following tables in your Supabase database:

### plugin_purchases
Stores plugin purchases (one-time and subscriptions):
```sql
- id: UUID primary key
- user_id: UUID (references auth.users)
- plugin_id: UUID (references plugins table)
- purchased_at: Timestamp
- purchase_price: Numeric(10,2)
- is_subscription: Boolean
- subscription_status: 'active' | 'expired' | 'cancelled'
- subscription_end_date: Timestamp (for subscription validation)
- subscription_start_date: Timestamp
- will_renew: Boolean
- cancellation_reason: Text
- gateway_subscription_id: String (Stripe/PayPal ID)
```

### linked_servers
Stores linked Minecraft servers:
```sql
- id: UUID primary key
- server_id: String (unique server identifier)
- owner_uuid: UUID (Minecraft player UUID of server owner)
- verification_token: String (for validation)
- linked_at: Timestamp
- last_validated: Timestamp
```

### verification_codes
Temporary codes for linking servers:
```sql
- id: UUID primary key
- user_id: UUID (references auth.users)
- code: String (unique, one-time use)
- used: Boolean
- created_at: Timestamp
- expires_at: Timestamp (default 1 hour expiry)
```

## Supabase Edge Functions

You'll need to create the following Edge Functions:

### link-server
Validates the verification code and links the server.

Expected request:
```json
{
  "serverId": "uuid",
  "ownerUUID": "uuid",
  "verificationCode": "code"
}
```

Expected response:
```json
{
  "valid": true,
  "message": "Server linked successfully",
  "serverId": "uuid",
  "purchasedPlugins": ["Plugin1", "Plugin2"]
}
```

Logic:
1. Verify code exists and matches user_id
2. Check code not expired and not used
3. Create linked_servers record
4. Query plugin_purchases for user_id (checking subscription_status and subscription_end_date)
5. Mark verification code as used
6. Return list of authorized plugin names

### validate-server
Validates an already linked server and returns current purchases.

Expected request:
```json
{
  "serverId": "uuid",
  "verificationToken": "token"
}
```

Expected response:
```json
{
  "valid": true,
  "message": "Server validated",
  "serverId": "uuid",
  "purchasedPlugins": ["Plugin1", "Plugin2"]
}
```

Logic:
1. Verify server_id and verification_token match in linked_servers
2. Query plugin_purchases for the server owner's user_id
3. Filter purchases: is_subscription=false OR (subscription_status='active' AND subscription_end_date > NOW())
4. Update last_validated timestamp
5. Return list of authorized plugin names

### check-plugin
Checks if a specific plugin is purchased for a server.

Expected request:
```json
{
  "serverId": "uuid",
  "pluginName": "PluginName"
}
```

Expected response:
```json
{
  "purchased": true
}
```

Logic:
1. Get owner_uuid from linked_servers by server_id
2. Find plugin_id from plugins table by plugin name
3. Query plugin_purchases for user_id + plugin_id
4. Verify purchase is valid (not subscription OR subscription active and not expired)
5. Return boolean

## Configuration

```yaml
server:
  linked: false
  server-id: ""
  owner-uuid: ""
  verification-token: ""

cache:
  validation-ttl: 3600
  purchased-plugins: []
```

- `validation-ttl`: Time in seconds before revalidating with API (default: 3600 = 1 hour)

**Note**: Supabase API credentials are hardcoded in `SupabaseConfig.java` before building and are not exposed to server owners.

## Requirements

- Java 21
- Paper/Spigot 1.21+
- Supabase account with Edge Functions configured

## License

Copyright (c) 2025 Trenton - plugcore.io

