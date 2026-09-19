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

public class TeamChatCommand extends Command {
    private final TeamChat teamChat;
    private final MessageManager messageManager;
    private final String prefix;

    public TeamChatCommand(SiedlerPlugin plugin) {
        super("teamchat", "Sendet eine Nachricht an alle Mitglieder deines Teams", "/teamchat <Nachricht>");
        Objects.requireNonNull(plugin, "plugin");
        setPermission("siedler.basic");
        this.messageManager = new MessageManager();
        setPermissionMessage(messageManager.getCommandMessage("no-permission"));
        this.teamChat = new TeamChat(plugin);
        this.prefix = messageManager.getPrefix("chat.team-chat");
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().then(RouteNode.argument("message", new StringNode()).exec(context -> {
            CommandSender sender = context.getSender();
            if (!(sender instanceof Player player)) {
                sender.sendMessage(prefix + messageManager.getCommandMessage("only-player-command"));
                return CommandResult.success();
            }
            teamChat.send(player, context.getArg("message"));
            return CommandResult.success();
        }));
    }
}
