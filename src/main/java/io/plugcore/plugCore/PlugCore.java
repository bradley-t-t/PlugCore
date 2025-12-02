package io.plugcore.plugCore;

import io.plugcore.plugCore.commands.PlugCoreCommand;
import io.plugcore.plugCore.config.DatabaseConfig;
import io.plugcore.plugCore.services.DatabaseService;
import io.plugcore.plugCore.services.PluginDependencyService;
import io.plugcore.plugCore.services.ValidationService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class PlugCore extends JavaPlugin {
    private static PlugCore instance;
    private ValidationService validationService;
    private PluginDependencyService dependencyService;

    public static PlugCore getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
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
        instance = null;
    }

    public ValidationService getValidationService() {
        return validationService;
    }

    public PluginDependencyService getDependencyService() { return dependencyService; }

    public boolean isServerLinked() {
        if (validationService == null) {
            return false;
        }
        try {
            return validationService.validateServerLinkSync();
        } catch (Exception e) {
            getLogger().warning("Error checking server link status: " + e.getMessage());
            return false;
        }
    }
}
