package de.jawollo07.siedler.chat;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;
import org.powernukkitx.command.route.node.RouteNode;
import org.powernukkitx.command.tree.node.StringNode;

import java.util.Objects;

public class DirektMessage extends Command {
    private final SiedlerPlugin plugin;
    private final ChatLogger chatLogger;
    private final MessageManager messageManager;
    private final String prefix;

    public DirektMessage(SiedlerPlugin plugin) {
        super("dm", "Sendet eine Direktnachricht an einen Spieler", "/dm <Spieler> <Nachricht>");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.chatLogger = new ChatLogger(plugin.getStorage());
        this.messageManager = new MessageManager();
        this.prefix = messageManager.getPrefix("chat.dm");
        setPermission("siedler.basic");
        setPermissionMessage(messageManager.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().then(RouteNode.argument("player", new StringNode())
                .then(RouteNode.argument("message", new StringNode()).exec(context -> {
                    sendMessage(context.getSender(), context.getArg("player"), context.getArg("message"));
                    return CommandResult.success();
                })));
    }

    private void sendMessage(CommandSender sender, String targetPlayerName, String message) {
        if (!(sender instanceof Player senderPlayer)) {
            sender.sendMessage(prefix + messageManager.getCommandMessage("only-player-command"));
            return;
        }
        plugin.getServer().getScheduler().scheduleTask(plugin, () ->
                plugin.getServer().getOnlinePlayers().values().stream()
                        .filter(player -> player.getName().equalsIgnoreCase(targetPlayerName))
                        .findFirst()
                        .ifPresentOrElse(targetPlayer -> {
                            targetPlayer.sendMessage(prefix + "§e" + senderPlayer.getName() + " §7-> §eDir: §f" + message);
                            senderPlayer.sendMessage(prefix + "§eDu §7-> §e" + targetPlayer.getName() + ": §f" + message);
                            chatLogger.saveChatMessage(
                                    senderPlayer.getUniqueId().toString(),
                                    senderPlayer.getName(),
                                    senderPlayer.getLevel().getName(),
                                    targetPlayer.getName(),
                                    senderPlayer.getX(), senderPlayer.getY(), senderPlayer.getZ(), message);
                        }, () -> sender.sendMessage(prefix + "§cSpieler " + targetPlayerName + " ist nicht online.")));
    }
}
