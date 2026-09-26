
package de.jawollo07.siedler.claim;

import java.sql.SQLException;
import java.util.Set;

import org.powernukkitx.Player;
import org.powernukkitx.Server;
import org.powernukkitx.block.Block;
import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.block.BlockBreakEvent;
import org.powernukkitx.event.block.BlockPlaceEvent;
import org.powernukkitx.event.player.PlayerInteractEvent;
import org.powernukkitx.utils.Config;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.ConfigManager;
import de.jawollo07.siedler.core.MessageManager;
import de.jawollo07.siedler.team.Elimination;

public class Protection implements Listener {

    private final String prefix;
    private final ConfigManager configManager = new ConfigManager();
    private final Elimination elimination;
    private final MessageManager messageManager;
    private final Utils utils;
    private final Config config;
    private final boolean claimsProtectionEnabled;
    private final Set<String> interactionBlacklist;
    private final String eliminationBlock;

    public Protection(SiedlerPlugin plugin) {
        this.utils = new Utils(plugin);
        this.configManager.initialize(plugin.getDataFolder());
        this.config = configManager.getConfig();
        this.claimsProtectionEnabled = config.getBoolean("claims.protection.enabled");
        this.interactionBlacklist = Set.copyOf(
            config.getStringList("claims.protection.interaction-blacklist")
        );
        this.eliminationBlock = config.getString("elimination.block");
        this.elimination = new Elimination(plugin);
        this.messageManager = new MessageManager();
        this.prefix = messageManager.getPrefix("claim");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) throws SQLException {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (!hasClaimAccess(player)) {
            event.setCancelled(true);
            player.sendMessage(
                prefix + messageManager.getMessage("claim", "protection.block-breaking-not-allowed")
            );
        }
        if (!event.isCancelled() && block.getId() == this.eliminationBlock) {
            double blockX = block.getX();
            double blockY = block.getY();
            double blockZ = block.getZ();
            String claimId = utils.get_claimID(player);
            if (claimId != null && !claimId.isEmpty() && utils.isBlockInClaim(player.getLevel().getName(), blockX, blockY, blockZ)) {
                String teamId = utils.getClaimTeam(claimId);
                if (teamId != null && !teamId.isBlank()) {
                    elimination.eliminationBlockModified(teamId);
                    Server.getInstance().getScheduler().scheduleDelayedTask(() -> {
                        if (!event.isCancelled()) {
                            elimination.eliminationBlockModified(teamId);
                        }
                    }, 1);
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (!hasClaimAccess(player)) {
            event.setCancelled(true);
            player.sendMessage(
                prefix + messageManager.getMessage("claim", "protection.block-placing-not-allowed")
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

        String playerID = player.getUniqueId().toString();

        // Besitzer und berechtigte Spieler dürfen interagieren
        if (utils.hasAccess(playerID, claimID)) {
            return;
            }

            String blockID = event.getBlock().getId();

            if (this.interactionBlacklist.contains(blockID)) {
                event.setCancelled(true);
                player.sendMessage(
                    prefix + messageManager.getMessage("claim", "protection.block-interacting-not-allowed")
                );
            }
    }
    private boolean hasClaimAccess(Player player) {
        String claimID = utils.get_claimID(player);
        if (claimID == null || claimID.isEmpty()) {
            return true;
        }

        String playerID = player.getUniqueId().toString();

        return utils.hasAccess(playerID, claimID);
    }
}