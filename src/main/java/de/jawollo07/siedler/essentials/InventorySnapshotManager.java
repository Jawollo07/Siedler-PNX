package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.storage.StorageManager;
import org.powernukkitx.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persistent inventory snapshots independent from death points.
 *
 * Snapshots are kept in the database so they survive plugin/server restarts.
 */
public final class InventorySnapshotManager {
    public record InventorySnapshot(
            String id,
            String playerId,
            String reason,
            long capturedAt,
            String inventoryData
    ) {}

    private final SiedlerPlugin plugin;
    private final StorageManager storage;

    public InventorySnapshotManager(SiedlerPlugin plugin) {
        this.plugin = plugin;
        this.storage = plugin.getStorage();
    }

    public void snapshot(Player player, String reason) throws SQLException {
        if (player == null) return;

        ensurePlayer(player);

        String data = serializeInventory(player);
        String sql = """
                INSERT INTO inventory_snapshots
                    (id, player_id, reason, captured_at, inventory_data)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, player.getUniqueId().toString());
            statement.setString(3, normalizeReason(reason));
            statement.setLong(4, System.currentTimeMillis());
            statement.setString(5, data);
            statement.executeUpdate();
        }
    }

    public InventorySnapshot getLatest(String playerId) throws SQLException {
        if (playerId == null || playerId.isBlank()) return null;

        String sql = """
                SELECT id, player_id, reason, captured_at, inventory_data
                FROM inventory_snapshots
                WHERE player_id = ?
                ORDER BY captured_at DESC
                LIMIT 1
                """;

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, playerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return null;
                return map(resultSet);
            }
        }
    }

    public List<InventorySnapshot> getHistory(String playerId, int limit) throws SQLException {
        if (playerId == null || playerId.isBlank()) return List.of();

        int safeLimit = Math.max(1, Math.min(limit, 500));
        String sql = """
                SELECT id, player_id, reason, captured_at, inventory_data
                FROM inventory_snapshots
                WHERE player_id = ?
                ORDER BY captured_at DESC
                LIMIT ?
                """;

        List<InventorySnapshot> history = new ArrayList<>();
        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, playerId);
            statement.setInt(2, safeLimit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    history.add(map(resultSet));
                }
            }
        }
        return history;
    }

    private InventorySnapshot map(ResultSet resultSet) throws SQLException {
        return new InventorySnapshot(
                resultSet.getString("id"),
                resultSet.getString("player_id"),
                resultSet.getString("reason"),
                resultSet.getLong("captured_at"),
                resultSet.getString("inventory_data")
        );
    }

    private void ensurePlayer(Player player) throws SQLException {
        String sql = """
                INSERT OR IGNORE INTO players
                    (id, last_name, first_join, last_seen)
                VALUES (?, ?, ?, ?)
                """;
        long now = System.currentTimeMillis();
        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, player.getUniqueId().toString());
            statement.setString(2, player.getName());
            statement.setLong(3, now);
            statement.setLong(4, now);
            statement.executeUpdate();
        }

        try (PreparedStatement statement = storage.getConnection().prepareStatement(
                "UPDATE players SET last_name = ?, last_seen = ? WHERE id = ?")) {
            statement.setString(1, player.getName());
            statement.setLong(2, now);
            statement.setString(3, player.getUniqueId().toString());
            statement.executeUpdate();
        }
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) return "UNKNOWN";
        return reason.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private String serializeInventory(Player player) {
        StringBuilder out = new StringBuilder();
        try {
            Object inventory = player.getClass().getMethod("getInventory").invoke(player);
            appendInventory(out, "main", inventory);
            appendInventoryMethod(out, "armor", inventory, "getArmorContents");
            appendInventoryMethod(out, "offhand", player, "getOffhandInventory");
        } catch (Exception e) {
            out.append("inventory_error=")
                    .append(escapeInventoryValue(e.getMessage()))
                    .append("\n");
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
                    .append(escapeInventoryValue(e.getMessage()))
                    .append("\n");
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
        return String.valueOf(value)
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("|", "\\|");
    }
}
