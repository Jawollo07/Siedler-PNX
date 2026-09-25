package de.jawollo07.siedler.monsters;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import de.jawollo07.siedler.team.Team;
import de.jawollo07.siedler.team.TeamManager;
import org.powernukkitx.Player;
import org.powernukkitx.scheduler.TaskHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class OutpostManager implements Runnable {
    private final SiedlerPlugin plugin;
    private final TeamManager teamManager;
    private final MessageManager messageManager;
    private final Map<String, CaptureState> captures = new HashMap<>();
    private TaskHandler task;

    public OutpostManager(SiedlerPlugin plugin) {
        this.plugin = plugin;
        this.teamManager = new TeamManager(plugin);
        this.messageManager = new MessageManager();
    }

    public void start() {
        if (!enabled()) {
            plugin.getLogger().info("Outpost system disabled by configuration.");
            return;
        }
        if (task != null) return;
        task = plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, this, 20);
        plugin.getLogger().info("Outpost system started.");
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        captures.clear();
    }

    @Override
    public void run() {
        if (!enabled()) return;
        try {
            for (Outpost outpost : getOutposts()) tick(outpost);
        } catch (Exception exception) {
            plugin.getLogger().warning("Outpost tick failed: " + safe(exception));
        }
    }

    public Outpost create(String name, Player player, int radius) throws SQLException {
        if (player == null || player.getLevel() == null) {
            throw new IllegalArgumentException("Spieler oder Welt fehlt.");
        }

        String cleanName = requireName(name);
        int safeRadius = Math.max(1, radius);
        String world = player.getLevel().getName();
        int x = (int) Math.floor(player.getX());
        int y = (int) Math.floor(player.getY());
        int z = (int) Math.floor(player.getZ());

        if (getByName(cleanName) != null) {
            throw new IllegalArgumentException("Ein Outpost mit diesem Namen existiert bereits.");
        }

        for (Outpost existing : getOutposts()) {
            if (!existing.world().equals(world)) continue;
            double dx = existing.x() - x;
            double dz = existing.z() - z;
            if ((dx * dx + dz * dz)
                    <= (double) (existing.radius() + safeRadius) * (existing.radius() + safeRadius)) {
                throw new IllegalArgumentException("Der Outpost überschneidet sich mit einem bestehenden Outpost.");
            }
        }

        String id = UUID.randomUUID().toString();
        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(
                "INSERT INTO outposts " +
                        "(id, name, world, x, y, z, radius, owner_team_id, captured_at, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NULL, NULL, ?)")) {
            statement.setString(1, id);
            statement.setString(2, cleanName);
            statement.setString(3, world);
            statement.setInt(4, x);
            statement.setInt(5, y);
            statement.setInt(6, z);
            statement.setInt(7, safeRadius);
            statement.setLong(8, System.currentTimeMillis());
            statement.executeUpdate();
        }
        return getById(id);
    }

    public boolean delete(String name) throws SQLException {
        Outpost outpost = getByName(name);
        if (outpost == null) return false;

        removeBonus(outpost.ownerTeamId(), outpost.id());
        captures.remove(outpost.id());

        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(
                "DELETE FROM outposts WHERE id = ?")) {
            statement.setString(1, outpost.id());
            return statement.executeUpdate() > 0;
        }
    }

    public Outpost getByName(String name) throws SQLException {
        if (name == null || name.isBlank()) return null;
        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(
                "SELECT id, name, world, x, y, z, radius, owner_team_id, captured_at, created_at " +
                        "FROM outposts WHERE name = ? LIMIT 1")) {
            statement.setString(1, name.trim());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    public Outpost getById(String id) throws SQLException {
        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(
                "SELECT id, name, world, x, y, z, radius, owner_team_id, captured_at, created_at " +
                        "FROM outposts WHERE id = ? LIMIT 1")) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    public List<Outpost> getOutposts() throws SQLException {
        List<Outpost> outposts = new ArrayList<>();
        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(
                "SELECT id, name, world, x, y, z, radius, owner_team_id, captured_at, created_at " +
                        "FROM outposts ORDER BY name");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) outposts.add(map(result));
        }
        return outposts;
    }

    public int getCaptureSeconds() {
        return Math.max(1, plugin.getConfig().getInt("monsters.outposts.capture-seconds", 10));
    }

    public int getProgressSeconds(String outpostId) {
        CaptureState state = captures.get(outpostId);
        return state == null ? 0 : state.progressSeconds;
    }

    private void tick(Outpost outpost) {
        List<TeamPresence> present = findPresentTeams(outpost);

        if (present.size() > 1) {
            CaptureState state = captures.computeIfAbsent(
                    outpost.id(), ignored -> new CaptureState(present.get(0).team().id()));
            if (!state.contested) {
                notifyTeams(present, messageManager.getMessage("messages.monsters.outpost-contested")
                        .replace("{outpost}", outpost.name()));
                state.contested = true;
            }
            return;
        }

        if (present.isEmpty()) {
            decay(outpost.id());
            return;
        }

        Team team = present.get(0).team();
        CaptureState existingState = captures.get(outpost.id());
        if (existingState != null) existingState.contested = false;
        if (outpost.ownerTeamId() != null && outpost.ownerTeamId().equals(team.id())) {
            decay(outpost.id());
            return;
        }

        CaptureState state = captures.computeIfAbsent(
                outpost.id(), ignored -> new CaptureState(team.id()));

        if (!state.teamId.equals(team.id())) {
            state.teamId = team.id();
            state.progressSeconds = 0;
        }

        state.progressSeconds++;
        if (state.progressSeconds >= getCaptureSeconds()) {
            try {
                capture(outpost, team);
            } catch (SQLException exception) {
                plugin.getLogger().warning("Could not capture outpost "
                        + outpost.name() + ": " + safe(exception));
            }
        }
    }

    private void decay(String outpostId) {
        CaptureState state = captures.get(outpostId);
        if (state == null) return;
        state.progressSeconds--;
        if (state.progressSeconds <= 0) captures.remove(outpostId);
    }

    private List<TeamPresence> findPresentTeams(Outpost outpost) {
        Map<String, TeamPresence> teams = new HashMap<>();

        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            if (player.getLevel() == null || !outpost.world().equals(player.getLevel().getName())) {
                continue;
            }

            double dx = player.getX() - outpost.x();
            double dz = player.getZ() - outpost.z();
            if (dx * dx + dz * dz > (double) outpost.radius() * outpost.radius()) continue;

            try {
                Team team = teamManager.getTeamForPlayer(player.getUniqueId().toString());
                if (team == null || team.eliminated() != 0) continue;
                teams.putIfAbsent(team.id(), new TeamPresence(team));
            } catch (Exception ignored) {
                // Invalid team data must not stop capture processing.
            }
        }
        return new ArrayList<>(teams.values());
    }

    private void capture(Outpost outpost, Team team) throws SQLException {
        String oldOwner = outpost.ownerTeamId();
        if (team.id().equals(oldOwner)) {
            captures.remove(outpost.id());
            return;
        }

        if (oldOwner != null) removeBonus(oldOwner, outpost.id());
        addBonus(team.id(), outpost.id());

        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(
                "UPDATE outposts SET owner_team_id = ?, captured_at = ? WHERE id = ?")) {
            statement.setString(1, team.id());
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, outpost.id());
            statement.executeUpdate();
        }

        captures.remove(outpost.id());

        String message = messageManager.getMessage("messages.monsters.outpost-captured")
                .replace("{outpost}", outpost.name())
                .replace("{team}", team.name())
                .replace("{bonus}", String.valueOf(Math.max(1,
                        plugin.getConfig().getInt("monsters.outposts.tax-bonus", 1))));

        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            player.sendMessage(message);
        }
    }

    private void addBonus(String teamId, String outpostId) throws SQLException {
        int bonus = Math.max(1, plugin.getConfig().getInt("monsters.outposts.tax-bonus", 1));
        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(
                "INSERT INTO team_bonus_sources " +
                        "(id, team_id, source_type, source_id, amount, permanent, created_at) " +
                        "VALUES (?, ?, 'OUTPOST', ?, ?, 1, ?)")) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, teamId);
            statement.setString(3, "outpost:" + outpostId);
            statement.setInt(4, bonus);
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private void removeBonus(String teamId, String outpostId) throws SQLException {
        if (teamId == null || teamId.isBlank()) return;
        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(
                "DELETE FROM team_bonus_sources " +
                        "WHERE team_id = ? AND source_type = 'OUTPOST' AND source_id = ?")) {
            statement.setString(1, teamId);
            statement.setString(2, "outpost:" + outpostId);
            statement.executeUpdate();
        }
    }

    private void notifyTeams(List<TeamPresence> teams, String message) {
        Set<String> ids = new HashSet<>();
        for (TeamPresence presence : teams) ids.add(presence.team().id());

        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            try {
                Team team = teamManager.getTeamForPlayer(player.getUniqueId().toString());
                if (team != null && ids.contains(team.id())) player.sendMessage(message);
            } catch (Exception ignored) {
            }
        }
    }

    private Outpost map(ResultSet result) throws SQLException {
        long capturedAt = result.getLong("captured_at");
        boolean capturedAtNull = result.wasNull();

        return new Outpost(
                result.getString("id"),
                result.getString("name"),
                result.getString("world"),
                result.getInt("x"),
                result.getInt("y"),
                result.getInt("z"),
                result.getInt("radius"),
                result.getString("owner_team_id"),
                capturedAt,
                capturedAtNull,
                result.getLong("created_at"));
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Outpost-Name darf nicht leer sein.");
        }
        String clean = name.trim();
        if (clean.length() > 64) {
            throw new IllegalArgumentException("Outpost-Name darf maximal 64 Zeichen lang sein.");
        }
        return clean;
    }

    private String safe(Exception exception) {
        return exception.getMessage() == null ? "Unbekannter Fehler" : exception.getMessage();
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("monsters.outposts.enabled", true);
    }

    private static final class CaptureState {
        private String teamId;
        private int progressSeconds;
        private boolean contested;

        private CaptureState(String teamId) {
            this.teamId = teamId;
        }
    }

    private record TeamPresence(Team team) {}

    public record Outpost(
            String id,
            String name,
            String world,
            int x,
            int y,
            int z,
            int radius,
            String ownerTeamId,
            long capturedAt,
            boolean capturedAtNull,
            long createdAt
    ) {}
}
