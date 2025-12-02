package io.plugcore.plugCore.api;

import io.plugcore.plugCore.PlugCore;

public class PlugCoreAPI {
    private static PlugCoreAPI instance;
    private final PlugCore plugCore;

    private PlugCoreAPI(PlugCore plugCore) {
        this.plugCore = plugCore;
    }

    public static void initialize(PlugCore plugCore) {
        if (instance == null) {
            instance = new PlugCoreAPI(plugCore);
        }
    }

    public static PlugCoreAPI getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PlugCoreAPI has not been initialized yet!");
        }
        return instance;
    }

    public boolean isServerLinked() {
        if (plugCore.getValidationService() == null) {
            return false;
        }
        try {
            return plugCore.getValidationService().validateServerLinkSync();
        } catch (Exception e) {
            plugCore.getLogger().warning("Error checking server link status: " + e.getMessage());
            return false;
        }
    }
}

