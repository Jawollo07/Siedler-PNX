package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;
import org.powernukkitx.command.route.node.RouteNode;
import org.powernukkitx.command.tree.node.StringNode;

public final class TPACommand extends Command {
    private final TPAManager tpa;
    private final MessageManager messages = new MessageManager();
    private final String prefix;

    public TPACommand(SiedlerPlugin plugin, TPAManager tpa) {
        super("tpa", "Sendet eine Teleportanfrage", "/tpa <Spieler>");
        this.tpa = tpa;
        this.prefix = messages.getPrefix("essentials");
        setPermission("siedler.command.tpa");
        setPermissionMessage(messages.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().then(RouteNode.argument("player", new StringNode()).exec(context -> {
            CommandSender sender = context.getSender();
            if (!(sender instanceof Player requester)) {
                sender.sendMessage(prefix + messages.getMessage("messages.essentials.player-required"));
                return CommandResult.fail("Spieler erforderlich");
            }

            Player target = findPlayer(context.getArg("player"));
            if (target == null) {
                requester.sendMessage(prefix + messages.getMessage("messages.essentials.tpa-player-not-found"));
                return CommandResult.fail("Spieler nicht gefunden");
            }

            TPAManager.Result result = tpa.request(requester, target);
            if (result == TPAManager.Result.SENT) {
                requester.sendMessage(prefix + messages.getMessage("messages.essentials.tpa-sent")
                        .replace("{player}", target.getName()));
                target.sendMessage(prefix + messages.getMessage("messages.essentials.tpa-received")
                        .replace("{player}", requester.getName()));
                return CommandResult.success();
            }

            String key = result == TPAManager.Result.SELF ? "tpa-self"
                    : result == TPAManager.Result.ALREADY_PENDING ? "tpa-already-pending" : "tpa-error";
            requester.sendMessage(prefix + messages.getMessage("messages.essentials." + key));
            return CommandResult.fail(result.name());
        }));
    }

    private Player findPlayer(String name) {
        if (name == null) return null;
        for (Player player : SiedlerPlugin.getInstance().getServer().getOnlinePlayers().values()) {
            if (player.getName().equalsIgnoreCase(name.trim())) return player;
        }
        return null;
    }
}
