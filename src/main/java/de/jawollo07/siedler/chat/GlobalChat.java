package de.jawollo07.siedler.chat;

import de.jawollo07.siedler.storage.StorageManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public class GlobalChat {
	private static final String FALLBACK_FORMAT = "§8[§7Global§8] §f%s§7: §f%s";

	private GlobalChat() {
	}

	public static String getFormat(StorageManager storageManager, String playerId) throws SQLException {
		String sql = "SELECT t.name, t.color FROM players p "
				+ "JOIN teams t ON t.id = p.team_id WHERE p.id = ?";

		try (PreparedStatement statement = storageManager.getConnection().prepareStatement(sql)) {
			statement.setString(1, playerId);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return FALLBACK_FORMAT;
				}

				String teamColor = getMinecraftColor(resultSet.getString("color"));
				return "§8[" + teamColor + resultSet.getString("name")
						+ "§8] §f%s§7: §f%s";
			}
		}
	}

	private static String getMinecraftColor(String color) {
		return switch (color == null ? "" : color.trim().toUpperCase(Locale.ROOT)) {
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
