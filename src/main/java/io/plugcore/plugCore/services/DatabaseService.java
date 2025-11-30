package io.plugcore.plugCore.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.plugcore.plugCore.models.ValidationResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseService {
    private final HttpClient httpClient;
    private final Gson gson;
    private final String baseUrl;
    private final String anonKey;

    public DatabaseService(String baseUrl, String anonKey) {
        this.baseUrl = baseUrl;
        this.anonKey = anonKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
    }

    public CompletableFuture<ValidationResponse> linkServer(String token, String serverName, String minecraftVersion, UUID serverUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("token", token);
                requestBody.addProperty("serverName", serverName);
                requestBody.addProperty("minecraftVersion", minecraftVersion);
                requestBody.addProperty("serverUuid", serverUuid.toString());

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/link-server"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + anonKey)
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                        .timeout(Duration.ofSeconds(30))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                    boolean success = jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean();
                    String message = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "Server linked";

                    return new ValidationResponse(success, message, List.of(), serverUuid.toString());
                } else {
                    JsonObject errorResponse = gson.fromJson(response.body(), JsonObject.class);
                    String errorMessage = errorResponse.has("error") ? errorResponse.get("error").getAsString() : "Failed to link server";
                    return new ValidationResponse(false, errorMessage, List.of(), "");
                }
            } catch (IOException | InterruptedException e) {
                return new ValidationResponse(false, "Error connecting to API: " + e.getMessage(), List.of(), "");
            }
        });
    }

    public CompletableFuture<ValidationResponse> validateServer(UUID serverUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("serverUuid", serverUuid.toString());

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/validate-server"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + anonKey)
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                        .timeout(Duration.ofSeconds(30))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                    boolean valid = jsonResponse.has("valid") && jsonResponse.get("valid").getAsBoolean();
                    String message = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "Validated";

                    List<String> plugins = List.of();
                    if (jsonResponse.has("purchasedPlugins")) {
                        var pluginsArray = jsonResponse.getAsJsonArray("purchasedPlugins");
                        plugins = new java.util.ArrayList<>();
                        for (var element : pluginsArray) {
                            plugins.add(element.getAsString());
                        }
                    }

                    return new ValidationResponse(valid, message, plugins, serverUuid.toString());
                } else {
                    JsonObject errorResponse = gson.fromJson(response.body(), JsonObject.class);
                    String errorMessage = errorResponse.has("error") ? errorResponse.get("error").getAsString() : "Validation failed";
                    return new ValidationResponse(false, errorMessage, List.of(), "");
                }
            } catch (IOException | InterruptedException e) {
                return new ValidationResponse(false, "Error connecting to API: " + e.getMessage(), List.of(), "");
            }
        });
    }

    public CompletableFuture<Boolean> checkPluginPurchase(UUID serverUuid, String jarHash) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("serverUuid", serverUuid.toString());
                requestBody.addProperty("jarHash", jarHash);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/check-plugin"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + anonKey)
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                        .timeout(Duration.ofSeconds(30))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                    return jsonResponse.has("purchased") && jsonResponse.get("purchased").getAsBoolean();
                }
                return false;
            } catch (IOException | InterruptedException e) {
                return false;
            }
        });
    }
}

