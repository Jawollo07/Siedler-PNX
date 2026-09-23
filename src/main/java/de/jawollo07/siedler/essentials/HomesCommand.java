package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;
import org.powernukkitx.command.route.node.RouteNode;
import org.powernukkitx.form.window.SimpleForm;

import java.util.List;

public final class HomesCommand extends Command {
    private final HomeManager homeManager;
    private final MessageManager messages = new MessageManager();
    private final String prefix;

    public HomesCommand(SiedlerPlugin plugin) {
        super("homes", "Zeigt deine gespeicherten Homes", "/homes");
        this.homeManager = new HomeManager();
        this.prefix = messages.getPrefix("essentials");
        setPermission("siedler.command.homes");
        setPermissionMessage(messages.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().exec(context -> {
            CommandSender sender = context.getSender();
            if (!(sender instanceof Player player)) {
                sender.sendMessage(prefix + messages.getMessage("messages.essentials.player-required"));
                return CommandResult.fail(messages.getMessage("messages.essentials.player-required"));
            }

            openHomes(player);
            return CommandResult.success();
        });
    }

    private void openHomes(Player player) {
        try {
            List<Home> homes = homeManager.getHomes(player);

            if (homes.isEmpty()) {
                player.sendMessage(prefix + messages.getMessage("messages.essentials.homes-empty"));
                return;
            }

            SimpleForm form = new SimpleForm(
                    messages.getMessage("messages.essentials.homes-title"),
                    messages.getMessage("messages.essentials.homes-header")
                            .replace("{count}", String.valueOf(homes.size()))
            );

            for (Home home : homes) {
                form.addButton(
                        messages.getMessage("messages.essentials.home-button")
                                .replace("{name}", home.getName())
                                .replace("{world}", home.getWorld())
                                .replace("{x}", format(home.getX()))
                                .replace("{y}", format(home.getY()))
                                .replace("{z}", format(home.getZ())),
                        ignored -> teleport(player, home)
                );
            }

            form.addButton(messages.getMessage("messages.essentials.home-close"));
            form.send(player);
        } catch (Exception exception) {
            player.sendMessage(prefix + messages.getMessage("messages.essentials.home-error")
                    .replace("{error}", safe(exception)));
        }
    }

    private void teleport(Player player, Home home) {
        try {
            homeManager.teleport(player, home);
            player.sendMessage(prefix + messages.getMessage("messages.essentials.home-teleported")
                    .replace("{name}", home.getName()));
        } catch (Exception exception) {
            player.sendMessage(prefix + messages.getMessage("messages.essentials.home-teleport-error")
                    .replace("{error}", safe(exception)));
        }
    }

    private String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private String safe(Exception exception) {
        return exception.getMessage() == null ? "Unbekannter Fehler" : exception.getMessage();
    }
}
