package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;

public final class TPADenyCommand extends Command {
    private final TPAManager tpa;
    private final MessageManager messages = new MessageManager();
    private final String prefix;

    public TPADenyCommand(SiedlerPlugin plugin, TPAManager tpa) {
        super("tpdeny", "Lehnt eine Teleportanfrage ab", "/tpdeny");
        this.tpa = tpa;
        this.prefix = messages.getPrefix("essentials");
        setPermission("siedler.command.tpdeny");
        setPermissionMessage(messages.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().exec(context -> {
            CommandSender sender = context.getSender();
            if (!(sender instanceof Player target)) {
                sender.sendMessage(prefix + messages.getMessage("messages.essentials.player-required"));
                return CommandResult.fail(messages.getMessage("messages.essentials.player-required"));
            }
            TPAManager.Request request = tpa.getIncoming(target);
            if (request == null) {
                target.sendMessage(prefix + messages.getMessage("messages.essentials.tpa-none"));
                return CommandResult.fail(messages.getMessage("messages.essentials.tpa-none"));
            }
            TPAManager.Result result = tpa.decline(target);
            target.sendMessage(prefix + messages.getMessage("messages.essentials.tpa-declined")
                    .replace("{player}", request.requesterName()));
            return result == TPAManager.Result.DECLINED ? CommandResult.success() : CommandResult.fail(result.name());
        });
    }
}
