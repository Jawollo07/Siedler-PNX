package de.jawollo07.siedler.chat;

import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.EventPriority;
import org.powernukkitx.event.Listener;

import java.sql.SQLException;

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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChatMessage(PlayerChatEvent event) throws SQLException {
        Player player = event.getPlayer();
        String playerID = player.getUniqueId().toString();
        String message = event.getMessage();
        String format = GlobalChat.getFormat(storageManager, playerID);
        String end_format = format + player.getName() + "§7: §f" + message;
        event.setFormat(end_format);

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