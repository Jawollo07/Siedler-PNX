package de.jawollo07.siedler.market;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.entity.Entity;
import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.block.BlockBreakEvent;
import org.powernukkitx.event.block.BlockPlaceEvent;
import org.powernukkitx.event.entity.EntitySpawnEvent;
import org.powernukkitx.event.player.PlayerInteractEvent;
import org.powernukkitx.Player;
import org.powernukkitx.utils.ConfigSection;

import java.util.ArrayList;
import java.util.List;

public final class MarketManager implements Listener {
    public record Market(String id, String world, int minX, int minZ, int maxX, int maxZ,
                         double spawnX, double spawnY, double spawnZ, boolean enabled) {
        public boolean contains(String level, double x, double z) {
            if (!enabled || !world.equals(level)) return false;
            return x >= Math.min(minX, maxX) && x <= Math.max(minX, maxX)
                    && z >= Math.min(minZ, maxZ) && z <= Math.max(minZ, maxZ);
        }
    }

    private final SiedlerPlugin plugin;
    private final MessageManager messages = new MessageManager();
    private final List<Market> markets = new ArrayList<>();

    public MarketManager(SiedlerPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        markets.clear();
        ConfigSection section = plugin.getConfig().getSection("market");
        if (!section.getBoolean("enabled", true)) return;

        for (Object raw : section.getList("markets", List.of())) {
            if (!(raw instanceof ConfigSection s)) continue;
            String id = s.getString("id", "market");
            String world = normalizeWorld(s.getString("world", "overworld"));
            int minX = s.getInt("min-x", 0), minZ = s.getInt("min-z", 0);
            int maxX = s.getInt("max-x", 0), maxZ = s.getInt("max-z", 0);
            ConfigSection spawn = s.getSection("trader-spawn");
            double x = spawn.getDouble("x", (minX + maxX) / 2.0);
            double y = spawn.getDouble("y", 64);
            double z = spawn.getDouble("z", (minZ + maxZ) / 2.0);
            markets.add(new Market(id, world, minX, minZ, maxX, maxZ, x, y, z, s.getBoolean("enabled", true)));
        }
    }

    public List<Market> getMarkets() {
        return List.copyOf(markets);
    }

    public Market getMarketAt(String world, double x, double z) {
        String normalized = normalizeWorld(world);
        for (Market market : markets) if (market.contains(normalized, x, z)) return market;
        return null;
    }

    public Market getMarket(String id) {
        for (Market market : markets) if (market.id().equalsIgnoreCase(id)) return market;
        return null;
    }

    public boolean isInsideMarket(Entity entity) {
        if (entity == null) return false;
        return getMarketAt(entity.getLevel().getName(), entity.getX(), entity.getZ()) != null;
    }

    public boolean isInsideMarket(Player player) {
        return getMarketAt(player.getLevel().getName(), player.getX(), player.getZ()) != null;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!isInsideMarket(event.getPlayer())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(messages.getMessage("messages.market.block-breaking"));
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!isInsideMarket(event.getPlayer())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(messages.getMessage("messages.market.block-placing"));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!isInsideMarket(event.getPlayer())) return;
        // The market is intentionally protected from redstone/control-block interaction.
        String id = event.getBlock().getId();
        if (plugin.getConfig().getStringList("market.interaction-blacklist").contains(id)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(messages.getMessage("messages.market.block-interacting"));
        }
    }

    @EventHandler
    public void onMonsterSpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        if (!isHostile(entity) || !isInsideMarket(entity)) return;
        event.setCancelled(true);
    }

    public void cleanupMonsters() {
        for (Market market : markets) {
            if (!market.enabled()) continue;
            var level = plugin.getServer().getLevelByName(market.world());
            if (level == null) continue;
            for (Entity entity : level.getEntities()) {
                if (isHostile(entity) && market.contains(level.getName(), entity.getX(), entity.getZ())) {
                    entity.close();
                }
            }
        }
    }

    private boolean isHostile(Entity entity) {
        String id = entity.getIdentifier();
        return switch (id) {
            case "minecraft:zombie", "minecraft:husk", "minecraft:drowned",
                 "minecraft:skeleton", "minecraft:stray", "minecraft:bogged",
                 "minecraft:creeper", "minecraft:spider", "minecraft:cave_spider",
                 "minecraft:enderman", "minecraft:witch", "minecraft:slime",
                 "minecraft:magma_cube", "minecraft:phantom", "minecraft:silverfish",
                 "minecraft:endermite", "minecraft:guardian", "minecraft:elder_guardian",
                 "minecraft:blaze", "minecraft:ghast", "minecraft:wither_skeleton",
                 "minecraft:piglin", "minecraft:piglin_brute", "minecraft:zombified_piglin",
                 "minecraft:hoglin", "minecraft:zoglin", "minecraft:ravager",
                 "minecraft:vindicator", "minecraft:evoker", "minecraft:pillager",
                 "minecraft:vex", "minecraft:warden" -> true;
            default -> false;
        };
    }

    private String normalizeWorld(String world) {
        if (world == null) return "";
        return world.startsWith("minecraft:") ? world.substring(10) : world;
    }
}
