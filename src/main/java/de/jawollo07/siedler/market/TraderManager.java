package de.jawollo07.siedler.market;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.entity.Entity;
import org.powernukkitx.entity.passive.EntityVillagerV2;
import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.player.PlayerInteractEntityEvent;
import org.powernukkitx.item.Item;
import org.powernukkitx.inventory.TradeInventory;
import org.powernukkitx.utils.ConfigSection;
import org.powernukkitx.utils.TradeRecipeBuildUtils;

import java.util.ArrayList;
import java.util.List;

public final class TraderManager implements Listener, Runnable {
    public record Trade(String buyId, int buyCount, String buy2Id, int buy2Count,
                        String sellId, int sellCount, int maxUses, int tier, int traderExp,
                        float multiplier) {}
    public record TraderType(String id, String name, List<Trade> trades) {}

    private final SiedlerPlugin plugin;
    private final MarketManager marketManager;
    private final MessageManager messages = new MessageManager();
    private final List<TraderType> types = new ArrayList<>();

    public TraderManager(SiedlerPlugin plugin, MarketManager marketManager) {
        this.plugin = plugin;
        this.marketManager = marketManager;
        load();
    }

    public void load() {
        types.clear();
        ConfigSection section = plugin.getConfig().getSection("market.traders");
        ConfigSection configuredTypes = section.getSection("types");
        for (var entry : configuredTypes.entrySet()) {
            if (!(entry.getValue() instanceof ConfigSection s)) continue;
            String id = entry.getKey();
            String name = s.getString("name", id);
            List<Trade> trades = new ArrayList<>();
            for (Object raw : s.getList("trades", List.of())) {
                if (!(raw instanceof ConfigSection t)) continue;
                ConfigSection buy = t.getSection("buy");
                ConfigSection buy2 = t.getSection("buy2");
                ConfigSection sell = t.getSection("sell");
                String buyId = buy.getString("item", "");
                String buy2Id = buy2.getString("item", "");
                String sellId = sell.getString("item", "");
                if (buyId.isBlank() || sellId.isBlank()) continue;
                trades.add(new Trade(
                        buyId, Math.max(1, buy.getInt("count", 1)),
                        buy2Id, Math.max(0, buy2.getInt("count", 0)),
                        sellId, Math.max(1, sell.getInt("count", 1)),
                        Math.max(1, t.getInt("max-uses", 16)),
                        Math.max(1, t.getInt("tier", 1)),
                        Math.max(0, t.getInt("trader-exp", 1)),
                        (float)t.getDouble("price-multiplier", 0)
                ));
            }
            types.add(new TraderType(id, name, List.copyOf(trades)));
        }
    }

    @Override
    public void run() { maintain(); }

    public void stop() { }

    public List<TraderType> getTypes() {
        return List.copyOf(types);
    }

    public EntityVillagerV2 spawnTrader(Player player, String type) {
        TraderType traderType = getType(type);
        if (traderType == null) {
            player.sendMessage(messages.getMessage("messages.market.trader-unknown")
                    .replace("{type}", type));
            return null;
        }
        MarketManager.Market market = marketManager.getMarketAt(
                player.getLevel().getName(), player.getX(), player.getZ());
        if (market == null) {
            player.sendMessage(messages.getMessage("messages.market.trader-market-only"));
            return null;
        }
        Entity entity = Entity.createEntity(
                Entity.VILLAGER_V2,
                new org.powernukkitx.level.Position(player.getX() + 1, player.getY(), player.getZ() + 1, player.getLevel())
        );
        if (!(entity instanceof EntityVillagerV2 trader)) return null;
        configure(trader, traderType);
        trader.spawnToAll();
        player.sendMessage(messages.getMessage("messages.market.trader-spawned")
                .replace("{name}", traderType.name()));
        return trader;
    }

    public void maintain() {
        if (!plugin.getConfig().getBoolean("market.traders.automatic", true)) return;
        for (MarketManager.Market market : marketManager.getMarkets()) {
            if (!market.enabled()) continue;
            var level = plugin.getServer().getLevelByName(market.world());
            if (level == null) continue;

            int target = Math.max(0, plugin.getConfig().getInt("market.traders.count-per-type", 1));
            for (TraderType type : types) {
                int count = 0;
                for (Entity entity : level.getEntities()) {
                    if (!(entity instanceof EntityVillagerV2 trader)) continue;
                    if (!trader.containTag("siedler:trader:" + type.id())) continue;
                    if (market.contains(level.getName(), trader.getX(), trader.getZ())) count++;
                }
                while (count < target) {
                    Entity entity = Entity.createEntity(
                            Entity.VILLAGER_V2,
                            new org.powernukkitx.level.Position(market.spawnX(), market.spawnY(), market.spawnZ(), level)
                    );
                    if (!(entity instanceof EntityVillagerV2 trader)) break;
                    configure(trader, type);
                    trader.spawnToAll();
                    count++;
                }
            }
        }
    }

    @EventHandler
    public void onTraderInteract(PlayerInteractEntityEvent event) {
        if (!(event.getEntity() instanceof EntityVillagerV2 trader)) return;
        if (!trader.containTag("siedler:trader:")) return;
        TraderType type = null;
        for (TraderType candidate : types) {
            if (trader.containTag("siedler:trader:" + candidate.id())) {
                type = candidate;
                break;
            }
        }
        if (type == null) return;

        event.setCancelled(true);
        configure(trader, type);
        TradeInventory inventory = new TradeInventory(trader);
        event.getPlayer().addWindow(inventory);
    }

    private void configure(EntityVillagerV2 trader, TraderType type) {
        trader.getTradeNetIds().clear();
        trader.setProfession(0, false);
        trader.setDisplayName(type.name());
        trader.setCanTrade(true);
        trader.setTradeTier(1);
        trader.setMaxTradeTier(5);
        trader.getTradeNetIds().clear();
        trader.removeAllTags();
        trader.addTag("siedler:trader:" + type.id());

        for (Trade trade : type.trades()) {
            Item buy = Item.get(trade.buyId(), 0, trade.buyCount());
            Item sell = Item.get(trade.sellId(), 0, trade.sellCount());
            TradeRecipeBuildUtils builder;
            if (!trade.buy2Id().isBlank() && trade.buy2Count() > 0) {
                builder = TradeRecipeBuildUtils.of(buy, Item.get(trade.buy2Id(), 0, trade.buy2Count()), sell);
            } else {
                builder = TradeRecipeBuildUtils.of(buy, sell);
            }
            CompoundTag recipe = builder.setMaxUses(trade.maxUses())
                    .setTier(trade.tier())
                    .setTraderExp(trade.traderExp())
                    .setPriceMultiplierA(trade.multiplier())
                    .setRewardExp((byte)1)
                    .build();
            trader.getTradeNetIds().add(recipe.getInt("netId"));
        }
    }

    private TraderType getType(String id) {
        for (TraderType type : types) if (type.id().equalsIgnoreCase(id)) return type;
        return null;
    }
}
