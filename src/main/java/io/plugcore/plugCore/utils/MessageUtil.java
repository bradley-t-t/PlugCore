package io.plugcore.plugCore.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class MessageUtil {
    private static final TextColor PRIMARY = TextColor.fromHexString("#00AA00");
    private static final TextColor SUCCESS = TextColor.fromHexString("#55FF55");
    private static final TextColor ERROR = TextColor.fromHexString("#FFFF55");
    private static final TextColor WARNING = TextColor.fromHexString("#FFFF55");

    private static final String PREFIX = "[PlugCore] ";

    public static Component info(String message) {
        return Component.text(PREFIX).color(PRIMARY)
                .append(Component.text(message).color(WARNING));
    }

    public static Component success(String message) {
        return Component.text(PREFIX).color(PRIMARY)
                .append(Component.text(message).color(SUCCESS));
    }

    public static Component error(String message) {
        return Component.text(PREFIX).color(PRIMARY)
                .append(Component.text(message).color(ERROR));
    }

    public static Component warning(String message) {
        return Component.text(PREFIX).color(PRIMARY)
                .append(Component.text(message).color(WARNING));
    }
}
