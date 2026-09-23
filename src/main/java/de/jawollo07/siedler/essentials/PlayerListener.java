package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.ConfigManager;
import de.jawollo07.siedler.core.MessageManager;
import de.jawollo07.siedler.team.Elimination;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.player.PlayerJoinEvent;
import org.powernukkitx.utils.Config;

import java.sql.SQLException;

import org.powernukkitx.Player;
import org.powernukkitx.entity.effect.Effect;
import org.powernukkitx.entity.effect.EffectWeakness;
import org.powernukkitx.event.EventHandler;

public class PlayerListener implements Listener{
    private final MessageManager messageManager;
    private final Elimination elimination;
    private final SiedlerPlugin plugin;
    private final ConfigManager configManager;
    private final Config config;\n    private final ModerationManager moderationManager;
    public PlayerListener() {
        this.plugin = SiedlerPlugin.getInstance();
        this.messageManager = new MessageManager();
        this.configManager = new ConfigManager();
        if (this.plugin != null) {
            this.configManager.initialize(this.plugin.getDataFolder());
        }
        this.config = this.configManager.getConfig();
        this.elimination = new Elimination(plugin);\n        this.moderationManager = new ModerationManager(plugin);
    }
    public void give_weakness(Player player) {
        if (config == null) {
            return;
        }
        Effect weakness = new EffectWeakness();
        Boolean enabled = config.getBoolean("weakness.enabled");
        Integer duration = config.getInt("weakness.duration");
        Integer amplifier = config.getInt("weakness.amplifier");
        Boolean isVisible = config.getBoolean("weakness.isVisible");
        if(enabled) {
            weakness.setAmplifier(amplifier);
            weakness.setVisible(isVisible);
            if(duration == 0) {
                weakness.setInfinite();
            } else {
                weakness.setDuration(duration);
            }
            player.addEffect(weakness);
        }
    }
    @EventHandler
    public void onJoin(PlayerJoinEvent event) throws SQLException {
        Player player = event.getPlayer();
        player.sendMessage(messageManager.getMessage("essentials", "welcome-message"));
        if((elimination.isPlayerEliminated(player))) {
            elimination.playerIsEliminated(player);
        }
        give_weakness(player);
        player.setGamemode(0);
    }
}
