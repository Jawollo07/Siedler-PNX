package de.jawollo07.siedler.claim;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.storage.StorageManager;
import de.jawollo07.siedler.claim.Utils;
import de.jawollo07.siedler.claim.Claim;
import de.jawollo07.siedler.claim.ClaimManager;

import org.powernukkitx.Player;
import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.block.BlockBreakEvent;

public class Protection {
    private final SiedlerPlugin plugin = new SiedlerPlugin();
    Utils utils = new Utils(plugin.getStorage());
    public class BlockListener implements Listener {
        @EventHandler
        public void onBlockBreak(BlockBreakEvent event) {
            Player player = event.getPlayer();
            Claim claim = 
            boolean access = utils.hasAccess(player.getUniqueId().toString(), )
        }
    }
}