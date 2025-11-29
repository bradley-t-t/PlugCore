package io.plugcore.plugCore.models;

public class LinkRequest {
    private final String serverId;
    private final String ownerUUID;
    private final String verificationCode;

    public LinkRequest(String serverId, String ownerUUID, String verificationCode) {
        this.serverId = serverId;
        this.ownerUUID = ownerUUID;
        this.verificationCode = verificationCode;
    }

    public String getServerId() {
        return serverId;
    }

    public String getOwnerUUID() {
        return ownerUUID;
    }

    public String getVerificationCode() {
        return verificationCode;
    }
}

