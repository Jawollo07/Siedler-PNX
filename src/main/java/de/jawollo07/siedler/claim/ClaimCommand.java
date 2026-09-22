package de.jawollo07.siedler.claim;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandSender;

/** Commands for creating, inspecting and deleting claims. */
public final class ClaimCommand extends Command {
    private final ClaimManager claimManager;
    private final Utils utils;
    private final MessageManager messageManager;
    private final String prefix;
    public ClaimCommand(SiedlerPlugin plugin) {
        super("claim", "Verwaltung von Claims", "/claim <set|info|delete|help>");
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin darf nicht null sein");
        }
        setPermission("siedler.command.claim");
        this.claimManager = new ClaimManager(plugin);
        this.utils = new Utils(plugin);
        this.messageManager = new MessageManager();
        this.setPermissionMessage(messageManager.getCommandMessage("no-permission"));
        this.prefix = messageManager.getPrefix("claim");
    }

    private boolean sendHelp(CommandSender sender) {
        sender.sendMessage(messageManager.getMessage("claim", "help.1"));
        sender.sendMessage(messageManager.getMessage("claim", "help.2"));
        sender.sendMessage(messageManager.getMessage("claim", "help.3"));
        sender.sendMessage(messageManager.getMessage("claim", "help.4"));
        sender.sendMessage(messageManager.getMessage("claim", "help.5"));
        return true;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (args == null || args.length == 0) {
            return sendHelp(sender);
        }

        String subcommand = args[0].toLowerCase(java.util.Locale.ROOT);
        if (subcommand.equals("help")) {
            return sendHelp(sender);
        }
        if (!subcommand.equals("set") && !subcommand.equals("info") && !subcommand.equals("delete")) {
            sender.sendMessage(messageManager.getCommandMessage("unknown-subcommand") + args[0]);
            return sendHelp(sender);
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageManager.getCommandMessage("only-player-command"));
            return false;
        }

        switch (subcommand) {
            case "set" -> {
                if (args.length != 2 || args[1].isBlank()) {
                    player.sendMessage(messageManager.getMessage("claim", "command-usage.set"));
                    return false;
                }
                return claimManager.setClaim(args[1].trim(), player) != null;
            }
            case "info" -> {
                if (args.length != 1) {
                    player.sendMessage(messageManager.getMessage("claim", "command-usage.info"));
                    return false;
                }
                Claim claim = claimManager.ClaimInfoByPlayer(player);
                if (claim == null) {
                    player.sendMessage(messageManager.getMessage("claim", "here-is-no-claim"));
                    return false;
                }
                player.sendMessage("§6Claim-Informationen:");
                player.sendMessage("§7ID: §f" + claim.id());
                player.sendMessage("§7Team: §f" +  utils.getClaimTeam(claim.id()));
                player.sendMessage("§7Welt: §f" + claim.world());
                player.sendMessage("§7Chunks: §f" + claim.min_x() + ", " + claim.min_z()
                        + " §7bis §f" + claim.max_x() + ", " + claim.max_z());
                return true;
            }
            case "delete" -> {
                if (args.length != 1) {
                    player.sendMessage(messageManager.getMessage("claim", "command-usage.delete"));
                    return false;
                }
                return claimManager.deleteClaim(player);
            }
            default -> {
                return sendHelp(sender);
            }
        }
    }
}
