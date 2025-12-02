package io.plugcore.plugCore.commands;

import io.plugcore.plugCore.services.PluginDependencyService;
import io.plugcore.plugCore.services.ValidationService;
import io.plugcore.plugCore.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlugCoreCommand implements CommandExecutor, TabCompleter {
    private final ValidationService validationService;
    private final PluginDependencyService dependencyService;

    public PlugCoreCommand(ValidationService validationService, PluginDependencyService dependencyService) {
        this.validationService = validationService;
        this.dependencyService = dependencyService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(MessageUtil.error("You must be an operator to use this command."));
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "link":
                handleLink(sender, args);
                break;
            case "unlink":
                handleUnlink(sender);
                break;
            case "plugins":
                handlePlugins(sender);
                break;
            default:
                sendHelpMessage(sender);
                break;
        }

        return true;
    }

    private void handleLink(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.error("Only players can execute this command."));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtil.error("Command usage: /plugcore link <token>"));
            sender.sendMessage(MessageUtil.info("Obtain your linking token from your account dashboard."));
            return;
        }

        String token = args[1];

        String serverName = player.getServer().getName();
        String minecraftVersion = player.getServer().getMinecraftVersion();

        sender.sendMessage(MessageUtil.info("→ Linking your server... Please wait."));

        validationService.linkServer(token, serverName, minecraftVersion).thenAccept(response -> {
            if (response.isValid()) {
                sender.sendMessage(MessageUtil.success("✔ Server linked successfully!"));
                sender.sendMessage(MessageUtil.info("● Server: " + serverName));

                dependencyService.validateDependentPlugins();
            } else {
                sender.sendMessage(MessageUtil.error("❌ Linking failed: " + response.getMessage()));
                sender.sendMessage(MessageUtil.info("⚠ Ensure your token is valid from plugcore.io"));
            }
        }).exceptionally(throwable -> {
            sender.sendMessage(MessageUtil.error("❌ An error occurred during linking: " + throwable.getMessage()));
            return null;
        });
    }

    private void handleUnlink(CommandSender sender) {
        sender.sendMessage(MessageUtil.warning("⚠ Unlinking server..."));
        validationService.unlinkServer();
        sender.sendMessage(MessageUtil.success("✔ Server unlinked successfully."));
    }

    private void handlePlugins(CommandSender sender) {
        var authorized = dependencyService.getAuthorizedPlugins();
        var unauthorized = dependencyService.getUnauthorizedPlugins();

        sender.sendMessage(MessageUtil.info("Plugin Status:"));

        if (!authorized.isEmpty()) {
            sender.sendMessage(MessageUtil.success("✔ Authorized Plugins (" + authorized.size() + "):"));
            authorized.forEach(jarHash -> {
                String pluginName = getPluginNameFromHash(jarHash);
                sender.sendMessage(MessageUtil.info("  ◦ " + pluginName));
            });
        }

        if (!unauthorized.isEmpty()) {
            sender.sendMessage(MessageUtil.error("❌ Unauthorized Plugins (" + unauthorized.size() + "):"));
            unauthorized.forEach(jarHash -> {
                String pluginName = getPluginNameFromHash(jarHash);
                sender.sendMessage(MessageUtil.warning("  ⚠ " + pluginName));
            });
        }

        if (authorized.isEmpty() && unauthorized.isEmpty()) {
            sender.sendMessage(MessageUtil.info("No dependent plugins found."));
        }
    }

    private String getPluginNameFromHash(String jarHash) {
        for (org.bukkit.plugin.Plugin plugin : org.bukkit.Bukkit.getPluginManager().getPlugins()) {
            String pluginHash = dependencyService.calculatePluginJarHash(plugin);
            if (jarHash.equals(pluginHash)) {
                return plugin.getName();
            }
        }
        return jarHash;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(MessageUtil.info("PlugCore Commands:"));
        sender.sendMessage(MessageUtil.info("◦ /plugcore link <token> - Link your server to your account"));
        sender.sendMessage(MessageUtil.info("◦ /plugcore unlink - Disconnect your server"));
        sender.sendMessage(MessageUtil.info("◦ /plugcore plugins - View plugin status"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("link", "unlink", "plugins");
        }

        return new ArrayList<>();
    }
}
