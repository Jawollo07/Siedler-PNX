package de.jawollo07.siedler.team;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.storage.StorageManager;
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

    public TeamManager(SiedlerPlugin plugin) {
        this.plugin = plugin;
        this.storage = plugin.getStorage();
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

        return getTeamById(id);
    }

    private Team getTeamById(String id) throws SQLException {
        String sql = "SELECT id, name, color, tax_bonus, eliminated, elimination_block, created_at, balance "
                + "FROM teams WHERE id = ?";

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
        String sql = "SELECT id, name, color, tax_bonus, eliminated, elimination_block, created_at, balance "
                + "FROM teams ORDER BY name";

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

        String sql = "SELECT id, name, color, tax_bonus, eliminated, elimination_block, created_at, balance "
                + "FROM teams WHERE name = ?";

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

        String sql = "UPDATE players SET team_id = ? WHERE id = ?";

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, team.id());
            statement.setString(2, playerId);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Adding player to team failed.");
            }
        }

        return true;
    }

    public boolean removePlayerFromTeam(String playerName) throws SQLException {
        if (playerName == null || playerName.isBlank()) {
            throw new IllegalArgumentException("Player name must not be blank");
        }

        String sql = "UPDATE players SET team_id = NULL WHERE last_name = ?";

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, playerName.trim());
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Removing player from team failed.");
            }
        }

        return true;
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
}
