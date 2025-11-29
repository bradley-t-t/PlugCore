package io.plugcore.plugCore.models;

import java.util.List;

public class ValidationResponse {
    private final boolean valid;
    private final String message;
    private final List<String> purchasedPlugins;
    private final String serverId;

    public ValidationResponse(boolean valid, String message, List<String> purchasedPlugins, String serverId) {
        this.valid = valid;
        this.message = message;
        this.purchasedPlugins = purchasedPlugins;
        this.serverId = serverId;
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getPurchasedPlugins() {
        return purchasedPlugins;
    }

    public String getServerId() {
        return serverId;
    }
}

