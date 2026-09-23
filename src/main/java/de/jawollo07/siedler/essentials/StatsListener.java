package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import org.powernukkitx.Player;
import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.player.PlayerDeathEvent;
import org.powernukkitx.event.player.PlayerJoinEvent;
import org.powernukkitx.event.player.PlayerQuitEvent;

import java.lang.reflect.Method;

/**
 * Updates persistent player statistics from player lifecycle events.
 */
public final class StatsListener implements Listener {
    private final StatsManager statsManager;

    public StatsListener(StatsManager statsManager) {
        this.statsManager = statsManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        try {
            statsManager.onJoin(event.getPlayer());
        } catch (Exception e) {
            SiedlerPlugin.getInstance().getLogger().warning(
                    "Could not initialize player statistics: " + e.getMessage());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        try {
            statsManager.onQuit(event.getPlayer());
        } catch (Exception e) {
            SiedlerPlugin.getInstance().getLogger().warning(
                    "Could not save player statistics: " + e.getMessage());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        try {
            statsManager.incrementDeath(victim.getUniqueId().toString());

            Player killer = findKiller(victim);
            if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
                statsManager.incrementKill(killer.getUniqueId().toString());
            }
        } catch (Exception e) {
            SiedlerPlugin.getInstance().getLogger().warning(
                    "Could not update combat statistics: " + e.getMessage());
        }
    }

    private Player findKiller(Player victim) {
        try {
            Method method = victim.getClass().getMethod("getKiller");
            Object result = method.invoke(victim);
            return result instanceof Player player ? player : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
