package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;
import org.powernukkitx.command.route.node.RouteNode;
import org.powernukkitx.command.tree.node.StringNode;

public final class StartCommand extends Command {
    private final StartManager startManager;
    private final MessageManager messages = new MessageManager();

    public StartCommand(SiedlerPlugin plugin, StartManager startManager) {
        super("start", "Siedler Start-System", "/start");
        this.startManager = startManager;
        setPermission("siedler.command.start");
        setPermissionMessage(messages.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().then(RouteNode.literal("help").exec(ctx -> {
            ctx.getSender().sendMessage(messages.getMessage("messages.start.help"));
            return CommandResult.success();
        }));
        tree.getRoot().then(RouteNode.literal("status").exec(ctx -> {
            ctx.getSender().sendMessage(messages.getMessage("messages.start.status")
                    .replace("{teams}", String.valueOf(startManager.getConfiguredTeams().size())));
            return CommandResult.success();
        }));
        tree.getRoot().then(RouteNode.literal("kit").exec(ctx -> {
            if (!(ctx.getSender() instanceof Player player)) return CommandResult.fail();
            startManager.giveStarterKit(player, false);
            return CommandResult.success();
        }));
        tree.getRoot().then(RouteNode.literal("admin")
                .permission("siedler.admin", messages.getCommandMessage("no-permission"))
                .then(RouteNode.literal("set")
                        .then(RouteNode.argument("team", new StringNode()).exec(ctx -> {
                            if (!(ctx.getSender() instanceof Player player)) return CommandResult.fail();
                            startManager.setTeamStart(player, ctx.getArg("team"));
                            return CommandResult.success();
                        })))
                .then(RouteNode.literal("clear")
                        .then(RouteNode.argument("team", new StringNode()).exec(ctx -> {
                            startManager.clearTeamStart(ctx.getSender(), ctx.getArg("team"));
                            return CommandResult.success();
                        })))
                .then(RouteNode.literal("teamtp")
                        .then(RouteNode.argument("player", new StringNode()).exec(ctx -> {
                            Player target = resolvePlayer(ctx.getArg("player"));
                            if (target == null) {
                                ctx.getSender().sendMessage(messages.getMessage("messages.start.player-not-found"));
                                return CommandResult.fail();
                            }
                            startManager.teleportToTeam(target);
                            return CommandResult.success();
                        })))
                .then(RouteNode.literal("starterkit")
                        .then(RouteNode.argument("player", new StringNode()).exec(ctx -> {
                            Player target = resolvePlayer(ctx.getArg("player"));
                            if (target == null) {
                                ctx.getSender().sendMessage(messages.getMessage("messages.start.player-not-found"));
                                return CommandResult.fail();
                            }
                            startManager.giveStarterKit(target, true);
                            return CommandResult.success();
                        })))
                .then(RouteNode.literal("game").exec(ctx -> {
                    startManager.startGame(ctx.getSender());
                    return CommandResult.success();
                }))
                .then(RouteNode.literal("help").exec(ctx -> {
                    ctx.getSender().sendMessage(messages.getMessage("messages.start.admin-help"));
                    return CommandResult.success();
                })));
    }

    private Player resolvePlayer(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        for (Player player : SiedlerPlugin.getInstance().getServer().getOnlinePlayers().values()) {
            if (player.getName().equalsIgnoreCase(normalized)
                    || player.getUniqueId().toString().equalsIgnoreCase(normalized)) return player;
        }
        return null;
    }
}
