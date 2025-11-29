package io.plugcore.plugCore.services;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.stream.Collectors;

public class PluginDependencyService {
    private final Plugin corePlugin;
    private final ValidationService validationService;
    private final Map<String, Boolean> dependentPlugins;

    public PluginDependencyService(Plugin corePlugin, ValidationService validationService) {
        this.corePlugin = corePlugin;
        this.validationService = validationService;
        this.dependentPlugins = new HashMap<>();
    }

    public void scanPlugins() {
        dependentPlugins.clear();
        Plugin[] plugins = Bukkit.getPluginManager().getPlugins();

        for (Plugin plugin : plugins) {
            if (plugin.equals(corePlugin)) {
                continue;
            }

            var meta = plugin.getPluginMeta();
            List<String> dependencies = meta.getPluginDependencies();
            List<String> softDependencies = meta.getPluginSoftDependencies();

            boolean dependsOnCore = dependencies.contains("PlugCore");
            boolean softDependsOnCore = softDependencies.contains("PlugCore");

            if (dependsOnCore || softDependsOnCore) {
                dependentPlugins.put(plugin.getName(), false);
            }
        }
    }

    public void validateDependentPlugins() {
        if (dependentPlugins.isEmpty()) {
            return;
        }

        for (String pluginName : dependentPlugins.keySet()) {
            validationService.isPluginAuthorized(pluginName).thenAccept(authorized -> {
                dependentPlugins.put(pluginName, authorized);

                if (!authorized) {
                    Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
                    if (plugin != null && plugin.isEnabled()) {
                        Bukkit.getScheduler().runTask(corePlugin, () -> {
                            Bukkit.getPluginManager().disablePlugin(plugin);
                            corePlugin.getLogger().warning(
                                    "Disabled " + pluginName + " - Server not authorized for this plugin"
                            );
                        });
                    }
                }
            });
        }
    }

    public boolean isPluginAuthorized(String pluginName) {
        return dependentPlugins.getOrDefault(pluginName, false);
    }

    public List<String> getUnauthorizedPlugins() {
        return dependentPlugins.entrySet().stream()
                .filter(entry -> !entry.getValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<String> getAuthorizedPlugins() {
        return dependentPlugins.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public Map<String, Boolean> getAllDependentPlugins() {
        return new HashMap<>(dependentPlugins);
    }
}

