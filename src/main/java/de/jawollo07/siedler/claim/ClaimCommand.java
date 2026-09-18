package de.jawollo07.siedler.claim;

import de.jawollo07.siedler.SiedlerPlugin;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandSender;

/** Commands for creating, inspecting and deleting claims. */
public final class ClaimCommand extends Command {
    private final ClaimManager claimManager;
    private final Utils utils;

    public ClaimCommand(SiedlerPlugin plugin) {
        super("claim", "Verwaltung von Claims", "/claim <set|info|delete|help>");
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin darf nicht null sein");
        }
        setPermission("siedler.command.claim");
        this.claimManager = new ClaimManager(plugin);
        this.utils = new Utils(plugin.getStorage());
    }

    private boolean help(CommandSender sender) {
        sender.sendMessage("§6§lSiedler §7- §fClaim-Verwaltung");
        sender.sendMessage("§7/claim set <team> §8- §fErstellt einen Claim (5x5 Chunks)");
        sender.sendMessage("§7/claim info §8- §fZeigt Informationen zum Claim hier");
        sender.sendMessage("§7/claim delete §8- §fLöscht den Claim hier");
        return true;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (args == null || args.length == 0 || args[0].equalsIgnoreCase("help")) {
            return help(sender);
        }

        String subcommand = args[0].toLowerCase(java.util.Locale.ROOT);
        if (!subcommand.equals("set") && !subcommand.equals("info") && !subcommand.equals("delete")) {
            sender.sendMessage("§cUnbekannter Unterbefehl.");
            return help(sender);
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl kann nur von einem Spieler ausgeführt werden.");
            return false;
        }

        switch (subcommand) {
            case "set" -> {
                if (args.length != 2 || args[1].isBlank()) {
                    player.sendMessage("§cVerwendung: /claim set <team>");
                    return false;
                }
                return claimManager.setClaim(args[1].trim(), player) != null;
            }
            case "info" -> {
                if (args.length != 1) {
                    player.sendMessage("§cVerwendung: /claim info");
                    return false;
                }
                Claim claim = claimManager.ClaimInfoByPlayer(player);
                if (claim == null) {
                    player.sendMessage("§cHier befindet sich kein Claim.");
                    return false;
                }
                player.sendMessage("§6Claim-Informationen:");
                player.sendMessage("§7ID: §f" + claim.id());
                player.sendMessage("§7Team-ID: §f" + claim.teamID());
                player.sendMessage("§7Welt: §f" + claim.world());
                player.sendMessage("§7Chunks: §f" + claim.min_x() + ", " + claim.min_z()
                        + " §7bis §f" + claim.max_x() + ", " + claim.max_z());
                return true;
            }
            case "delete" -> {
                if (args.length != 1) {
                    player.sendMessage("§cVerwendung: /claim delete");
                    return false;
                }
                String claimId = utils.get_claimID(player);
                if (claimId == null) {
                    player.sendMessage("§cHier befindet sich kein Claim.");
                    return false;
                }
                return claimManager.deleteClaim(claimId);
            }
            default -> {
                return help(sender);
            }
        }
    }
}
