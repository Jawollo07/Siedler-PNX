package de.jawollo07.siedler.chat;

import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.Listener;
import org.powernukkitx.Player;
import org.powernukkitx.event.player.PlayerChatEvent;
import de.jawollo07.siedler.storage.StorageManager;

public class ChatListener implements Listener {
    private final ChatLogger chatLogger;

    public ChatListener(StorageManager storageManager) {
        this.chatLogger = new ChatLogger(storageManager);
    }

    @EventHandler
    public void onChatMessage(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String playerID = player.getUniqueId().toString();
        String message = event.getMessage();

        chatLogger.saveChatMessage(
            playerID,
            player.getName(),
            player.getLevel().getName(),
            player.getX(),
            player.getY(),
            player.getZ(),
            message
        );
    }
}