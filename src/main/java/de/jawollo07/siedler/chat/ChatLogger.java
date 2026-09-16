package de.jawollo07.siedler.chat;

import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.UUID;
import de.jawollo07.siedler.storage.StorageManager;
import de.jawollo07.siedler.SiedlerPlugin;

public class ChatLogger {
    private final StorageManager storageManager;

    public ChatLogger(StorageManager storageManager) {
        this.storageManager = storageManager;
    }
    private final SiedlerPlugin plugin = SiedlerPlugin.getInstance();
    public boolean saveChatMessage(String playerID, String playerName, String world,
                                   double x, double y, double z, String message) {
        try {
            long now = System.currentTimeMillis();
            String updatePlayerSql = "UPDATE players SET last_name = ?, last_seen = ? WHERE id = ?";
            try (PreparedStatement statement = storageManager.getConnection().prepareStatement(updatePlayerSql)) {
                statement.setString(1, playerName);
                statement.setLong(2, now);
                statement.setString(3, playerID);
                if (statement.executeUpdate() == 0) {
                    String insertPlayerSql = "INSERT INTO players "
                            + "(id, last_name, first_join, last_seen) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement insertStatement = storageManager.getConnection()
                            .prepareStatement(insertPlayerSql)) {
                        insertStatement.setString(1, playerID);
                        insertStatement.setString(2, playerName);
                        insertStatement.setLong(3, now);
                        insertStatement.setLong(4, now);
                        insertStatement.executeUpdate();
                    }
                }
            }

            String sql = "INSERT INTO chat_messages "
                    + "(id, player_id, player_name, world, x, y, z, message, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = storageManager.getConnection().prepareStatement(sql)) {
                preparedStatement.setString(1, UUID.randomUUID().toString());
                preparedStatement.setString(2, playerID);
                preparedStatement.setString(3, playerName);
                preparedStatement.setString(4, world);
                preparedStatement.setDouble(5, x);
                preparedStatement.setDouble(6, y);
                preparedStatement.setDouble(7, z);
                preparedStatement.setString(8, message);
                preparedStatement.setLong(9, now);
                preparedStatement.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save chat message: " + e.getMessage());
            return false;
        }
    }
}
