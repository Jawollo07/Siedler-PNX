package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;

/**
 * Opens the player's persistent native Ender Chest inventory.
 *
 * The native inventory must be opened through Inventory#open(Player).
 * Calling Player#addWindow(...) directly can register the window without
 * correctly opening the Ender Chest UI in current PowerNukkitX versions,
 * which leaves the player without a visible UI while subsequent calls
 * report the inventory as already open.
 */
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

            try {
                player.getEnderChestInventory().open(player);
            } catch (Exception exception) {
                player.sendMessage(messageManager.getMessage("messages.essentials.enderchest-error"));
                SiedlerPlugin.getInstance().getLogger().warning(
                        "Enderchest konnte für " + player.getName() + " nicht geöffnet werden: "
                                + exception.getMessage()
                );
            }

            return CommandResult.success();
        });
    }
}
