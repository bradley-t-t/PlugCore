# PlugCoreExamplePlugin

**Example plugin demonstrating PlugCore integration**

This example plugin shows how to integrate PlugCore validation into your premium plugins sold on plugcore.io. It
demonstrates the simple two-step integration process that protects your premium plugins.

---

## What is PlugCore?

PlugCore is the official plugin validation system for plugcore.io. It validates premium Minecraft plugins purchased from
plugcore.io, ensuring that only authorized servers can run premium plugins.

---

## Quick Start

Making your plugin work with PlugCore requires just two simple steps:

### 1. Add Dependency to `plugin.yml`

```yaml
name: MyPremiumPlugin
version: 1.0.0
main: com.example.MyPremiumPlugin
api-version: '1.21'
depend: [PlugCore]
```

### 2. Add Authorization Check

```java
package com.example;

import io.plugcore.plugCore.api.PlugCoreAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPremiumPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        try {
            if (!PlugCoreAPI.requireAuthorization(this)) {
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            getLogger().info("PlugCoreExamplePlugin enabled and authorized!");
        } catch (NoClassDefFoundError e) {
            getLogger().severe("PlugCore not found! Download from https://plugcore.io");
            getServer().getPluginManager().disablePlugin(this);
        } catch (Exception e) {
            getLogger().severe("Authorization error: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }
}
```

**That's all you need!**

---

## How It Works

### For Server Owners

1. Install PlugCore on your server
2. Link your server to your plugcore.io account using `/plugcore link <token>`
3. Purchase the plugin from plugcore.io
4. Install the plugin on your server
5. The plugin verifies your purchase and enables automatically

If you haven't purchased the plugin or your server isn't linked, the plugin will disable itself with a helpful error
message.

### For Plugin Developers

When you integrate PlugCore into your plugin:

1. Server owner installs your plugin
2. Your plugin checks authorization on startup
3. PlugCore validates the server's license
4. If authorized, plugin runs normally
5. If not authorized, plugin disables itself automatically

---

## Maven Setup

### Add PlugCore Dependency to `pom.xml`

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.bradley-t-t</groupId>
        <artifactId>PlugCore</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

---

## What Gets Validated

- Server is linked to plugcore.io
- Server owner has purchased your plugin
- Subscription is active (if applicable)
- Plugin files haven't been tampered with

---

## Protection Layers

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

## Building

```bash
mvn clean package
```

Output: `target/PlugCoreExamplePlugin-1.9-SNAPSHOT.jar`

---

## Best Practices

### DO

- Call `requireAuthorization(this)` as the first line in `onEnable()`
- Return immediately if authorization fails
- Add `depend: [PlugCore]` to plugin.yml
- Use `<scope>provided</scope>` in pom.xml

### DON'T

- Continue loading if authorization fails
- Skip the authorization check
- Remove the dependency from plugin.yml

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
        }
```

---

## FAQ

**Q: Why do I need both `depend: [PlugCore]` and `requireAuthorization()`?**

A: This provides two layers of protection. The dependency ensures PlugCore loads first, and the API call performs the
actual validation.

**Q: What if someone modifies my plugin?**

A: PlugCore detects modifications and the plugin will fail authorization.

**Q: Can I use this for free plugins?**

A: This validation system is designed for premium plugins sold on plugcore.io. Free plugins don't need authorization.

**Q: What if PlugCore is temporarily unavailable?**

A: The plugin will fail to authorize and disable itself. This ensures plugins only run when properly validated.

**Q: Do I need to upload my plugin to plugcore.io?**

A: Yes, you need to list your plugin on plugcore.io for the validation system to work.

---

## Support

- **Website:** [plugcore.io](https://plugcore.io)

---

**Developed by Trenton Taylor | plugcore.io | © 2025**
