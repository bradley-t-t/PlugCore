package io.plugcore.plugCore.services;

import io.plugcore.plugCore.models.ServerLinkData;
import io.plugcore.plugCore.models.ValidationResponse;
import io.plugcore.plugCore.utils.ConfigUtil;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ValidationService {
    private final DatabaseService databaseService;
    private final ConfigUtil configUtil;
    private final Map<String, Long> validationCache;

    public ValidationService(Plugin plugin, DatabaseService databaseService, ConfigUtil configUtil) {
        this.databaseService = databaseService;
        this.configUtil = configUtil;
        this.validationCache = new ConcurrentHashMap<>();
    }

    public CompletableFuture<ValidationResponse> linkServer(String token, String serverName, String minecraftVersion) {
        UUID serverUuid = configUtil.getServerUUID();
        if (serverUuid == null) {
            serverUuid = UUID.randomUUID();
            configUtil.setServerUUID(serverUuid);
        }

        final UUID finalServerUuid = serverUuid;

        return databaseService.linkServer(token, serverName, minecraftVersion, finalServerUuid)
                .thenApply(response -> {
                    if (response.isValid()) {
                        configUtil.setServerLinked(true);
                        configUtil.setServerId(finalServerUuid.toString());
                        validationCache.put(finalServerUuid.toString(), System.currentTimeMillis());
                    }
                    return response;
                });
    }

    public CompletableFuture<Boolean> validateServerLink() {
        if (!configUtil.isServerLinked()) {
            return CompletableFuture.completedFuture(false);
        }

        UUID serverUuid = configUtil.getServerUUID();
        if (serverUuid == null) {
            return CompletableFuture.completedFuture(false);
        }

        String serverId = serverUuid.toString();
        long lastValidation = validationCache.getOrDefault(serverId, 0L);
        long currentTime = System.currentTimeMillis();
        long ttl = configUtil.getValidationTTL() * 1000;

        if (currentTime - lastValidation < ttl) {
            return CompletableFuture.completedFuture(true);
        }

        return databaseService.validateServer(serverUuid)
                .thenApply(response -> {
                    if (response.isValid()) {
                        validationCache.put(serverId, currentTime);
                        return true;
                    } else {
                        configUtil.setServerLinked(false);
                        validationCache.remove(serverId);
                        return false;
                    }
                });
    }

    public CompletableFuture<Boolean> isPluginAuthorized(String pluginName) {
        if (!configUtil.isServerLinked()) {
            return CompletableFuture.completedFuture(false);
        }

        UUID serverUuid = configUtil.getServerUUID();
        if (serverUuid == null) {
            return CompletableFuture.completedFuture(false);
        }

        return databaseService.checkPluginPurchase(serverUuid, pluginName);
    }

    public ServerLinkData getCurrentLinkData() {
        return new ServerLinkData(
                configUtil.getServerId(),
                configUtil.getOwnerUUID(),
                configUtil.getVerificationToken(),
                configUtil.isServerLinked(),
                List.of(),
                validationCache.getOrDefault(configUtil.getServerId(), 0L)
        );
    }

    public void clearCache() {
        validationCache.clear();
    }

    public void unlinkServer() {
        configUtil.setServerLinked(false);
        configUtil.setServerId("");
        configUtil.setOwnerUUID(UUID.randomUUID());
        configUtil.setVerificationToken("");
        clearCache();
    }
}

