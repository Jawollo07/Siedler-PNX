package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.claim.Utils;

import org.powernukkitx.Player;
import org.powernukkitx.command.Command;

import de.jawollo07.siedler.claim.ClaimManager;
import de.jawollo07.siedler.team.TeamManager;
import de.jawollo07.siedler.core.MessageManager;

public class StartSystem extends Command {

    private MessageManager messageManager;

    public StartSystem() {
        super("start", "Starts the game", "/start");
        this.setPermission("siedler.admin");
        this.setPermission("siedler.command.start");
        this.messageManager = new MessageManager();
        this.setPermissionMessage(messageManager.getCommandMessage("no-permission"));
    }

    public void startGame() {

    }
    public void giveStarterKit(Player player) {
        
    }
    public void giveAllStarterKit() {

    }
    public void broadcastStart() {

    }
    public void SetTeamStarterPosition() {

    }
    public void teleportPlayerToStarterPosition() {

    }
    public void teleportTeamToStarterPos() {
        
    }
}
