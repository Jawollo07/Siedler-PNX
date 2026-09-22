package de.jawollo07.siedler.claim;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;
import org.powernukkitx.command.route.node.RouteNode;
import org.powernukkitx.command.tree.node.StringNode;
import org.powernukkitx.Player;

/**
 * Commands for creating, inspecting and deleting claims.
 *
 * <p>Uses the PowerNukkitX Tree Command API exclusively.</p>
 */
public final class ClaimCommand extends Command {
    private final ClaimManager claimManager;
    private final MessageManager messageManager;
    private final String prefix;

    public ClaimCommand(SiedlerPlugin plugin) {
        super("claim", "Verwaltung von Claims", "/claim <set|info|delete|help>");
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin darf nicht null sein");
        }

        setPermission("siedler.command.claim");
        this.claimManager = new ClaimManager(plugin);
        this.messageManager = new MessageManager();
        this.prefix = messageManager.getPrefix("claim");
        setPermissionMessage(messageManager.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().then(
                RouteNode.literal("help")
                        .exec(context -> {
                            sendHelp(context.getSender());
                            return CommandResult.success();
                        })
        );

        tree.getRoot().then(
                RouteNode.literal("set")
                        .then(RouteNode.argument("team", new StringNode()).exec(context -> {
                            CommandSender sender = context.getSender();
                            if (!(sender instanceof Player player)) {
                                sender.sendMessage(
                                        prefix + messageManager.getCommandMessage("only-player-command")
                                );
                                return CommandResult.success();
                            }

                            Claim claim = claimManager.setClaim(
                                    context.getArg("team"),
                                    player
                            );

                            return claim != null
                                    ? CommandResult.success()
                                    : CommandResult.fail("Claim konnte nicht erstellt werden");
                        }))
        );

        tree.getRoot().then(
                RouteNode.literal("info")
                        .exec(context -> {
                            CommandSender sender = context.getSender();
                            if (!(sender instanceof Player player)) {
                                sender.sendMessage(
                                        prefix + messageManager.getCommandMessage("only-player-command")
                                );
                                return CommandResult.success();
                            }

                            Claim claim = claimManager.claimInfoByPlayer(player);
                            if (claim == null) {
                                player.sendMessage(
                                        prefix + messageManager.getMessage("claim", "here-is-no-claim")
                                );
                                return CommandResult.fail("Kein Claim an dieser Position");
                            }

                            sendClaimInfo(player, claim);
                            return CommandResult.success();
                        })
        );

        tree.getRoot().then(
                RouteNode.literal("delete")
                        .exec(context -> {
                            CommandSender sender = context.getSender();
                            if (!(sender instanceof Player player)) {
                                sender.sendMessage(
                                        prefix + messageManager.getCommandMessage("only-player-command")
                                );
                                return CommandResult.success();
                            }

                            return claimManager.deleteClaim(player)
                                    ? CommandResult.success()
                                    : CommandResult.fail("Claim konnte nicht gelöscht werden");
                        })
        );
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(prefix + messageManager.getMessage("claim", "help.1"));
        sender.sendMessage(prefix + messageManager.getMessage("claim", "help.2"));
        sender.sendMessage(prefix + messageManager.getMessage("claim", "help.3"));
        sender.sendMessage(prefix + messageManager.getMessage("claim", "help.4"));
        sender.sendMessage(prefix + messageManager.getMessage("claim", "help.5"));
    }

    private void sendClaimInfo(Player player, Claim claim) {
        player.sendMessage("§6Claim-Informationen:");
        player.sendMessage("§7ID: §f" + claim.id());
        player.sendMessage("§7Team: §f" + claim.teamID());
        player.sendMessage("§7Welt: §f" + claim.world());
        player.sendMessage(
                "§7Chunks: §f"
                        + claim.min_x()
                        + ", "
                        + claim.min_z()
                        + " §7bis §f"
                        + claim.max_x()
                        + ", "
                        + claim.max_z()
        );
    }
}
