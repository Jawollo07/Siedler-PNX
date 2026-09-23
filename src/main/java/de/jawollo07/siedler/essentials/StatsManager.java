package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import org.powernukkitx.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent player statistics and playtime tracking.
 */
public final class StatsManager {
    private final SiedlerPlugin plugin;
    private final Map<String, Long> sessionStarts = new ConcurrentHashMap<>();

    public StatsManager(SiedlerPlugin plugin) {
        this.plugin = plugin;
    }

    public record PlayerStats(
            String playerId,
            String playerName,
            int kills,
            int deaths,
            int soldierKills,
            int monsterKills,
            long playtimeSeconds
    ) {
        public double killDeathRatio() {
            return deaths <= 0 ? kills : (double) kills / deaths;
        }
    }

    public record GlobalStats(
            long players,
            long kills,
            long deaths,
            long soldierKills,
            long monsterKills,
            long playtimeSeconds
    ) {}

    public void onJoin(Player player) throws SQLException {
        String id = player.getUniqueId().toString();
        ensurePlayer(player);
        sessionStarts.put(id, System.currentTimeMillis());
    }

    public void onQuit(Player player) throws SQLException {
        String id = player.getUniqueId().toString();
        Long started = sessionStarts.remove(id);
        if (started != null) {
            addPlaytime(id, Math.max(0L, (System.currentTimeMillis() - started) / 1000L));
        }
        ensurePlayer(player);
        updateLastSeen(id);
    }

