package de.jawollo07.siedler.team;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.powernukkitx.Player;
import org.powernukkitx.Server;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;
import org.powernukkitx.command.route.node.RouteNode;
import org.powernukkitx.command.tree.node.StringNode;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import de.jawollo07.siedler.storage.StorageManager;

public class Elimination extends Command {
    private final SiedlerPlugin plugin;
    private final MessageManager messageManager;
    private final TeamManager teamManager;
    private final StorageManager storageManager;
    private final String prefix;

    public Elimination(SiedlerPlugin plugin) {
        super("elimination", "Verwaltet die Eliminations");
        this.plugin = plugin;
        this.messageManager = new MessageManager();
        this.teamManager = new TeamManager(plugin);
        this.storageManager = plugin.getStorage();
        this.prefix = messageManager.getPrefix("elimination");
        this.setPermission("siedler.command.elimination");
        this.setPermissionMessage(messageManager.getCommandMessage("no-permission"));
        this.enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().then(
            RouteNode.literal("help").exec(context -> {
                sendHelpMessage(context.getSender());
                return CommandResult.success();
            })
        );

        tree.getRoot().then(
            RouteNode.literal("eliminate")
                .permission("siedler.command.elimination.eliminate", messageManager.getCommandMessage("no-permission"))
                .then(RouteNode.argument("team", new StringNode()).exec(context -> {
                    setTeamEliminated(context.getArg("team"), context.getSender());
                    return CommandResult.success();
                }))
        );

        tree.getRoot().then(
            RouteNode.literal("deeliminate")
                .permission("siedler.command.elimination.deeliminate", messageManager.getCommandMessage("no-permission"))
                .then(RouteNode.argument("team", new StringNode()).exec(context -> {
                    setTeamDeEliminated(context.getArg("team"), context.getSender());
                    return CommandResult.success();
                }))
        );

        tree.getRoot().then(
            RouteNode.literal("list")
                .permission("siedler.command.elimination.list", messageManager.getCommandMessage("no-permission"))
                .exec(context -> {
                    listEleminations(context.getSender());
                    return CommandResult.success();
                })
        );
    }

    public void sendHelpMessage(CommandSender sender) {
        if (sender == null) return;
        sender.sendMessage(prefix + "§eElimination-Befehle:");
        sender.sendMessage("§7/elimination help §8- §fZeigt diese Hilfe");
        sender.sendMessage("§7/elimination list §8- §fListet eliminierte Teams");
        sender.sendMessage("§7/elimination eliminate <Team> §8- §fEliminiert ein Team");
        sender.sendMessage("§7/elimination deeliminate <Team> §8- §fHebt eine Eliminierung auf");
    }

    public void listEleminations(CommandSender sender) {
        if (sender == null) return;

        String sql = "SELECT id FROM teams WHERE eliminated = 1 ORDER BY name";
        StringBuilder message = new StringBuilder(prefix).append("Eliminierte Teams:");
        boolean found = false;

        try (PreparedStatement statement = storageManager.getConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String teamId = resultSet.getString("id");
                String teamName = teamManager.getTeamByID(teamId);
                message.append("\n- ").append(teamName == null || teamName.isBlank() ? teamId : teamName);
                found = true;
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not list eliminated teams: " + exception.getMessage());
            sender.sendMessage(prefix + "Die eliminierten Teams konnten nicht geladen werden.");
            return;
        }

        if (!found) message.append("\nKeine Teams sind eliminiert.");
        sender.sendMessage(message.toString());
    }

    public void teamEliminated(String teamId) {
        String teamName = teamManager.getTeamByID(teamId);
        if (teamName == null || teamName.isBlank()) teamName = teamId;
        String message = messageManager.getMessage("elimination", "broadcast") + teamName;
        Server.getInstance().broadcast(message, "siedler.broadcast.elimination");
    }

    public boolean isTeamEliminated(String teamId) {
        if (teamId == null || teamId.isBlank()) return false;

        String sql = "SELECT eliminated FROM teams WHERE id = ?";
        try (PreparedStatement statement = storageManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, teamId.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt("eliminated") == 1;
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not check elimination state for team " + teamId + ": " + exception.getMessage());
            return false;
        }
    }

    public boolean isPlayerEliminated(Player player) throws SQLException {
        if (player == null) return false;

        // getPlayerTeams returns the team's name, not its ID.
        String teamName = teamManager.getPlayerTeams(player.getUniqueId().toString());
        if (teamName == null || teamName.isBlank()) return false;

        Team team = teamManager.getTeamByName(teamName);
        return team != null && team.eliminated() == 1;
    }

    public void setTeamEliminated(String teamName) {
        setTeamEliminated(teamName, null);
    }

    private void setTeamEliminated(String teamName, CommandSender sender) {
        Team team = resolveTeam(teamName, sender);
        if (team == null) return;

        if (team.eliminated() == 1) {
            if (sender != null) sender.sendMessage(prefix + "Dieses Team ist bereits eliminiert.");
            return;
        }

        String sql = "UPDATE teams SET eliminated = 1 WHERE id = ? AND eliminated = 0";
        try (PreparedStatement statement = storageManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, team.id());
            if (statement.executeUpdate() > 0) {
                teamEliminated(team.id());
                if (sender != null) sender.sendMessage(prefix + "Team " + team.name() + " wurde eliminiert.");
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not eliminate team " + team.id() + ": " + exception.getMessage());
            if (sender != null) sender.sendMessage(prefix + "Das Team konnte nicht eliminiert werden.");
        }
    }

    public void setTeamDeEliminated(String teamName) {
        setTeamDeEliminated(teamName, null);
    }

    private void setTeamDeEliminated(String teamName, CommandSender sender) {
        Team team = resolveTeam(teamName, sender);
        if (team == null) return;

        if (team.eliminated() == 0) {
            if (sender != null) sender.sendMessage(prefix + "Dieses Team ist nicht eliminiert.");
            return;
        }

        String sql = "UPDATE teams SET eliminated = 0 WHERE id = ? AND eliminated = 1";
        try (PreparedStatement statement = storageManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, team.id());
            if (statement.executeUpdate() > 0 && sender != null) {
                sender.sendMessage(prefix + "Die Eliminierung von Team " + team.name() + " wurde aufgehoben.");
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not restore team " + team.id() + ": " + exception.getMessage());
            if (sender != null) sender.sendMessage(prefix + "Die Eliminierung konnte nicht aufgehoben werden.");
        }
    }

    private Team resolveTeam(String input, CommandSender sender) {
        if (input == null || input.isBlank()) {
            if (sender != null) sender.sendMessage(prefix + "Bitte gib einen Teamnamen an.");
            return null;
        }

        try {
            return teamManager.getTeamByName(input.trim());
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not find team '" + input + "': " + exception.getMessage());
            if (sender != null) sender.sendMessage(prefix + "Das Team '" + input + "' wurde nicht gefunden.");
            return null;
        }
    }

    public void eliminationBlockModified(String teamId) {
        if (teamId == null || teamId.isBlank() || isTeamEliminated(teamId)) return;

        String sql = "UPDATE teams SET eliminated = 1 WHERE id = ? AND eliminated = 0";
        try (PreparedStatement statement = storageManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, teamId.trim());
            if (statement.executeUpdate() > 0) teamEliminated(teamId.trim());
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not update elimination state for team " + teamId + ": " + exception.getMessage());
        }
    }

    public void playerIsEliminated(Player player) {
        if (player == null) return;
        player.sendMessage(messageManager.getMessage("elimination", "welcome-messsage"));
        player.setGamemode(3);
    }
}
