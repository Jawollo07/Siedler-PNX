package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.storage.StorageManager;
import org.powernukkitx.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/** Stores and retrieves death points for players. */
public final class DeathManager {
    public record DeathPoint(String id, String playerId, String world,
                             double x, double y, double z,
                             float yaw, float pitch, long createdAt, String inventoryData) {}

    private final StorageManager storage;

    public DeathManager(SiedlerPlugin plugin) {
        this.storage = plugin.getStorage();
    }

    public void saveDeath(Player player) throws SQLException {
        if (player == null || player.getLevel() == null) return;

        String inventoryData = serializeInventory(player);
        String sql = """
                INSERT INTO death_points
                    (id, player_id, world, x, y, z, yaw, pitch, created_at, inventory_data)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement insert = storage.getConnection().prepareStatement(sql)) {
            insert.setString(1, UUID.randomUUID().toString());
            insert.setString(2, player.getUniqueId().toString());
            insert.setString(3, player.getLevel().getName());
            insert.setDouble(4, player.getPosition().getX());
            insert.setDouble(5, player.getPosition().getY());
            insert.setDouble(6, player.getPosition().getZ());
            insert.setFloat(7, (float) player.getYaw());
            insert.setFloat(8, (float) player.getPitch());
            insert.setLong(9, System.currentTimeMillis());
            insert.setString(10, inventoryData);
            insert.executeUpdate();
        }
    }

    private String serializeInventory(Player player) {
        StringBuilder out = new StringBuilder();
        try {
            Object inventory = player.getClass().getMethod("getInventory").invoke(player);
            appendInventory(out, "main", inventory);
            appendInventoryMethod(out, "armor", inventory, "getArmorContents");
            appendInventoryMethod(out, "offhand", player, "getOffhandInventory");
        } catch (Exception e) {
            out.append("inventory_error=").append(escapeInventoryValue(e.getMessage())).append("\n");
        }
        return out.toString();
    }

    private void appendInventoryMethod(StringBuilder out, String section, Object owner, String method) {
        try {
            appendInventory(out, section, owner.getClass().getMethod(method).invoke(owner));
        } catch (Exception ignored) {
        }
    }

    private void appendInventory(StringBuilder out, String section, Object inventory) {
        if (inventory == null) return;
        try {
            if (inventory.getClass().isArray()) {
                int length = java.lang.reflect.Array.getLength(inventory);
                for (int i = 0; i < length; i++) {
                    appendItem(out, section, i, java.lang.reflect.Array.get(inventory, i));
                }
                return;
            }
            int size = ((Number) inventory.getClass().getMethod("getSize").invoke(inventory)).intValue();
            for (int i = 0; i < size; i++) {
                Object item = inventory.getClass().getMethod("getItem", int.class).invoke(inventory, i);
                appendItem(out, section, i, item);
            }
        } catch (Exception e) {
            out.append(section).append("|error=")
                    .append(escapeInventoryValue(e.getMessage())).append("\n");
        }
    }

    private void appendItem(StringBuilder out, String section, int slot, Object item) {
        if (item == null) return;
        Object count = invokeOptional(item, "getCount");
        if (count == null) count = invokeOptional(item, "getAmount");
        if (count instanceof Number n && n.intValue() <= 0) return;

        out.append(section).append("|slot=").append(slot)
                .append("|id=").append(escapeInventoryValue(invokeOptional(item, "getId")))
                .append("|name=").append(escapeInventoryValue(invokeOptional(item, "getName")))
                .append("|count=").append(escapeInventoryValue(count))
                .append("|damage=").append(escapeInventoryValue(invokeOptional(item, "getDamage")))
                .append("|nbt=").append(escapeInventoryValue(invokeOptional(item, "getCompoundTag")))
                .append("\n");
    }

    private Object invokeOptional(Object target, String method) {
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String escapeInventoryValue(Object value) {
        if (value == null) return "";
        return String.valueOf(value).replace("\\", "\\\\")
                .replace("\n", "\\n").replace("|", "\\|");
    }

    public DeathPoint getDeathPoint(Player player) throws SQLException {
        if (player == null) return null;
        return getDeathPoint(player.getUniqueId().toString());
    }

    public DeathPoint getDeathPoint(String playerId) throws SQLException {
        if (playerId == null || playerId.isBlank()) return null;

        String sql = """
                SELECT id, player_id, world, x, y, z, yaw, pitch, created_at, inventory_data FROM death_points
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
                        resultSet.getLong("created_at"),
                        resultSet.getString("inventory_data")
                );
            }
        }
    }

    public java.util.List<DeathPoint> getDeathHistory(String playerId) throws SQLException {
        if (playerId == null || playerId.isBlank()) return java.util.List.of();

        String sql = """
                SELECT id, player_id, world, x, y, z, yaw, pitch, created_at, inventory_data FROM death_points
                WHERE player_id = ?
                ORDER BY created_at DESC
                """;

        java.util.List<DeathPoint> history = new java.util.ArrayList<>();
        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, playerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    history.add(new DeathPoint(
                            resultSet.getString("id"),
                            resultSet.getString("player_id"),
                            resultSet.getString("world"),
                            resultSet.getDouble("x"),
                            resultSet.getDouble("y"),
                            resultSet.getDouble("z"),
                            resultSet.getFloat("yaw"),
                            resultSet.getFloat("pitch"),
                            resultSet.getLong("created_at"),
                            resultSet.getString("inventory_data")
                    ));
                }
            }
        }
        return history;
    }

    /** Legacy helper; death points are retained for the admin history. */
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