    public void flushOnlineSessions() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : sessionStarts.entrySet()) {
            long seconds = Math.max(0L, (now - entry.getValue()) / 1000L);
            if (seconds <= 0) {
                continue;
            }
            try {
                addPlaytime(entry.getKey(), seconds);
                sessionStarts.put(entry.getKey(), now);
            } catch (SQLException e) {
                plugin.getLogger().warning("Could not save player playtime: " + e.getMessage());
            }
        }
    }

    public void incrementKill(String playerId) throws SQLException {
        changeStat(playerId, "kills");
    }

    public void incrementDeath(String playerId) throws SQLException {
        changeStat(playerId, "deaths");
    }

    public void incrementSoldierKill(String playerId) throws SQLException {
        changeStat(playerId, "soldier_kills");
    }

    public void incrementMonsterKill(String playerId) throws SQLException {
        changeStat(playerId, "monster_kills");
    }

    public PlayerStats getStats(String playerId) throws SQLException {
        String sql = "SELECT p.id, p.last_name, " +
                "COALESCE(s.kills, 0) AS kills, " +
                "COALESCE(s.deaths, 0) AS deaths, " +
                "COALESCE(s.soldier_kills, 0) AS soldier_kills, " +
                "COALESCE(s.monster_kills, 0) AS monster_kills, " +
                "COALESCE(s.playtime_seconds, 0) AS playtime_seconds " +
                "FROM players p LEFT JOIN player_stats s ON s.player_id = p.id " +
                "WHERE p.id = ?";
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setString(1, playerId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return new PlayerStats(playerId, playerId, 0, 0, 0, 0, 0);
                }
                return map(result);
            }
        }
    }

    public List<PlayerStats> getAllStats() throws SQLException {
        List<PlayerStats> stats = new ArrayList<>();
        String sql = "SELECT p.id, p.last_name, " +
                "COALESCE(s.kills, 0) AS kills, " +
                "COALESCE(s.deaths, 0) AS deaths, " +
                "COALESCE(s.soldier_kills, 0) AS soldier_kills, " +
                "COALESCE(s.monster_kills, 0) AS monster_kills, " +
                "COALESCE(s.playtime_seconds, 0) AS playtime_seconds " +
                "FROM players p LEFT JOIN player_stats s ON s.player_id = p.id " +
                "ORDER BY p.last_name COLLATE NOCASE";
        try (PreparedStatement statement = connection().prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                stats.add(map(result));
            }
        }
        return stats;
    }

    public GlobalStats getGlobalStats() throws SQLException {
        String sql = "SELECT COUNT(*) AS players, " +
                "COALESCE(SUM(kills), 0) AS kills, " +
                "COALESCE(SUM(deaths), 0) AS deaths, " +
                "COALESCE(SUM(soldier_kills), 0) AS soldier_kills, " +
                "COALESCE(SUM(monster_kills), 0) AS monster_kills, " +
                "COALESCE(SUM(playtime_seconds), 0) AS playtime_seconds " +
                "FROM player_stats";
        try (PreparedStatement statement = connection().prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return new GlobalStats(
                    result.getLong("players"),
                    result.getLong("kills"),
                    result.getLong("deaths"),
                    result.getLong("soldier_kills"),
                    result.getLong("monster_kills"),
                    result.getLong("playtime_seconds")
            );
        }
    }

    private PlayerStats map(ResultSet result) throws SQLException {
        return new PlayerStats(
                result.getString("id"),
                result.getString("last_name"),
                result.getInt("kills"),
                result.getInt("deaths"),
                result.getInt("soldier_kills"),
                result.getInt("monster_kills"),
                result.getLong("playtime_seconds")
        );
    }

    private void changeStat(String playerId, String column) throws SQLException {
        if (!"kills".equals(column) && !"deaths".equals(column)
                && !"soldier_kills".equals(column) && !"monster_kills".equals(column)) {
            throw new IllegalArgumentException("Unsupported player statistic: " + column);
        }
        ensureStatsRow(playerId);
        try (PreparedStatement statement = connection().prepareStatement(
                "UPDATE player_stats SET " + column + " = " + column + " + 1 WHERE player_id = ?")) {
            statement.setString(1, playerId);
            statement.executeUpdate();
        }
    }

    private void addPlaytime(String playerId, long seconds) throws SQLException {
        if (seconds <= 0) {
            return;
        }
        ensureStatsRow(playerId);
        try (PreparedStatement statement = connection().prepareStatement(
                "UPDATE player_stats SET playtime_seconds = playtime_seconds + ? WHERE player_id = ?")) {
            statement.setLong(1, seconds);
            statement.setString(2, playerId);
            statement.executeUpdate();
        }
    }

    private void ensurePlayer(Player player) throws SQLException {
        String id = player.getUniqueId().toString();
        long now = System.currentTimeMillis();

        try (PreparedStatement select = connection().prepareStatement(
                "SELECT id FROM players WHERE id = ?")) {
            select.setString(1, id);
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) {
                    try (PreparedStatement update = connection().prepareStatement(
                            "UPDATE players SET last_name = ?, last_seen = ? WHERE id = ?")) {
                        update.setString(1, player.getName());
                        update.setLong(2, now);
                        update.setString(3, id);
                        update.executeUpdate();
                    }
                } else {
                    try (PreparedStatement insert = connection().prepareStatement(
                            "INSERT INTO players (id, last_name, first_join, last_seen) VALUES (?, ?, ?, ?)")) {
                        insert.setString(1, id);
                        insert.setString(2, player.getName());
                        insert.setLong(3, now);
                        insert.setLong(4, now);
                        insert.executeUpdate();
                    }
                }
            }
        }
        ensureStatsRow(id);
    }

    private void ensureStatsRow(String playerId) throws SQLException {
        try (PreparedStatement select = connection().prepareStatement(
                "SELECT player_id FROM player_stats WHERE player_id = ?")) {
            select.setString(1, playerId);
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) {
                    return;
                }
            }
        }

        try (PreparedStatement insert = connection().prepareStatement(
                "INSERT INTO player_stats " +
                        "(player_id, kills, deaths, soldier_kills, monster_kills, playtime_seconds) " +
                        "VALUES (?, 0, 0, 0, 0, 0)")) {
            insert.setString(1, playerId);
            insert.executeUpdate();
        }
    }

    private void updateLastSeen(String playerId) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(
                "UPDATE players SET last_seen = ? WHERE id = ?")) {
            statement.setLong(1, System.currentTimeMillis());
            statement.setString(2, playerId);
            statement.executeUpdate();
        }
    }

    private Connection connection() {
        return plugin.getStorage().getConnection();
    }
}
