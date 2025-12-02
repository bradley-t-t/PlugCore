package io.plugcore.plugCore;

import io.plugcore.plugCore.commands.PlugCoreCommand;
import io.plugcore.plugCore.config.DatabaseConfig;
import io.plugcore.plugCore.services.DatabaseService;
import io.plugcore.plugCore.services.PluginDependencyService;
import io.plugcore.plugCore.services.ValidationService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class PlugCore extends JavaPlugin {
    public static ValidationService validationService;
    public static PluginDependencyService dependencyService;

    @Override
    public void onLoad() {
        DatabaseService databaseService = new DatabaseService(DatabaseConfig.getBaseUrl(), DatabaseConfig.getAnonKey());
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

    public static ValidationService getValidationService() {
        return validationService;
    }

    public static PluginDependencyService getDependencyService() {
        return dependencyService;
    }
}
