package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.player.PlayerJoinEvent;
import org.powernukkitx.event.player.PlayerMoveEvent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks real player movement and disconnects players that remain inactive
 * longer than the configured Anti-AFK timeout.
 */
public final class AntiAfkManager implements Listener, Runnable {

    private final SiedlerPlugin plugin;
    private final MessageManager messageManager;
    private final Map<UUID, Long> lastActivity = new HashMap<>();
    private final Map<UUID, Boolean> warned = new HashMap<>();

    public AntiAfkManager(SiedlerPlugin plugin) {
        this.plugin = plugin;
        this.messageManager = new MessageManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        markActive(event.getPlayer());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Looking around without changing position does not count as activity.
        if (event.getFrom().getX() == event.getTo().getX()
                && event.getFrom().getY() == event.getTo().getY()
                && event.getFrom().getZ() == event.getTo().getZ()) {
            return;
        }

        markActive(player);
    }

    @Override
    public void run() {
        if (!isEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        long timeout = getTimeoutMillis();
        long warningBefore = Math.min(getWarningBeforeMillis(), timeout);
        long warningAt = timeout - warningBefore;

        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            if (isExempt(player)) {
                markActive(player);
                continue;
            }

            UUID uuid = player.getUniqueId();
            long last = lastActivity.computeIfAbsent(uuid, ignored -> now);
            long inactive = now - last;

            if (inactive >= timeout) {
                disconnect(player, messageManager.getMessage(
                        "messages.essentials.antiafk-kick"));
                lastActivity.remove(uuid);
                warned.remove(uuid);
                continue;
            }

            if (warningBefore > 0 && inactive >= warningAt && !warned.getOrDefault(uuid, false)) {
                long remainingSeconds = Math.max(1, (timeout - inactive) / 1000L);
                player.sendMessage(messageManager.getMessage(
                        "messages.essentials.antiafk-warning")
                        .replace("{seconds}", String.valueOf(remainingSeconds)));
                warned.put(uuid, true);
            }
        }

        // Remove stale entries after players leave.
        lastActivity.keySet().removeIf(uuid -> plugin.getServer().getOnlinePlayers().values()
                .stream().noneMatch(player -> player.getUniqueId().equals(uuid)));
        warned.keySet().removeIf(uuid -> !lastActivity.containsKey(uuid));
    }

    public void markActive(Player player) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        lastActivity.put(uuid, System.currentTimeMillis());
        warned.put(uuid, false);
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean("antiafk.enabled", true);
    }

    private long getTimeoutMillis() {
        int minutes = Math.max(1, plugin.getConfig().getInt("antiafk.timeout-minutes", 15));
        return minutes * 60_000L;
    }

    private long getWarningBeforeMillis() {
        int seconds = Math.max(0, plugin.getConfig().getInt("antiafk.warning-seconds", 60));
        return seconds * 1000L;
    }

    private boolean isExempt(Player player) {
        String permission = plugin.getConfig().getString(
                "antiafk.exempt-permission", "siedler.admin");
        return permission != null && !permission.isBlank() && player.hasPermission(permission);
    }

    private void disconnect(Player player, String reason) {
        try {
            Method method;
            try {
                method = player.getClass().getMethod("kick", String.class);
            } catch (NoSuchMethodException ignored) {
                method = player.getClass().getMethod("disconnect", String.class);
            }
            method.invoke(player, reason);
        } catch (Exception exception) {
            plugin.getLogger().warning(
                    "Anti-AFK konnte " + player.getName() + " nicht trennen: "
                            + exception.getMessage());
        }
    }
}
