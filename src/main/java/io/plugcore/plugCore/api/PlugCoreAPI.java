package io.plugcore.plugCore.api;

import io.plugcore.plugCore.PlugCore;

import java.util.concurrent.CompletableFuture;

public class PlugCoreAPI {
    private static PlugCore instance;

    public static void setInstance(PlugCore plugCore) {
        instance = plugCore;
    }

    public static boolean isServerLinked() {
        return instance != null && instance.getValidationService().getCurrentLinkData().isLinked();
    }

    public static CompletableFuture<Boolean> isPluginAuthorizedByHash(String jarHash) {
        if (instance == null) {
            return CompletableFuture.completedFuture(false);
        }
        return instance.getValidationService().isPluginAuthorized(jarHash);
    }

    public static CompletableFuture<Boolean> validateServer() {
        if (instance == null) {
            return CompletableFuture.completedFuture(false);
        }
        return instance.getValidationService().validateServerLink();
    }

    public static boolean requireAuthorization(org.bukkit.plugin.Plugin plugin) {
        if (instance == null) {
            plugin.getLogger().severe("PlugCore not found! This plugin requires PlugCore to function.");
            plugin.getLogger().severe("Download PlugCore from plugcore.io");
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                org.bukkit.Bukkit.getPluginManager().disablePlugin(plugin);
            });
            return false;
        }

        if (!isServerLinked()) {
            plugin.getLogger().severe("Server not linked to PlugCore!");
            plugin.getLogger().severe("This plugin cannot run on unlinked servers.");
            plugin.getLogger().severe("Link your server: /plugcore link <token>");
            plugin.getLogger().severe("Get your token from your account page!");
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                org.bukkit.Bukkit.getPluginManager().disablePlugin(plugin);
            });
            return false;
        }

        String jarHash = instance.getDependencyService().calculatePluginJarHash(plugin);
        if (jarHash == null) {
            plugin.getLogger().severe("Failed to calculate plugin hash!");
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                org.bukkit.Bukkit.getPluginManager().disablePlugin(plugin);
            });
            return false;
        }

        instance.getValidationService().isPluginAuthorized(jarHash).thenAccept(authorized -> {
            if (!authorized) {
                plugin.getLogger().severe("This plugin is NOT authorized!");
                plugin.getLogger().severe("You have not purchased this plugin.");
                plugin.getLogger().severe("Purchase at plugcore.io");
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    org.bukkit.Bukkit.getPluginManager().disablePlugin(plugin);
                });
            } else {
                plugin.getLogger().info("Plugin authorized! ✓");
            }
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Failed to validate authorization: " + throwable.getMessage());
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                org.bukkit.Bukkit.getPluginManager().disablePlugin(plugin);
            });
            return null;
        });

        return true;
    }

    public static PlugCore getInstance() {
        return instance;
    }
}
