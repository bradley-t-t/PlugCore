package io.plugcore.plugCore.models;

import java.util.List;
import java.util.UUID;

public class ServerLinkData {
    private final String serverId;
    private final UUID ownerUUID;
    private final String verificationToken;
    private final boolean linked;
    private final List<String> purchasedPlugins;
    private final long lastValidation;

    public ServerLinkData(String serverId, UUID ownerUUID, String verificationToken,
                         boolean linked, List<String> purchasedPlugins, long lastValidation) {
        this.serverId = serverId;
        this.ownerUUID = ownerUUID;
        this.verificationToken = verificationToken;
        this.linked = linked;
        this.purchasedPlugins = purchasedPlugins;
        this.lastValidation = lastValidation;
    }

    public String getServerId() {
        return serverId;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public boolean isLinked() {
        return linked;
    }

    public List<String> getPurchasedPlugins() {
        return purchasedPlugins;
    }

    public long getLastValidation() {
        return lastValidation;
    }
}

