package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;
import org.powernukkitx.command.route.node.RouteNode;
import org.powernukkitx.command.tree.node.StringNode;

public final class DelHomeCommand extends Command {
    private final HomeManager homeManager;
    private final MessageManager messages = new MessageManager();
    private final String prefix;

    public DelHomeCommand(SiedlerPlugin plugin) {
        super("delhome", "Löscht ein gespeichertes Home", "/delhome <Name>");
        this.homeManager = new HomeManager();
        this.prefix = messages.getPrefix("essentials");
        setPermission("siedler.command.delhome");
        setPermissionMessage(messages.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().then(
                RouteNode.argument("name", new StringNode()).exec(context -> {
                    CommandSender sender = context.getSender();
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(prefix + messages.getMessage("messages.essentials.player-required"));
                        return CommandResult.fail("Spieler erforderlich");
                    }

                    String name = context.getArg("name");
                    try {
                        if (!homeManager.deleteHome(player, name)) {
                            sender.sendMessage(prefix + messages.getMessage("messages.essentials.home-not-found")
                                    .replace("{name}", name));
                            return CommandResult.fail("Home nicht gefunden");
                        }

                        sender.sendMessage(prefix + messages.getMessage("messages.essentials.home-deleted")
                                .replace("{name}", name));
                        return CommandResult.success();
                    } catch (Exception exception) {
                        sender.sendMessage(prefix + messages.getMessage("messages.essentials.home-error")
                                .replace("{error}", safe(exception)));
                        return CommandResult.fail(safe(exception));
                    }
                })
        );
    }

    private String safe(Exception exception) {
        return exception.getMessage() == null ? "Unbekannter Fehler" : exception.getMessage();
    }
}
