# Implementing PlugCore Dependency in Your Plugin

This guide shows you how to require PlugCore as a dependency in your Minecraft plugin, enabling automatic license verification through plugcore.io.

---

## Overview

PlugCore is a licensing and authentication system that validates plugin purchases through plugcore.io. When your plugin depends on PlugCore, it will:

- ✅ Automatically verify the server owner has purchased your plugin
- ✅ Disable itself if not authorized
- ✅ Handle subscription validation (active status and expiration dates)
- ✅ Provide real-time purchase verification

---

## Quick Start

### 1. Add PlugCore as a Dependency

In your `plugin.yml`, add PlugCore as a required dependency:

```yaml
name: YourPluginName
version: 1.0.0
main: com.yourname.yourplugin.YourPlugin
api-version: '1.21'
depend: [PlugCore]
```

**That's it!** PlugCore will automatically:
- Detect your plugin on server startup
- Check if the server owner purchased it from plugcore.io
- Disable your plugin if unauthorized

---

## Implementation Methods

### Method 1: Hard Dependency (Recommended)

Use this when your plugin **requires** PlugCore to function.

**plugin.yml:**
```yaml
name: YourPluginName
version: 1.0.0
main: com.yourname.yourplugin.YourPlugin
depend: [PlugCore]
```

**YourPlugin.java:**
```java
package com.yourname.yourplugin;

import org.bukkit.plugin.java.JavaPlugin;

public class YourPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        getLogger().info("YourPluginName enabled!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("YourPluginName disabled.");
    }
}
```

**Behavior:**
- If PlugCore is missing, your plugin won't load
- If server isn't linked, PlugCore disables your plugin
- If plugin isn't purchased, PlugCore disables your plugin

---

### Method 2: Soft Dependency with Manual Checks

Use this when you want to provide limited functionality without PlugCore.

**plugin.yml:**
```yaml
name: YourPluginName
version: 1.0.0
main: com.yourname.yourplugin.YourPlugin
softdepend: [PlugCore]
```

**YourPlugin.java:**
```java
package com.yourname.yourplugin;

import io.plugcore.plugCore.api.PlugCoreAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class YourPlugin extends JavaPlugin {
    private boolean premiumFeatures = false;
    
    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("PlugCore") != null) {
            checkLicense();
        } else {
            getLogger().warning("PlugCore not found. Running in limited mode.");
            loadBasicFeatures();
        }
    }
    
    private void checkLicense() {
        if (!PlugCoreAPI.isServerLinked()) {
            getLogger().warning("Server not linked to PlugCore!");
            loadBasicFeatures();
            return;
        }
        
        PlugCoreAPI.isPluginAuthorized("YourPluginName").thenAccept(authorized -> {
            if (authorized) {
                getLogger().info("Premium features unlocked!");
                premiumFeatures = true;
                loadPremiumFeatures();
            } else {
                getLogger().warning("Plugin not authorized. Running in limited mode.");
                loadBasicFeatures();
            }
        });
    }
    
    private void loadBasicFeatures() {
        getLogger().info("Loading basic features...");
    }
    
    private void loadPremiumFeatures() {
        getLogger().info("Loading premium features...");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("YourPluginName disabled.");
    }
}
```

**Behavior:**
- Works without PlugCore (limited features)
- Enables premium features if authorized
- Provides fallback functionality

---

### Method 3: Advanced with Event Listeners

Listen to PlugCore events for advanced integration.

**plugin.yml:**
```yaml
name: YourPluginName
version: 1.0.0
main: com.yourname.yourplugin.YourPlugin
depend: [PlugCore]
```

