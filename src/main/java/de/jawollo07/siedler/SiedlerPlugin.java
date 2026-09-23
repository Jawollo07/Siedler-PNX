package de.jawollo07.siedler;

import de.jawollo07.siedler.chat.ChatListener;
import de.jawollo07.siedler.core.CommandManager;
import de.jawollo07.siedler.core.SiedlerManager;
import de.jawollo07.siedler.storage.StorageManager;
import de.jawollo07.siedler.team.Elimination;
import de.jawollo07.siedler.team.RelationCommands;
import de.jawollo07.siedler.team.TeamCommand;
import de.jawollo07.siedler.chat.DirektMessage;
import de.jawollo07.siedler.chat.TeamChatCommand;
import de.jawollo07.siedler.claim.ClaimCommand;
import de.jawollo07.siedler.eco.EcoCommand;
import de.jawollo07.siedler.eco.TaxManager;
import de.jawollo07.siedler.claim.Protection;
import de.jawollo07.siedler.core.MessageManager;
import de.jawollo07.siedler.essentials.PlayerListener;
import de.jawollo07.siedler.essentials.ManagementCommand;
import de.jawollo07.siedler.essentials.PermanentEffect;
import org.powernukkitx.plugin.PluginBase;
import org.powernukkitx.utils.Config;
import org.powernukkitx.utils.TextFormat;

import java.io.File;

public final class SiedlerPlugin extends PluginBase {

    private static SiedlerPlugin instance;

    private Config config;
    private StorageManager storage;
    private SiedlerManager siedlerManager;
    private CommandManager commandManager;
    private ClaimCommand claimCommand;
    private MessageManager messageManager;
    private EcoCommand ecoCommand;
    private TaxManager taxManager;

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
        messageManager = new MessageManager();
        messageManager.initialize(getDataFolder());
        final String prefix = messageManager.getPrefix("main");

        storage = new StorageManager(this);
        storage.initialize();

        siedlerManager = new SiedlerManager(this, storage);

        commandManager = new CommandManager(this);

        taxManager = new TaxManager(this);
        taxManager.start();

        // Registration
        registerCommands();
        registerEvents();
        registerListener();
        registerTasks();
        
        getLogger().info(
                TextFormat.GREEN + prefix + messageManager.getMessage("main", "enable")
        );

        getLogger().info(
                TextFormat.WHITE + prefix + messageManager.getMessage("main", "info")
        );

        getLogger().info(
                TextFormat.GRAY + "Registered "
                        + commandManager.size()
                        + " command(s)."
        );
    }

    private void registerCommands() {
        commandManager.register(new TeamCommand());
        commandManager.register(new DirektMessage(this));
        commandManager.register(new TeamChatCommand(this));
        commandManager.register(new RelationCommands(this));
        commandManager.register(new ClaimCommand(this));
        commandManager.register(new Elimination(this));
        commandManager.register(new EcoCommand(this));
        commandManager.register(new ManagementCommand(this));
        commandManager.register(new SetHomeCommand(this));
        commandManager.register(new HomeCommand(this));
        commandManager.register(new HomesCommand(this));
        commandManager.register(new DelHomeCommand(this));
    }

    private void registerEvents() {
        try {
            this.getServer().getPluginManager().registerEvents(new Protection(this), this);
        } catch (Exception e) {
            this.getLogger().error("Error with Event registration: " + e);
        }
        getLogger().info("All Events registered");
    }
    private void registerTasks() {
        try {
            this.getServer().getScheduler().scheduleRepeatingTask(this, new PermanentEffect(), 20);
        } catch (Exception e) {
            this.getLogger().error("Error with Task registration: " + e);
        }
        getLogger().info("All Tasks registered");
    }
    private void registerListener() {
        try {
            getServer().getPluginManager().registerEvents(new ChatListener(storage), this);
            getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        } catch (Exception e) {
            this.getLogger().error("Error with Listener registration: " + e);
        }
        getLogger().info("All Listener registered");
    }
    @Override
    public void onDisable() {
        if (taxManager != null) {
            taxManager.stop();
            taxManager = null;
        }

        if (storage != null) {
            storage.close();
        }

        if (messageManager != null && messageManager.getConfig() != null) {
            getLogger().info(
                TextFormat.RED + messageManager.getPrefix("main") + messageManager.getMessage("main", "disable")
            );
        }
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
