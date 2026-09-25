package de.jawollo07.siedler.monsters;

import de.jawollo07.siedler.SiedlerPlugin;
import org.powernukkitx.entity.Entity;
import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.entity.EntitySpawnEvent;
import org.powernukkitx.level.Level;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Central controller for ordinary hostile mob spawning.
 *
 * <p>Token and raid entities can explicitly bypass this controller while they
 * are being created. This keeps special Siedler encounters independent from
 * the normal-world monster rules.</p>
 */
public final class MonsterManager implements Listener {

    private static final Set<String> DEFAULT_MONSTERS = Set.of(
            "minecraft:zombie",
            "minecraft:husk",
            "minecraft:drowned",
            "minecraft:skeleton",
            "minecraft:stray",
            "minecraft:bogged",
            "minecraft:creeper",
            "minecraft:spider",
            "minecraft:cave_spider",
            "minecraft:enderman",
            "minecraft:witch",
            "minecraft:slime",
            "minecraft:magma_cube",
            "minecraft:phantom",
            "minecraft:silverfish",
            "minecraft:endermite",
            "minecraft:guardian",
            "minecraft:elder_guardian",
            "minecraft:blaze",
            "minecraft:ghast",
            "minecraft:wither_skeleton",
            "minecraft:piglin",
            "minecraft:piglin_brute",
            "minecraft:zombified_piglin",
            "minecraft:hoglin",
            "minecraft:zoglin",
            "minecraft:ravager",
            "minecraft:vindicator",
            "minecraft:evoker",
            "minecraft:pillager",
            "minecraft:vex",
            "minecraft:warden"
    );

    private static final ThreadLocal<Boolean> BYPASS = ThreadLocal.withInitial(() -> false);

    private final SiedlerPlugin plugin;

    public MonsterManager(SiedlerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes an entity creation while bypassing the normal monster rules.
     * Used only for controlled Siedler encounters such as Tokens and Raids.
     */
    public static void runWithBypass(Runnable action) {
        boolean previous = BYPASS.get();
        BYPASS.set(true);
        try {
            action.run();
        } finally {
            BYPASS.set(previous);
        }
    }

    /**
     * Returns whether the current entity creation is exempt from normal
     * monster control.
     */
    public static boolean isBypassed() {
        return BYPASS.get();
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event == null || isBypassed()) return;

        Entity entity = event.getEntity();
        if (entity == null) return;

        try {
            // Villager population control is independent from hostile-monster
            // control. Trader entities use the explicit bypass while spawning.
            if (isVillager(entity)) {
                if (villagerLimitReached(entity)) {
                    event.setCancelled(true);
                }
                return;
            }

            if (!enabled() || !isControlledMonster(entity)) return;

            String identifier = entity.getIdentifier();
            if (identifier == null || identifier.isBlank()) return;

            if (isBlacklisted(identifier)) {
                event.setCancelled(true);
                return;
            }

            if (isWorldBlacklisted(entity)) {
                event.setCancelled(true);
                return;
            }

            if (isInsideClaim(entity)) {
                event.setCancelled(true);
                return;
            }

            int quota = quotaPercent(identifier);
            if (quota <= 0 || (quota < 100
                    && ThreadLocalRandom.current().nextInt(100) >= quota)) {
                event.setCancelled(true);
                return;
            }

            int maxPerChunk = Math.max(0, plugin.getConfig()
                    .getInt("monsters.control.max-per-chunk", 12));
            if (maxPerChunk > 0 && countInChunk(entity, identifier) >= maxPerChunk) {
                event.setCancelled(true);
                return;
            }

            int maxPerWorld = Math.max(0, plugin.getConfig()
                    .getInt("monsters.control.max-per-world", 200));
            if (maxPerWorld > 0 && countInWorld(entity, identifier) >= maxPerWorld) {
                event.setCancelled(true);
            }
        } catch (Exception exception) {
            // Never allow a database/config failure to silently create an
            // uncontrolled mob population.
            event.setCancelled(true);
            plugin.getLogger().warning(
                    "Monster spawn was blocked because MonsterManager failed: "
                            + safe(exception));
        }
    }

    private boolean isVillager(Entity entity) {
        String identifier = entity.getIdentifier();
        return "minecraft:villager_v2".equalsIgnoreCase(identifier)
                || "minecraft:villager".equalsIgnoreCase(identifier);
    }

