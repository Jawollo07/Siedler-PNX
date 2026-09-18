package de.jawollo07.siedler.claim;

import de.jawollo07.siedler.storage.StorageManager;

import java.util.ArrayList;
import java.util.List;

import org.powernukkitx.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Utils {
    private final StorageManager storage;

    public Utils(StorageManager storage) {
        if (storage == null) {
            throw new IllegalArgumentException("StorageManager darf nicht null sein");
        }
        this.storage = storage;
    }

    public List<Chunk> get4x4ChunksCentered(double blockX, double blockZ) {
        int centerChunkX = (int) Math.floor(blockX / 16);
        int centerChunkZ = (int) Math.floor(blockZ / 16);
        return get4x4ChunksFromChunk(centerChunkX, centerChunkZ);
    }

    public List<Chunk> get4x4ChunksFromChunk(int centerChunkX, int centerChunkZ) {
        int half = 2;
        int startX = centerChunkX - half;
        int startZ = centerChunkZ - half;
        List<Chunk> chunks = new ArrayList<>(25);

        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < 5; dz++) {
                chunks.add(new Chunk(startX + dx, startZ + dz));
            }
        }

        return chunks;
    }
    public record Chunk(int x, int z) {
    }
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
        if (player.getLocation() == null) {
            throw new IllegalArgumentException("Location darf nicht null sein");
        }

        String world = player.getLevel().getName();

        int chunkX = (int) Math.floor(player.getX() / 16);
        int chunkZ = (int) Math.floor(player.getZ() / 16);

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
        } catch (SQLException e) {
            throw new IllegalStateException("Claim-ID konnte nicht ermittelt werden", e);
        }
    }
}