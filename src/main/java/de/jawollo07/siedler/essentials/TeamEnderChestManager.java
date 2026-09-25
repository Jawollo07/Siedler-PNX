package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.storage.StorageManager;
import org.powernukkitx.Player;
import org.powernukkitx.item.Item;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TeamEnderChestManager {

    private static final String INVENTORY_TYPE = "teamchest";
    private static final int SIZE = 54;

    private final SiedlerPlugin plugin;
    private final StorageManager storage;
    private final Map<String, TeamEnderChestInventory> inventories = new ConcurrentHashMap<>();

    public TeamEnderChestManager(SiedlerPlugin plugin) {
        this.plugin = plugin;
        this.storage = plugin.getStorage();
    }

    public TeamEnderChestInventory getOrCreate(Player player, String teamId) throws SQLException {
        TeamEnderChestInventory inventory = inventories.get(teamId);
        if (inventory != null) {
            return inventory;
        }

        TeamEnderChestInventory created = new TeamEnderChestInventory(player, teamId, this);
        load(created);

        TeamEnderChestInventory existing = inventories.putIfAbsent(teamId, created);
        return existing != null ? existing : created;
    }

    public void saveSlot(TeamEnderChestInventory inventory, int slot) {
        if (slot < 0 || slot >= SIZE) {
            return;
        }

        try {
            Item item = inventory.getItem(slot);

            if (item == null || item.isNull() || item.getCount() <= 0) {
                try (PreparedStatement statement = storage.getConnection().prepareStatement(
                        "DELETE FROM team_inventories WHERE team_id = ? AND inventory_type = ? AND slot = ?")) {
                    statement.setString(1, inventory.getTeamId());
                    statement.setString(2, INVENTORY_TYPE);
                    statement.setInt(3, slot);
                    statement.executeUpdate();
                }
                return;
            }

            String data = serialize(item);
            String updateSql = "UPDATE team_inventories SET item_data = ? "
                    + "WHERE team_id = ? AND inventory_type = ? AND slot = ?";
            try (PreparedStatement update = storage.getConnection().prepareStatement(updateSql)) {
                update.setString(1, data);
                update.setString(2, inventory.getTeamId());
                update.setString(3, INVENTORY_TYPE);
                update.setInt(4, slot);

                if (update.executeUpdate() > 0) {
                    return;
                }
            }

            String insertSql = "INSERT INTO team_inventories "
                    + "(team_id, inventory_type, slot, item_data) VALUES (?, ?, ?, ?)";
            try (PreparedStatement insert = storage.getConnection().prepareStatement(insertSql)) {
                insert.setString(1, inventory.getTeamId());
                insert.setString(2, INVENTORY_TYPE);
                insert.setInt(3, slot);
                insert.setString(4, data);
                insert.executeUpdate();
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Team-Enderchest konnte nicht gespeichert werden: " + exception.getMessage());
        }
    }

    public void saveAll() {
        for (TeamEnderChestInventory inventory : inventories.values()) {
            for (int slot = 0; slot < SIZE; slot++) {
                saveSlot(inventory, slot);
            }
        }
    }

    private void load(TeamEnderChestInventory inventory) throws SQLException {
        String sql = "SELECT slot, item_data FROM team_inventories "
                + "WHERE team_id = ? AND inventory_type = ? ORDER BY slot";

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, inventory.getTeamId());
            statement.setString(2, INVENTORY_TYPE);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int slot = resultSet.getInt("slot");
                    if (slot < 0 || slot >= SIZE) {
                        continue;
                    }

                    Item item = deserialize(resultSet.getString("item_data"));
                    if (item != null && !item.isNull()) {
                        inventory.setItem(slot, item, false);
                    }
                }
            }
        }
    }

    private String serialize(Item item) {
        byte[] nbt = item.getNbtBytes();
        String nbtData = nbt == null ? "" : Base64.getEncoder().encodeToString(nbt);
        return item.getId() + ";" + item.getDamage() + ";" + item.getCount() + ";" + nbtData;
    }

    private Item deserialize(String data) {
        try {
            if (data == null || data.isBlank()) {
                return null;
            }

            String[] parts = data.split(";", -1);
            if (parts.length < 4) {
                return null;
            }

            int id = Integer.parseInt(parts[0]);
            int damage = Integer.parseInt(parts[1]);
            int count = Integer.parseInt(parts[2]);
            byte[] nbt = parts[3].isEmpty() ? null : Base64.getDecoder().decode(parts[3]);

            return Item.get(String.valueOf(id), damage, count, nbt);
        } catch (Exception exception) {
            plugin.getLogger().warning("Ungültiger Team-Enderchest-Itemdatensatz: " + exception.getMessage());
            return null;
        }
    }
}
