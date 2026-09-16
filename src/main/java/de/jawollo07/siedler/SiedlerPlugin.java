package de.jawollo07.siedler;

import de.jawollo07.siedler.chat.ChatListener;
import de.jawollo07.siedler.core.CommandManager;
import de.jawollo07.siedler.core.SiedlerManager;
import de.jawollo07.siedler.storage.StorageManager;
import de.jawollo07.siedler.team.RelationCommands;
import de.jawollo07.siedler.team.TeamCommand;
import de.jawollo07.siedler.chat.DirektMessage;
import de.jawollo07.siedler.chat.TeamChatCommand;
import org.powernukkitx.plugin.PluginBase;
import org.powernukkitx.utils.Config;
import org.powernukkitx.utils.TextFormat;

import java.io.File;

/**
 * Main entry point for Siedler.
 *
 * <p>The plugin uses managers to keep the main plugin class clean and
 * separates command registration, game logic and storage handling.</p>
 */
public final class SiedlerPlugin extends PluginBase {

    private static SiedlerPlugin instance;

    private Config config;
    private StorageManager storage;
    private SiedlerManager siedlerManager;
    private CommandManager commandManager;

    /**
     * Returns the currently loaded Siedler plugin instance.
     *
     * @return plugin instance
     */
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

        /*
         * Initialize storage.
         */
        storage = new StorageManager(this);
        storage.initialize();
        getServer().getPluginManager().registerEvents(new ChatListener(storage), this);

        /*
         * Initialize core managers.
         */
        siedlerManager = new SiedlerManager(this, storage);

        /*
         * Initialize and register commands.
         */
        commandManager = new CommandManager(this);
        registerCommands();

        getLogger().info(
                TextFormat.GREEN + "Siedler enabled."
        );

        getLogger().info(
                TextFormat.GRAY + "Native PowerNukkitX foundation initialized."
        );

        getLogger().info(
                TextFormat.GRAY + "Registered "
                        + commandManager.size()
                        + " command(s)."
        );
    }

    /**
     * Registers all commands used by Siedler.
     *
     * <p>To add a new command, simply add another
     * {@code commandManager.register(...)} call here.</p>
     */
    private void registerCommands() {
        commandManager.register(new TeamCommand());
        commandManager.register(new DirektMessage(this));
        commandManager.register(new TeamChatCommand(this));
        commandManager.register(new RelationCommands(this));
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.close();
        }

        getLogger().info(
                TextFormat.RED + "Siedler disabled."
        );

        instance = null;
    }

    /**
     * Returns the plugin configuration.
     *
     * @return configuration
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Returns the storage manager.
     *
     * @return storage manager
     */
    public StorageManager getStorage() {
        return storage;
    }

    /**
     * Returns the Siedler manager.
     *
     * @return Siedler manager
     */
    public SiedlerManager getSiedlerManager() {
        return siedlerManager;
    }

    /**
     * Returns the command manager.
     *
     * @return command manager
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }
}
