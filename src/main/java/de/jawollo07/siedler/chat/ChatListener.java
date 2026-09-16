package de.jawollo07.siedler.chat;

import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.Listener;
import org.powernukkitx.Player;
import org.powernukkitx.event.player.PlayerChatEvent;
import de.jawollo07.siedler.storage.StorageManager;

public class ChatListener implements Listener {
    private final ChatLogger chatLogger;
    private final StorageManager storageManager;

    public ChatListener(StorageManager storageManager) {
        this.storageManager = storageManager;
        this.chatLogger = new ChatLogger(storageManager);
    }

    @EventHandler
    public void onChatMessage(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String playerID = player.getUniqueId().toString();
        String message = event.getMessage();

        try {
            event.setFormat(GlobalChat.getFormat(storageManager, playerID));
        } catch (java.sql.SQLException exception) {
            event.setFormat("§8[§7Global§8] §f%s§7: §f%s");
        }

        chatLogger.saveChatMessage(
            playerID,
            player.getName(),
            player.getLevel().getName(),
            "global",
            player.getX(),
            player.getY(),
            player.getZ(),
            message
        );
    }
}