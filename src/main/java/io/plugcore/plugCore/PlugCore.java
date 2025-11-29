package io.plugcore.plugCore;

import io.plugcore.plugCore.api.PlugCoreAPI;
import io.plugcore.plugCore.commands.PlugCoreCommand;
import io.plugcore.plugCore.config.DatabaseConfig;
import io.plugcore.plugCore.services.PluginDependencyService;
import io.plugcore.plugCore.services.DatabaseService;
import io.plugcore.plugCore.services.ValidationService;
import io.plugcore.plugCore.utils.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlugCore extends JavaPlugin {
    private ConfigUtil configUtil;
    private DatabaseService databaseService;
    private ValidationService validationService;
    private PluginDependencyService dependencyService;

    @Override
    public void onLoad() {
        saveDefaultConfig();
        configUtil = new ConfigUtil(this);
        databaseService = new DatabaseService(DatabaseConfig.getBaseUrl(), DatabaseConfig.getAnonKey());
        validationService = new ValidationService(this, databaseService, configUtil);
        dependencyService = new PluginDependencyService(this, validationService);

        if (!configUtil.isServerLinked()) {
            getLogger().warning("Server not linked! Plugins requiring PlugCore may fail to load.");
            getLogger().warning("Link your server: /plugcore link <token>");
            return;
        }

        getLogger().info("Pre-validating STARTUP plugins...");
        dependencyService.scanAndValidateStartupPlugins();
    }

    @Override
    public void onEnable() {
        PlugCoreAPI.setInstance(this);

        PlugCoreCommand command = new PlugCoreCommand(validationService, dependencyService);
        if (getCommand("plugcore") != null) {
            getCommand("plugcore").setExecutor(command);
            getCommand("plugcore").setTabCompleter(command);
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            dependencyService.scanPlugins();

            if (configUtil.isServerLinked()) {
                getLogger().info("Server is linked. Validating with PlugCore...");
                validationService.validateServerLink().thenAccept(valid -> {
                    if (valid) {
                        getLogger().info("Server validation successful!");
                        dependencyService.validateDependentPlugins();
                    } else {
                        getLogger().warning("Server validation failed. Please re-link your server.");
                    }
                });
            } else {
                getLogger().warning("Server is not linked to PlugCore.");
                getLogger().warning("Use /plugcore link <code> to link your server.");
            }
        }, 20L);

        startValidationTask();

        getLogger().info("PlugCore has been enabled!");
    }

    @Override
    public void onDisable() {
        if (validationService != null) {
            validationService.clearCache();
        }
        getLogger().info("PlugCore has been disabled!");
    }

    private void startValidationTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (configUtil.isServerLinked()) {
                validationService.validateServerLink().thenAccept(valid -> {
                    if (valid) {
                        dependencyService.validateDependentPlugins();
                    }
                });
            }
        }, 6000L, 6000L);
    }

    public ValidationService getValidationService() {
        return validationService;
    }

    public PluginDependencyService getDependencyService() {
        return dependencyService;
    }

    public ConfigUtil getConfigUtil() {
        return configUtil;
    }
}
