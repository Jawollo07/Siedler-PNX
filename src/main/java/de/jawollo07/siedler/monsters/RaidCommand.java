package de.jawollo07.siedler.monsters;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;
import org.powernukkitx.command.route.node.RouteNode;
import org.powernukkitx.command.tree.node.StringNode;

public final class RaidCommand extends Command {
    private final SiedlerPlugin plugin;
    private final RaidManager raidManager;
    private final MessageManager messages = new MessageManager();

    public RaidCommand(SiedlerPlugin plugin, RaidManager raidManager) {
        super("raid", "Verwaltet Siedler-Pillager-Raids", "/raid <status|admin>");
        this.plugin = plugin;
        this.raidManager = raidManager;
        setPermission("siedler.command.raid");
        setPermissionMessage(messages.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().then(RouteNode.literal("status").exec(context -> {
            status(context.getSender());
            return CommandResult.success();
        }));
        tree.getRoot().then(RouteNode.literal("admin")
                .permission("siedler.admin", messages.getCommandMessage("no-permission"))
                .then(RouteNode.literal("help").exec(context -> {
                    help(context.getSender());
                    return CommandResult.success();
                }))
                .then(RouteNode.literal("start")
                        .then(RouteNode.argument("outpost", new StringNode()).exec(context -> {
                            start(context.getSender(), context.getArg("outpost"));
                            return CommandResult.success();
                        })))
                .then(RouteNode.literal("stop").exec(context -> {
                    stop(context.getSender());
                    return CommandResult.success();
                })));
    }

    private void start(CommandSender sender, String outpost) {
        try {
            if (raidManager.startRaid(outpost)) {
                sender.sendMessage(messages.getMessage("messages.monsters.raid-admin-started")
                        .replace("{outpost}", outpost));
            } else {
                sender.sendMessage(messages.getMessage("messages.monsters.raid-start-failed"));
            }
        } catch (Exception exception) {
            sender.sendMessage(messages.getMessage("messages.monsters.raid-error")
                    .replace("{error}", safe(exception)));
        }
    }

    private void stop(CommandSender sender) {
        try {
            RaidManager.Raid raid = raidManager.getActiveRaid();
            if (raid == null) {
                sender.sendMessage(messages.getMessage("messages.monsters.raid-none"));
                return;
            }
            raidManager.stopRaid(raid.id());
            sender.sendMessage(messages.getMessage("messages.monsters.raid-stopped")
                    .replace("{outpost}", raid.outpostName()));
        } catch (Exception exception) {
            sender.sendMessage(messages.getMessage("messages.monsters.raid-error")
                    .replace("{error}", safe(exception)));
        }
    }

    private void status(CommandSender sender) {
        try {
            RaidManager.Raid raid = raidManager.getActiveRaid();
            if (raid == null) {
                sender.sendMessage(messages.getMessage("messages.monsters.raid-none"));
                return;
            }
            sender.sendMessage(messages.getMessage("messages.monsters.raid-status")
                    .replace("{outpost}", raid.outpostName())
                    .replace("{wave}", String.valueOf(raid.wave()))
                    .replace("{total}", String.valueOf(plugin.getConfig().getInt(
                            "monsters.raids.waves", 3))));
        } catch (Exception exception) {
            sender.sendMessage(messages.getMessage("messages.monsters.raid-error")
                    .replace("{error}", safe(exception)));
        }
    }

    private void help(CommandSender sender) {
        sender.sendMessage(messages.getMessage("messages.monsters.raid-help"));
    }

    private String safe(Exception exception) {
        return exception.getMessage() == null ? "Unbekannter Fehler" : exception.getMessage();
    }
}
