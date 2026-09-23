package de.jawollo07.siedler.eco;

import java.sql.SQLException;

import org.powernukkitx.level.Level;
import org.powernukkitx.scheduler.Task;
import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.eco.TaxManager;

public class DailyTax extends Task {
    private final SiedlerPlugin plugin;
    private boolean alreadyTriggered = false;
    private final TaxManager taxManager;
    public DailyTax(SiedlerPlugin plugin) {
        this.plugin = plugin;
        this.taxManager = new TaxManager(plugin);
    }

    @Override
    public void onRun(int tick) {
        Level level = plugin.getServer().getDefaultLevel();
        if (level != null) {
            int time = level.getDayTime(); // dayTime geht in PNX von 0 bis 23999

            // Zeitfenster für den Morgen (0 - 200 Ticks)
            if (time >= 0 && time < 200) {
                if (!alreadyTriggered) {
                    alreadyTriggered = true;
                    try {
                        taxManager.collectDueTaxes();
                    } catch (Exception e) {
                    }
                }
            } else {
                alreadyTriggered = false;
            }
        }
    }
}