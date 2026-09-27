package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import de.jawollo07.siedler.team.Team;
import de.jawollo07.siedler.team.TeamManager;
import org.powernukkitx.Player;
import org.powernukkitx.item.Item;
import org.powernukkitx.level.Level;
import org.powernukkitx.level.Location;
import org.powernukkitx.utils.ConfigSection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public final class StartManager {
    public record StartPosition(String teamId, String world, double x, double y, double z, double yaw, double pitch) {}
    public record StarterItem(String item, int count) {}

    private final SiedlerPlugin plugin;
    private final TeamManager teamManager;
    private final MessageManager messages = new MessageManager();

    public StartManager(SiedlerPlugin plugin) {
        this.plugin = plugin;
        this.teamManager = new TeamManager(plugin);
    }

    public List<String> getConfiguredTeams() {
        List<String> result = new ArrayList<>();
        try {
            for (Team team : teamManager.getTeams()) if (getTeamStart(team) != null) result.add(team.name());
        } catch (Exception e) { plugin.getLogger().warning("Startpunkte konnten nicht geladen werden: " + e.getMessage()); }
        return result;
    }

    public boolean setTeamStart(Player player, String teamName) {
        try {
            Team team = teamManager.getTeamByName(teamName);
            String value = String.join("|", player.getLevel().getName(), Double.toString(player.getX()),
                    Double.toString(player.getY()), Double.toString(player.getZ()),
                    Double.toString(player.getYaw()), Double.toString(player.getPitch()));
            saveSetting("start.team." + team.id(), value);
            player.sendMessage(messages.getMessage("messages.start.team-start-set")
                    .replace("{team}", team.name()).replace("{world}", player.getLevel().getName())
                    .replace("{x}", format(player.getX())).replace("{y}", format(player.getY())).replace("{z}", format(player.getZ())));
            return true;
        } catch (Exception e) {
            player.sendMessage(messages.getMessage("messages.start.error").replace("{error}", safe(e)));
            return false;
        }
    }

    public boolean clearTeamStart(org.powernukkitx.command.CommandSender sender, String teamName) {
        try {
            Team team = teamManager.getTeamByName(teamName);
            deleteSetting("start.team." + team.id());
            sender.sendMessage(messages.getMessage("messages.start.team-start-cleared").replace("{team}", team.name()));
            return true;
        } catch (Exception e) {
            sender.sendMessage(messages.getMessage("messages.start.error").replace("{error}", safe(e)));
            return false;
        }
    }

    public StartPosition getTeamStart(Team team) {
        if (team == null) return null;
        String raw = getSetting("start.team." + team.id());
        if (raw == null || raw.isBlank()) return null;
        try {
            String[] p = raw.split("\\|", -1);
            // Stored format: world|x|y|z|yaw|pitch (6 fields).
            // Older versions of StartManager accidentally required 7 fields,
            // which made every valid start point appear as if it did not exist.
            if (p.length != 6) {
                plugin.getLogger().warning("Ungültiger Startpunkt für Team " + team.name() + ": erwartet 6 Felder, erhalten " + p.length);
                return null;
            }
            if (p[0].isBlank()) return null;
            return new StartPosition(team.id(), p[0], Double.parseDouble(p[1]), Double.parseDouble(p[2]),
                    Double.parseDouble(p[3]), Double.parseDouble(p[4]), Double.parseDouble(p[5]));
        } catch (Exception e) {
            plugin.getLogger().warning("Startpunkt für Team " + team.name() + " konnte nicht gelesen werden: " + safe(e));
            return null;
        }
    }

    public boolean teleportToTeam(Player player) {
        try {
            Team team = teamManager.getTeamForPlayer(player.getUniqueId().toString());
            if (team == null) { player.sendMessage(messages.getMessage("messages.start.no-team")); return false; }
            StartPosition start = getTeamStart(team);
            if (start == null) { player.sendMessage(messages.getMessage("messages.start.no-team-start").replace("{team}", team.name())); return false; }
            Level level = plugin.getServer().getLevelByName(start.world());
            if (level == null) { player.sendMessage(messages.getMessage("messages.start.world-not-loaded").replace("{world}", start.world())); return false; }
            player.teleport(new Location(start.x(), start.y(), start.z(), start.yaw(), start.pitch(), level));
            player.sendMessage(messages.getMessage("messages.start.teleported").replace("{team}", team.name()));
            return true;
        } catch (Exception e) {
            player.sendMessage(messages.getMessage("messages.start.teleport-error").replace("{error}", safe(e)));
            return false;
        }
    }

    public boolean giveStarterKit(Player player, boolean force) {
        if (player == null || !player.isOnline()) return false;
        if (!force && "1".equals(getSetting("start.kit." + player.getUniqueId()))) {
            player.sendMessage(messages.getMessage("messages.start.kit-already")); return false;
        }
        List<StarterItem> kit = loadStarterKit();
        if (kit.isEmpty()) { player.sendMessage(messages.getMessage("messages.start.kit-empty")); return false; }
        int added = 0, failed = 0;
        for (StarterItem entry : kit) {
            try {
                Item item = Item.get(entry.item(), 0, entry.count());
                if (item == null || item.isNull()) { failed += entry.count(); continue; }
                Item[] leftovers = player.getInventory().addItem(item);
                int left = 0;
                if (leftovers != null) for (Item leftover : leftovers) if (leftover != null && !leftover.isNull()) left += leftover.getCount();
                if (left == 0) added++; else { failed += left; if (left < entry.count()) added++; }
            } catch (Exception e) { failed += entry.count(); plugin.getLogger().warning("Starterkit-Item " + entry.item() + ": " + e.getMessage()); }
        }
        if (added == 0) { player.sendMessage(messages.getMessage("messages.start.kit-failed")); return false; }
        saveSetting("start.kit." + player.getUniqueId(), "1");
        player.sendMessage(messages.getMessage(failed > 0 ? "messages.start.kit-partial" : "messages.start.kit-received")
                .replace("{failed}", String.valueOf(failed)));
        return true;
    }

    public boolean startGame(org.powernukkitx.command.CommandSender sender) {
        int teleported = 0, kits = 0, skippedTeams = 0, skippedPlayers = 0;
        try {
            for (Team team : teamManager.getTeams()) {
                if (team.eliminated() != 0) { skippedTeams++; continue; }
                StartPosition start = getTeamStart(team);
                if (start == null) { skippedTeams++; continue; }
                Level level = plugin.getServer().getLevelByName(start.world());
                if (level == null) { skippedTeams++; continue; }
                for (Player player : plugin.getServer().getOnlinePlayers().values()) {
                    Team playerTeam = teamManager.getTeamForPlayer(player.getUniqueId().toString());
                    if (playerTeam == null || !playerTeam.id().equals(team.id())) continue;
                    try {
                        player.teleport(new Location(start.x(), start.y(), start.z(), start.yaw(), start.pitch(), level));
                        teleported++; if (giveStarterKit(player, false)) kits++;
                    } catch (Exception e) { skippedPlayers++; }
                }
            }
        } catch (Exception e) { sender.sendMessage(messages.getMessage("messages.start.error").replace("{error}", safe(e))); return false; }
        if (teleported == 0) { sender.sendMessage(messages.getMessage("messages.start.no-players")); return false; }
        plugin.getServer().broadcastMessage(messages.getMessage("messages.start.game-started")
                .replace("{players}", String.valueOf(teleported)).replace("{kits}", String.valueOf(kits)));
        plugin.getLogger().info("Siedler-Spiel gestartet: " + teleported + " teleportiert, " + kits + " Starterkits, " + skippedTeams + " Teams übersprungen, " + skippedPlayers + " Spieler fehlgeschlagen.");
        return true;
    }

    private List<StarterItem> loadStarterKit() {
        List<StarterItem> result = new ArrayList<>();
        for (Object raw : plugin.getConfig().getList("start-system.starter-kit", List.of())) {
            if (!(raw instanceof ConfigSection s)) continue;
            String item = s.getString("item", "").trim();
            int count = Math.max(1, s.getInt("count", 1));
            if (!item.isBlank()) result.add(new StarterItem(item, count));
        }
        return result;
    }

    private String getSetting(String key) {
        try (PreparedStatement ps = plugin.getStorage().getConnection().prepareStatement("SELECT value FROM settings WHERE `key` = ?")) {
            ps.setString(1, key); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString("value") : null; }
        } catch (Exception e) { plugin.getLogger().warning("Start-Setting konnte nicht gelesen werden: " + e.getMessage()); return null; }
    }

    private void saveSetting(String key, String value) {
        String storageType = plugin.getStorage().getActiveType();
        String sql = "mariadb".equalsIgnoreCase(storageType)
                ? "INSERT INTO settings (`key`, value) VALUES (?, ?) ON DUPLICATE KEY UPDATE value = VALUES(value)"
                : "INSERT INTO settings (`key`, value) VALUES (?, ?) ON CONFLICT(`key`) DO UPDATE SET value = excluded.value";
        try (PreparedStatement ps = plugin.getStorage().getConnection().prepareStatement(sql)) { ps.setString(1, key); ps.setString(2, value); ps.executeUpdate(); }
        catch (Exception e) { plugin.getLogger().warning("Start-Setting konnte nicht gespeichert werden: " + e.getMessage()); }
    }

    private void deleteSetting(String key) {
        try (PreparedStatement ps = plugin.getStorage().getConnection().prepareStatement("DELETE FROM settings WHERE `key` = ?")) { ps.setString(1, key); ps.executeUpdate(); }
        catch (Exception e) { plugin.getLogger().warning("Start-Setting konnte nicht gelöscht werden: " + e.getMessage()); }
    }

    private String format(double v) { return String.format(java.util.Locale.ROOT, "%.2f", v); }
    private String safe(Exception e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
}