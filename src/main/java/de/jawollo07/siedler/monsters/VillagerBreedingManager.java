package de.jawollo07.siedler.monsters;

import org.powernukkitx.block.BlockBed;
import org.powernukkitx.entity.Entity;
import org.powernukkitx.entity.passive.EntityVillagerV2;
import org.powernukkitx.inventory.EntityEquipmentInventory;
import org.powernukkitx.item.Item;
import org.powernukkitx.level.Level;
import org.powernukkitx.plugin.PluginBase;
import org.powernukkitx.entity.ai.memory.CoreMemoryTypes;
import org.cloudburstmc.protocol.bedrock.data.actor.ActorFlags;

import java.util.HashMap;
import java.util.Map;

/**
 * Siedler-owned villager breeding fallback.
 *
 * Breeding is deliberately independent of PowerNukkitX Village membership:
 * two adult ordinary villagers + enough food + one free valid bed nearby
 * are sufficient to create a baby villager anywhere in the world.
 *
 * Siedler traders are excluded by their persistent siedler:trader:* tags.
 */
public final class VillagerBreedingManager implements Runnable {

    private static final int FOOD_POINTS_REQUIRED = 12;
    private static final long DEFAULT_COOLDOWN_TICKS = 6000L; // 5 minutes
    private static final int DEFAULT_PAIR_RADIUS = 16;
    private static final int BED_SCAN_RADIUS = 16;
    private static final int BED_SCAN_VERTICAL = 5;

    private final PluginBase plugin;
    private final Map<Long, Long> cooldownUntil = new HashMap<>();

    public VillagerBreedingManager(PluginBase plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("villagers.control.breeding.enabled", true)) {
            return;
        }

        long now = System.currentTimeMillis();
        cooldownUntil.entrySet().removeIf(entry -> entry.getValue() <= now);

