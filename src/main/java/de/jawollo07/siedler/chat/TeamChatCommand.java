package de.jawollo07.siedler.chat;

import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.Player;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;

public class TeamChatCommand extends Command {
    private final TeamChat teamChat;
    private final MessageManager messageManager;
    private final String prefix;
    public TeamChatCommand(SiedlerPlugin plugin) {
        super("teamchat", "Sendet eine Nachricht an alle Mitglieder deines Teams", "/teamchat <Nachricht>");
        this.setPermission("siedler.command.teamchat");
        this.setPermission("siedler.basic");
        this.messageManager = new MessageManager();
        this.setPermissionMessage(messageManager.getCommandMessage("no-permission"));
        this.teamChat = new TeamChat(plugin);
        this.prefix = messageManager.getPrefix("chat.team-chat");
    }
    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(prefix + messageManager.getMessage("chat", "team-chat.command-usage"));
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + messageManager.getCommandMessage("only-player-command"));
            return false;
        }

        String message = String.join(" ", args);
        return teamChat.send(player, message);
    }
}
