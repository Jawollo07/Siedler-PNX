package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.storage.StorageManager;
import org.powernukkitx.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/** Stores and retrieves the latest death point of each player. */
public final class DeathManager {
    public record DeathPoint(String id, String playerId, String world,
                             double x, double y, double z,
                             float yaw, float pitch, long createdAt) {}

    private final StorageManager storage;

    public DeathManager(SiedlerPlugin plugin) {
        this.storage = plugin.getStorage();
    }

    public void saveDeath(Player player) throws SQLException {
        if (player == null || player.getLevel() == null) return;

        String playerId = player.getUniqueId().toString();
        String sql = """
                INSERT INTO death_points
                    (id, player_id, world, x, y, z, yaw, pitch, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement delete = storage.getConnection().prepareStatement(
                "DELETE FROM death_points WHERE player_id = ?")) {
            delete.setString(1, playerId);
            delete.executeUpdate();
        }

        try (PreparedStatement insert = storage.getConnection().prepareStatement(sql)) {
            insert.setString(1, UUID.randomUUID().toString());
            insert.setString(2, playerId);
            insert.setString(3, player.getLevel().getName());
            insert.setDouble(4, player.getPosition().getX());
            insert.setDouble(5, player.getPosition().getY());
            insert.setDouble(6, player.getPosition().getZ());
            insert.setFloat(7, (float) player.getYaw());
            insert.setFloat(8, (float) player.getPitch());
            insert.setLong(9, System.currentTimeMillis());
            insert.executeUpdate();
        }
    }

    public DeathPoint getDeathPoint(Player player) throws SQLException {
        if (player == null) return null;
        return getDeathPoint(player.getUniqueId().toString());
    }

    public DeathPoint getDeathPoint(String playerId) throws SQLException {
        if (playerId == null || playerId.isBlank()) return null;

        String sql = """
                SELECT id, player_id, world, x, y, z, yaw, pitch, created_at
                FROM death_points
                WHERE player_id = ?
                ORDER BY created_at DESC
                LIMIT 1
                """;

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, playerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return null;
                return new DeathPoint(
                        resultSet.getString("id"),
                        resultSet.getString("player_id"),
                        resultSet.getString("world"),
                        resultSet.getDouble("x"),
                        resultSet.getDouble("y"),
                        resultSet.getDouble("z"),
                        resultSet.getFloat("yaw"),
                        resultSet.getFloat("pitch"),
                        resultSet.getLong("created_at")
                );
            }
        }
    }

    public boolean deleteDeathPoint(Player player) throws SQLException {
        if (player == null) return false;
        try (PreparedStatement statement = storage.getConnection().prepareStatement(
                "DELETE FROM death_points WHERE player_id = ?")) {
            statement.setString(1, player.getUniqueId().toString());
            return statement.executeUpdate() > 0;
        }
    }

    public void teleport(Player player, DeathPoint point) throws Exception {
        if (player == null || point == null) throw new IllegalArgumentException("Ungültiger Death Point.");

        org.powernukkitx.level.Level level = SiedlerPlugin.getInstance().getServer()
                .getLevelByName(point.world());
        if (level == null) throw new IllegalStateException("Die Todeswelt ist nicht geladen.");

        Class<?> locationClass = Class.forName("org.powernukkitx.level.Location");
        Object location = null;
        for (java.lang.reflect.Constructor<?> constructor : locationClass.getConstructors()) {
            Class<?>[] types = constructor.getParameterTypes();
            if (types.length == 6 && types[0] == double.class && types[1] == double.class
                    && types[2] == double.class && types[3] == float.class
                    && types[4] == float.class && types[5].isAssignableFrom(level.getClass())) {
                location = constructor.newInstance(point.x(), point.y(), point.z(),
                        point.yaw(), point.pitch(), level);
                break;
            }
        }
        if (location == null) throw new IllegalStateException("Die PNX-Location-API konnte nicht aufgelöst werden.");

        for (java.lang.reflect.Method method : player.getClass().getMethods()) {
            if (!"teleport".equals(method.getName()) || method.getParameterCount() != 2) continue;
            if (!method.getParameterTypes()[0].isAssignableFrom(locationClass)) continue;
            Class<?> causeType = method.getParameterTypes()[1];
            if (!causeType.isEnum()) continue;
            Object cause = null;
            for (Object constant : causeType.getEnumConstants()) {
                if ("COMMAND".equals(String.valueOf(constant))) { cause = constant; break; }
            }
            if (cause == null && causeType.getEnumConstants().length > 0) cause = causeType.getEnumConstants()[0];
            if (cause == null) throw new IllegalStateException("Keine Teleport-Ursache verfügbar.");
            Object result = method.invoke(player, location, cause);
            if (result instanceof Boolean success && !success) throw new IllegalStateException("Der Teleport wurde abgelehnt.");
            return;
        }
        throw new IllegalStateException("Die PNX-Teleport-API konnte nicht aufgelöst werden.");
    }
}
