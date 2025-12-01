package io.plugcore.plugCore.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ServerLinkedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String serverId;
    private final String ownerUUID;

    public ServerLinkedEvent(String serverId, String ownerUUID) {
        this.serverId = serverId;
        this.ownerUUID = ownerUUID;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public String getServerId() {
        return serverId;
    }

    public String getOwnerUUID() {
        return ownerUUID;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}

