package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;
import org.powernukkitx.command.route.node.RouteNode;
import org.powernukkitx.form.window.SimpleForm;
import java.util.Map;

public class ManagementCommand extends Command {
    private final SiedlerPlugin plugin;
    private final MessageManager messageManager;
    private final String prefix;
    private final ModerationManager moderationManager;

    public ManagementCommand(SiedlerPlugin plugin) {
        super("verwaltung", "Öffnet die Siedler-Verwaltung", "/verwaltung");
        this.plugin = plugin;
        this.messageManager = new MessageManager();
        this.prefix = messageManager.getPrefix("essentials");
        this.moderationManager = new ModerationManager(plugin);
        setPermission("siedler.admin");
        setPermissionMessage(messageManager.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().then(RouteNode.literal("help").exec(context -> {
            sendHelp(context.getSender());
            return CommandResult.success();
        }));
        tree.getRoot().then(RouteNode.literal("menu").exec(context -> open(context.getSender())));

        tree.getRoot().then(RouteNode.literal("warn")
                .then(RouteNode.argument("player", new org.powernukkitx.command.tree.node.StringNode())
                        .then(RouteNode.argument("reason", new org.powernukkitx.command.tree.node.StringNode()).exec(context -> {
                            punish(context.getSender(), context.getArg("player"), "warn", context.getArg("reason"), 0);
                            return CommandResult.success();
                        }))));
        tree.getRoot().then(RouteNode.literal("kick")
                .then(RouteNode.argument("player", new org.powernukkitx.command.tree.node.StringNode())
                        .then(RouteNode.argument("reason", new org.powernukkitx.command.tree.node.StringNode()).exec(context -> {
                            punish(context.getSender(), context.getArg("player"), "kick", context.getArg("reason"), 0);
                            return CommandResult.success();
                        }))));
        tree.getRoot().then(RouteNode.literal("ban")
                .then(RouteNode.argument("player", new org.powernukkitx.command.tree.node.StringNode())
                        .then(RouteNode.argument("reason", new org.powernukkitx.command.tree.node.StringNode()).exec(context -> {
                            punish(context.getSender(), context.getArg("player"), "ban", context.getArg("reason"), 0);
                            return CommandResult.success();
                        }))));
        tree.getRoot().then(RouteNode.literal("tempban")
                .then(RouteNode.argument("player", new org.powernukkitx.command.tree.node.StringNode())
                        .then(RouteNode.argument("minutes", new org.powernukkitx.command.tree.node.IntNode())
                                .then(RouteNode.argument("reason", new org.powernukkitx.command.tree.node.StringNode()).exec(context -> {
                                    punish(context.getSender(), context.getArg("player"), "tempban", context.getArg("reason"),
                                            Integer.parseInt(context.getArg("minutes")));
                                    return CommandResult.success();
                                })))));
        tree.getRoot().then(RouteNode.literal("unban")
                .then(RouteNode.argument("player", new org.powernukkitx.command.tree.node.StringNode()).exec(context -> {
                    unban(context.getSender(), context.getArg("player"));
                    return CommandResult.success();
                })));

        tree.getRoot().exec(context -> open(context.getSender()));
    }

    private CommandResult open(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.player-required"));
            return CommandResult.fail("Spieler erforderlich");
        }
        openMainMenu(player);
        return CommandResult.success();
    }

    private void punish(CommandSender sender, String playerName, String type, String reason, int minutes) {
        try {
            String playerId = findPlayerId(playerName);
            if (playerId == null) {
                sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-player-not-found"));
                return;
            }
            String moderatorId = sender instanceof Player p ? p.getUniqueId().toString() : null;
            String moderatorName = sender instanceof Player p ? p.getName() : "Console";
            ModerationManager.Punishment result;
            if ("warn".equals(type)) result = moderationManager.warn(playerId, playerName, reason, moderatorId, moderatorName);
            else if ("kick".equals(type)) result = moderationManager.kick(playerId, playerName, reason, moderatorId, moderatorName);
            else if ("ban".equals(type)) result = moderationManager.ban(playerId, playerName, reason, moderatorId, moderatorName);
            else result = moderationManager.tempBan(playerId, playerName, reason, moderatorId, moderatorName, minutes * 60_000L);
            sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-action-success")
                    .replace("{type}", type).replace("{player}", playerName));
        } catch (Exception exception) {
            sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-action-error")
                    .replace("{error}", exception.getMessage() == null ? "Unbekannter Fehler" : exception.getMessage()));
        }
    }

    private void unban(CommandSender sender, String playerName) {
        try {
            String playerId = findPlayerId(playerName);
            if (playerId == null) {
                sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-player-not-found"));
                return;
            }
            String moderatorId = sender instanceof Player p ? p.getUniqueId().toString() : null;
            if (moderationManager.unban(playerId, moderatorId, "Unban durch Verwaltung")) {
                sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-action-success")
                        .replace("{type}", "unban").replace("{player}", playerName));
            } else {
                sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-no-active-ban"));
            }
        } catch (Exception exception) {
            sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-action-error")
                    .replace("{error}", exception.getMessage() == null ? "Unbekannter Fehler" : exception.getMessage()));
        }
    }

    private String findPlayerId(String playerName) throws java.sql.SQLException {
        String id = new ModerationManager(plugin).findPlayerIdByName(playerName);
        if (id != null) return id;
        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            if (player.getName().equalsIgnoreCase(playerName)) return player.getUniqueId().toString();
        }
        return null;
    }

    private void openMainMenu(Player player) {
        int online = plugin.getServer().getOnlinePlayers().size();
        new SimpleForm(
                messageManager.getMessage("messages.essentials.management-title"),
                messageManager.getMessage("messages.essentials.management-header").replace("{online}", String.valueOf(online)))
                .addButton(messageManager.getMessage("messages.essentials.management-players"), ignored -> openPlayerList(player))
                .addButton(messageManager.getMessage("messages.essentials.management-close"))
                .send(player);
    }

    private void openPlayerList(Player admin) {
        Map<?, Player> players = plugin.getServer().getOnlinePlayers();
        SimpleForm form = new SimpleForm(
                messageManager.getMessage("messages.essentials.management-players-title"),
                messageManager.getMessage("messages.essentials.management-players-header").replace("{online}", String.valueOf(players.size())));
        for (Player target : players.values()) {
            form.addButton(target.getName(), ignored -> openPlayerInfo(admin, target));
        }
        form.addButton(messageManager.getMessage("messages.essentials.management-back"), ignored -> openMainMenu(admin));
        form.send(admin);
    }

    private void openPlayerInfo(Player admin, Player target) {
        String info = messageManager.getMessage("messages.essentials.management-player-info")
                .replace("{name}", target.getName())
                .replace("{uuid}", target.getUniqueId().toString())
                .replace("{world}", target.getLevel() == null ? "-" : target.getLevel().getName())
                .replace("{x}", String.valueOf((int) target.getFloorX()))
                .replace("{y}", String.valueOf((int) target.getFloorY()))
                .replace("{z}", String.valueOf((int) target.getFloorZ()));
        new SimpleForm(messageManager.getMessage("messages.essentials.management-player-title"), info)
                .addButton(messageManager.getMessage("messages.essentials.management-back"), ignored -> openPlayerList(admin))
                .send(admin);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-help"));
    }
}
