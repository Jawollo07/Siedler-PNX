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
import org.powernukkitx.scheduler.TaskHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class TokenManager implements Listener, Runnable {
    private static final String TOKEN_TAG = "siedler:token";
    private final SiedlerPlugin plugin;
    private final MessageManager messageManager;
    private final TeamManager teamManager;
    private TaskHandler task;
    private long lastAutomaticSpawn;

    public TokenManager(SiedlerPlugin plugin) {
        this.plugin = plugin;
        this.messageManager = new MessageManager();
        this.teamManager = new TeamManager(plugin);
    }

    public void start() {
        if (!enabled()) return;
        task = plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, this, 20 * 5);
        plugin.getLogger().info("Token system started.");
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public synchronized void run() {
        if (!enabled() || !automatic()) return;
        long interval = Math.max(1, plugin.getConfig().getInt(
                "monsters.token.spawn-interval-minutes", 30)) * 60_000L;
        long now = System.currentTimeMillis();
        if (lastAutomaticSpawn != 0 && now - lastAutomaticSpawn < interval) return;

        try {
            if (countActiveTokens() >= maxActive()) return;
            Player target = chooseOnlinePlayer();
            if (target == null) return;
            if (spawnToken(target)) {
                lastAutomaticSpawn = now;
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Automatic token spawn failed: " + exception.getMessage());
        }
    }

    public synchronized boolean spawnToken(Player near) throws SQLException {
        if (!enabled() || near == null) return false;
        if (countActiveTokens() >= maxActive()) return false;

        Level level = near.getLevel();
        if (level == null) return false;

        int radius = Math.max(4, plugin.getConfig().getInt(
                "monsters.token.spawn-radius", 16));
        for (int attempt = 0; attempt < 12; attempt++) {
            int x = near.getFloorX() + ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            int z = near.getFloorZ() + ThreadLocalRandom.current().nextInt(-radius, radius + 1);

            if (!level.isChunkGenerated(x >> 4, z >> 4)) continue;

            int y = level.getHighestBlockAt(x, z) + 1;
            if (y <= level.getMinHeight() || y >= level.getMaxHeight()) continue;

            org.powernukkitx.level.Position position =
                    new org.powernukkitx.level.Position(x + 0.5, y, z + 0.5, level);
            Entity entity = Entity.createEntity(
                    plugin.getConfig().getString("monsters.token.entity", "minecraft:zombie"),
                    position);
            if (entity == null) continue;

            entity.addTag(TOKEN_TAG);
            entity.setNameTag(messageManager.getMessage(
                    "messages.monsters.token-name"));
            entity.setNameTagVisible(true);
            entity.setNameTagAlwaysVisible(true);
            entity.spawnToAll();

            String roundId = getOrCreateRound();
            insertToken(roundId, entity);
            return true;
        }
        return false;
    }

    @EventHandler
    public void onTokenDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || !entity.containTag(TOKEN_TAG)) return;

        try {
            String entityId = entity.getUniqueId().toString();
            TokenRecord token = findToken(entityId);
            if (token == null || token.defeated()) return;

            markDefeated(token.id());
            awardBonus(event, entity);
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not process token defeat: "
                    + exception.getMessage());
        }
    }

    private void awardBonus(EntityDeathEvent event, Entity token) throws SQLException {
        Entity killerEntity = token.getLastDamageCause() instanceof
                org.powernukkitx.event.entity.EntityDamageByEntityEvent damage
                ? damage.getDamager() : null;
        if (!(killerEntity instanceof Player killer)) return;

        Team team = teamManager.getTeamForPlayer(killer.getUniqueId().toString());
        if (team == null || team.eliminated() != 0) return;

        int bonus = Math.max(1, plugin.getConfig().getInt(
                "monsters.token.tax-bonus", 1));
        String sourceId = "token:" + token.getUniqueId();

        String sql = "INSERT INTO team_bonus_sources " +
                "(id, team_id, source_type, source_id, amount, permanent, created_at) " +
                "VALUES (?, ?, ?, ?, ?, 1, ?)";
        try (PreparedStatement statement = plugin.getStorage().getConnection()
                .prepareStatement(sql)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, team.id());
            statement.setString(3, "TOKEN");
            statement.setString(4, sourceId);
            statement.setInt(5, bonus);
            statement.setLong(6, System.currentTimeMillis());
            statement.executeUpdate();
        }

        killer.sendMessage(messageManager.getMessage(
                "messages.monsters.token-reward")
                .replace("{bonus}", String.valueOf(bonus)));
    }

    private String getOrCreateRound() throws SQLException {
        try (PreparedStatement select = plugin.getStorage().getConnection()
                .prepareStatement("SELECT id FROM token_rounds WHERE completed = 0 ORDER BY started_at DESC LIMIT 1");
             ResultSet result = select.executeQuery()) {
            if (result.next()) return result.getString("id");
        }

        String id = UUID.randomUUID().toString();
        try (PreparedStatement insert = plugin.getStorage().getConnection().prepareStatement(
                "INSERT INTO token_rounds (id, started_at, completed) VALUES (?, ?, 0)")) {
            insert.setString(1, id);
            insert.setLong(2, System.currentTimeMillis());
            insert.executeUpdate();
        }
        return id;
    }

    private void insertToken(String roundId, Entity entity) throws SQLException {
        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(
                "INSERT INTO tokens (id, round_id, entity_uuid, world, x, y, z, defeated, spawned_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?)")) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, roundId);
            statement.setString(3, entity.getUniqueId().toString());
            statement.setString(4, entity.getLevel().getName());
            statement.setDouble(5, entity.getX());
            statement.setDouble(6, entity.getY());
            statement.setDouble(7, entity.getZ());
            statement.setLong(8, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private TokenRecord findToken(String entityUuid) throws SQLException {
        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(
                "SELECT id, round_id, defeated FROM tokens WHERE entity_uuid = ? LIMIT 1")) {
            statement.setString(1, entityUuid);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                return new TokenRecord(result.getString("id"),
                        result.getString("round_id"),
                        result.getInt("defeated") != 0);
            }
        }
    }

    private void markDefeated(String tokenId) throws SQLException {
        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(
                "UPDATE tokens SET defeated = 1, defeated_at = ? WHERE id = ? AND defeated = 0")) {
            statement.setLong(1, System.currentTimeMillis());
            statement.setString(2, tokenId);
            statement.executeUpdate();
        }
    }

    public int countActiveTokens() throws SQLException {
        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(
                "SELECT COUNT(*) FROM tokens WHERE defeated = 0");
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }

    public boolean startRound() throws SQLException {
        return getOrCreateRound() != null;
    }

    public record TokenRecord(String id, String roundId, boolean defeated) {}

    private boolean enabled() {
        return plugin.getConfig().getBoolean("monsters.token.enabled", true);
    }

    private boolean automatic() {
        return plugin.getConfig().getBoolean("monsters.token.automatic", false);
    }

    private int maxActive() {
        return Math.max(1, plugin.getConfig().getInt("monsters.token.max-active", 4));
    }

    private Player chooseOnlinePlayer() {
        List<Player> players = new ArrayList<>(plugin.getServer().getOnlinePlayers().values());
        if (players.isEmpty()) return null;
        return players.get(ThreadLocalRandom.current().nextInt(players.size()));
    }
}
