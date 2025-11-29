package io.plugcore.plugCore.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class ConfigUtil {
    private final Plugin plugin;
    private FileConfiguration config;

    public ConfigUtil(Plugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }


    public boolean isServerLinked() {
        return config.getBoolean("server.linked", false);
    }

    public void setServerLinked(boolean linked) {
        config.set("server.linked", linked);
        save();
    }

    public String getServerId() {
        return config.getString("server.server-id", "");
    }

    public void setServerId(String serverId) {
        config.set("server.server-id", serverId);
        save();
    }

    public UUID getOwnerUUID() {
        String uuid = config.getString("server.owner-uuid", "");
        return uuid.isEmpty() ? null : UUID.fromString(uuid);
    }

    public void setOwnerUUID(UUID uuid) {
        config.set("server.owner-uuid", uuid.toString());
        save();
    }

    public UUID getServerUUID() {
        String uuid = config.getString("server.server-uuid", "");
        return uuid.isEmpty() ? null : UUID.fromString(uuid);
    }

    public void setServerUUID(UUID uuid) {
        config.set("server.server-uuid", uuid.toString());
        save();
    }

    public String getVerificationToken() {
        return config.getString("server.verification-token", "");
    }

    public void setVerificationToken(String token) {
        config.set("server.verification-token", token);
        save();
    }

    public long getValidationTTL() {
        return config.getLong("cache.validation-ttl", 300);
    }

    private void save() {
        plugin.saveConfig();
    }
}