**YourPlugin.java:**
```java
package com.yourname.yourplugin;

import io.plugcore.plugCore.api.PlugCoreAPI;
import io.plugcore.plugCore.events.ServerLinkedEvent;
import io.plugcore.plugCore.events.PluginValidatedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class YourPlugin extends JavaPlugin implements Listener {
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        
        if (PlugCoreAPI.isServerLinked()) {
            checkAuthorization();
        } else {
            getLogger().warning("Server not linked to PlugCore!");
            disablePlugin();
        }
    }
    
    private void checkAuthorization() {
        PlugCoreAPI.isPluginAuthorized("YourPluginName").thenAccept(authorized -> {
            if (!authorized) {
                getLogger().severe("Not authorized! Disabling plugin.");
                disablePlugin();
            } else {
                getLogger().info("Authorization verified!");
            }
        });
    }
    
    @EventHandler
    public void onServerLinked(ServerLinkedEvent event) {
        getLogger().info("Server linked! Owner: " + event.getOwnerUUID());
        checkAuthorization();
    }
    
    @EventHandler
    public void onPluginValidated(PluginValidatedEvent event) {
        if (event.getPluginName().equals("YourPluginName")) {
            if (event.isAuthorized()) {
                getLogger().info("Plugin validation successful!");
            } else {
                getLogger().warning("Plugin validation failed!");
                disablePlugin();
            }
        }
    }
    
    private void disablePlugin() {
        getServer().getScheduler().runTask(this, () -> {
            getServer().getPluginManager().disablePlugin(this);
        });
    }
    
    @Override
    public void onDisable() {
        getLogger().info("YourPluginName disabled.");
    }
}
```

---

## PlugCore API Reference

### Static Methods

```java
// Check if server is linked to a plugcore.io account
boolean isLinked = PlugCoreAPI.isServerLinked();

// Check if specific plugin is authorized (async)
CompletableFuture<Boolean> future = PlugCoreAPI.isPluginAuthorized("YourPluginName");
future.thenAccept(authorized -> {
    if (authorized) {
        // Plugin is purchased and authorized
    }
});

// Force server validation (async)
CompletableFuture<Boolean> validation = PlugCoreAPI.validateServer();

// Get PlugCore instance
PlugCore instance = PlugCoreAPI.getInstance();
```

### Events

```java
@EventHandler
public void onServerLinked(ServerLinkedEvent event) {
    String serverId = event.getServerId();
    String ownerUUID = event.getOwnerUUID();
}

@EventHandler
public void onPluginValidated(PluginValidatedEvent event) {
    String pluginName = event.getPluginName();
    boolean authorized = event.isAuthorized();
}
```

---

## How It Works

### Server Owner's Perspective

1. Install your plugin on their server
2. Install PlugCore plugin
3. Visit plugcore.io/account → Your Servers tab
4. Generate a linking token
5. Run `/plugcore link <token>` in-game
6. PlugCore validates their purchase of your plugin
7. Your plugin remains enabled if purchased, disabled if not

### Validation Flow

```
Server Startup
    ↓
PlugCore scans for plugins with depend: [PlugCore]
    ↓
For each dependent plugin:
    ↓
Check: Is server linked? (5-minute cache)
    ↓
Query: plugcore.io API → check-plugin endpoint
    ↓
Database: plugin_purchases table
    ↓
Validate: One-time purchase OR active subscription
    ↓
Result: Keep enabled OR disable plugin
```

### Real-Time Checks

- **Server Validation:** Every 5 minutes (configurable)
- **Plugin Authorization:** Real-time on each check (no caching)
- **Subscription Validation:** Checks status and expiration date

---

## Publishing Your Plugin on plugcore.io

1. **Create Account** at plugcore.io
2. **Upload Plugin** via Account → Your Plugins → Upload New
3. **Set Price:** One-time or subscription
4. **Add to Marketplace:** Public or private listing
5. **Set Plugin Name:** Must match your plugin's name in code

**Important:** The plugin name in your `isPluginAuthorized()` call must **exactly match** the name in the plugcore.io database.

---

## Testing Your Integration

### Test Checklist

- [ ] Plugin loads with PlugCore present
- [ ] Plugin fails to load without PlugCore (if hard dependency)
- [ ] Authorization check works on server startup
- [ ] Plugin disables when not authorized
- [ ] Subscription expiration is respected
- [ ] Manual validation with `/plugcore validate` works

