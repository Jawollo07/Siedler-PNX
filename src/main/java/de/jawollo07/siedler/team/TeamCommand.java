package de.jawollo07.siedler.team;

import java.sql.SQLException;
import java.util.List;

import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;
import org.powernukkitx.command.route.node.RouteNode;
import org.powernukkitx.command.tree.node.StringNode;
import org.powernukkitx.Player;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;

/** Team management commands, registered through the PowerNukkitX RouteTree API. */
public final class TeamCommand extends Command {
    private final SiedlerPlugin plugin;
    private final TeamManager teamManager;
    private final MessageManager messages = new MessageManager();
    private final String prefix;

    public TeamCommand() {
        super("team", "Manage teams", "/team help");
        this.plugin = SiedlerPlugin.getInstance();
        this.teamManager = new TeamManager(plugin);
        this.prefix = messages.getPrefix("team");
        setPermission("siedler.command.team");
        setPermissionMessage(messages.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().then(RouteNode.literal("help").exec(context -> {
            sendHelp(context.getSender());
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

        RouteNode admin = RouteNode.literal("admin")
                .permission("siedler.admin", messages.getCommandMessage("no-permission"));

        admin.then(RouteNode.literal("help").exec(context -> {
            sendAdminHelp(context.getSender());
            return CommandResult.success();
        }));
        admin.then(RouteNode.literal("create")
                .then(RouteNode.argument("name", new StringNode())
                        .then(RouteNode.argument("color", new StringNode()).exec(context -> {
                            create(context.getSender(), context.getArg("name"), context.getArg("color"));
                            return CommandResult.success();
                        }))));
        admin.then(RouteNode.literal("delete")
                .then(RouteNode.argument("name", new StringNode()).exec(context -> {
                    delete(context.getSender(), context.getArg("name"));
                    return CommandResult.success();
                })));
        admin.then(RouteNode.literal("add")
                .then(RouteNode.argument("player", new StringNode())
                        .then(RouteNode.argument("team", new StringNode()).exec(context -> {
                            add(context.getSender(), context.getArg("player"), context.getArg("team"));
                            return CommandResult.success();
                        }))));
        admin.then(RouteNode.literal("remove")
                .then(RouteNode.argument("player", new StringNode()).exec(context -> {
                    remove(context.getSender(), context.getArg("player"));
                    return CommandResult.success();
                })));
        admin.then(RouteNode.literal("setcolor")
                .then(RouteNode.argument("name", new StringNode())
                        .then(RouteNode.argument("color", new StringNode()).exec(context -> {
                            setColor(context.getSender(), context.getArg("name"), context.getArg("color"));
                            return CommandResult.success();
                        }))));
        tree.getRoot().then(admin);
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(prefix + messages.getMessage("messages.team.admin-help-title"));
        sender.sendMessage(messages.getMessage("messages.team.admin-help-create"));
        sender.sendMessage(messages.getMessage("messages.team.admin-help-delete"));
        sender.sendMessage(messages.getMessage("messages.team.admin-help-add"));
        sender.sendMessage(messages.getMessage("messages.team.admin-help-remove"));
        sender.sendMessage(messages.getMessage("messages.team.admin-help-setcolor"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(prefix + messages.getMessage("messages.team.help-title"));
        sender.sendMessage(messages.getMessage("messages.team.help-create"));
        sender.sendMessage(messages.getMessage("messages.team.help-delete"));
        sender.sendMessage(messages.getMessage("messages.team.help-add"));
        sender.sendMessage(messages.getMessage("messages.team.help-remove"));
        sender.sendMessage(messages.getMessage("messages.team.help-list"));
        sender.sendMessage(messages.getMessage("messages.team.help-info"));
        sender.sendMessage(messages.getMessage("messages.team.help-setcolor"));
    }

    private void create(CommandSender sender, String name, String color) {
        try {
            Team team = teamManager.createTeam(name, color);
            sender.sendMessage(prefix + messages.getMessage("messages.team.created").replace("{team}", team.name()).replace("{color}", team.color()));
        } catch (Exception e) {
            sender.sendMessage(prefix + messages.getMessage("messages.team.create-error").replace("{error}", safe(e)));
        }
    }

    private void delete(CommandSender sender, String name) {
        try {
            teamManager.deleteTeam(name);
            sender.sendMessage(prefix + messages.getMessage("messages.team.deleted").replace("{team}", name));
        } catch (Exception e) {
            sender.sendMessage(prefix + messages.getMessage("messages.team.delete-error").replace("{error}", safe(e)));
        }
    }

    private void add(CommandSender sender, String player, String team) {
        try {
            teamManager.addPlayerToTeam(player, team);
            sender.sendMessage(prefix + messages.getMessage("messages.team.player-added").replace("{player}", player).replace("{team}", team));
        } catch (Exception e) {
            sender.sendMessage(prefix + messages.getMessage("messages.team.player-add-error").replace("{error}", safe(e)));
        }
    }

    private void remove(CommandSender sender, String player) {
        try {
            teamManager.removePlayerFromTeam(player);
            sender.sendMessage(prefix + messages.getMessage("messages.team.player-removed").replace("{player}", player));
        } catch (Exception e) {
            sender.sendMessage(prefix + messages.getMessage("messages.team.player-remove-error").replace("{error}", safe(e)));
        }
    }

    private void list(CommandSender sender) {
        try {
            List<Team> teams = teamManager.getTeams();
            if (teams.isEmpty()) {
                sender.sendMessage(prefix + messages.getMessage("messages.team.none"));
                return;
            }
            sender.sendMessage(prefix + messages.getMessage("messages.team.list-title"));
            for (Team team : teams) {
                sender.sendMessage(messages.getMessage("messages.team.list-entry").replace("{team}", team.name()).replace("{color}", team.color()));
            }
        } catch (SQLException e) {
            sender.sendMessage(prefix + messages.getMessage("messages.team.list-error"));
            plugin.getLogger().warning("Could not list teams: " + e.getMessage());
        }
    }

    private void info(CommandSender sender, String name) {
        try {
            Team team = teamManager.getTeamByName(name);
            sender.sendMessage(prefix + messages.getMessage("messages.team.info-title"));
            sender.sendMessage(messages.getMessage("messages.team.info-name").replace("{name}", team.name()));
            sender.sendMessage(messages.getMessage("messages.team.info-color").replace("{color}", team.color()));
            sender.sendMessage(messages.getMessage("messages.team.info-tax-bonus").replace("{bonus}", String.valueOf(team.taxBonus())));
            sender.sendMessage(messages.getMessage("messages.team.info-eliminated").replace("{status}", team.eliminated() == 1 ? messages.getMessage("messages.team.yes") : messages.getMessage("messages.team.no")));
            sender.sendMessage(messages.getMessage("messages.team.info-balance").replace("{balance}", String.valueOf(team.balance())));
        } catch (SQLException e) {
            sender.sendMessage(prefix + messages.getMessage("messages.team.not-found").replace("{team}", name));
        }
    }

    private String safe(Exception e) { return e.getMessage() == null ? messages.getMessage("messages.essentials.error-unknown") : e.getMessage(); }

    private void setColor(CommandSender sender, String name, String color) {
        try {
            teamManager.setTeamColor(name, color);
            sender.sendMessage(prefix + messages.getMessage("messages.team.color-updated").replace("{team}", name).replace("{color}", color));
        } catch (Exception e) {
            sender.sendMessage(prefix + messages.getMessage("messages.team.color-error").replace("{error}", safe(e)));
        }
    }
}
