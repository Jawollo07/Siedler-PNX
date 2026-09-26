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
import de.jawollo07.siedler.essentials.ModerationManager;
import de.jawollo07.siedler.essentials.SetHomeCommand;
import de.jawollo07.siedler.essentials.HomeCommand;
import de.jawollo07.siedler.essentials.HomesCommand;
import de.jawollo07.siedler.essentials.DelHomeCommand;
import de.jawollo07.siedler.essentials.TPACommand;
import de.jawollo07.siedler.essentials.TPAAcceptCommand;
import de.jawollo07.siedler.essentials.TPADenyCommand;
import de.jawollo07.siedler.essentials.TPACancelCommand;
import de.jawollo07.siedler.essentials.DeathCommand;
import de.jawollo07.siedler.essentials.DeathListener;
import de.jawollo07.siedler.essentials.PermanentEffect;
import de.jawollo07.siedler.essentials.InventorySnapshotManager;
import de.jawollo07.siedler.essentials.InventorySnapshotListener;
import de.jawollo07.siedler.essentials.StatsManager;
import de.jawollo07.siedler.essentials.StatsListener;
import de.jawollo07.siedler.essentials.StatsCommand;
import de.jawollo07.siedler.essentials.EnderChestCommand;
import de.jawollo07.siedler.essentials.TeamEnderChestCommand;
import de.jawollo07.siedler.essentials.TeamEnderChestManager;
import de.jawollo07.siedler.essentials.AntiAfkManager;
import de.jawollo07.siedler.monsters.TokenManager;
import de.jawollo07.siedler.monsters.TokenCommand;
import de.jawollo07.siedler.monsters.OutpostManager;
import de.jawollo07.siedler.monsters.OutpostCommand;
import de.jawollo07.siedler.monsters.RaidManager;
import de.jawollo07.siedler.monsters.RaidCommand;
import de.jawollo07.siedler.monsters.MonsterManager;
import de.jawollo07.siedler.market.MarketManager;
import de.jawollo07.siedler.market.TraderManager;
import de.jawollo07.siedler.market.MarketCommand;
import de.jawollo07.siedler.essentials.TPAManager;
import de.jawollo07.siedler.essentials.DeathManager;
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
    private TPAManager tpaManager;
    private DeathManager deathManager;
    private InventorySnapshotManager inventorySnapshotManager;
    private StatsManager statsManager;
    private TeamEnderChestManager teamEnderChestManager;
    private AntiAfkManager antiAfkManager;
    private TokenManager tokenManager;
    private OutpostManager outpostManager;
    private RaidManager raidManager;
    private MonsterManager monsterManager;
    private MarketManager marketManager;
    private TraderManager traderManager;
    private ModerationManager moderationManager;

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
        tpaManager = new TPAManager(this);
        deathManager = new DeathManager(this);
        inventorySnapshotManager = new InventorySnapshotManager(this);
        statsManager = new StatsManager(this);
        teamEnderChestManager = new TeamEnderChestManager(this);
        antiAfkManager = new AntiAfkManager(this);
        tokenManager = new TokenManager(this);
        outpostManager = new OutpostManager(this);
        outpostManager.start();
        raidManager = new RaidManager(this);
        raidManager.start();
        monsterManager = new MonsterManager(this);
        marketManager = new MarketManager(this);
        traderManager = new TraderManager(this, marketManager);
        moderationManager = new ModerationManager(this);

        // Registration
        registerCommands();
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
        commandManager.register(new ManagementCommand(this, moderationManager));
        commandManager.register(new SetHomeCommand(this));
        commandManager.register(new HomeCommand(this));
        commandManager.register(new HomesCommand(this));
        commandManager.register(new DelHomeCommand(this));
        commandManager.register(new TPACommand(this, tpaManager));
        commandManager.register(new TPAAcceptCommand(this, tpaManager));
        commandManager.register(new TPADenyCommand(this, tpaManager));
        commandManager.register(new TPACancelCommand(this, tpaManager));
        commandManager.register(new DeathCommand(this, deathManager));
        commandManager.register(new StatsCommand(this, statsManager));
        commandManager.register(new EnderChestCommand(this));
        commandManager.register(new TeamEnderChestCommand(this, teamEnderChestManager));
        commandManager.register(new TokenCommand(this, tokenManager));
        commandManager.register(new OutpostCommand(this, outpostManager));
        commandManager.register(new RaidCommand(this, raidManager));
        commandManager.register(new MarketCommand(this, marketManager, traderManager));
    }

    private void registerTasks() {
        try {
            this.getServer().getScheduler().scheduleRepeatingTask(this, new PermanentEffect(), 20);
            this.getServer().getScheduler().scheduleRepeatingTask(this, new Runnable() {
                @Override
                public void run() {
                    if (inventorySnapshotManager != null) {
                        inventorySnapshotManager.snapshotOnlinePlayers("PERIODIC");
                    }
                }
            }, 20 * 60 * 3);

            if (antiAfkManager != null) {
                this.getServer().getScheduler().scheduleRepeatingTask(this, antiAfkManager, 20);
            }
            if (traderManager != null) {
                this.getServer().getScheduler().scheduleRepeatingTask(this, traderManager, 20 * 10);
            }
        } catch (Exception e) {
            this.getLogger().error("Error with Task registration: " + e);
        }
        getLogger().info("All Tasks registered");
    }
    private void registerListener() {
        registerListenerSafe(new ChatListener(storage), "ChatListener");
        registerListenerSafe(new PlayerListener(this, moderationManager), "PlayerListener");
        registerListenerSafe(new DeathListener(deathManager), "DeathListener");
        registerListenerSafe(new InventorySnapshotListener(inventorySnapshotManager), "InventorySnapshotListener");
        registerListenerSafe(new StatsListener(statsManager), "StatsListener");
        registerListenerSafe(antiAfkManager, "AntiAfkManager");
        registerListenerSafe(tokenManager, "TokenManager");
        registerListenerSafe(raidManager, "RaidManager");
        registerListenerSafe(monsterManager, "MonsterManager");
        registerListenerSafe(marketManager, "MarketManager");
        registerListenerSafe(traderManager, "TraderManager");
        getLogger().info("Listener registration completed.");
    }

    private void registerListenerSafe(org.powernukkitx.event.Listener listener, String name) {
        try {
            getServer().getPluginManager().registerEvents(listener, this);
        } catch (Exception e) {
            getLogger().error("Failed to register " + name + ": " + e);
        }
    }
    @Override
    public void onDisable() {
        if (inventorySnapshotManager != null) {
            inventorySnapshotManager.snapshotOnlinePlayers("SHUTDOWN");
        }
        if (teamEnderChestManager != null) {
            teamEnderChestManager.saveAll();
        }

        if (statsManager != null) {
            statsManager.flushOnlineSessions();
        }

        if (tokenManager != null) {
            tokenManager.stop();
            tokenManager = null;
        }

        if (raidManager != null) {
            raidManager.stop();
            raidManager = null;
        }

        if (outpostManager != null) {
            outpostManager.stop();
            outpostManager = null;
        }

        if (traderManager != null) {
            traderManager.stop();
            traderManager = null;
        }

        if (taxManager != null) {
            taxManager.stop();
            taxManager = null;
        }

        if (storage != null) {
            storage.close();
        }

        if (messageManager != null && messageManager.isInitialized()) {
            getLogger().info(
                TextFormat.RED + messageManager.getPrefix("main") + messageManager.getMessage("main", "disable")
            );
        }
        instance = null;
    }

    public Config getConfig() {
        return config;
    }

    public OutpostManager getOutpostManager() {
        return outpostManager;
    }

    public ModerationManager getModerationManager() {
        return moderationManager;
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
