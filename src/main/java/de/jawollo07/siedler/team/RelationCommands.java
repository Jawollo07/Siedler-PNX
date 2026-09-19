package de.jawollo07.siedler.team;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;
import org.powernukkitx.command.route.node.RouteNode;
import org.powernukkitx.command.tree.node.StringNode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

public class RelationCommands extends Command {
    private final SiedlerPlugin plugin;
    private final Relations relations;
    private final MessageManager messageManager;
    private final String prefix;

    public RelationCommands(SiedlerPlugin plugin) {
        super("diplomatie", "Ändert die Diplomatien zwischen Teams", "/diplomatie");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.relations = new Relations(plugin);
        this.messageManager = new MessageManager();
        this.prefix = messageManager.getPrefix("team");
        setPermission("siedler.command.diplomatie");
        setPermissionMessage(messageManager.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().then(RouteNode.literal("help").exec(context -> {
            help(context.getSender());
            return CommandResult.success();
        }));
        tree.getRoot().then(RouteNode.literal("set")
                .then(RouteNode.argument("team", new StringNode())
                        .then(RouteNode.argument("relation", new StringNode()).exec(context -> {
                            setRelation(context.getSender(), context.getArg("team"), context.getArg("relation"));
                            return CommandResult.success();
                        }))));
        tree.getRoot().then(RouteNode.literal("show").exec(context -> {
            showRelations(context.getSender(), null);
            return CommandResult.success();
        }).then(RouteNode.argument("team", new StringNode()).exec(context -> {
            showRelations(context.getSender(), context.getArg("team"));
            return CommandResult.success();
        })));
        tree.getRoot().then(RouteNode.literal("list").permission("siedler.admin", messageManager.getCommandMessage("no-permission"))
                .exec(context -> {
                    listAllRelations(context.getSender());
                    return CommandResult.success();
                }));
        tree.getRoot().then(RouteNode.literal("admin").permission("siedler.admin", messageManager.getCommandMessage("no-permission"))
                .then(RouteNode.literal("set")
                        .then(RouteNode.argument("teamA", new StringNode())
                                .then(RouteNode.argument("teamB", new StringNode())
                                        .then(RouteNode.argument("relation", new StringNode()).exec(context -> {
                                            setAdminRelation(context.getSender(), context.getArg("teamA"), context.getArg("teamB"), context.getArg("relation"));
                                            return CommandResult.success();
                                        }))))
                .then(RouteNode.literal("show").then(RouteNode.argument("team", new StringNode()).exec(context -> {
                    showRelations(context.getSender(), context.getArg("team"));
                    return CommandResult.success();
                })).exec(context -> {
                    listAllRelations(context.getSender());
                    return CommandResult.success();
                }))
                .then(RouteNode.literal("list").exec(context -> {
                    listAllRelations(context.getSender());
                    return CommandResult.success();
                })));
    }

    public String help(CommandSender sender) {
        sender.sendMessage(prefix + messageManager.getMessage("team", "help.1"));
        sender.sendMessage(prefix + messageManager.getMessage("team", "help.2"));
        sender.sendMessage(prefix + messageManager.getMessage("team", "help.3"));
        sender.sendMessage(prefix + messageManager.getMessage("team", "help.4"));
        if (sender.hasPermission("siedler.admin")) {
            sender.sendMessage(prefix + messageManager.getMessage("team", "help.admin.1"));
            sender.sendMessage(prefix + messageManager.getMessage("team", "help.admin.2"));
        }
        return "help";
    }

    private void setRelation(CommandSender sender, String targetTeam, String relation) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + messageManager.getMessage("team", "only-player-command"));
            return;
        }
        String ownTeam = resolvePlayerTeamName(player);
        if (ownTeam == null) {
            sender.sendMessage(prefix + messageManager.getMessage("team", "player-has-no-team"));
            return;
        }
        if (!Relations.isValidRelation(relation)) {
            sender.sendMessage(prefix + messageManager.getMessage("team", "invalid-relation"));
            return;
        }
        try {
            if (relations.setTeamRelation(ownTeam, targetTeam, relation)) {
                sender.sendMessage(prefix + messageManager.getMessage("team", "relation-set") + ownTeam + " §aund §f" + targetTeam + " §aauf §f" + relation + " §agesetzt.");
            } else {
                sender.sendMessage(prefix + messageManager.getMessage("team", "relation-not-set"));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not set diplomacy: " + e.getMessage());
            sender.sendMessage(prefix + messageManager.getMessage("team", "relation-save-error") + e.getMessage());
        }
    }

