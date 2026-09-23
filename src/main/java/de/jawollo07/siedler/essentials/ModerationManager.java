package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.storage.StorageManager;
import org.powernukkitx.Player;

import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ModerationManager {
    public record Punishment(String id, String playerId, String playerName, String type,
                             String reason, String moderatorName, long createdAt,
                             Long expiresAt, boolean active) {}

    private final SiedlerPlugin plugin;
    private final StorageManager storage;

    public ModerationManager(SiedlerPlugin plugin) {
        this.plugin = plugin;
        this.storage = plugin.getStorage();
    }

    public synchronized Punishment warn(String playerId, String playerName, String reason,
                                        String moderatorId, String moderatorName) throws SQLException {
        return create(playerId, playerName, "WARN", reason, moderatorId, moderatorName, null);
    }

    public synchronized Punishment kick(String playerId, String playerName, String reason,
                                        String moderatorId, String moderatorName) throws SQLException {
        Punishment punishment = create(playerId, playerName, "KICK", reason, moderatorId, moderatorName, null);
        Player player = findOnline(playerName);
        if (player != null) {
            disconnect(player, "Du wurdest vom Server gekickt.\nGrund: " + reason);
        }
        return punishment;
    }

    public synchronized Punishment ban(String playerId, String playerName, String reason,
                                       String moderatorId, String moderatorName) throws SQLException {
        Punishment punishment = create(playerId, playerName, "BAN", reason, moderatorId, moderatorName, null);
        Player player = findOnline(playerName);
        if (player != null) {
            disconnect(player, "Du wurdest vom Server gebannt.\nGrund: " + reason);
        }
        return punishment;
    }

    public synchronized Punishment tempBan(String playerId, String playerName, String reason,
                                            String moderatorId, String moderatorName, long durationMillis) throws SQLException {
        if (durationMillis <= 0) {
            throw new IllegalArgumentException("Die Bann-Dauer muss größer als 0 sein.");
        }
        long expiresAt = System.currentTimeMillis() + durationMillis;
        Punishment punishment = create(playerId, playerName, "TEMPBAN", reason, moderatorId, moderatorName, expiresAt);
        Player player = findOnline(playerName);
        if (player != null) {
            disconnect(player, "Du wurdest temporär gebannt.\nGrund: " + reason);
        }
        return punishment;
    }

    public synchronized boolean unban(String playerId, String moderatorId, String reason) throws SQLException {
        try (PreparedStatement statement = storage.getConnection().prepareStatement(
                "UPDATE moderation_punishments SET active = 0, revoked_at = ?, revoked_by = ?, revoke_reason = ? " +
                "WHERE player_id = ? AND active = 1 AND type IN ('BAN', 'TEMPBAN')")) {
            statement.setLong(1, System.currentTimeMillis());
            statement.setString(2, moderatorId);
            statement.setString(3, reason == null ? "Unban" : reason);
            statement.setString(4, playerId);
            return statement.executeUpdate() > 0;
        }
    }

    public synchronized String findPlayerIdByName(String playerName) throws SQLException {
        try (PreparedStatement statement = storage.getConnection().prepareStatement(
                "SELECT id FROM players WHERE last_name = ? COLLATE NOCASE ORDER BY last_seen DESC LIMIT 1")) {
            statement.setString(1, playerName);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getString("id") : null;
            }
        }
    }

    public synchronized Punishment getActiveBan(String playerId) throws SQLException {
        long now = System.currentTimeMillis();
        try (PreparedStatement statement = storage.getConnection().prepareStatement(
                "SELECT id, player_id, player_name, type, reason, moderator_name, created_at, expires_at, active " +
                "FROM moderation_punishments " +
                "WHERE player_id = ? AND active = 1 AND type IN ('BAN', 'TEMPBAN') " +
                "ORDER BY created_at DESC LIMIT 1")) {
            statement.setString(1, playerId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return null;
                Long expires = rs.getObject("expires_at") == null ? null : rs.getLong("expires_at");
                if (expires != null && expires <= now) {
                    deactivate(rs.getString("id"));
                    return null;
                }
                return map(rs);
            }
        }
    }

    public synchronized boolean enforce(Player player) throws SQLException {
        Punishment punishment = getActiveBan(player.getUniqueId().toString());
        if (punishment == null) return false;
        String expiry = punishment.expiresAt() == null ? "" :
                "\\nAblauf: " + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date(punishment.expiresAt()));
        disconnect(player, "Du bist gebannt.\\nGrund: " + punishment.reason() + expiry);
        return true;
    }

    public synchronized List<Punishment> getHistory(String playerId) throws SQLException {
        List<Punishment> result = new ArrayList<>();
        try (PreparedStatement statement = storage.getConnection().prepareStatement(
                "SELECT id, player_id, player_name, type, reason, moderator_name, created_at, expires_at, active " +
                "FROM moderation_punishments WHERE player_id = ? ORDER BY created_at DESC")) {
            statement.setString(1, playerId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
        }
        return result;
    }

    private Punishment create(String playerId, String playerName, String type, String reason,
                              String moderatorId, String moderatorName, Long expiresAt) throws SQLException {
        ensurePlayer(playerId, playerName);
        String id = UUID.randomUUID().toString();
        try (PreparedStatement statement = storage.getConnection().prepareStatement(
                "INSERT INTO moderation_punishments " +
                "(id, player_id, player_name, type, reason, moderator_id, moderator_name, created_at, expires_at, active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)")) {
            statement.setString(1, id);
            statement.setString(2, playerId);
            statement.setString(3, playerName);
            statement.setString(4, type);
            statement.setString(5, reason == null || reason.isBlank() ? "Kein Grund angegeben" : reason);
            statement.setString(6, moderatorId);
            statement.setString(7, moderatorName);
            statement.setLong(8, System.currentTimeMillis());
            if (expiresAt == null) statement.setNull(9, Types.BIGINT);
            else statement.setLong(9, expiresAt);
            statement.executeUpdate();
        }
        return getById(id);
    }

    private Punishment getById(String id) throws SQLException {
        try (PreparedStatement statement = storage.getConnection().prepareStatement(
                "SELECT id, player_id, player_name, type, reason, moderator_name, created_at, expires_at, active " +
                "FROM moderation_punishments WHERE id = ?")) {
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new SQLException("Moderationsmaßnahme konnte nicht geladen werden.");
                return map(rs);
            }
        }
    }

    private void deactivate(String id) throws SQLException {
        try (PreparedStatement statement = storage.getConnection().prepareStatement(
                "UPDATE moderation_punishments SET active = 0 WHERE id = ?")) {
            statement.setString(1, id);
            statement.executeUpdate();
        }
    }

    private Punishment map(ResultSet rs) throws SQLException {
        Long expires = rs.getObject("expires_at") == null ? null : rs.getLong("expires_at");
        return new Punishment(rs.getString("id"), rs.getString("player_id"),
                rs.getString("player_name"), rs.getString("type"), rs.getString("reason"),
                rs.getString("moderator_name"), rs.getLong("created_at"), expires,
                rs.getInt("active") == 1);
    }

    private void ensurePlayer(String playerId, String playerName) throws SQLException {
        try (PreparedStatement check = storage.getConnection().prepareStatement(
                "SELECT id FROM players WHERE id = ?")) {
            check.setString(1, playerId);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) return;
            }
        }
        try (PreparedStatement insert = storage.getConnection().prepareStatement(
                "INSERT INTO players (id, last_name, first_join, last_seen) VALUES (?, ?, ?, ?)")) {
            long now = System.currentTimeMillis();
            insert.setString(1, playerId);
            insert.setString(2, playerName);
            insert.setLong(3, now);
            insert.setLong(4, now);
            insert.executeUpdate();
        }
    }

    private Player findOnline(String name) {
        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            if (player.getName().equalsIgnoreCase(name)) return player;
        }
        return null;
    }

    private void disconnect(Player player, String reason) {
        try {
            Method method;
            try {
                method = player.getClass().getMethod("kick", String.class);
            } catch (NoSuchMethodException ignored) {
                method = player.getClass().getMethod("disconnect", String.class);
            }
            method.invoke(player, reason);
        } catch (Exception exception) {
            plugin.getLogger().warning("Spieler konnte nicht getrennt werden: " + exception.getMessage());
        }
    }
}
