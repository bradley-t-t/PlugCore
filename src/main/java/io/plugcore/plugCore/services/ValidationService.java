package io.plugcore.plugCore.services;

import io.plugcore.plugCore.models.ServerLinkData;
import io.plugcore.plugCore.models.ValidationResponse;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ValidationService {
    private final DatabaseService databaseService;
    private final Map<String, Long> validationCache;
    private final HttpClient httpClient;
    private volatile boolean serverLinked = false;

    public ValidationService(Plugin plugin, DatabaseService databaseService) {
        this.databaseService = databaseService;
        this.validationCache = new ConcurrentHashMap<>();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private CompletableFuture<String> getExternalIP() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.ipify.org"))
                        .timeout(Duration.ofSeconds(10))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return response.body().trim();
                } else {
                    throw new IOException("Failed to get IP: " + response.statusCode());
                }
            } catch (Exception e) {
                throw new RuntimeException("Error fetching external IP", e);
            }
        });
    }

    public CompletableFuture<ValidationResponse> linkServer(String token, String serverName, String minecraftVersion) {
        String fingerprint = UUID.randomUUID().toString();

        return getExternalIP().thenCompose(ip -> {
            return databaseService.linkServer(token, serverName, minecraftVersion, ip, fingerprint)
                    .thenApply(response -> {
                        if (response.isValid()) {
                            validationCache.put(ip, System.currentTimeMillis());
                            serverLinked = true;
                        }
                        return response;
                    });
        });
    }

    public CompletableFuture<Boolean> validateServerLink() {
        return getExternalIP().thenCompose(ip -> {
            long lastValidation = validationCache.getOrDefault(ip, 0L);
            long currentTime = System.currentTimeMillis();
            long ttl = 300 * 1000;

            if (currentTime - lastValidation < ttl) {
                serverLinked = true;
                return CompletableFuture.completedFuture(true);
            }

            return databaseService.validateServer(ip)
                    .thenApply(response -> {
                        if (response.isValid()) {
                            validationCache.put(ip, currentTime);
                            serverLinked = true;
                            return true;
                        } else {
                            validationCache.remove(ip);
                            serverLinked = false;
                            return false;
                        }
                    });
        });
    }

    public CompletableFuture<Boolean> isPluginAuthorized(String jarHash) {
        return getExternalIP().thenCompose(ip -> {
            return databaseService.checkPluginPurchase(ip, jarHash);
        });
    }

    public boolean isPluginAuthorizedSync(String jarHash) {
        try {
            String ip = getExternalIP().get();
            return databaseService.checkPluginPurchase(ip, jarHash).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate plugin synchronously", e);
        }
    }

    public ServerLinkData getCurrentLinkData() {
        return new ServerLinkData(
                "",
                null,
                "",
                serverLinked,
                List.of(),
                0L
        );
    }

    public void clearCache() {
        validationCache.clear();
    }

    public void unlinkServer() {
        clearCache();
        serverLinked = false;
    }
}
