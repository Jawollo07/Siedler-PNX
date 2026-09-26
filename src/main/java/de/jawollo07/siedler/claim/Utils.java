package de.jawollo07.siedler.claim;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.storage.StorageManager;
import org.powernukkitx.Player;
import org.powernukkitx.level.Level;
import org.powernukkitx.level.Position;
import org.powernukkitx.level.format.IChunk;
import org.powernukkitx.entity.Entity;
import org.powernukkitx.entity.passive.EntityVillager;
import org.powernukkitx.entity.passive.EntityVillagerV2;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Shared helpers for claim coordinate and access checks. Claim bounds are chunk coordinates. */
public class Utils {
    private final StorageManager storage;
    private final SiedlerPlugin plugin;
    private final ClaimManager claimManager;
    public Utils(SiedlerPlugin plugin) {
        if (plugin.getStorage() == null) {
            throw new IllegalArgumentException("StorageManager darf nicht null sein");
        }
        this.storage = plugin.getStorage();
        this.plugin = plugin;
        this.claimManager = new ClaimManager(plugin);
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
        return isBlockInClaim(null, x, y, z);
    }

    /**
     * Checks whether the block position belongs to a claim in the given world.
     * The world filter is important because chunk coordinates are reused by every world.
     */
    public boolean isBlockInClaim(String world, Double x, Double y, Double z) {
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
                + "AND min_z <= ? AND max_z >= ? "
                + (world == null ? "" : "AND world = ? ")
                + "LIMIT 1";

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            int index = 1;
            statement.setInt(index++, chunkX);
            statement.setInt(index++, chunkX);
            statement.setInt(index++, chunkZ);
            statement.setInt(index++, chunkZ);
            if (world != null && !world.isBlank()) {
                statement.setString(index, world);
            }
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
    /** Counts villagers in all chunks covered by the given claim. */
    public Integer countVillagerInClaim(String claimID) {
        if (claimID == null || claimID.isBlank()) {
            throw new IllegalArgumentException("Claim-ID darf nicht leer sein");
        }

        Claim claim = claimManager.claimInfoById(claimID);
        if (claim == null) return 0;

        Level level = plugin.getServer().getLevelByName(claim.world());
        if (level == null) return 0;

        int count = 0;
        java.util.Set<String> counted = new java.util.HashSet<>();

        for (int x = claim.min_x(); x <= claim.max_x(); x++) {
            for (int z = claim.min_z(); z <= claim.max_z(); z++) {
                IChunk chunk = level.getProvider().getLoadedChunk(x, z);
                boolean wasLoaded = chunk != null;
                if (!wasLoaded && !level.getProvider().loadChunk(x, z, false)) continue;
                if (!wasLoaded) chunk = level.getProvider().getLoadedChunk(x, z);

                if (chunk != null) {
                    for (Entity entity : chunk.getEntities().values()) {
                        if (entity instanceof EntityVillagerV2
                                || entity instanceof EntityVillager
                                || "minecraft:villager".equals(entity.getIdentifier())) {
                            String key = claim.world() + ":" + entity.getId();
                            if (counted.add(key)) count++;
                        }
                    }
                }

                if (!wasLoaded && chunk != null) chunk.unload(true, true);
            }
        }
        return count;
    }
}

