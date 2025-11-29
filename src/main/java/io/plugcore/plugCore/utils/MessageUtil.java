package io.plugcore.plugCore.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class MessageUtil {
    private static final TextColor PRIMARY = TextColor.fromHexString("#00D9FF");
    private static final TextColor SUCCESS = TextColor.fromHexString("#00FF9F");
    private static final TextColor ERROR = TextColor.fromHexString("#FF4757");
    private static final TextColor WARNING = TextColor.fromHexString("#FFA502");

    private static final String PREFIX = "[PlugCore] ";

    public static Component info(String message) {
        return Component.text(PREFIX).color(PRIMARY)
                .append(Component.text(message).color(TextColor.fromHexString("#FFFFFF")));
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

