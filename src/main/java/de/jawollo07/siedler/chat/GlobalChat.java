package de.jawollo07.siedler.chat;

import de.jawollo07.siedler.storage.StorageManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Builds the chat format used for global player messages.
 */
public final class GlobalChat {
    private static final String FALLBACK_FORMAT = "§8[§7Global§8] §f%s§7: §f%s";
    private static final String TEAM_FORMAT = "§8[%s%s§8] §f%s§7: §f%s";

    private GlobalChat() {
        // Utility class
    }

    /**
     * Returns the global chat format for the player's team, or the default
     * global format if the player has no team.
     *
     * @param storageManager active storage manager
     * @param playerId       UUID string stored in the players table
     * @return a format containing the two placeholders expected by chat events
     * @throws SQLException if the database query fails
     */
    public static String getFormat(StorageManager storageManager, String playerId) throws SQLException {
        if (storageManager == null || playerId == null || playerId.isBlank()) {
            return FALLBACK_FORMAT;
        }

        Connection connection = storageManager.getConnection();
        if (connection == null || connection.isClosed()) {
            return FALLBACK_FORMAT;
        }

        // LEFT JOIN is important: players without a team must still be found.
        String sql = "SELECT t.name, t.color FROM players p "
                + "LEFT JOIN teams t ON t.id = p.team_id WHERE p.id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return FALLBACK_FORMAT;
                }

                String teamName = resultSet.getString("name");
                if (teamName == null || teamName.isBlank()) {
                    return FALLBACK_FORMAT;
                }

                String teamColor = getMinecraftColor(resultSet.getString("color"));
                return String.format(Locale.ROOT, TEAM_FORMAT, teamColor, teamName);
            }
        }
    }

    private static String getMinecraftColor(String color) {
        if (color == null || color.isBlank()) {
            return "§7";
        }

        return switch (color.trim().toUpperCase(Locale.ROOT)) {
            case "BLACK" -> "§0";
            case "DARK_BLUE" -> "§1";
            case "DARK_GREEN" -> "§2";
            case "DARK_AQUA" -> "§3";
            case "DARK_RED" -> "§4";
            case "DARK_PURPLE" -> "§5";
            case "GOLD" -> "§6";
            case "GRAY", "GREY" -> "§7";
            case "DARK_GRAY", "DARK_GREY" -> "§8";
            case "BLUE" -> "§9";
            case "GREEN" -> "§a";
            case "AQUA" -> "§b";
            case "RED" -> "§c";
            case "LIGHT_PURPLE" -> "§d";
            case "YELLOW" -> "§e";
            case "WHITE" -> "§f";
            default -> "§7";
        };
    }
}
