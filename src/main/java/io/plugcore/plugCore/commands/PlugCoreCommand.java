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
            case "status":
                handleStatus(sender);
                break;
            case "validate":
                handleValidate(sender);
                break;
            case "plugins":
                handlePlugins(sender);
                break;
            case "reload":
                handleReload(sender);
                break;
            default:
                sendHelpMessage(sender);
                break;
        }

        return true;
    }

    private void handleLink(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.error("Only players can link their account."));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtil.error("Usage: /plugcore link <token>"));
            sender.sendMessage(MessageUtil.info("Get your linking token from your account page!"));
            return;
        }

        String token = args[1];

        String serverName = player.getServer().getName();
        String minecraftVersion = player.getServer().getMinecraftVersion();

        sender.sendMessage(MessageUtil.info("Linking server to your PlugCore account..."));

        validationService.linkServer(token, serverName, minecraftVersion).thenAccept(response -> {
            if (response.isValid()) {
                sender.sendMessage(MessageUtil.success("Server successfully linked to your PlugCore account!"));
                sender.sendMessage(MessageUtil.info("Server: " + serverName));

                dependencyService.validateDependentPlugins();
            } else {
                sender.sendMessage(MessageUtil.error("Failed to link server: " + response.getMessage()));
                sender.sendMessage(MessageUtil.info("Make sure you have a valid linking token from plugcore.io"));
            }
        }).exceptionally(throwable -> {
            sender.sendMessage(MessageUtil.error("An error occurred while linking: " + throwable.getMessage()));
            return null;
        });
    }

    private void handleUnlink(CommandSender sender) {
        sender.sendMessage(MessageUtil.warning("Unlinking server from PlugCore..."));
        validationService.unlinkServer();
        sender.sendMessage(MessageUtil.success("Server has been unlinked."));
    }

    private void handleStatus(CommandSender sender) {
        var linkData = validationService.getCurrentLinkData();

        sender.sendMessage(MessageUtil.info("=== PlugCore Status ==="));
        sender.sendMessage(MessageUtil.info("Linked: " + (linkData.isLinked() ? "Yes" : "No")));

        if (linkData.isLinked()) {
            sender.sendMessage(MessageUtil.info("Server ID: " + linkData.getServerId()));
            sender.sendMessage(MessageUtil.info("Owner UUID: " + linkData.getOwnerUUID()));
            sender.sendMessage(MessageUtil.info("Purchased Plugins: " + linkData.getPurchasedPlugins().size()));
        }
    }

    private void handleValidate(CommandSender sender) {
        sender.sendMessage(MessageUtil.info("Validating server with PlugCore..."));

        validationService.validateServerLink().thenAccept(valid -> {
            if (valid) {
                sender.sendMessage(MessageUtil.success("Server validation successful!"));
                dependencyService.validateDependentPlugins();
            } else {
                sender.sendMessage(MessageUtil.error("Server validation failed. Please link your server again."));
            }
        }).exceptionally(throwable -> {
            sender.sendMessage(MessageUtil.error("Validation error: " + throwable.getMessage()));
            return null;
        });
    }

    private void handlePlugins(CommandSender sender) {
        var authorized = dependencyService.getAuthorizedPlugins();
        var unauthorized = dependencyService.getUnauthorizedPlugins();

        sender.sendMessage(MessageUtil.info("=== Dependent Plugins ==="));

        if (!authorized.isEmpty()) {
            sender.sendMessage(MessageUtil.success("Authorized (" + authorized.size() + "):"));
            authorized.forEach(plugin -> sender.sendMessage(MessageUtil.info("  - " + plugin)));
        }

        if (!unauthorized.isEmpty()) {
            sender.sendMessage(MessageUtil.error("Unauthorized (" + unauthorized.size() + "):"));
            unauthorized.forEach(plugin -> sender.sendMessage(MessageUtil.warning("  - " + plugin)));
        }

        if (authorized.isEmpty() && unauthorized.isEmpty()) {
            sender.sendMessage(MessageUtil.info("No plugins depend on PlugCore."));
        }
    }

    private void handleReload(CommandSender sender) {
        sender.sendMessage(MessageUtil.info("Reloading PlugCore..."));
        dependencyService.scanPlugins();
        dependencyService.validateDependentPlugins();
        sender.sendMessage(MessageUtil.success("PlugCore reloaded successfully."));
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(MessageUtil.info("=== PlugCore Commands ==="));
        sender.sendMessage(MessageUtil.info("/plugcore link <token> - Link your server"));
        sender.sendMessage(MessageUtil.info("/plugcore unlink - Unlink your server"));
        sender.sendMessage(MessageUtil.info("/plugcore status - Check link status"));
        sender.sendMessage(MessageUtil.info("/plugcore validate - Validate server"));
        sender.sendMessage(MessageUtil.info("/plugcore plugins - List dependent plugins"));
        sender.sendMessage(MessageUtil.info("/plugcore reload - Reload plugin checks"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("link", "unlink", "status", "validate", "plugins", "reload");
        }

        return new ArrayList<>();
    }
}