    /**
     * Limits ordinary villager spawning by horizontal X/Z distance.
     * Existing Siedler traders are ignored because they are intentionally
     * created through the MonsterManager bypass.
     */
    private boolean villagerLimitReached(Entity target) {
        if (!plugin.getConfig().getBoolean("villagers.control.enabled", true)) {
            return false;
        }

        int max = Math.max(0, plugin.getConfig()
                .getInt("villagers.control.max-per-radius", 5));
        if (max == 0) return true;

        double radius = Math.max(0.0D, plugin.getConfig()
                .getDouble("villagers.control.radius", 9.0D));
        double radiusSquared = radius * radius;

        Level level = target.getLevel();
        if (level == null) return false;

        int count = 0;
        for (Entity entity : level.getEntities()) {
            if (entity == target || !isVillager(entity)) continue;

            boolean trader = false;
            for (String tag : entity.getTags()) {
                if (tag != null && tag.startsWith("siedler:trader:")) {
                    trader = true;
                    break;
                }
            }
            if (trader) continue;

            double dx = entity.getX() - target.getX();
            double dz = entity.getZ() - target.getZ();
            if ((dx * dx) + (dz * dz) <= radiusSquared) {
                count++;
                if (count >= max) return true;
            }
        }

        return false;
    }

    private boolean isControlledMonster(Entity entity) {
        String identifier = entity.getIdentifier();
        if (identifier == null) return false;

        List<String> configured = plugin.getConfig()
                .getStringList("monsters.control.monsters");

        if (configured != null && !configured.isEmpty()) {
            return configured.stream()
                    .map(String::trim)
                    .anyMatch(identifier::equalsIgnoreCase);
        }

        return DEFAULT_MONSTERS.contains(identifier.toLowerCase());
    }

    private boolean isBlacklisted(String identifier) {
        List<String> blacklist = plugin.getConfig()
                .getStringList("monsters.control.blacklist");
        if (blacklist == null) return false;

        return blacklist.stream()
                .map(String::trim)
                .anyMatch(identifier::equalsIgnoreCase);
    }

    /**
     * Spawn quota in percent. The configuration uses entries such as
     * minecraft:zombie:50. Missing entries use default-quota-percent.
     */
    private int quotaPercent(String identifier) {
        List<String> quotas = plugin.getConfig()
                .getStringList("monsters.control.quotas");

        int defaultQuota = clampPercent(plugin.getConfig()
                .getInt("monsters.control.default-quota-percent", 100));

        if (quotas == null) return defaultQuota;

        for (String raw : quotas) {
            if (raw == null || raw.isBlank()) continue;
            String[] parts = raw.trim().split(":", 3);
            if (parts.length != 3) continue;

            String configuredId = parts[0] + ":" + parts[1];
            if (!configuredId.equalsIgnoreCase(identifier)) continue;

            try {
                return clampPercent(Integer.parseInt(parts[2].trim()));
            } catch (NumberFormatException ignored) {
                return defaultQuota;
            }
        }

        return defaultQuota;
    }

    private int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private boolean isWorldBlacklisted(Entity entity) {
        if (entity.getLevel() == null) return false;
        List<String> worlds = plugin.getConfig()
                .getStringList("monsters.control.world-blacklist");
        if (worlds == null) return false;
        String world = entity.getLevel().getName();
        return worlds.stream().map(String::trim)
                .anyMatch(world::equalsIgnoreCase);
    }

    private boolean isInsideClaim(Entity entity) throws SQLException {
        Level level = entity.getLevel();
        if (level == null) return false;

        int chunkX = entity.getFloorX() >> 4;
        int chunkZ = entity.getFloorZ() >> 4;

        String sql = "SELECT 1 FROM claims " +
                "WHERE world = ? AND min_x <= ? AND max_x >= ? " +
                "AND min_z <= ? AND max_z >= ? LIMIT 1";

        try (PreparedStatement statement = plugin.getStorage().getConnection()
                .prepareStatement(sql)) {
            statement.setString(1, level.getName());
            statement.setInt(2, chunkX);
            statement.setInt(3, chunkX);
            statement.setInt(4, chunkZ);
            statement.setInt(5, chunkZ);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private int countInChunk(Entity target, String identifier) {
        Level level = target.getLevel();
        if (level == null) return 0;

        int chunkX = target.getFloorX() >> 4;
        int chunkZ = target.getFloorZ() >> 4;
        int count = 0;

        for (Entity entity : level.getEntities()) {
            if (entity == target || entity.getIdentifier() == null) continue;
            if (!identifier.equalsIgnoreCase(entity.getIdentifier())) continue;
            if ((entity.getFloorX() >> 4) == chunkX
                    && (entity.getFloorZ() >> 4) == chunkZ) {
                count++;
            }
        }
        return count;
    }

    private int countInWorld(Entity target, String identifier) {
        Level level = target.getLevel();
        if (level == null) return 0;

        int count = 0;
        for (Entity entity : level.getEntities()) {
            if (entity == target || entity.getIdentifier() == null) continue;
            if (identifier.equalsIgnoreCase(entity.getIdentifier())) count++;
        }
        return count;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("monsters.control.enabled", true);
    }

    private String safe(Exception exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
}
