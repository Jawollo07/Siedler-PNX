package de.jawollo07.siedler.chat;

import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.Player;

import de.jawollo07.siedler.SiedlerPlugin;

public class TeamChatCommand extends Command {
    private final TeamChat teamChat;

    public TeamChatCommand(SiedlerPlugin plugin) {
        super("teamchat", "Sendet eine Nachricht an alle Mitglieder deines Teams", "/teamchat <Nachricht>");
        this.setPermission("siedler.command.teamchat");
        this.setPermission("siedler.basic");
        this.teamChat = new TeamChat(plugin);
    }
    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cVerwendung: /teamchat <Nachricht>");
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl kann nur von einem Spieler verwendet werden.");
            return false;
        }

        String message = String.join(" ", args);
        return teamChat.send(player, message);
    }
}
