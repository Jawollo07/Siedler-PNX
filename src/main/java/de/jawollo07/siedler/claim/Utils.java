package de.jawollo07.siedler.claim;

import de.jawollo07.siedler.storage.StorageManager;
import org.powernukkitx.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Shared helpers for claim coordinate and access checks. Claim bounds are chunk coordinates. */
public class Utils {
    private final StorageManager storage;

    public Utils(StorageManager storage) {
        if (storage == null) {
            throw new IllegalArgumentException("StorageManager darf nicht null sein");
        }
        this.storage = storage;
    }

    /** Converts block coordinates to chunk coordinates using floor (also correct below zero). */
    public List<Chunk> get4x4ChunksCentered(double blockX, double blockZ) {
        int centerChunkX = (int) Math.floor(blockX / 16.0);
        int centerChunkZ = (int) Math.floor(blockZ / 16.0);
        return get4x4ChunksFromChunk(centerChunkX, centerChunkZ);
    }

    /**
     * Returns the project's existing 5x5 claim footprint (25 chunks), centered on a chunk.
     * The legacy method name is retained for source compatibility.
     */
    public List<Chunk> get4x4ChunksFromChunk(int centerChunkX, int centerChunkZ) {
        List<Chunk> chunks = new ArrayList<>(25);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                chunks.add(new Chunk(centerChunkX + dx, centerChunkZ + dz));
            }
        }
        return chunks;
    }

    public record Chunk(int x, int z) { }

    public boolean hasAccess(String playerId, String claimId) {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("Spieler-ID darf nicht leer sein");
        }
        if (claimId == null || claimId.isBlank()) {
            throw new IllegalArgumentException("Claim-ID darf nicht leer sein");
        }

        String sql = "SELECT 1 FROM players p "
                + "JOIN claims c ON c.team_id = p.team_id "
                + "WHERE p.id = ? AND c.id = ? LIMIT 1";
        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, playerId.trim());
            statement.setString(2, claimId.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Claim-Zugriff konnte nicht geprüft werden", exception);
        }
    }

    public String get_claimID(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player darf nicht null sein");
        }
        if (player.getLevel() == null) {
            throw new IllegalArgumentException("Level darf nicht null sein");
        }

        String world = player.getLevel().getName();
        int chunkX = player.getChunkX();
        int chunkZ = player.getChunkZ();
        String sql = "SELECT id FROM claims "
                + "WHERE world = ? AND min_x <= ? AND max_x >= ? "
                + "AND min_z <= ? AND max_z >= ? LIMIT 1";

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setInt(2, chunkX);
            statement.setInt(3, chunkX);
            statement.setInt(4, chunkZ);
            statement.setInt(5, chunkZ);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("id") : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Claim-ID konnte nicht ermittelt werden", exception);
        }
    }
    public boolean isBlockInClaim(Double x, Double y, Double z) {
        if (x == null || y == null || z == null) {
            throw new IllegalArgumentException("Koordinaten dürfen nicht null sein");
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Koordinaten müssen endlich sein");
        }

        int chunkX = (int) Math.floor(x / 16.0);
        int chunkZ = (int) Math.floor(z / 16.0);
        String sql = "SELECT 1 FROM claims "
                + "WHERE min_x <= ? AND max_x >= ? "
                + "AND min_z <= ? AND max_z >= ? LIMIT 1";

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setInt(1, chunkX);
            statement.setInt(2, chunkX);
            statement.setInt(3, chunkZ);
            statement.setInt(4, chunkZ);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Es konnte nicht geprüft werden, ob der Block in einem Claim liegt", exception);
        }
    }
    public String getClaimTeam(String claimID) {
        if (claimID == null || claimID.isBlank()) {
            throw new IllegalArgumentException("Claim-ID darf nicht leer sein");
        }

        String sql = "SELECT team_id FROM claims WHERE id = ? LIMIT 1";
        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, claimID.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("team_id") : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Claim-Team konnte nicht ermittelt werden", exception);
        }
    }
}
