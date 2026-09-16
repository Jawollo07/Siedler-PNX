package de.jawollo07.siedler.claim;

import de.jawollo07.siedler.storage.StorageManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.powernukkitx.Player;

import de.jawollo07.siedler.SiedlerPlugin;

public class ClaimManager {
    private final StorageManager storage;
    private final SiedlerPlugin plugin;
    public ClaimManager(SiedlerPlugin plugin) {
        this.plugin = plugin;
        this.storage = plugin.getStorage();
    }
    private Claim ClaimMap(ResultSet resultSet) throws SQLException {
        return new Claim(
            resultSet.getString("id"),
            resultSet.getString("team_id"),
            resultSet.getString("world"),
            resultSet.getInt("min_x"),
            resultSet.getInt("min_z"),
            resultSet.getInt("max_x"),
            resultSet.getInt("max_z")
        );
    }

    public Claim ClaimInfo(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player darf nicht null sein");
        }

        String sql = "SELECT id, team_id, world, min_x, min_z, max_x, max_z "
                + "FROM claims WHERE world = ? AND min_x <= ? AND max_x >= ? "
                + "AND min_z <= ? AND max_z >= ? LIMIT 1";
        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, player.getLevel().getName());
            statement.setInt(2, player.getChunkX());
            statement.setInt(3, player.getChunkX());
            statement.setInt(4, player.getChunkZ());
            statement.setInt(5, player.getChunkZ());

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? ClaimMap(resultSet) : null;
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Claim-Info konnte nicht geladen werden: " + exception.getMessage());
            throw new IllegalStateException("Claim-Info konnte nicht geladen werden", exception);
        }
    }

    public Claim ClaimInfo(String claimId) {
        if (claimId == null || claimId.isBlank()) {
            throw new IllegalArgumentException("Claim-ID darf nicht leer sein");
        }

        String sql = "SELECT id, team_id, world, min_x, min_z, max_x, max_z "
                + "FROM claims WHERE id = ?";
        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, claimId.trim());

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? ClaimMap(resultSet) : null;
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Claim-Info konnte nicht geladen werden: " + exception.getMessage());
            throw new IllegalStateException("Claim-Info konnte nicht geladen werden", exception);
        }
    }

    public boolean deleteClaim(Player player) {
        Claim claim = ClaimInfo(player);
        if (claim == null) {
            player.sendMessage("Hier befindet sich kein Claim");
            return false;
        }

        boolean deleted = deleteClaim(claim.id());
        if (deleted) {
            player.sendMessage("Claim erfolgreich gelöscht");
        }
        return deleted;
    }

    public boolean deleteClaim(String claimId) {
        if (claimId == null || claimId.isBlank()) {
            throw new IllegalArgumentException("Claim-ID darf nicht leer sein");
        }

        String sql = "DELETE FROM claims WHERE id = ?";
        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, claimId.trim());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            plugin.getLogger().warning("Claim konnte nicht gelöscht werden: " + exception.getMessage());
            throw new IllegalStateException("Claim konnte nicht gelöscht werden", exception);
        }
    }

    public Claim setClaim(String team, Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player darf nicht null sein");
        }

        if (team == null || team.isBlank()) {
            player.sendMessage("Der Team Name darf nicht leer sein");
            throw new IllegalArgumentException("Team Name darf nicht leer sein");
        }

        int centerChunkX = player.getChunkX();
        int centerChunkZ = player.getChunkZ();
        int minX = centerChunkX - 2;
        int minZ = centerChunkZ - 2;
        int maxX = centerChunkX + 2;
        int maxZ = centerChunkZ + 2;
        String world = player.getLevel().getName();

        try {
            String teamSql = "SELECT id FROM teams WHERE name = ?";
            String teamId;
            try (PreparedStatement statement = storage.getConnection().prepareStatement(teamSql)) {
                statement.setString(1, team.trim());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        player.sendMessage("Das Team wurde nicht gefunden");
                        return null;
                    }
                    teamId = resultSet.getString("id");
                }
            }

            String overlapSql = "SELECT 1 FROM claims "
                    + "WHERE world = ? AND min_x <= ? AND max_x >= ? "
                    + "AND min_z <= ? AND max_z >= ? LIMIT 1";
            try (PreparedStatement statement = storage.getConnection().prepareStatement(overlapSql)) {
                statement.setString(1, world);
                statement.setInt(2, maxX);
                statement.setInt(3, minX);
                statement.setInt(4, maxZ);
                statement.setInt(5, minZ);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        player.sendMessage("Der Bereich überschneidet sich mit einem bestehenden Claim");
                        return null;
                    }
                }
            }

            String id = UUID.randomUUID().toString();
            String insertSql = "INSERT INTO claims "
                    + "(id, team_id, world, min_x, min_z, max_x, max_z, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = storage.getConnection().prepareStatement(insertSql)) {
                statement.setString(1, id);
                statement.setString(2, teamId);
                statement.setString(3, world);
                statement.setInt(4, minX);
                statement.setInt(5, minZ);
                statement.setInt(6, maxX);
                statement.setInt(7, maxZ);
                statement.setLong(8, System.currentTimeMillis());
                statement.executeUpdate();
            }

            Claim claim = new Claim(id, teamId, world, minX, minZ, maxX, maxZ);
            player.sendMessage("Claim erfolgreich erstellt");
            return claim;
        } catch (SQLException exception) {
            plugin.getLogger().warning("Claim konnte nicht erstellt werden: " + exception.getMessage());
            throw new IllegalStateException("Claim konnte nicht erstellt werden", exception);
        }
    }
}
