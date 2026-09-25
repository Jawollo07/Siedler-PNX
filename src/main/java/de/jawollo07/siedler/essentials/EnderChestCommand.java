package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;

public final class EnderChestCommand extends Command {

    private final MessageManager messageManager;

    public EnderChestCommand(SiedlerPlugin plugin) {
        super("ec", "Öffnet deine persönliche Enderchest", "/ec");
        this.messageManager = new MessageManager();
        setPermission("siedler.command.ec");
        setPermissionMessage(messageManager.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().exec(context -> {
            CommandSender sender = context.getSender();

            if (!(sender instanceof Player player)) {
                sender.sendMessage(messageManager.getCommandMessage("only-player-command"));
                return CommandResult.success();
            }

            int windowId = player.addWindow(player.getEnderChestInventory());
            if (windowId == -1) {
                sender.sendMessage(messageManager.getMessage("messages.essentials.enderchest-already-open"));
            }

            return CommandResult.success();
        });
    }
}
