package de.jawollo07.siedler.claim;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import de.jawollo07.siedler.storage.StorageManager;
import org.powernukkitx.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Manages Siedler claims.
 *
 * <p>Claim coordinates are stored as chunk coordinates, not block coordinates.
 * A newly created claim covers a 5x5 chunk area centered on the player.</p>
 */
public class ClaimManager {
    private static final int CLAIM_RADIUS_CHUNKS = 2;

    private final StorageManager storage;
    private final SiedlerPlugin plugin;
    private final MessageManager messageManager;
    private final String prefix;

    public ClaimManager(SiedlerPlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin darf nicht null sein");
        }
        if (plugin.getStorage() == null) {
            throw new IllegalArgumentException("StorageManager darf nicht null sein");
        }

        this.plugin = plugin;
        this.storage = plugin.getStorage();
        this.messageManager = new MessageManager();
        this.prefix = messageManager.getPrefix("claim");
    }

    private Claim mapClaim(ResultSet resultSet) throws SQLException {
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

    /**
     * Returns the claim containing the player's current chunk, or null.
     */
    public Claim claimInfoByPlayer(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player darf nicht null sein");
        }
        if (player.getLevel() == null) {
            throw new IllegalArgumentException("Level darf nicht null sein");
        }

        return getClaimAtChunk(
                player.getLevel().getName(),
                player.getChunkX(),
                player.getChunkZ()
        );
    }

    /**
     * Legacy method name retained for compatibility.
     */
    public Claim ClaimInfoByPlayer(Player player) {
        return claimInfoByPlayer(player);
    }

    /**
     * Returns the claim with the given ID, or null when it does not exist.
     */
    public Claim claimInfoById(String claimId) {
        String id = requireClaimId(claimId);

        String sql = "SELECT id, team_id, world, min_x, min_z, max_x, max_z "
                + "FROM claims WHERE id = ? LIMIT 1";

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapClaim(resultSet) : null;
            }
        } catch (SQLException exception) {
            logSqlError("Claim-Info konnte nicht geladen werden", exception);
            throw new IllegalStateException("Claim-Info konnte nicht geladen werden", exception);
        }
    }

    /**
     * Legacy method name retained for compatibility.
     */
    public Claim ClaimInfoByID(String claimId) {
        return claimInfoById(claimId);
    }

    /**
     * Finds a claim at an exact chunk position in a world.
     */
    public Claim getClaimAtChunk(String world, int chunkX, int chunkZ) {
        if (world == null || world.isBlank()) {
            throw new IllegalArgumentException("Welt darf nicht leer sein");
        }

        String sql = "SELECT id, team_id, world, min_x, min_z, max_x, max_z "
                + "FROM claims "
                + "WHERE world = ? "
                + "AND min_x <= ? AND max_x >= ? "
                + "AND min_z <= ? AND max_z >= ? "
                + "LIMIT 1";

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setInt(2, chunkX);
            statement.setInt(3, chunkX);
            statement.setInt(4, chunkZ);
            statement.setInt(5, chunkZ);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapClaim(resultSet) : null;
            }
        } catch (SQLException exception) {
            logSqlError("Claim-Info konnte nicht geladen werden", exception);
            throw new IllegalStateException("Claim-Info konnte nicht geladen werden", exception);
        }
    }

    /**
     * Deletes the claim at the player's current position.
     *
     * <p>The player must belong to the claim's team. This prevents a player
     * from deleting another team's claim through the command layer.</p>
     */
    public boolean deleteClaim(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player darf nicht null sein");
        }

        Claim claim = claimInfoByPlayer(player);
        if (claim == null) {
            player.sendMessage(prefix + messageManager.getMessage("claim", "here-is-no-claim"));
            return false;
        }

        String playerId = player.getUniqueId().toString();
        if (!hasTeamAccess(playerId, claim.teamID())) {
            player.sendMessage(prefix + messageManager.getMessage(
                    "claim", "protection.block-breaking-not-allowed"
            ));
            return false;
        }

        boolean deleted = deleteClaim(claim.id());
        if (deleted) {
            player.sendMessage(prefix + messageManager.getMessage("claim", "succesfull-deleted"));
        }
        return deleted;
    }

    /**
     * Deletes a claim by ID.
     *
     * <p>This is the low-level deletion method. Callers that operate on
     * behalf of a player should use {@link #deleteClaim(Player)}.</p>
     */
    public boolean deleteClaim(String claimId) {
        String id = requireClaimId(claimId);

        String sql = "DELETE FROM claims WHERE id = ?";
        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            logSqlError("Claim konnte nicht gelöscht werden", exception);
            throw new IllegalStateException("Claim konnte nicht gelöscht werden", exception);
        }
    }

    /**
     * Creates the standard 5x5 chunk claim around the player.
     */
    public Claim setClaim(String team, Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player darf nicht null sein");
        }
        if (player.getLevel() == null) {
            throw new IllegalArgumentException("Level darf nicht null sein");
        }
        if (team == null || team.isBlank()) {
            player.sendMessage(prefix + messageManager.getMessage("team", "team-name-empty"));
            throw new IllegalArgumentException("Team Name darf nicht leer sein");
        }

        String teamName = team.trim();
        String world = player.getLevel().getName();

        int centerChunkX = player.getChunkX();
        int centerChunkZ = player.getChunkZ();

        int minX = centerChunkX - CLAIM_RADIUS_CHUNKS;
        int minZ = centerChunkZ - CLAIM_RADIUS_CHUNKS;
        int maxX = centerChunkX + CLAIM_RADIUS_CHUNKS;
        int maxZ = centerChunkZ + CLAIM_RADIUS_CHUNKS;

        try {
            String teamId = findTeamId(teamName);
            if (teamId == null) {
                player.sendMessage(prefix + messageManager.getMessage("team", "team-not-found"));
                return null;
            }

            // A player can never create a second overlapping claim, including
            // one belonging to the same team.
            if (claimOverlaps(world, minX, minZ, maxX, maxZ)) {
                player.sendMessage(prefix + messageManager.getMessage(
                        "claim", "claim-in-other-claim"
                ));
                return null;
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
            player.sendMessage(prefix + messageManager.getMessage("claim", "succesfull-created"));
            return claim;
        } catch (SQLException exception) {
            logSqlError("Claim konnte nicht erstellt werden", exception);
            throw new IllegalStateException("Claim konnte nicht erstellt werden", exception);
        }
    }

    /**
     * Returns the four stored claim bounds: minX, minZ, maxX, maxZ.
     */
    public Integer[] getClaimPosition(String claimId) throws SQLException {
        String id = requireClaimId(claimId);

        String sql = "SELECT min_x, min_z, max_x, max_z FROM claims WHERE id = ? LIMIT 1";
        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return new Integer[] {
                        resultSet.getInt("min_x"),
                        resultSet.getInt("min_z"),
                        resultSet.getInt("max_x"),
                        resultSet.getInt("max_z")
                };
            }
        }
    }

    /**
     * Legacy typo retained for source compatibility.
     */
    @Deprecated
    public Integer[] getClaimPostion(String claimID) throws SQLException {
        return getClaimPosition(claimID);
    }

    private String findTeamId(String teamName) throws SQLException {
        String sql = "SELECT id FROM teams WHERE name = ? LIMIT 1";
        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, teamName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("id") : null;
            }
        }
    }

    private boolean claimOverlaps(
            String world,
            int minX,
            int minZ,
            int maxX,
            int maxZ
    ) throws SQLException {
        String sql = "SELECT 1 FROM claims "
                + "WHERE world = ? "
                + "AND min_x <= ? AND max_x >= ? "
                + "AND min_z <= ? AND max_z >= ? "
                + "LIMIT 1";

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setInt(2, maxX);
            statement.setInt(3, minX);
            statement.setInt(4, maxZ);
            statement.setInt(5, minZ);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean hasTeamAccess(String playerId, String teamId) throws SQLException {
        String sql = "SELECT 1 FROM players "
                + "WHERE id = ? AND team_id = ? LIMIT 1";

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, playerId);
            statement.setString(2, teamId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private String requireClaimId(String claimId) {
        if (claimId == null || claimId.isBlank()) {
            throw new IllegalArgumentException("Claim-ID darf nicht leer sein");
        }
        return claimId.trim();
    }

    private void logSqlError(String message, SQLException exception) {
        plugin.getLogger().warning(message + ": " + exception.getMessage());
    }
}
