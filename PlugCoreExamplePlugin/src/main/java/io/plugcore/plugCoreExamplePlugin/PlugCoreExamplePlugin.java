package io.plugcore.plugCoreExamplePlugin;

import io.plugcore.plugCore.PlugCore;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlugCoreExamplePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        try {
            PlugCore.requireAuthorization(this);
            getLogger().info("PlugCoreExamplePlugin enabled - authorization check scheduled.");
        } catch (NoClassDefFoundError e) {
            getLogger().severe("PlugCore not found! Download from https://plugcore.io");
            getServer().getPluginManager().disablePlugin(this);
        } catch (Exception e) {
            getLogger().severe("Authorization error: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("PlugCoreExamplePlugin disabled.");
    }
}
