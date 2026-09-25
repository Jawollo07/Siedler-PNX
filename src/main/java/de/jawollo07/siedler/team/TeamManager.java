package de.jawollo07.siedler.team;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.storage.StorageManager;
import de.jawollo07.siedler.core.MessageManager;
import de.jawollo07.siedler.eco.EcoManager;

import org.powernukkitx.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TeamManager {
    private final SiedlerPlugin plugin;
    private final StorageManager storage;
    private final MessageManager messageManager;
    private final String prefix;
    private final EcoManager ecoManager;

    public TeamManager(SiedlerPlugin plugin) {
        this.plugin = plugin;
        this.storage = plugin.getStorage();
        this.messageManager = new MessageManager();
        this.ecoManager = new EcoManager(plugin);
        this.prefix = messageManager.getPrefix("team");
    }

    private Team mapTeam(ResultSet resultSet) throws SQLException {
        return new Team(
                resultSet.getString("id"),
                resultSet.getString("name"),
                resultSet.getString("color"),
                resultSet.getInt("tax_bonus"),
                resultSet.getInt("eliminated"),
                resultSet.getString("elimination_block"),
                resultSet.getLong("created_at"),
                resultSet.getInt("balance")
        );
    }

    public Team createTeam(String name, String color) throws SQLException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Team name must not be blank");
        }

        String trimmedName = name.trim();
        String normalizedColor = (color == null || color.isBlank()) ? "WHITE" : color.trim().toUpperCase(Locale.ROOT);

        int maxTeams = plugin.getConfig().getInt("teams.max-teams", 0);
        if (maxTeams > 0 && getTeams().size() >= maxTeams) {
            throw new IllegalStateException("The maximum number of teams has been reached.");
        }

        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO teams (id, name, color, tax_bonus, eliminated, created_at) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, trimmedName);
            statement.setString(3, normalizedColor);
            statement.setInt(4, 1);
            statement.setInt(5, 0);
            statement.setLong(6, System.currentTimeMillis());
            statement.executeUpdate();
        }
        ecoManager.teamCreation(id);
        return getTeamById(id);
    }
    public String getPlayerTeams(String player_id) {
        if (player_id == null || player_id.isBlank()) {
            return null;
        }

        String sql = "SELECT team_id FROM players WHERE id = ?";
        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, player_id.trim());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                String teamId = resultSet.getString("team_id");
                if (teamId == null || teamId.isBlank()) {
                    return null;
                }

                return getTeamByID(teamId);
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not load player team for player " + player_id + ": " + exception.getMessage());
            return null;
        }
    }
    private Team getTeamById(String id) throws SQLException {
        String sql = "SELECT t.id, t.name, t.color, (t.tax_bonus + COALESCE((SELECT SUM(bs.amount) FROM team_bonus_sources bs WHERE bs.team_id = t.id AND bs.permanent = 1), 0)) AS tax_bonus, t.eliminated, t.elimination_block, t.created_at, COALESCE((SELECT tm.balance FROM team_money tm WHERE tm.team_id = t.id LIMIT 1), 0) AS balance "
                + "FROM teams t WHERE t.id = ?";

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Created team could not be loaded: " + id);
                }
                return mapTeam(resultSet);
            }
        }
    }

    public List<Team> getTeams() throws SQLException {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT t.id, t.name, t.color, (t.tax_bonus + COALESCE((SELECT SUM(bs.amount) FROM team_bonus_sources bs WHERE bs.team_id = t.id AND bs.permanent = 1), 0)) AS tax_bonus, t.eliminated, t.elimination_block, t.created_at, COALESCE((SELECT tm.balance FROM team_money tm WHERE tm.team_id = t.id LIMIT 1), 0) AS balance "
                + "FROM teams t ORDER BY t.name";

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                teams.add(mapTeam(resultSet));
            }
        }

        return teams;
    }

    public Team getTeamByName(String name) throws SQLException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Team name must not be blank");
        }

        String sql = "SELECT t.id, t.name, t.color, (t.tax_bonus + COALESCE((SELECT SUM(bs.amount) FROM team_bonus_sources bs WHERE bs.team_id = t.id AND bs.permanent = 1), 0)) AS tax_bonus, t.eliminated, t.elimination_block, t.created_at, COALESCE((SELECT tm.balance FROM team_money tm WHERE tm.team_id = t.id LIMIT 1), 0) AS balance "
                + "FROM teams t WHERE t.name = ?";

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, name.trim());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Team not found: " + name);
                }
                return mapTeam(resultSet);
            }
        }
    }

    public boolean deleteTeam(String name) throws SQLException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Team name must not be blank");
        }

        Team team = getTeamByName(name);
        String sql = "DELETE FROM teams WHERE name = ?";

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, team.name().trim());
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting team failed.");
            }
        }

        return true;
    }

    public Team updateTeam(String name, String newName, String newColor) throws SQLException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Team name must not be blank");
        }

        Team team = getTeamByName(name);
        String finalName = (newName == null || newName.isBlank()) ? team.name() : newName.trim();
        String finalColor = (newColor == null || newColor.isBlank()) ? team.color() : newColor.trim().toUpperCase(Locale.ROOT);

        String sql = "UPDATE teams SET name = ?, color = ? WHERE name = ?";

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, finalName);
            statement.setString(2, finalColor);
            statement.setString(3, team.name());
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating team failed.");
            }
        }

        return getTeamByName(finalName);
    }
    public boolean isPlayerInTeam(Player player) {
        if (player == null) {
            return false;
        }

        String sql = "SELECT team_id FROM players WHERE id = ? AND team_id IS NOT NULL";
        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, player.getUniqueId().toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getString("team_id") != null;
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not check team membership for player "
                    + player.getName() + ": " + exception.getMessage());
            return false;
        }
    }
    public boolean addPlayerToTeam(String playerName, String teamName) throws SQLException {
        if (playerName == null || playerName.isBlank()) {
            throw new IllegalArgumentException("Player name must not be blank");
        }
        if (teamName == null || teamName.isBlank()) {
            throw new IllegalArgumentException("Team name must not be blank");
        }

        Team team = getTeamByName(teamName);
        Player onlinePlayer = plugin.getServer().getOnlinePlayers().values().stream()
                .filter(player -> player.getName().equalsIgnoreCase(playerName.trim()))
                .findFirst()
                .orElse(null);
        if (onlinePlayer == null) {
            throw new SQLException("Player must be online before being added to a team.");
        }
        String playerId = onlinePlayer.getUniqueId().toString();
        Team currentTeam = getTeamForPlayer(playerId);
        if (currentTeam != null) {
            throw new IllegalStateException(
                    "Player is already a member of team '" + currentTeam.name() + "'."
            );
        }

        int maxPlayersPerTeam = plugin.getConfig().getInt("teams.max-players-per-team", 0);
        if (maxPlayersPerTeam > 0 && getTeamMemberCount(team.id()) >= maxPlayersPerTeam) {
            throw new IllegalStateException("Team '" + team.name() + "' is full.");
        }

        long now = System.currentTimeMillis();
        String upsertPlayerSql = "UPDATE players SET last_name = ?, last_seen = ? WHERE id = ?";
        try (PreparedStatement statement = storage.getConnection().prepareStatement(upsertPlayerSql)) {
            statement.setString(1, onlinePlayer.getName());
            statement.setLong(2, now);
            statement.setString(3, playerId);
            if (statement.executeUpdate() == 0) {
                String insertPlayerSql = "INSERT INTO players (id, last_name, first_join, last_seen) VALUES (?, ?, ?, ?)";
                try (PreparedStatement insertStatement = storage.getConnection()
                        .prepareStatement(insertPlayerSql)) {
                    insertStatement.setString(1, playerId);
                    insertStatement.setString(2, onlinePlayer.getName());
                    insertStatement.setLong(3, now);
                    insertStatement.setLong(4, now);
                    insertStatement.executeUpdate();
                }
            }
        }

        String sql = "UPDATE players SET team_id = ? WHERE id = ? AND team_id IS NULL";

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, team.id());
            statement.setString(2, playerId);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                Team assignedTeam = getTeamForPlayer(playerId);
                if (assignedTeam != null) {
                    throw new IllegalStateException(
                            "Player is already a member of team '" + assignedTeam.name() + "'."
                    );
                }
                throw new SQLException("Adding player to team failed.");
            }
        }

        return true;
    }

    public boolean removePlayerFromTeam(String playerName) throws SQLException {
        if (playerName == null || playerName.isBlank()) {
            throw new IllegalArgumentException("Player name must not be blank");
        }

        Player onlinePlayer = plugin.getServer().getOnlinePlayers().values().stream()
                .filter(player -> player.getName().equalsIgnoreCase(playerName.trim()))
                .findFirst()
                .orElse(null);
        if (onlinePlayer == null) {
            throw new SQLException("Player must be online before being removed from a team.");
        }

        String sql = "UPDATE players SET team_id = NULL WHERE id = ? AND team_id IS NOT NULL";

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, onlinePlayer.getUniqueId().toString());
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Player is not a member of a team.");
            }
        }

        return true;
    }

    public Team getTeamForPlayer(String playerId) throws SQLException {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("Player ID must not be blank");
        }

        String sql = "SELECT t.id, t.name, t.color, (t.tax_bonus + COALESCE((SELECT SUM(bs.amount) FROM team_bonus_sources bs WHERE bs.team_id = t.id AND bs.permanent = 1), 0)) AS tax_bonus, t.eliminated, "
                + "t.elimination_block, t.created_at, "
                + "COALESCE((SELECT tm.balance FROM team_money tm WHERE tm.team_id = t.id LIMIT 1), 0) AS balance "
                + "FROM players p JOIN teams t ON t.id = p.team_id WHERE p.id = ?";

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, playerId.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapTeam(resultSet) : null;
            }
        }
    }

    private int getTeamMemberCount(String teamId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM players WHERE team_id = ?";
        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, teamId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    public boolean setTeamColor(String teamName, String color) throws SQLException {
        if (teamName == null || teamName.isBlank()) {
            throw new IllegalArgumentException("Team name must not be blank");
        }

        getTeamByName(teamName);
        String sql = "UPDATE teams SET color = ? WHERE name = ?";

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, (color == null || color.isBlank()) ? "WHITE" : color.trim().toUpperCase(Locale.ROOT));
            statement.setString(2, teamName.trim());
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating team color failed.");
            }
        }

        return true;
    }
    public boolean notifyAllTeamMembers(String team_id, String message) {
        if (team_id == null || team_id.isBlank()) {
            return false;
        }
        if (message == null || message.isBlank()){
            return false;
        }
        String sql = "SELECT id FROM players WHERE team_id = ?";
        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, team_id.trim());

            try (ResultSet resultSet = statement.executeQuery()) {
                List<String> memberIds = new ArrayList<>();
                while (resultSet.next()) {
                    memberIds.add(resultSet.getString("id"));
                }

                for (Player player : plugin.getServer().getOnlinePlayers().values()) {
                    if (memberIds.contains(player.getUniqueId().toString())) {
                        player.sendMessage(prefix + message);
                    }
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not notify team members: " + exception.getMessage());
            return false;
        }

        return true;
    }
    public String getTeamByID(String team_id) {
        if (team_id == null || team_id.isBlank()) {
            return null;
        }

        String sql = "SELECT name FROM teams WHERE id = ?";
        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, team_id.trim());

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("name") : null;
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not load team: " + exception.getMessage());
            return null;
        }
    }
}
