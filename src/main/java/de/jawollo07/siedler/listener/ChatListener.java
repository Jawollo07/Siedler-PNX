package de.jawollo07.siedler.listener;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.team.Team;
import de.jawollo07.siedler.team.TeamManager;
import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.player.PlayerChatEvent;

import java.sql.SQLException;
import java.util.Locale;

public final class ChatListener implements Listener {

    private final SiedlerPlugin plugin;
    private final TeamManager teamManager;

    public ChatListener(SiedlerPlugin plugin) {
        this.plugin = plugin;
        this.teamManager = new TeamManager(plugin);
    }

    @EventHandler
    public void onChat(PlayerChatEvent event) {
        try {
            Team team = teamManager.getTeamByPlayer(event.getPlayer().getName());

            if (team == null) {
                event.setFormat("§7%1$s§f: %2$s");
                return;
            }

            String teamColor = getTeamColor(team.color());
            event.setFormat(teamColor + "[" + team.name() + "] §f%1$s§7: §f%2$s");
        } catch (SQLException exception) {
            plugin.getLogger().warning(
                    "Could not determine team for player "
                            + event.getPlayer().getName()
                            + ": "
                            + exception.getMessage()
            );

            event.setFormat("§7%1$s§f: §f%2$s");
        }
    }

    private String getTeamColor(String color) {
        if (color == null || color.isBlank()) {
            return "§f";
        }

        return switch (color.trim().toUpperCase(Locale.ROOT)) {
            case "BLACK" -> "§0";
            case "DARK_BLUE" -> "§1";
            case "DARK_GREEN" -> "§2";
            case "DARK_AQUA" -> "§3";
            case "DARK_RED" -> "§4";
            case "DARK_PURPLE" -> "§5";
            case "GOLD" -> "§6";
            case "GRAY" -> "§7";
            case "DARK_GRAY" -> "§8";
            case "BLUE" -> "§9";
            case "GREEN" -> "§a";
            case "AQUA" -> "§b";
            case "RED" -> "§c";
            case "LIGHT_PURPLE" -> "§d";
            case "YELLOW" -> "§e";
            case "WHITE" -> "§f";
            default -> "§f";
        };
    }
}
