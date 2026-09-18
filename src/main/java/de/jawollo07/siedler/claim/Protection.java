
package de.jawollo07.siedler.claim;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.ConfigManager;

import java.util.Set;

import org.powernukkitx.Player;
import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.block.BlockBreakEvent;
import org.powernukkitx.event.block.BlockPlaceEvent;
import org.powernukkitx.event.player.PlayerInteractEvent;
import org.powernukkitx.utils.Config;

public class Protection implements Listener {

    private static final String PREFIX = "§8[§6CLAIMS§8] §r";
    private final ConfigManager configManager = new ConfigManager();
    private final Utils utils;
    public Protection(SiedlerPlugin plugin) {
        this.utils = new Utils(plugin.getStorage());
        this.configManager.initialize(plugin.getDataFolder());
        this.interactionBlacklist = Set.copyOf(
            config.getStringList("claims.protection.interaction-blacklist")
        );
    }
    Config config = configManager.getConfig();
    boolean claimsProtectionEnabled = config.getBoolean("claims.protection.enabled");
    private final Set<String> interactionBlacklist;
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (!hasClaimAccess(player)) {
            event.setCancelled(true);
            player.sendMessage(
                PREFIX + "§cDu kannst hier nichts abbauen!"
            );
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (!hasClaimAccess(player)) {
            event.setCancelled(true);
            player.sendMessage(
                PREFIX + "§cDu kannst hier nichts platzieren!"
            );
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        String claimID = utils.get_claimID(player);

        // Außerhalb eines Claims erlauben
        if (claimID == null || claimID.isEmpty()) {
            return;
        }

        String playerID = String.valueOf(player.getId());

        // Besitzer und berechtigte Spieler dürfen interagieren
        if (utils.hasAccess(playerID, claimID)) {
            return;
            }

            String blockID = event.getBlock().getId();

            if (this.interactionBlacklist.contains(blockID)) {
                event.setCancelled(true);
                player.sendMessage(
                    PREFIX + "§cDu kannst diesen Block hier nicht benutzen!"
                );
            }
    }
    private boolean hasClaimAccess(Player player) {
        String claimID = utils.get_claimID(player);
        if (claimID == null || claimID.isEmpty()) {
            return true;
        }

        String playerID = String.valueOf(player.getId());

        return utils.hasAccess(playerID, claimID);
    }
}