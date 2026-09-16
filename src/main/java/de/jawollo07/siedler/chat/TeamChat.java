package de.jawollo07.siedler.chat;

import de.jawollo07.siedler.SiedlerPlugin;
import org.powernukkitx.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class TeamChat {
	private final SiedlerPlugin plugin;

	public TeamChat(SiedlerPlugin plugin) {
		this.plugin = plugin;
	}

	public boolean send(Player sender, String message) {
		String teamSql = "SELECT t.id, t.name FROM players p "
				+ "JOIN teams t ON t.id = p.team_id WHERE p.id = ?";

		try (PreparedStatement teamStatement = plugin.getStorage().getConnection().prepareStatement(teamSql)) {
			teamStatement.setString(1, sender.getUniqueId().toString());

			try (ResultSet teamResult = teamStatement.executeQuery()) {
				if (!teamResult.next()) {
					sender.sendMessage("§cDu bist keinem Team zugeordnet.");
					return false;
				}

				String teamId = teamResult.getString("id");
				String teamName = teamResult.getString("name");
				Set<String> memberIds = getMemberIds(teamId);
				String formattedMessage = "§7[TeamChat] §e" + sender.getName()
						+ " §8(" + teamName + ") §7: §f" + message;

				for (Player onlinePlayer : plugin.getServer().getOnlinePlayers().values()) {
					if (memberIds.contains(onlinePlayer.getUniqueId().toString())) {
						onlinePlayer.sendMessage(formattedMessage);
					}
				}
				return true;
			}
		} catch (SQLException exception) {
			plugin.getLogger().warning("Failed to send team chat message: " + exception.getMessage());
			sender.sendMessage("§cDie Teamnachricht konnte nicht gesendet werden.");
			return false;
		}
	}

	private Set<String> getMemberIds(String teamId) throws SQLException {
		Set<String> memberIds = new HashSet<>();
		String memberSql = "SELECT id FROM players WHERE team_id = ?";

		try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(memberSql)) {
			statement.setString(1, teamId);
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					memberIds.add(resultSet.getString("id"));
				}
			}
		}
		return memberIds;
	}
}
