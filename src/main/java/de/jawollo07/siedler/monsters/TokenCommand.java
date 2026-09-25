package de.jawollo07.siedler.monsters;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;
import org.powernukkitx.command.route.node.RouteNode;

public final class TokenCommand extends Command {
    private final SiedlerPlugin plugin;
    private final MessageManager messageManager;
    private final TokenManager tokenManager;

    public TokenCommand(SiedlerPlugin plugin, TokenManager tokenManager) {
        super("token", "Verwaltet Token-Runden", "/token");
        this.plugin = plugin;
        this.messageManager = new MessageManager();
        this.tokenManager = tokenManager;
        setPermission("siedler.command.token");
        setPermissionMessage(messageManager.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().then(RouteNode.literal("admin")
                .permission("siedler.admin", messageManager.getCommandMessage("no-permission"))
                .then(RouteNode.literal("help").exec(context -> {
                    sendHelp(context.getSender());
                    return CommandResult.success();
                }))
                .then(RouteNode.literal("start").exec(context -> {
                    start(context.getSender());
                    return CommandResult.success();
                }))
                .then(RouteNode.literal("spawn").exec(context -> {
                    spawn(context.getSender());
                    return CommandResult.success();
                }))
                .then(RouteNode.literal("status").exec(context -> {
                    status(context.getSender());
                    return CommandResult.success();
                })));
        tree.getRoot().then(RouteNode.literal("help").exec(context -> {
            sendHelp(context.getSender());
            return CommandResult.success();
        }));
    }

    private void start(CommandSender sender) {
        try {
            tokenManager.startRound();
            sender.sendMessage(messageManager.getMessage(
                    "messages.monsters.token-round-started"));
        } catch (Exception exception) {
            sender.sendMessage(messageManager.getMessage(
                    "messages.monsters.token-error")
                    .replace("{error}", error(exception)));
        }
    }

    private void spawn(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageManager.getMessage(
                    "messages.essentials.player-required"));
            return;
        }
        try {
            if (tokenManager.spawnToken(player)) {
                player.sendMessage(messageManager.getMessage(
                        "messages.monsters.token-spawned"));
            } else {
                player.sendMessage(messageManager.getMessage(
                        "messages.monsters.token-spawn-failed"));
            }
        } catch (Exception exception) {
            player.sendMessage(messageManager.getMessage(
                    "messages.monsters.token-error")
                    .replace("{error}", error(exception)));
        }
    }

    private void status(CommandSender sender) {
        try {
            sender.sendMessage(messageManager.getMessage(
                    "messages.monsters.token-status")
                    .replace("{active}", String.valueOf(tokenManager.countActiveTokens()))
                    .replace("{max}", String.valueOf(plugin.getConfig().getInt(
                            "monsters.token.max-active", 4))));
        } catch (Exception exception) {
            sender.sendMessage(messageManager.getMessage(
                    "messages.monsters.token-error")
                    .replace("{error}", error(exception)));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(messageManager.getMessage(
                "messages.monsters.token-help"));
    }

    private String error(Exception exception) {
        return exception.getMessage() == null ? "Unbekannter Fehler" : exception.getMessage();
    }
}
