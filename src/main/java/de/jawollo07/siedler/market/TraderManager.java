package de.jawollo07.siedler.market;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import de.jawollo07.siedler.eco.EcoManager;
import de.jawollo07.siedler.team.Team;
import de.jawollo07.siedler.team.TeamManager;
import org.powernukkitx.Player;
import org.powernukkitx.entity.Entity;
import org.powernukkitx.entity.passive.EntityVillagerV2;
import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.player.PlayerInteractEntityEvent;
import org.powernukkitx.form.window.SimpleForm;
import org.powernukkitx.item.Item;
import org.powernukkitx.nbt.tag.CompoundTag;
import org.powernukkitx.utils.ConfigSection;

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
    private final TeamManager teamManager;
    private final EcoManager ecoManager;
    private final List<TraderType> types = new ArrayList<>();

    public TraderManager(SiedlerPlugin plugin, MarketManager marketManager) {
        this.plugin = plugin;
        this.marketManager = marketManager;
        this.teamManager = new TeamManager(plugin);
        this.ecoManager = new EcoManager(plugin);
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
                        buyId,
                        Math.max(1, buy.getInt("count", 1)),
                        buy2Id,
                        Math.max(0, buy2.getInt("count", 0)),
                        sellId,
                        Math.max(1, sell.getInt("count", 1)),
                        Math.max(1, t.getInt("max-uses", 16)),
                        Math.max(1, t.getInt("tier", 1)),
                        Math.max(0, t.getInt("trader-exp", 1)),
                        (float) t.getDouble("price-multiplier", 0)
                ));
            }

            types.add(new TraderType(id, name, List.copyOf(trades)));
        }
    }

    @Override
    public void run() {
        maintain();
    }

    /** Removes every Siedler trader from every currently loaded world. */
    public int removeAllTraders() {
        int removed = 0;
        for (org.powernukkitx.level.Level level : plugin.getServer().getLevels().values()) {
            for (Entity entity : level.getEntities()) {
                if (!(entity instanceof EntityVillagerV2 trader)) continue;
                if (!isSiedlerTrader(trader)) continue;
                try {
                    trader.close();
                    removed++;
                } catch (Exception exception) {
                    plugin.getLogger().warning("Siedler-Händler konnte nicht entfernt werden: " + safeError(exception));
                }
            }
        }
        if (removed > 0) plugin.getLogger().info(removed + " Siedler-Händler entfernt.");
        return removed;
    }

    /** Removes old traders and creates a fresh set from the current config. */
    public void respawnAllTraders() {
        removeAllTraders();
        maintain();
    }

    public void stop() {
        // Traders are intentionally not persisted between server sessions.
        removeAllTraders();
    }

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

        EntityVillagerV2 trader = createTrader(
                player.getLevel(),
                player.getX() + 1,
                player.getY(),
                player.getZ() + 1,
                traderType
        );

        if (trader == null) {
            player.sendMessage(messages.getMessage("messages.market.trader-error")
                    .replace("{error}", "Der Händler konnte von PowerNukkitX nicht erzeugt werden."));
            return null;
        }

        player.sendMessage(messages.getMessage("messages.market.trader-spawned")
                .replace("{name}", traderType.name()));

        return trader;
    }

    public void maintain() {
        if (!plugin.getConfig().getBoolean("market.traders.automatic", true)) return;

        for (MarketManager.Market market : marketManager.getMarkets()) {
            if (!market.enabled()) continue;

            var level = resolveLevel(market.world());
            if (level == null) {
                plugin.getLogger().warning(
                        "Markt '" + market.id() + "' konnte nicht geladen werden: Welt '"
                                + market.world() + "' ist nicht geladen."
                );
                continue;
            }

            int target = Math.max(
                    0,
                    plugin.getConfig().getInt("market.traders.count-per-type", 1)
            );

            for (TraderType type : types) {
                int count = 0;

                for (Entity entity : level.getEntities()) {
                    if (!(entity instanceof EntityVillagerV2 trader)) continue;
                    if (!trader.containTag("siedler:trader:" + type.id())) continue;

                    if (market.contains(
                            level.getName(),
                            trader.getX(),
                            trader.getZ()
                    )) {
                        count++;
                    }
                }

                while (count < target) {
                    EntityVillagerV2 trader = createTrader(
                            level,
                            market.spawnX(),
                            market.spawnY(),
                            market.spawnZ(),
                            type
                    );

                    if (trader == null) {
                        plugin.getLogger().warning(
                                "Händler '" + type.id() + "' konnte im Markt '"
                                        + market.id() + "' nicht gespawnt werden."
                        );
                        break;
                    }

                    count++;
                }
            }
        }
    }

    /**
     * Creates a persistent Siedler trader. PowerNukkitX fires EntitySpawnEvent
     * during Entity.createEntity(), therefore the MonsterManager bypass must
     * surround the actual creation. The Persistent NBT flag prevents the
     * villager from being treated like an ordinary despawnable mob.
     */
    private EntityVillagerV2 createTrader(
            org.powernukkitx.level.Level level,
            double x,
            double y,
            double z,
            TraderType type
    ) {
        try {
            org.powernukkitx.level.Position position =
                    new org.powernukkitx.level.Position(x, y, z, level);
            CompoundTag nbt = Entity.getDefaultNBT(position);
            nbt.putBoolean("Persistent", true);

            Entity[] created = new Entity[1];
            de.jawollo07.siedler.monsters.MonsterManager.runWithBypass(() ->
                    created[0] = Entity.createEntity(
                            Entity.VILLAGER_V2,
                            position.getChunk(),
                            nbt
                    )
            );

            if (!(created[0] instanceof EntityVillagerV2 trader)) {
                return null;
            }

            configure(trader, type);
            trader.spawnToAll();
            return trader;
        } catch (Exception exception) {
            plugin.getLogger().warning(
                    "Händler '" + type.id() + "' konnte nicht erzeugt werden: "
                            + safeError(exception)
            );
            return null;
        }
    }

    /**
     * Accepts both the configured PNX level name and the common
     * "overworld"/"world" aliases.
     */
    private org.powernukkitx.level.Level resolveLevel(String configuredWorld) {
        if (configuredWorld == null || configuredWorld.isBlank()) return null;

        String world = configuredWorld.startsWith("minecraft:")
                ? configuredWorld.substring("minecraft:".length())
                : configuredWorld;

        var level = plugin.getServer().getLevelByName(world);
        if (level != null) return level;

        if ("overworld".equalsIgnoreCase(world)) {
            return plugin.getServer().getLevelByName("world");
        }
        if ("world".equalsIgnoreCase(world)) {
            return plugin.getServer().getLevelByName("overworld");
        }

        return null;
    }

    @EventHandler
    public void onTraderInteract(PlayerInteractEntityEvent event) {
        if (!(event.getEntity() instanceof EntityVillagerV2 trader)) return;

        TraderType type = findTraderType(trader);
        if (type == null) return;

        event.setCancelled(true);
        openTraderMenu(event.getPlayer(), type);
    }

    private void openTraderMenu(Player player, TraderType type) {
        Team team;

        try {
            team = teamManager.getTeamForPlayer(player.getUniqueId().toString());
        } catch (Exception exception) {
            player.sendMessage(messages.getMessage("messages.market.trader-error")
                    .replace("{error}", safeError(exception)));
            return;
        }

        if (team == null) {
            player.sendMessage(messages.getMessage("messages.market.trader-no-team"));
            return;
        }

        if (team.eliminated() != 0) {
            player.sendMessage(messages.getMessage("messages.market.trader-team-eliminated"));
            return;
        }

        Integer balance = ecoManager.getMoney(team.id());
        if (balance == null) {
            player.sendMessage(messages.getMessage("messages.market.trader-error")
                    .replace("{error}", "Teamkonto nicht gefunden"));
            return;
        }

        String symbol = ecoManager.getCurrency("s");
        SimpleForm form = new SimpleForm(
                type.name(),
                messages.getMessage("messages.market.trader-header")
                        .replace("{team}", team.name())
                        .replace("{balance}", String.valueOf(balance))
                        .replace("{symbol}", symbol)
        );

        if (type.trades().isEmpty()) {
            form.addButton(messages.getMessage("messages.market.trader-no-trades"));
        } else {
            for (int index = 0; index < type.trades().size(); index++) {
                int tradeIndex = index;
                Trade trade = type.trades().get(index);

                form.addButton(
                        formatTradeButton(trade, symbol),
                        ignored -> executeTrade(player, type, tradeIndex)
                );
            }
        }

        form.addButton(messages.getMessage("messages.market.trader-close"));
        form.send(player);
    }

    private void executeTrade(Player player, TraderType type, int tradeIndex) {
        if (tradeIndex < 0 || tradeIndex >= type.trades().size()) return;

        Trade trade = type.trades().get(tradeIndex);

        Team team;
        try {
            team = teamManager.getTeamForPlayer(player.getUniqueId().toString());
        } catch (Exception exception) {
            player.sendMessage(messages.getMessage("messages.market.trader-error")
                    .replace("{error}", safeError(exception)));
            return;
        }

        if (team == null) {
            player.sendMessage(messages.getMessage("messages.market.trader-no-team"));
            return;
        }

        if (team.eliminated() != 0) {
            player.sendMessage(messages.getMessage("messages.market.trader-team-eliminated"));
            return;
        }

        int cost = Math.max(1, trade.buyCount());
        Item output = Item.get(trade.sellId(), 0, trade.sellCount());

        if (!player.getInventory().canAddItem(output)) {
            player.sendMessage(messages.getMessage("messages.market.trader-inventory-full"));
            return;
        }

        Item required = null;
        if (!trade.buy2Id().isBlank() && trade.buy2Count() > 0) {
            required = Item.get(trade.buy2Id(), 0, trade.buy2Count());

            if (!player.getInventory().contains(required)) {
                player.sendMessage(messages.getMessage("messages.market.trader-missing-item")
                        .replace("{item}", trade.buy2Id())
                        .replace("{count}", String.valueOf(trade.buy2Count())));
                return;
            }
        }

        Integer balance = ecoManager.getMoney(team.id());
        if (balance == null || balance < cost) {
            player.sendMessage(messages.getMessage("messages.market.trader-insufficient-money")
                    .replace("{cost}", String.valueOf(cost))
                    .replace("{symbol}", ecoManager.getCurrency("s"))
                    .replace("{balance}", String.valueOf(balance == null ? 0 : balance)));
            return;
        }

        try {
            // Deduct the shared team currency atomically.
            ecoManager.changeMoney(
                    team.id(),
                    -cost,
                    "MARKET_BUY",
                    "Händlerkauf: " + type.id() + " -> " + trade.sellId(),
                    player.getUniqueId().toString()
            );

            if (required != null) {
                Item[] leftovers = player.getInventory().removeItem(required);
                if (leftovers.length > 0) {
                    // Safety rollback if the required material could not be removed.
                    ecoManager.addMoney(team.id(), cost);
                    player.sendMessage(messages.getMessage("messages.market.trader-error")
                            .replace("{error}", "Benötigtes Material konnte nicht sicher entfernt werden."));
                    return;
                }
            }

            Item[] leftovers = player.getInventory().addItem(output);
            if (leftovers.length > 0) {
                // Safety rollback. The output should normally fit because canAddItem was checked.
                if (required != null) {
                    player.getInventory().addItem(required);
                }
                ecoManager.addMoney(team.id(), cost);
                player.sendMessage(messages.getMessage("messages.market.trader-error")
                        .replace("{error}", "Das Ergebnis konnte nicht sicher ins Inventar gelegt werden."));
                return;
            }

            player.sendMessage(messages.getMessage("messages.market.trader-purchased")
                    .replace("{item}", trade.sellId())
                    .replace("{count}", String.valueOf(trade.sellCount()))
                    .replace("{cost}", String.valueOf(cost))
                    .replace("{symbol}", ecoManager.getCurrency("s")));

        } catch (Exception exception) {
            player.sendMessage(messages.getMessage("messages.market.trader-error")
                    .replace("{error}", safeError(exception)));
        }
    }

    private String formatTradeButton(Trade trade, String symbol) {
        StringBuilder label = new StringBuilder();

        label.append(trade.sellCount())
                .append("x ")
                .append(trade.sellId())
                .append("  §8• §f")
                .append(trade.buyCount())
                .append(" ")
                .append(symbol);

        if (!trade.buy2Id().isBlank() && trade.buy2Count() > 0) {
            label.append(" + ")
                    .append(trade.buy2Count())
                    .append("x ")
                    .append(trade.buy2Id());
        }

        return label.toString();
    }

    private boolean isSiedlerTrader(EntityVillagerV2 trader) {
        for (TraderType type : types) {
            if (trader.containTag("siedler:trader:" + type.id())) return true;
        }
        return false;
    }

    private TraderType findTraderType(EntityVillagerV2 trader) {
        for (TraderType candidate : types) {
            if (trader.containTag("siedler:trader:" + candidate.id())) {
                return candidate;
            }
        }
        return null;
    }

    private void configure(EntityVillagerV2 trader, TraderType type) {
        trader.setProfession(0, false);
        trader.setDisplayName(type.name());
        trader.setCanTrade(false);
        trader.addTag("siedler:trader:" + type.id());
    }

    private TraderType getType(String id) {
        for (TraderType type : types) {
            if (type.id().equalsIgnoreCase(id)) return type;
        }
        return null;
    }

    private String safeError(Exception exception) {
        return exception.getMessage() == null
                ? "Unbekannter Fehler"
                : exception.getMessage();
    }

    private String format(String template, String... replacements) {
        String result = template;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            result = result.replace(
                    "{" + replacements[i] + "}",
                    replacements[i + 1]
            );
        }
        return result;
    }
}
