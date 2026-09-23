package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;

public final class DeathCommand extends Command {
    private final DeathManager deathManager;
    private final MessageManager messages = new MessageManager();
    private final String prefix;

    public DeathCommand(SiedlerPlugin plugin, DeathManager deathManager) {
        super("death", "Teleportiert dich zu deinem letzten Todespunkt", "/death");
        this.deathManager = deathManager;
        this.prefix = messages.getPrefix("essentials");
        setPermission("siedler.command.death");
        setPermissionMessage(messages.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().exec(context -> {
            CommandSender sender = context.getSender();
            if (!(sender instanceof Player player)) {
                sender.sendMessage(prefix + messages.getMessage("messages.essentials.player-required"));
                return CommandResult.fail("Spieler erforderlich");
            }

            try {
                DeathManager.DeathPoint point = deathManager.getDeathPoint(player);
                if (point == null) {
                    player.sendMessage(prefix + messages.getMessage("messages.essentials.death-none"));
                    return CommandResult.fail("Kein Todespunkt");
                }

                deathManager.teleport(player, point);
                player.sendMessage(prefix + messages.getMessage("messages.essentials.death-teleported")
                        .replace("{world}", point.world())
                        .replace("{x}", format(point.x()))
                        .replace("{y}", format(point.y()))
                        .replace("{z}", format(point.z())));
                return CommandResult.success();
            } catch (Exception e) {
                player.sendMessage(prefix + messages.getMessage("messages.essentials.death-error")
                        .replace("{error}", e.getMessage() == null ? "Unbekannter Fehler" : e.getMessage()));
                return CommandResult.fail("Teleport fehlgeschlagen");
            }
        });
    }

    private String format(double value) {
        return String.format(java.util.Locale.US, "%.1f", value);
    }
}
