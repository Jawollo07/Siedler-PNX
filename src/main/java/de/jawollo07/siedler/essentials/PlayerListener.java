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
import org.powernukkitx.event.EventHandler;

public class PlayerListener implements Listener{
    private final MessageManager messageManager;
    private final Elimination elimination;
    private final SiedlerPlugin plugin;
    private final ConfigManager configManager;
    private final Config config;
    private final PermanentEffect permanentEffect;
    private final ModerationManager moderationManager;
    public PlayerListener(SiedlerPlugin plugin, ModerationManager moderationManager) {
        this.plugin = plugin;
        if (this.plugin == null) {
            throw new IllegalArgumentException("Plugin darf nicht null sein");
        }
        this.moderationManager = moderationManager == null ? new ModerationManager(this.plugin) : moderationManager;
        this.messageManager = new MessageManager();
        this.configManager = new ConfigManager();
        this.configManager.initialize(this.plugin.getDataFolder());
        this.config = this.configManager.getConfig();
        this.elimination = new Elimination(plugin);
        this.permanentEffect = new PermanentEffect();
    }
    @EventHandler
    public void onJoin(PlayerJoinEvent event) throws SQLException {
        Player player = event.getPlayer();
        player.sendMessage(messageManager.getMessage("essentials", "welcome-message"));
        if (moderationManager.enforce(player)) {
            return;
        }

        if (elimination.isPlayerEliminated(player)) {
            elimination.playerIsEliminated(player);
            return;
        }

        permanentEffect.give_weakness(player);
        player.setGamemode(0);
    }
}