package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;

public final class TPAAcceptCommand extends Command {
    private final TPAManager tpa;
    private final MessageManager messages = new MessageManager();
    private final String prefix;

    public TPAAcceptCommand(SiedlerPlugin plugin, TPAManager tpa) {
        super("tpaccept", "Nimmt eine Teleportanfrage an", "/tpaccept");
        this.tpa = tpa;
        this.prefix = messages.getPrefix("essentials");
        setPermission("siedler.command.tpaccept");
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
            try {
                TPAManager.Result result = tpa.accept(target);
                if (result == TPAManager.Result.ACCEPTED) {
                    target.sendMessage(prefix + messages.getMessage("messages.essentials.tpa-accepted")
                            .replace("{player}", request.requesterName()));
                    return CommandResult.success();
                }
                target.sendMessage(prefix + messages.getMessage("messages.essentials.tpa-expired"));
                return CommandResult.fail(result.name());
            } catch (Exception e) {
                target.sendMessage(prefix + messages.getMessage("messages.essentials.tpa-error")
                        .replace("{error}", e.getMessage() == null ? "Unbekannter Fehler" : e.getMessage()));
                return CommandResult.fail(messages.getMessage("messages.essentials.tpa-error"));
            }
        });
    }
}