### Test Scenarios

**Scenario 1: Authorized Server**
```bash
# Link server
/plugcore link <token-from-website>

# Check status
/plugcore status
# Should show: Linked: Yes

# Check plugins
/plugcore plugins
# Should show your plugin as "Authorized"
```

**Scenario 2: Unauthorized Server**
```bash
# Server not linked or plugin not purchased
# Expected: Your plugin will be disabled
# Console: "Disabled YourPluginName - Server not authorized"
```

**Scenario 3: Subscription Expired**
```bash
# Subscription ended
# Expected: Plugin disables after next validation (within 5 minutes)
```

---

## Error Messages

### Common Issues

**"Server not linked to PlugCore"**
- Server owner hasn't run `/plugcore link <token>`
- Solution: Direct them to plugcore.io/account

**"Plugin not authorized"**
- Server owner hasn't purchased your plugin
- Solution: Purchase at plugcore.io/marketplace

**"Subscription expired or cancelled"**
- Subscription ended or was cancelled
- Solution: Renew subscription at plugcore.io/account

**"PlugCore not found"**
- PlugCore plugin not installed
- Solution: Download from plugcore.io

---

## Best Practices

### ✅ Do's

- **Use meaningful error messages** that guide users to plugcore.io
- **Check authorization on plugin enable** to fail fast
- **Use async checks** to avoid blocking the main thread
- **Provide clear instructions** in your plugin description
- **Match plugin names exactly** between code and plugcore.io listing

### ❌ Don'ts

- **Don't cache authorization results** - PlugCore handles this
- **Don't bypass PlugCore checks** - defeats the purpose
- **Don't hardcode authorization** - always use the API
- **Don't spam the API** - PlugCore rate limits for you

---

## Support

**For Plugin Developers:**
- Documentation: plugcore.io/developers
- Support: support@plugcore.io
- Discord: discord.gg/plugcore

**For Server Owners:**
- Help Center: plugcore.io/support
- Commands: `/plugcore help`
- Account: plugcore.io/account

---

## Example Plugin Structure

```
YourPlugin/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/yourname/yourplugin/
│       │       └── YourPlugin.java
│       └── resources/
│           └── plugin.yml
├── pom.xml
└── README.md
```

**pom.xml dependencies:**
```xml
<dependencies>
    <dependency>
        <groupId>io.papermc.paper</groupId>
        <artifactId>paper-api</artifactId>
        <version>1.21-R0.1-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
    <!-- PlugCore will be provided at runtime -->
</dependencies>
```

---

## Minimum Requirements

- **Minecraft Version:** 1.21+
- **Server Type:** Paper/Spigot/Purpur
- **Java Version:** 21+
- **PlugCore Version:** 1.0+

---

## License Verification Timeline

```
t=0s    : Plugin loads
t=0s    : PlugCore detects dependency
t=0s    : Authorization check initiated
t=1s    : API response received
t=1s    : Plugin enabled/disabled based on result
t=5min  : Automatic revalidation
t=10min : Automatic revalidation
...continues every 5 minutes
```

---

## FAQ

**Q: What happens if plugcore.io is down?**
A: Last validation result is cached for 5 minutes. Plugin continues running during cache period.

**Q: Can I test without purchasing?**
A: Yes, link your test server and use the same account that uploaded the plugin.

**Q: Does this work offline?**
A: No, PlugCore requires internet connection for validation.

**Q: How much does PlugCore cost?**
A: PlugCore is free for plugin developers. You set your own plugin prices.

**Q: Can I use PlugCore for multiple plugins?**
A: Yes! Each plugin is validated independently.

---

## Complete Example Project

Visit: https://github.com/plugcore/example-plugin

This repository contains a complete, working example plugin with PlugCore integration.

---

**Last Updated:** November 29, 2024  
**PlugCore Version:** 1.0  
**Author:** PlugCore Team  
**Website:** plugcore.io

