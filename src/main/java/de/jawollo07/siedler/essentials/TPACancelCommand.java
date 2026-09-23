package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;

public final class TPACancelCommand extends Command {
    private final TPAManager tpa;
    private final MessageManager messages = new MessageManager();
    private final String prefix;

    public TPACancelCommand(SiedlerPlugin plugin, TPAManager tpa) {
        super("tpacancel", "Bricht deine Teleportanfrage ab", "/tpacancel");
        this.tpa = tpa;
        this.prefix = messages.getPrefix("essentials");
        setPermission("siedler.command.tpacancel");
        setPermissionMessage(messages.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().exec(context -> {
            CommandSender sender = context.getSender();
            if (!(sender instanceof Player player)) {
                sender.sendMessage(prefix + messages.getMessage("messages.essentials.player-required"));
                return CommandResult.fail("Spieler erforderlich");
            }
            TPAManager.Result result = tpa.cancel(player);
            String key = result == TPAManager.Result.DECLINED ? "tpa-cancelled" : "tpa-none";
            player.sendMessage(prefix + messages.getMessage("messages.essentials." + key));
            return result == TPAManager.Result.DECLINED ? CommandResult.success() : CommandResult.fail(result.name());
        });
    }
}
