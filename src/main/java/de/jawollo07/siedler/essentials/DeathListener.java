package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.player.PlayerDeathEvent;

import java.sql.SQLException;

/** Captures a player's latest death position. */
public final class DeathListener implements Listener {
    private final DeathManager deathManager;

    public DeathListener(DeathManager deathManager) {
        this.deathManager = deathManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        try {
            deathManager.saveDeath(event.getEntity());
        } catch (SQLException e) {
            SiedlerPlugin.getInstance().getLogger().error("Could not save death point: " + e);
        }
    }
}
