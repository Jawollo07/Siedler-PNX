package de.jawollo07.siedler.monsters;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import de.jawollo07.siedler.team.Team;
import de.jawollo07.siedler.team.TeamManager;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;
import org.powernukkitx.command.route.node.RouteNode;
import org.powernukkitx.command.tree.node.StringNode;

import java.util.List;

public final class OutpostCommand extends Command {
    private final SiedlerPlugin plugin;
    private final OutpostManager outpostManager;
    private final TeamManager teamManager;
    private final MessageManager messages = new MessageManager();
    private final String prefix;

    public OutpostCommand(SiedlerPlugin plugin, OutpostManager outpostManager) {
        super("outpost", "Verwaltet Siedler-Outposts", "/outpost <list|info|admin>");
        this.plugin = plugin;
        this.outpostManager = outpostManager;
        this.teamManager = new TeamManager(plugin);
        this.prefix = messages.getPrefix("monsters");
        setPermission("siedler.command.outpost");
        setPermissionMessage(messages.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().then(RouteNode.literal("help").exec(context -> {
            help(context.getSender());
            return CommandResult.success();
        }));
        tree.getRoot().then(RouteNode.literal("list").exec(context -> {
            list(context.getSender());
            return CommandResult.success();
        }));
        tree.getRoot().then(RouteNode.literal("info")
                .then(RouteNode.argument("name", new StringNode()).exec(context -> {
                    info(context.getSender(), context.getArg("name"));
                    return CommandResult.success();
                })));

        tree.getRoot().then(RouteNode.literal("admin")
                .permission("siedler.admin", messages.getCommandMessage("no-permission"))
                .then(RouteNode.literal("help").exec(context -> {
                    help(context.getSender());
                    return CommandResult.success();
                }))
                .then(RouteNode.literal("create")
                        .then(RouteNode.argument("name", new StringNode()).exec(context -> {
                            create(context.getSender(), context.getArg("name"),
                                    plugin.getConfig().getInt("monsters.outposts.radius", 12));
                            return CommandResult.success();
                        })))
                .then(RouteNode.literal("delete")
                        .then(RouteNode.argument("name", new StringNode()).exec(context -> {
                            delete(context.getSender(), context.getArg("name"));
                            return CommandResult.success();
                        })));
    }

    private void create(CommandSender sender, String name, int radius) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.getMessage("messages.monsters.outpost-player-required"));
            return;
        }
        if (radius <= 0) return;

        try {
            OutpostManager.Outpost outpost = outpostManager.create(name, player, radius);
            sender.sendMessage(messages.getMessage("messages.monsters.outpost-created")
                    .replace("{outpost}", outpost.name())
                    .replace("{radius}", String.valueOf(outpost.radius())));
        } catch (Exception exception) {
            sender.sendMessage(messages.getMessage("messages.monsters.outpost-error")
                    .replace("{error}", safe(exception)));
        }
    }

    private void delete(CommandSender sender, String name) {
        try {
            if (outpostManager.delete(name)) {
                sender.sendMessage(messages.getMessage("messages.monsters.outpost-deleted")
                        .replace("{outpost}", name));
            } else {
                sender.sendMessage(messages.getMessage("messages.monsters.outpost-not-found"));
            }
        } catch (Exception exception) {
            sender.sendMessage(messages.getMessage("messages.monsters.outpost-error")
                    .replace("{error}", safe(exception)));
        }
    }

    private void list(CommandSender sender) {
        try {
            List<OutpostManager.Outpost> outposts = outpostManager.getOutposts();
            if (outposts.isEmpty()) {
                sender.sendMessage(messages.getMessage("messages.monsters.outpost-none"));
                return;
            }

            sender.sendMessage(messages.getMessage("messages.monsters.outpost-list-title"));
            for (OutpostManager.Outpost outpost : outposts) {
                sender.sendMessage(messages.getMessage("messages.monsters.outpost-list-entry")
                        .replace("{outpost}", outpost.name())
                        .replace("{owner}", ownerName(outpost.ownerTeamId()))
                        .replace("{world}", outpost.world())
                        .replace("{x}", String.valueOf(outpost.x()))
                        .replace("{z}", String.valueOf(outpost.z())));
            }
        } catch (Exception exception) {
            sender.sendMessage(messages.getMessage("messages.monsters.outpost-error")
                    .replace("{error}", safe(exception)));
        }
    }

    private void info(CommandSender sender, String name) {
        try {
            OutpostManager.Outpost outpost = outpostManager.getByName(name);
            if (outpost == null) {
                sender.sendMessage(messages.getMessage("messages.monsters.outpost-not-found"));
                return;
            }

            String progress = outpostManager.getProgressSeconds(outpost.id())
                    + "/" + outpostManager.getCaptureSeconds();

            sender.sendMessage(messages.getMessage("messages.monsters.outpost-info")
                    .replace("{outpost}", outpost.name())
                    .replace("{owner}", ownerName(outpost.ownerTeamId()))
                    .replace("{world}", outpost.world())
                    .replace("{x}", String.valueOf(outpost.x()))
                    .replace("{y}", String.valueOf(outpost.y()))
                    .replace("{z}", String.valueOf(outpost.z()))
                    .replace("{radius}", String.valueOf(outpost.radius()))
                    .replace("{progress}", progress));
        } catch (Exception exception) {
            sender.sendMessage(messages.getMessage("messages.monsters.outpost-error")
                    .replace("{error}", safe(exception)));
        }
    }

    private String ownerName(String teamId) {
        if (teamId == null || teamId.isBlank()) {
            return messages.getMessage("messages.monsters.outpost-unclaimed");
        }
        try {
            for (Team team : teamManager.getTeams()) {
                if (team.id().equals(teamId)) return team.name();
            }
        } catch (Exception ignored) {
        }
        return teamId;
    }

    private void help(CommandSender sender) {
        sender.sendMessage(prefix + messages.getMessage("messages.monsters.outpost-help"));
    }

    private String safe(Exception exception) {
        return exception.getMessage() == null ? "Unbekannter Fehler" : exception.getMessage();
    }
}
