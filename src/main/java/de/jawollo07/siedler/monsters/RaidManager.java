package de.jawollo07.siedler.monsters;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import de.jawollo07.siedler.team.Team;
import de.jawollo07.siedler.team.TeamManager;
import org.powernukkitx.Player;
import org.powernukkitx.entity.Entity;
import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.entity.EntityDeathEvent;
import org.powernukkitx.level.Level;
import org.powernukkitx.level.Position;
import org.powernukkitx.scheduler.TaskHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class RaidManager implements Listener, Runnable {
    private static final String RAID_TAG = "siedler:raid:";
    private final SiedlerPlugin plugin;
    private final MessageManager messages;
    private final TeamManager teamManager;
    private TaskHandler task;
    private long lastAutomaticRaid;

    public RaidManager(SiedlerPlugin plugin) {
        this.plugin = plugin;
        this.messages = new MessageManager();
        this.teamManager = new TeamManager(plugin);
    }

    public void start() {
        if (!enabled()) return;
        reconcileAfterRestart();
        task = plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, this, 20 * 5);
        plugin.getLogger().info("Pillager raid system started.");
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void run() {
        if (!enabled() || !automatic()) return;

        long interval = Math.max(1, plugin.getConfig()
                .getInt("monsters.raids.spawn-interval-minutes", 60)) * 60_000L;
        long now = System.currentTimeMillis();
        if (lastAutomaticRaid != 0 && now - lastAutomaticRaid < interval) return;

        try {
            if (getActiveRaid() != null) return;

            OutpostManager.Outpost outpost = chooseRaidOutpost();
            if (outpost != null && startRaid(outpost.name())) lastAutomaticRaid = now;
        } catch (Exception exception) {
            plugin.getLogger().warning("Automatic raid failed: " + safe(exception));
        }
    }

    public boolean startRaid(String outpostName) throws SQLException {
        OutpostManager.Outpost outpost = plugin.getOutpostManager().getByName(outpostName);
        if (outpost == null) throw new IllegalArgumentException("Outpost wurde nicht gefunden.");
        if (!enabled()) return false;
        if (getActiveRaid() != null) return false;
        if (outpost.ownerTeamId() == null) {
            throw new IllegalArgumentException("Ein unbesetzter Outpost kann nicht Ziel eines Raids werden.");
        }

        Team owner = teamManager.getTeamByIdPublic(outpost.ownerTeamId());
        if (owner == null || owner.eliminated() != 0) return false;
        if (!isTeamOnline(owner.id())) {
            throw new IllegalArgumentException("Kein Mitglied des verteidigenden Teams ist online.");
        }

        String raidId = UUID.randomUUID().toString();
        insertRaid(raidId, outpost);
        broadcast(messages.getMessage("messages.monsters.raid-started")
                .replace("{outpost}", outpost.name())
                .replace("{team}", owner.name()));

        if (!spawnWave(raidId, outpost, 1)) {
            finishRaid(raidId, "FAILED");
            return false;
        }
        return true;
    }

    @EventHandler
    public void onRaidMobDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity == null) return;

        String raidId = findRaidId(entity);
        if (raidId == null) return;

        try {
            int remaining = decrementRemaining(raidId);
            if (remaining > 0) return;
            if (remaining < 0) return;

            Raid raid = getRaid(raidId);
            if (raid == null || !"ACTIVE".equals(raid.status())) return;

            int nextWave = raid.wave() + 1;
            int totalWaves = maxWaves();
            if (nextWave > totalWaves) {
                finishRaid(raidId, "COMPLETED");
                broadcast(messages.getMessage("messages.monsters.raid-completed")
                        .replace("{outpost}", raid.outpostName()));
                return;
            }

            OutpostManager.Outpost outpost = plugin.getOutpostManager().getById(raid.outpostId());
            if (outpost == null) {
                finishRaid(raidId, "FAILED");
                return;
            }

            if (!spawnWave(raidId, outpost, nextWave)) {
                finishRaid(raidId, "FAILED");
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not process raid death: " + safe(exception));
        }
    }

    public Raid getActiveRaid() throws SQLException {
        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(
                "SELECT id, outpost_id, outpost_name, team_id, wave, remaining_mobs, status, started_at, finished_at " +
                        "FROM raids WHERE status = 'ACTIVE' ORDER BY started_at LIMIT 1");
             ResultSet result = statement.executeQuery()) {
            return result.next() ? map(result) : null;
        }
    }

    public List<Raid> getRaids() throws SQLException {
        List<Raid> raids = new ArrayList<>();
        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(
                "SELECT id, outpost_id, outpost_name, team_id, wave, remaining_mobs, status, started_at, finished_at " +
                        "FROM raids ORDER BY started_at DESC LIMIT 20");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) raids.add(map(result));
        }
        return raids;
    }

    public void stopRaid(String raidId) throws SQLException {
        finishRaid(raidId, "STOPPED");
    }

    public int getConfiguredWaveSize(int wave) {
        int base = Math.max(1, plugin.getConfig().getInt("monsters.raids.base-size", 3));
        int growth = Math.max(0, plugin.getConfig().getInt("monsters.raids.wave-growth", 2));
        return Math.min(maxRaidSize(), base + ((wave - 1) * growth));
    }

    private boolean spawnWave(String raidId, OutpostManager.Outpost outpost, int wave) throws SQLException {
        Level level = findLevel(outpost.world());
        if (level == null) return false;

        int amount = getConfiguredWaveSize(wave);
        String tag = RAID_TAG + raidId;
        int spawned = 0;

        for (int i = 0; i < amount; i++) {
            Entity entity = spawnPillager(level, outpost, tag, i);
            if (entity != null) spawned++;
        }

        if (spawned == 0) return false;

        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(
                "UPDATE raids SET wave = ?, remaining_mobs = ? WHERE id = ?")) {
            statement.setInt(1, wave);
            statement.setInt(2, spawned);
            statement.setString(3, raidId);
            statement.executeUpdate();
        }

        broadcast(messages.getMessage("messages.monsters.raid-wave")
                .replace("{outpost}", outpost.name())
                .replace("{wave}", String.valueOf(wave))
                .replace("{total}", String.valueOf(maxWaves()))
                .replace("{count}", String.valueOf(spawned)));
        return true;
    }

    private Entity spawnPillager(Level level, OutpostManager.Outpost outpost, String tag, int index) {
        int radius = Math.max(outpost.radius() + 4,
                plugin.getConfig().getInt("monsters.raids.spawn-radius", 20));

        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
            double distance = ThreadLocalRandom.current().nextDouble(
                    Math.max(4, outpost.radius() + 2), radius);
            int x = outpost.x() + (int) Math.round(Math.cos(angle) * distance);
            int z = outpost.z() + (int) Math.round(Math.sin(angle) * distance);

            if (!level.isChunkGenerated(x >> 4, z >> 4)) continue;
            int y = level.getHighestBlockAt(x, z) + 1;
            if (y <= level.getMinHeight() || y >= level.getMaxHeight()) continue;

            String type = mobType(index);
            Entity entity = Entity.createEntity(type,
                    new Position(x + 0.5, y, z + 0.5, level));
            if (entity == null) continue;

            entity.addTag(tag);
            entity.addTag("siedler:pillager_squad");
            entity.setNameTag(messages.getMessage("messages.monsters.raid-mob-name"));
            entity.setNameTagVisible(false);
            entity.spawnToAll();
            return entity;
        }
        return null;
    }

    private String mobType(int index) {
        int cycle = index % 6;
        if (cycle == 5 && plugin.getConfig().getBoolean("monsters.raids.allow-ravager", true)) {
            return "minecraft:ravager";
        }
        if (cycle >= 3 && plugin.getConfig().getBoolean("monsters.raids.allow-vindicator", true)) {
            return "minecraft:vindicator";
        }
        return "minecraft:pillager";
    }

    private OutpostManager.Outpost chooseRaidOutpost() throws SQLException {
        List<OutpostManager.Outpost> outposts = plugin.getOutpostManager().getOutposts();
        List<OutpostManager.Outpost> candidates = new ArrayList<>();
        for (OutpostManager.Outpost outpost : outposts) {
            if (outpost.ownerTeamId() == null) continue;
            if (isTeamOnline(outpost.ownerTeamId())) candidates.add(outpost);
        }
        return candidates.isEmpty() ? null
                : candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private boolean isTeamOnline(String teamId) {
        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            try {
                Team team = teamManager.getTeamForPlayer(player.getUniqueId().toString());
                if (team != null && team.id().equals(teamId)) return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private Level findLevel(String name) {
        for (Level level : plugin.getServer().getLevels().values()) {
            if (level.getName().equals(name)) return level;
        }
        return null;
    }

    private void insertRaid(String id, OutpostManager.Outpost outpost) throws SQLException {
        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(
                "INSERT INTO raids " +
                        "(id, outpost_id, outpost_name, team_id, wave, remaining_mobs, status, started_at) " +
                        "VALUES (?, ?, ?, ?, 0, 0, 'ACTIVE', ?)")) {
            statement.setString(1, id);
            statement.setString(2, outpost.id());
            statement.setString(3, outpost.name());
            statement.setString(4, outpost.ownerTeamId());
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private void finishRaid(String raidId, String status) throws SQLException {
        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(
                "UPDATE raids SET status = ?, finished_at = ? WHERE id = ?")) {
            statement.setString(1, status);
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, raidId);
            statement.executeUpdate();
        }
        if (!"ACTIVE".equals(status)) removeRaidEntities(raidId);
    }

    private void removeRaidEntities(String raidId) {
        String tag = RAID_TAG + raidId;
        try {
            for (Level level : plugin.getServer().getLevels().values()) {
                for (Entity entity : level.getEntities()) {
                    if (entity.containTag(tag)) entity.close();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String findRaidId(Entity entity) {
        for (String tag : entity.getTags()) {
            if (tag.startsWith(RAID_TAG)) return tag.substring(RAID_TAG.length());
        }
        return null;
    }

    private int decrementRemaining(String raidId) throws SQLException {
        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(
                "UPDATE raids SET remaining_mobs = remaining_mobs - 1 " +
                        "WHERE id = ? AND status = 'ACTIVE' AND remaining_mobs > 0")) {
            statement.setString(1, raidId);
            if (statement.executeUpdate() == 0) return -1;
        }

        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(
                "SELECT remaining_mobs FROM raids WHERE id = ?")) {
            statement.setString(1, raidId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : -1;
            }
        }
    }

    private Raid getRaid(String id) throws SQLException {
        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(
                "SELECT id, outpost_id, outpost_name, team_id, wave, status, started_at, finished_at " +
                        "FROM raids WHERE id = ? LIMIT 1")) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    private Raid map(ResultSet result) throws SQLException {
        long finished = result.getLong("finished_at");
        boolean nullFinished = result.wasNull();
        return new Raid(
                result.getString("id"),
                result.getString("outpost_id"),
                result.getString("outpost_name"),
                result.getString("team_id"),
                result.getInt("wave"),
                result.getInt("remaining_mobs"),
                result.getString("status"),
                result.getLong("started_at"),
                finished,
                nullFinished);
    }

    private void reconcileAfterRestart() {
        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(
                "UPDATE raids SET status = 'ABORTED', finished_at = ? WHERE status = 'ACTIVE'")) {
            statement.setLong(1, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not reconcile active raids: " + safe(exception));
        }
    }

    private int maxWaves() {
        return Math.max(1, plugin.getConfig().getInt("monsters.raids.waves", 3));
    }

    private int maxRaidSize() {
        return Math.max(1, plugin.getConfig().getInt("monsters.raids.max-mobs-per-wave", 16));
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("monsters.raids.enabled", true);
    }

    private boolean automatic() {
        return plugin.getConfig().getBoolean("monsters.raids.automatic", true);
    }

    private void broadcast(String message) {
        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            player.sendMessage(message);
        }
    }

    private String safe(Exception exception) {
        return exception.getMessage() == null ? "Unbekannter Fehler" : exception.getMessage();
    }

    public record Raid(
            String id,
            String outpostId,
            String outpostName,
            String teamId,
            int wave,
            String status,
            long startedAt,
            long finishedAt,
            boolean finishedAtNull
    ) {}
}
