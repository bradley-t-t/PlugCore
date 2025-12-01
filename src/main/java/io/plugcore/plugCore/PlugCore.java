package io.plugcore.plugCore;

import io.plugcore.plugCore.commands.PlugCoreCommand;
import io.plugcore.plugCore.config.DatabaseConfig;
import io.plugcore.plugCore.models.ServerLinkData;
import io.plugcore.plugCore.services.DatabaseService;
import io.plugcore.plugCore.services.PluginDependencyService;
import io.plugcore.plugCore.services.ValidationService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

public final class PlugCore extends JavaPlugin {
    private static volatile PlugCore instance;
    private DatabaseService databaseService;
    private ValidationService validationService;
    private PluginDependencyService dependencyService;

    @Override
    public void onLoad() {
        instance = this;
        databaseService = new DatabaseService(DatabaseConfig.getBaseUrl(), DatabaseConfig.getAnonKey());
        validationService = new ValidationService(this, databaseService);
        dependencyService = new PluginDependencyService(this, validationService);
        try {
            boolean valid = validationService.validateServerLinkSync();
            if (valid) {
                getLogger().info("Server linked successfully during load.");
            } else {
                getLogger().warning("Server not linked during load.");
            }
        } catch (Exception e) {
            getLogger().severe("Failed to validate server during load: " + e.getMessage());
        }
        dependencyService.scanAndValidateStartupPlugins();
    }

    @Override
    public void onEnable() {
        onLoad();
        PlugCoreCommand command = new PlugCoreCommand(validationService, dependencyService);
        if (getCommand("plugcore") != null) {
            getCommand("plugcore").setExecutor(command);
            getCommand("plugcore").setTabCompleter(command);
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            dependencyService.scanPlugins();

            validationService.validateServerLink().thenAccept(valid -> {
                if (valid) {
                    getLogger().info("Server validation successful!");
                    dependencyService.validateDependentPlugins();
                } else {
                    getLogger().warning("Server validation failed. Please link your server again.");
                }
            }).exceptionally(throwable -> {
                getLogger().severe("Validation error: " + throwable.getMessage());
                return null;
            });
        }, 1L);
    }

    @Override
    public void onDisable() {
        getLogger().info("PlugCore has been disabled!");
    }

    public ValidationService getValidationService() {
        return validationService;
    }

    public PluginDependencyService getDependencyService() {
        return dependencyService;
    }

    public static boolean isServerLinked() {
        System.out.println("[PlugCore] isServerLinked() called");
        System.out.println("[PlugCore] instance is null: " + (instance == null));
        if (instance == null) {
            System.out.println("[PlugCore] Returning false - instance is null");
            return false;
        }
        System.out.println("[PlugCore] instance exists: " + instance);
        System.out.println("[PlugCore] ValidationService is null: " + (instance.getValidationService() == null));
        if (instance.getValidationService() == null) {
            System.out.println("[PlugCore] Returning false - ValidationService is null");
            return false;
        }
        ServerLinkData linkData = instance.getValidationService().getCurrentLinkData();
        System.out.println("[PlugCore] LinkData is null: " + (linkData == null));
        if (linkData == null) {
            System.out.println("[PlugCore] Returning false - LinkData is null");
            return false;
        }
        boolean linked = linkData.isLinked();
        System.out.println("[PlugCore] linkData.isLinked(): " + linked);
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
        }, 200L);
        return true;
    }

    public static PlugCore getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PlugCore is not available. Make sure PlugCore is loaded and enabled before using the API.");
        }
        return instance;
    }
}
