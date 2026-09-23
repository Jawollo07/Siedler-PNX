package de.jawollo07.siedler.essentials;

import org.powernukkitx.entity.effect.EffectWeakness;
import org.powernukkitx.entity.effect.Effect;
import org.powernukkitx.Player;
import org.powernukkitx.utils.Config;
import org.powernukkitx.scheduler.Task;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.ConfigManager;


public class PermanentEffect extends Task {
    private final ConfigManager configManager;
    private final Config config;
    private final SiedlerPlugin plugin;
    public PermanentEffect() {
        this.plugin = SiedlerPlugin.getInstance();
        this.configManager = new ConfigManager();
        if (this.plugin != null) {
            this.configManager.initialize(this.plugin.getDataFolder());
        }
        this.config = this.configManager.getConfig();
    }
    @Override
    public void onRun(int currentTick) {
        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            give_weakness(player);
        }
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
}