    private void setAdminRelation(CommandSender sender, String teamA, String teamB, String relation) {
        if (!Relations.isValidRelation(relation)) {
            sender.sendMessage(prefix + messageManager.getMessage("team", "invalid-relation"));
            return;
        }
        try {
            if (relations.setTeamRelation(teamA, teamB, relation)) {
                sender.sendMessage(prefix + messageManager.getMessage("team", "relation-set") + teamA + " §aund §f" + teamB + " §aauf §f" + relation + " §agesetzt.");
            } else {
                sender.sendMessage(prefix + messageManager.getMessage("team", "admin-relation-not-set"));
            }
        } catch (SQLException e) {
            sender.sendMessage(prefix + messageManager.getMessage("team", "admin-set-error") + e.getMessage());
        }
    }

    private void showRelations(CommandSender sender, String requestedTeam) {
        String teamName = requestedTeam != null ? requestedTeam : resolvePlayerTeamName(sender);
        if (teamName == null) {
            sender.sendMessage(prefix + messageManager.getMessage("team", "show-no-team"));
            return;
        }
        try {
            Map<String, String> entries = relations.getRelationsForTeamByName(teamName);
            if (entries.isEmpty()) {
                sender.sendMessage(prefix + messageManager.getMessage("team", "no-relations") + teamName + " §7sind noch keine Beziehungen eingetragen.");
                return;
            }
            sender.sendMessage(prefix + messageManager.getMessage("team", "relations-header") + teamName + "§6:");
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                sender.sendMessage(prefix + messageManager.getMessage("team", "relation-entry") + entry.getKey() + " §8→ §f" + entry.getValue());
            }
        } catch (SQLException e) {
            sender.sendMessage(prefix + messageManager.getMessage("team", "relations-load-error") + e.getMessage());
        }
    }

    private void listAllRelations(CommandSender sender) {
        try {
            TeamManager teamManager = new TeamManager(plugin);
            for (Team team : teamManager.getTeams()) {
                sender.sendMessage(prefix + messageManager.getMessage("team", "team-header") + team.name());
                Map<String, String> entries = relations.getRelationsForTeamByName(team.name());
                if (entries.isEmpty()) {
                    sender.sendMessage(prefix + messageManager.getMessage("team", "no-relations-item"));
                    continue;
                }
                for (Map.Entry<String, String> entry : entries.entrySet()) {
                    sender.sendMessage(prefix + messageManager.getMessage("team", "relation-entry") + entry.getKey() + " §8→ §f" + entry.getValue());
                }
            }
        } catch (SQLException e) {
            sender.sendMessage(prefix + messageManager.getMessage("team", "relations-list-error") + e.getMessage());
        }
    }

    private String resolvePlayerTeamName(CommandSender sender) {
        if (!(sender instanceof Player player)) return null;
        return resolvePlayerTeamName(player);
    }

    private String resolvePlayerTeamName(Player player) {
        String sql = "SELECT t.name FROM players p LEFT JOIN teams t ON t.id = p.team_id WHERE p.last_name = ?";
        try (Connection connection = plugin.getStorage().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, player.getName());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return resultSet.getString("name");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not resolve player team for " + player.getName() + ": " + e.getMessage());
        }
        return null;
    }
}
