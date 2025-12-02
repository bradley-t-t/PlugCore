# PlugCore

**The official plugin validation system for plugcore.io**

PlugCore validates premium Minecraft plugins purchased from plugcore.io. It ensures that only authorized servers can run
premium plugins through seamless integration with your plugcore.io account.

---

## Features

- **Automatic Validation** - Validates plugins purchased from plugcore.io automatically
- **Server Linking** - Link your server to your plugcore.io account with a simple command
- **Real-Time Checks** - Validates purchases and subscriptions in real-time
- **Subscription Support** - Works with both one-time purchases and recurring subscriptions
- **Easy Integration** - Plugin developers only need 1 line of code
- **Tamper Protection** - Detects and prevents unauthorized modifications

---

## For Server Owners

### Installation

1. Download PlugCore from [plugcore.io](https://plugcore.io)
2. Place the JAR in your `plugins/` folder
3. Start your server
4. Link your server:
   ```
   /plugcore link <token>
   ```
   Get your linking token from plugcore.io

5. Verify the link:
   ```
   /plugcore status
   ```

That's it! Any premium plugins that require PlugCore will now validate automatically.

### Commands

| Command                  | Description                     |
|--------------------------|---------------------------------|
| `/plugcore link <token>` | Link your server to plugcore.io |
| `/plugcore unlink`       | Unlink your server              |
| `/plugcore plugins`      | List all dependent plugins      |

**Aliases:** `/pc`

**Note:** All commands require operator permissions.

### What Happens When You Install a Premium Plugin

1. Plugin checks if your server is linked to PlugCore
2. PlugCore verifies you've purchased the plugin
3. If authorized, the plugin runs normally
4. If not authorized, the plugin disables itself with a clear error message

---

## For Plugin Developers

### Quick Start

Making your plugin work with PlugCore requires just two simple steps:

#### 1. Add Dependency to `plugin.yml`

```yaml
name: MyPremiumPlugin
version: 1.0.0
main: com.example.MyPremiumPlugin
api-version: '1.21'
depend: [ PlugCore ]
```

#### 2. Add Authorization Check

```java
package com.example;

import io.plugcore.plugCore.api.PlugCoreAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPremiumPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
       try {
          PlugCore plugCore = PlugCore.getInstance();
          if (plugCore == null || !plugCore.isServerLinked()) {
             getServer().getPluginManager().disablePlugin(this);
          }
       } catch (Exception e) {
          getServer().getPluginManager().disablePlugin(this);
       }
       
       // Rest of your code after validation. The PlugCore plugin handles the rest.
       
    }
}
```

#### 3. You need to include the Jitpack repository and dependency in your build tool configuration.

[![](https://jitpack.io/v/bradley-t-t/PlugCore.svg)](https://jitpack.io/#bradley-t-t/PlugCore)


**That's all you need!**

### How It Works

1. Server owner installs your plugin
2. Your plugin checks authorization on startup
3. PlugCore validates the server's license
4. If authorized, plugin runs normally
5. If not authorized, plugin disables itself automatically

### What Gets Validated

- Server is linked to plugcore.io
- Server owner has purchased your plugin
- Subscription is active (if applicable)
- Plugin files haven't been tampered with

### Protection Layers

Your plugin is protected by multiple security layers:

1. **Dependency Check** - Plugin won't load without PlugCore installed
2. **Authorization API** - Plugin must explicitly check authorization
3. **License Validation** - Real-time verification with plugcore.io
4. **Tamper Detection** - Modifications are automatically detected

---

## Example Scenarios

### Authorized Plugin (Success)

```
[PlugCore] Validating STARTUP plugin: MyPlugin
[PlugCore] STARTUP plugin 'MyPlugin' is authorized!
[MyPlugin] Enabling MyPlugin v1.0.0
[MyPlugin] Plugin authorized!
[MyPlugin] MyPlugin enabled and authorized!
```

### Server Not Linked

```
[MyPlugin] Enabling MyPlugin v1.0.0
[MyPlugin] Server not linked to PlugCore!
[MyPlugin] This plugin cannot run on unlinked servers.
[MyPlugin] Link your server: /plugcore link <token>
[MyPlugin] Disabling MyPlugin v1.0.0
```

### Plugin Not Purchased

```
[MyPlugin] Enabling MyPlugin v1.0.0
[MyPlugin] This plugin is NOT authorized!
[MyPlugin] You have not purchased this plugin.
[MyPlugin] Purchase at plugcore.io
[MyPlugin] Disabling MyPlugin v1.0.0
```

---

## Troubleshooting

### "Server not linked"

Run `/plugcore link <token>` with a token from plugcore.io

### "Plugin not purchased"

Purchase the plugin at plugcore.io or verify your purchase

### "Plugin not authorized"

This can mean:

1. Server is not linked - use `/plugcore link <token>`
2. Plugin not purchased - buy it on plugcore.io
3. Subscription expired - renew your subscription

### Plugin shows RED in `/pl` list

The plugin disabled itself due to authorization failure. Check the console logs for the specific reason.

---

## Updates

When a plugin developer releases an update:

1. Server owners download the new version
2. Old versions continue working during the transition
3. No re-validation or re-purchasing required
4. Update at your own pace

---

## API Reference

### `PlugCoreAPI.requireAuthorization(Plugin plugin)`

Validates that your plugin is authorized to run on this server.

**Returns:** `boolean`

- `true` if validation started successfully
- `false` if PlugCore is missing or server not linked

**Usage:**

```java
if(!PlugCoreAPI.requireAuthorization(this)){
        return;
        }
```

### `PlugCoreAPI.isServerLinked()`

Check if the server is linked to plugcore.io.

**Returns:** `boolean`

**Usage:**

```java
if(PlugCoreAPI.isServerLinked()){
        // Server is linked
        }
```

---

## Support

- **Website:** [plugcore.io](https://plugcore.io)

---

## License

PlugCore is proprietary software. All rights reserved.

Redistribution, modification, or reverse engineering is prohibited.

---

**Developed by Trenton Taylor | plugcore.io | © 2025**

