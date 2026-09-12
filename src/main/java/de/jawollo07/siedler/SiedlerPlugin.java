package de.jawollo07.siedler;

import org.powernukkitx.plugin.PluginBase;
import org.powernukkitx.utils.Config;
import org.powernukkitx.utils.TextFormat;
import de.jawollo07.siedler.command.SiedlerCommand;
import de.jawollo07.siedler.core.SiedlerManager;
import de.jawollo07.siedler.storage.StorageManager;

import java.io.File;

/**
 * Main entry point for Siedler 2.0.
 *
 * <p>The old Bedrock Script API implementation is intentionally not copied
 * one-to-one. Siedler 2.0 uses native PowerNukkitX services and persistent
 * storage behind managers, allowing gameplay systems to be migrated safely.</p>
 */
public final class SiedlerPlugin extends PluginBase {
    private static SiedlerPlugin instance;
    private Config config;
    private StorageManager storage;
    private SiedlerManager siedlerManager;

    public static SiedlerPlugin getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = new Config(new File(getDataFolder(), "config.yml"), Config.YAML);
        storage = new StorageManager(this);
        storage.initialize();

        siedlerManager = new SiedlerManager(this, storage);
        getServer().getCommandMap().register("siedler", new SiedlerCommand(this));

        getLogger().info(TextFormat.GREEN + "Siedler 2.0 enabled.");
        getLogger().info(TextFormat.GRAY + "Native PowerNukkitX foundation initialized.");
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.close();
        }
        getLogger().info(TextFormat.RED + "Siedler 2.0 disabled.");
        instance = null;
    }

    public Config getConfig() {
        return config;
    }

    public StorageManager getStorage() {
        return storage;
    }

    public SiedlerManager getSiedlerManager() {
        return siedlerManager;
    }
}