        for (Level level : plugin.getServer().getLevels().values()) {
            if (level == null) continue;

            Entity[] entities = level.getEntities();
            for (Entity entity : entities) {
                if (!(entity instanceof EntityVillagerV2 first)) continue;
                if (!isBreedable(first)) continue;
                if (isCoolingDown(first.getId(), now)) continue;

                EntityVillagerV2 second = findPartner(first, entities, now);
                if (second == null) continue;

                // No Village/POI lookup is performed here. A free valid bed is
                // the only bed requirement for the Siedler breeding fallback.
                if (!hasFreeBedNear(level, first)) continue;

                // Explicitly require enough actual food on both parents.
                if (first.getFoodPoints() < FOOD_POINTS_REQUIRED
                        || second.getFoodPoints() < FOOD_POINTS_REQUIRED) {
                    continue;
                }

                // Consume the food as part of this breeding operation so the
                // fallback has deterministic vanilla-like food requirements.
                if (!consumeFood(first) || !consumeFood(second)) {
                    continue;
                }

                Entity babyEntity = Entity.createEntity(
                        Entity.VILLAGER_V2,
                        first.getPosition()
                );
                if (babyEntity == null) {
                    continue;
                }
                if (!(babyEntity instanceof EntityVillagerV2)) {
                    babyEntity.close();
                    continue;
                }

                EntityVillagerV2 baby = (EntityVillagerV2) babyEntity;
                baby.setBaby(true);
                baby.spawnToAll();

                first.getMemoryStorage().put(CoreMemoryTypes.WILLING, false);
                second.getMemoryStorage().put(CoreMemoryTypes.WILLING, false);
                first.getMemoryStorage().clear(CoreMemoryTypes.ENTITY_SPOUSE);
                second.getMemoryStorage().clear(CoreMemoryTypes.ENTITY_SPOUSE);
                first.getMemoryStorage().put(CoreMemoryTypes.LAST_IN_LOVE_TIME, level.getTick());
                second.getMemoryStorage().put(CoreMemoryTypes.LAST_IN_LOVE_TIME, level.getTick());
                first.setDataFlag(ActorFlags.IN_LOVE, false);
                second.setDataFlag(ActorFlags.IN_LOVE, false);

                long cooldown = Math.max(
                        1000L,
                        plugin.getConfig().getLong(
                                "villagers.control.breeding.cooldown-seconds",
                                DEFAULT_COOLDOWN_TICKS / 20L
                        ) * 1000L
                );
                cooldownUntil.put(first.getId(), now + cooldown);
                cooldownUntil.put(second.getId(), now + cooldown);
            }
        }
    }

    private EntityVillagerV2 findPartner(
            EntityVillagerV2 first,
            Entity[] entities,
            long now
    ) {
        int radius = Math.max(
                1,
                plugin.getConfig().getInt("villagers.control.breeding.radius", DEFAULT_PAIR_RADIUS)
        );
        double maxDistanceSquared = radius * (double) radius;
        EntityVillagerV2 nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity entity : entities) {
            if (!(entity instanceof EntityVillagerV2 second)) continue;
            if (second == first || !isBreedable(second)) continue;
            if (isCoolingDown(second.getId(), now)) continue;
            if (second.distanceSquared(first) > maxDistanceSquared) continue;

            double distance = second.distanceSquared(first);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = second;
            }
        }

        return nearest;
    }

    private boolean isBreedable(EntityVillagerV2 villager) {
        return !villager.isClosed()
                && !villager.isBaby()
                && !isSiedlerTrader(villager);
    }

    private boolean isCoolingDown(long entityId, long now) {
        Long until = cooldownUntil.get(entityId);
        return until != null && until > now;
    }

    private boolean hasFreeBedNear(Level level, EntityVillagerV2 villager) {
        int radius = Math.max(
                1,
                plugin.getConfig().getInt("villagers.control.breeding.bed-radius", BED_SCAN_RADIUS)
        );
        int vertical = Math.max(
                1,
                plugin.getConfig().getInt(
                        "villagers.control.breeding.bed-vertical-radius",
                        BED_SCAN_VERTICAL
                )
        );

        int minX = villager.getFloorX() - radius;
        int maxX = villager.getFloorX() + radius;
        int minY = Math.max(0, villager.getFloorY() - vertical);
        int maxY = villager.getFloorY() + vertical;
        int minZ = villager.getFloorZ() - radius;
        int maxZ = villager.getFloorZ() + radius;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (level.getBlock(x, y, z, false) instanceof BlockBed bed
                            && bed.isBedValid()
                            && !bed.isOccupied()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean consumeFood(EntityVillagerV2 villager) {
        EntityEquipmentInventory inventory = villager.getInventory();
        if (inventory == null) return false;

        int remaining = FOOD_POINTS_REQUIRED;

        for (Map.Entry<Integer, Item> entry : inventory.getContents().entrySet()) {
            if (remaining <= 0) break;

            Item item = entry.getValue();
            if (item == null || item.isNull()) continue;

            int pointsPerItem = foodPoints(item);
            if (pointsPerItem <= 0) continue;

            int remove = Math.min(
                    item.getCount(),
                    (remaining + pointsPerItem - 1) / pointsPerItem
            );

            item.setCount(item.getCount() - remove);
            if (item.getCount() <= 0) {
                inventory.setItem(entry.getKey(), Item.AIR);
            } else {
                inventory.setItem(entry.getKey(), item);
            }

            remaining -= remove * pointsPerItem;
        }

        return remaining <= 0;
    }

    private int foodPoints(Item item) {
        return switch (item.getId()) {
            case Item.BREAD -> 4;
            case Item.CARROT, Item.POTATO -> 1;
            default -> 0;
        };
    }

    private boolean isSiedlerTrader(EntityVillagerV2 villager) {
        String[] traderTypes = {
                "food", "building", "resources", "tools",
                "weapons", "supplies", "soldiers", "enchantments"
        };
        for (String type : traderTypes) {
            if (villager.containTag("siedler:trader:" + type)) {
                return true;
            }
        }
        return false;
    }
}
