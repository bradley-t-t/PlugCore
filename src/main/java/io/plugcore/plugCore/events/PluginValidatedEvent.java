package io.plugcore.plugCore.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PluginValidatedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String pluginName;
    private final boolean authorized;

    public PluginValidatedEvent(String pluginName, boolean authorized) {
        this.pluginName = pluginName;
        this.authorized = authorized;
    }

    public String getPluginName() {
        return pluginName;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

