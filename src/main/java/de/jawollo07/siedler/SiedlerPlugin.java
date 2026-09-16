package de.jawollo07.siedler;

import de.jawollo07.siedler.command.SiedlerCommand;
import de.jawollo07.siedler.core.CommandManager;
import de.jawollo07.siedler.core.SiedlerManager;
import de.jawollo07.siedler.listener.ChatListener;
import de.jawollo07.siedler.storage.StorageManager;
import de.jawollo07.siedler.team.TeamCommand;
import org.powernukkitx.plugin.PluginBase;
import org.powernukkitx.utils.Config;
import org.powernukkitx.utils.TextFormat;

import java.io.File;

/**
 * Main entry point for Siedler 2.0.
 *
 * <p>The plugin uses managers to keep the main plugin class clean and
 * separates command registration, game logic, storage handling and
 * event listeners.</p>
 */
public final class SiedlerPlugin extends PluginBase {

    private static SiedlerPlugin instance;

    private Config config;
    private StorageManager storage;
    private SiedlerManager siedlerManager;
    private CommandManager commandManager;

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

        config = new Config(
                new File(getDataFolder(), "config.yml"),
                Config.YAML
        );

        storage = new StorageManager(this);
        storage.initialize();

        siedlerManager = new SiedlerManager(this, storage);

        commandManager = new CommandManager(this);
        registerCommands();

        getServer().getPluginManager().registerEvents(
                new ChatListener(this),
                this
        );

        getLogger().info(
                TextFormat.GREEN + "Siedler 2.0 enabled."
        );

        getLogger().info(
                TextFormat.GRAY + "Native PowerNukkitX foundation initialized."
        );

        getLogger().info(
                TextFormat.GRAY + "Registered "
                        + commandManager.size()
                        + " command(s)."
        );

        getLogger().info(
                TextFormat.GRAY + "Registered chat listener."
        );
    }

    private void registerCommands() {
        commandManager.register(new SiedlerCommand(this));
        commandManager.register(new TeamCommand());

        // Example:
        // commandManager.register(new ExampleCommand(this));
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.close();
        }

        getLogger().info(
                TextFormat.RED + "Siedler 2.0 disabled."
        );

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

    public CommandManager getCommandManager() {
        return commandManager;
    }
}
