package de.jawollo07.siedler.essentials;

import org.powernukkitx.Player;
import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.player.PlayerJoinEvent;
import org.powernukkitx.event.player.PlayerQuitEvent;

/** Captures player inventories when players enter or leave the server. */
public final class InventorySnapshotListener implements Listener {
    private final InventorySnapshotManager snapshotManager;

    public InventorySnapshotListener(InventorySnapshotManager snapshotManager) {
        this.snapshotManager = snapshotManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        snapshot(event.getPlayer(), "JOIN");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        snapshot(event.getPlayer(), "QUIT");
    }

    private void snapshot(Player player, String reason) {
        try {
            snapshotManager.snapshot(player, reason);
        } catch (Exception exception) {
            player.getServer().getLogger().warning(
                    "Inventar-Snapshot für " + player.getName() + " fehlgeschlagen: "
                            + exception.getMessage()
            );
        }
    }
}
