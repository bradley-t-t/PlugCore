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
                String jarHash = calculatePluginHash(plugin);

                if (jarHash == null) {
                    corePlugin.getLogger().severe("Failed to calculate hash for plugin: " + plugin.getName());
                    continue;
                }

                if (dependentPlugins.containsKey(jarHash)) {
                    continue;
                }

                dependentPlugins.put(jarHash, false);
                corePlugin.getLogger().info("Found dependent plugin: " + plugin.getName() + " (Hash: " + jarHash.substring(0, 8) + "...)");
            }
        }
    }

    public void scanAndValidateStartupPlugins() {
        Plugin[] plugins = Bukkit.getPluginManager().getPlugins();

        for (Plugin plugin : plugins) {
            if (plugin.equals(corePlugin)) {
                continue;
            }

            var description = plugin.getDescription();
            String loadPhase = description.getLoad() != null ? description.getLoad().toString() : "POSTWORLD";

            if (!"STARTUP".equals(loadPhase)) {
                continue;
            }

            var meta = plugin.getPluginMeta();
            List<String> dependencies = meta.getPluginDependencies();
            List<String> softDependencies = meta.getPluginSoftDependencies();

            boolean dependsOnCore = dependencies.contains("PlugCore");
            boolean softDependsOnCore = softDependencies.contains("PlugCore");

            if (dependsOnCore || softDependsOnCore) {
                String jarHash = calculatePluginHash(plugin);

                if (jarHash == null) {
                    corePlugin.getLogger().severe("Failed to calculate hash for STARTUP plugin: " + plugin.getName());
                    continue;
                }

                corePlugin.getLogger().info("Validating STARTUP plugin: " + plugin.getName() + " (Hash: " + jarHash.substring(0, 8) + "...)");

                try {
                    boolean authorized = validationService.isPluginAuthorizedSync(jarHash);

                    if (!authorized) {
                        corePlugin.getLogger().severe("STARTUP plugin '" + plugin.getName() + "' is NOT authorized!");
                        corePlugin.getLogger().severe("This plugin will be disabled. Purchase at plugcore.io");
                        dependentPlugins.put(jarHash, false);
                    } else {
                        corePlugin.getLogger().info("STARTUP plugin '" + plugin.getName() + "' is authorized! ✓");
                        dependentPlugins.put(jarHash, true);
                    }

                } catch (Exception e) {
                    corePlugin.getLogger().severe("Failed to validate STARTUP plugin '" + plugin.getName() + "': " + e.getMessage());
                    dependentPlugins.put(jarHash, false);
                }
            }
        }
    }

    private String calculatePluginHash(Plugin plugin) {
        try {
            java.io.File pluginFile = getPluginJarFile(plugin);
            if (pluginFile == null || !pluginFile.exists()) {
                return null;
            }

            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            java.io.FileInputStream fis = new java.io.FileInputStream(pluginFile);
            byte[] byteArray = new byte[1024];
            int bytesCount;

            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
            fis.close();

            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            corePlugin.getLogger().severe("Error calculating plugin hash: " + e.getMessage());
            return null;
        }
    }

    private java.io.File getPluginJarFile(Plugin plugin) {
        try {
            java.net.URL url = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
            return new java.io.File(url.toURI());
        } catch (Exception e) {
            return null;
        }
    }

    public void validateDependentPlugins() {
        if (dependentPlugins.isEmpty()) {
            corePlugin.getLogger().info("No dependent plugins found to validate.");
            return;
        }

        corePlugin.getLogger().info("Validating " + dependentPlugins.size() + " dependent plugin(s)...");

        for (String jarHash : dependentPlugins.keySet()) {
            corePlugin.getLogger().info("Checking authorization for hash: " + jarHash.substring(0, 16) + "...");

            validationService.isPluginAuthorized(jarHash).thenAccept(authorized -> {
                dependentPlugins.put(jarHash, authorized);

                if (!authorized) {
                    Plugin pluginToDisable = findPluginByHash(jarHash);
                    String pluginName = pluginToDisable != null ? pluginToDisable.getName() : "Unknown";

                    corePlugin.getLogger().warning("Plugin '" + pluginName + "' is NOT authorized!");
                    corePlugin.getLogger().warning("Make sure you have purchased this plugin on plugcore.io");

                    if (pluginToDisable != null && pluginToDisable.isEnabled()) {
                        Bukkit.getScheduler().runTask(corePlugin, () -> {
                            Bukkit.getPluginManager().disablePlugin(pluginToDisable);
                            corePlugin.getLogger().warning("Disabled " + pluginToDisable.getName() + " - Server not authorized");
                        });
                    }
                } else {
                    Plugin authorizedPlugin = findPluginByHash(jarHash);
                    String pluginName = authorizedPlugin != null ? authorizedPlugin.getName() : "Unknown";
                    corePlugin.getLogger().info("Plugin '" + pluginName + "' is authorized! ✓");
                }
            }).exceptionally(throwable -> {
                corePlugin.getLogger().severe("Error validating hash: " + throwable.getMessage());
                return null;
            });
        }
    }

    private Plugin findPluginByHash(String hash) {
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            String pluginHash = calculatePluginHash(plugin);
            if (hash.equals(pluginHash)) {
                return plugin;
            }
        }
        return null;
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

