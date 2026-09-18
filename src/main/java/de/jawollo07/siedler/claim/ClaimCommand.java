package de.jawollo07.siedler.claim;

import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.Player;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.claim.ClaimManager;
import de.jawollo07.siedler.team.TeamManager;
import de.jawollo07.siedler.claim.Utils;
import de.jawollo07.siedler.storage.StorageManager;

public class ClaimCommand extends Command {
    private final ClaimManager claimManager;
    private final TeamManager teamManager;
    private final Utils utils;
    private final StorageManager storageManager;
    public ClaimCommand(SiedlerPlugin plugin) {
        super("claim", "Befehl zur Verwaltung von Claims", "/claim");
        setPermission("siedler.command.claim");
        setPermission("siedler.admin");
        this.claimManager = new ClaimManager(plugin);
        this.teamManager = new TeamManager(plugin);
        this.storageManager = new StorageManager(plugin);
        this.utils = new Utils(storageManager);
    }
    public String help(CommandSender sender) {
        sender.sendMessage("§6§lSiedler 2.0 §7- §fClaim Management");
        sender.sendMessage("§7/claim set <team> §8- §fSet a new claim");
        sender.sendMessage("§7/claim delete §8- §fDelete a claim");
        sender.sendMessage("§7/claim info §8- §fGet information about a claim");
        return "help";
    }
    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (args.length < 1 ) {
            return help(sender).equals("help");
        }
        switch (args[0].toLowerCase()) {
            case "set":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cDieser Befehl kann nur von einem Spieler ausgeführt werden.");
                    return false;
                }
                return claimManager.setClaim(args[1] ,(Player) sender) != null;
            case "delete":
                String claim_id = utils.get_claimID((Player) sender);
                return claimManager.deleteClaim(claim_id);
            case "info":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cDieser Befehl kann nur von einem Spieler ausgeführt werden.");
                    return false;
                }
                return claimManager.ClaimInfoByPlayer((Player) sender) != null;
            case "help":
                return help(sender).equals("help");
            default:
                return help(sender).equals("help");
        }
    }
    public final class ClaimSetCommand {
        private final SiedlerPlugin plugin;
        public ClaimSetCommand(SiedlerPlugin plugin) {
            this.plugin = plugin;
            setPermission("siedler.command.claim.set");
            setPermission("siedler.admin");
        }
        
    }
}
