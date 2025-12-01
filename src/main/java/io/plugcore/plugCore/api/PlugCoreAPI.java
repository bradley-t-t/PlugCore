package io.plugcore.plugCore.api;

import io.plugcore.plugCore.PlugCore;
import io.plugcore.plugCore.models.ServerLinkData;

import java.util.concurrent.CompletableFuture;

public class PlugCoreAPI {
    private static volatile PlugCore instance;

    public static boolean isServerLinked() {
        System.out.println("[PlugCoreAPI] isServerLinked() called");
        System.out.println("[PlugCoreAPI] instance is null: " + (instance == null));
        if (instance == null) {
            System.out.println("[PlugCoreAPI] Returning false - instance is null");
            return false;
        }
        System.out.println("[PlugCoreAPI] instance exists: " + instance);
        System.out.println("[PlugCoreAPI] ValidationService is null: " + (instance.getValidationService() == null));
        if (instance.getValidationService() == null) {
            System.out.println("[PlugCoreAPI] Returning false - ValidationService is null");
            return false;
        }
        ServerLinkData linkData = instance.getValidationService().getCurrentLinkData();
        System.out.println("[PlugCoreAPI] LinkData is null: " + (linkData == null));
        if (linkData == null) {
            System.out.println("[PlugCoreAPI] Returning false - LinkData is null");
            return false;
        }
        boolean linked = linkData.isLinked();
        System.out.println("[PlugCoreAPI] linkData.isLinked(): " + linked);
        return linked;
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
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getLogger().info("Starting authorization check for plugin: " + plugin.getName());
            boolean serverLinked = isServerLinked();
            plugin.getLogger().info("isServerLinked() result: " + serverLinked);
            if (!serverLinked) {
                plugin.getLogger().severe("Server not linked to PlugCore!");
                plugin.getLogger().severe("This plugin cannot run on unlinked servers.");
                plugin.getLogger().severe("Link your server: /plugcore link <token>");
                plugin.getLogger().severe("Get your token from your account page!");
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    org.bukkit.Bukkit.getPluginManager().disablePlugin(plugin);
                });
                return;
            }

            String jarHash = instance.getDependencyService().calculatePluginJarHash(plugin);
            plugin.getLogger().info("Calculated jarHash: " + jarHash);
            if (jarHash == null) {
                plugin.getLogger().severe("Failed to calculate plugin hash!");
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    org.bukkit.Bukkit.getPluginManager().disablePlugin(plugin);
                });
                return;
            }

            instance.getValidationService().isPluginAuthorized(jarHash).thenAccept(authorized -> {
                plugin.getLogger().info("Authorization result for hash " + jarHash + ": " + authorized);
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
        }, 200L); // 10-second delay after startup
        return true;
    }

    public static PlugCore getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PlugCore API is not available. Make sure PlugCore is loaded and enabled before using the API.");
        }
        return instance;
    }

    public static void setInstance(PlugCore plugCore) {
        instance = plugCore;
    }
}
