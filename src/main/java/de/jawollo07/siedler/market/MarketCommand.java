package de.jawollo07.siedler.market;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;
import org.powernukkitx.command.route.node.RouteNode;

public final class MarketCommand extends Command {
    private final SiedlerPlugin plugin;
    private final MessageManager messages = new MessageManager();
    private final MarketManager marketManager;
    private final TraderManager traderManager;

    public MarketCommand(SiedlerPlugin plugin, MarketManager marketManager, TraderManager traderManager) {
        super("market", "Siedler Marktplatz", "/market");
        this.plugin = plugin;
        this.marketManager = marketManager;
        this.traderManager = traderManager;
        setPermission("siedler.command.market");
        setPermissionMessage(messages.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().then(RouteNode.literal("help").exec(ctx -> {
            ctx.getSender().sendMessage(messages.getMessage("messages.market.help"));
            return CommandResult.success();
        }));
        tree.getRoot().then(RouteNode.literal("info").exec(ctx -> info(ctx.getSender())));
        tree.getRoot().then(RouteNode.literal("types").exec(ctx -> types(ctx.getSender())));
        tree.getRoot().then(RouteNode.literal("admin")
                .permission("siedler.admin", messages.getCommandMessage("no-permission"))
                .then(RouteNode.literal("reload").exec(ctx -> { marketManager.load(); traderManager.load(); ctx.getSender().sendMessage(messages.getMessage("messages.market.reloaded")); return CommandResult.success(); }))
                .then(RouteNode.literal("cleanup").exec(ctx -> { marketManager.cleanupMonsters(); ctx.getSender().sendMessage(messages.getMessage("messages.market.cleanup")); return CommandResult.success(); }))
                .then(RouteNode.literal("spawn").then(RouteNode.argument("type").exec(ctx -> {
                    if (!(ctx.getSender() instanceof Player player)) return CommandResult.fail();
                    traderManager.spawnTrader(player, String.valueOf(ctx.getArgument("type")));
                    return CommandResult.success();
                })))
                .then(RouteNode.literal("help").exec(ctx -> { ctx.getSender().sendMessage(messages.getMessage("messages.market.admin-help")); return CommandResult.success(); })));
    }

    private CommandResult info(CommandSender sender) {
        if (marketManager.getMarkets().isEmpty()) {
            sender.sendMessage(messages.getMessage("messages.market.none"));
            return CommandResult.success();
        }
        for (MarketManager.Market market : marketManager.getMarkets()) {
            sender.sendMessage(messages.getMessage("messages.market.info")
                    .replace("{id}", market.id())
                    .replace("{world}", market.world())
                    .replace("{min}", market.minX()+","+market.minZ())
                    .replace("{max}", market.maxX()+","+market.maxZ()));
        }
        return CommandResult.success();
    }

    private CommandResult types(CommandSender sender) {
        sender.sendMessage(messages.getMessage("messages.market.types"));
        for (TraderManager.TraderType type : traderManager.getTypes()) {
            sender.sendMessage("§7- §f" + type.id() + " §8• §f" + type.name() + " §8• Trades: §f" + type.trades().size());
        }
        return CommandResult.success();
    }
}
