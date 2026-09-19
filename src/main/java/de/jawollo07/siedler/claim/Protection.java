
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
import de.jawollo07.siedler.team.TeamManager;
import de.jawollo07.siedler.team.Elimination;

public class Protection implements Listener {

    private final String prefix;
    private final ConfigManager configManager = new ConfigManager();
    private final TeamManager teamManager;
    private final Elimination elimination;
    private final MessageManager messageManager;
    private final Utils utils;
    private final Config config;
    private final boolean claimsProtectionEnabled;
    private final Set<String> interactionBlacklist;
    private final String eliminationBlock;

    public Protection(SiedlerPlugin plugin) {
        this.utils = new Utils(plugin.getStorage());
        this.configManager.initialize(plugin.getDataFolder());
        this.config = configManager.getConfig();
        this.claimsProtectionEnabled = config.getBoolean("claims.protection.enabled");
        this.interactionBlacklist = Set.copyOf(
            config.getStringList("claims.protection.interaction-blacklist")
        );
        this.eliminationBlock = config.getString("claims.elimination.block");
        this.teamManager = new TeamManager(plugin);
        this.elimination = new Elimination(plugin);
        this.messageManager = new MessageManager();
        this.prefix = messageManager.getPrefix("claim.protection");
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
        if (block.getId() == this.eliminationBlock) {
            double block_x = block.getX();
            double block_y = block.getY();
            double block_z = block.getZ();
            String team_id = teamManager.getTeamByName(player.getName()).toString();
            if(utils.isBlockInClaim(block_x, block_y, block_z)) {
                elimination.eliminationBlockModified(team_id);
                Server.getInstance().getScheduler().scheduleDelayedTask(()-> {
                  if(!(event.isCancelled())) {
                    elimination.eliminationBlockModified(team_id);
                  }  
                }, 1);
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