package io.plugcore.plugCore.api;

import io.plugcore.plugCore.PlugCore;

import java.util.concurrent.CompletableFuture;

public class PlugCoreAPI {
    private static PlugCore instance;

    public static void setInstance(PlugCore plugCore) {
        instance = plugCore;
    }

    public static boolean isServerLinked() {
        return instance != null && instance.getValidationService().getCurrentLinkData().isLinked();
    }

    public static CompletableFuture<Boolean> isPluginAuthorized(String pluginName) {
        if (instance == null) {
            return CompletableFuture.completedFuture(false);
        }
        return instance.getValidationService().isPluginAuthorized(pluginName);
    }

    public static CompletableFuture<Boolean> validateServer() {
        if (instance == null) {
            return CompletableFuture.completedFuture(false);
        }
        return instance.getValidationService().validateServerLink();
    }

    public static PlugCore getInstance() {
        return instance;
    }
}

