package de.jawollo07.siedler.claim;

import de.jawollo07.siedler.storage.StorageManager;

import java.util.ArrayList;
import java.util.List;
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
}