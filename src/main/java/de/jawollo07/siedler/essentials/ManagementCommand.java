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
import java.util.Map;

public class ManagementCommand extends Command {
    private final SiedlerPlugin plugin;
    private final MessageManager messageManager;
    private final String prefix;

    public ManagementCommand(SiedlerPlugin plugin) {
        super("verwaltung", "Öffnet die Siedler-Verwaltung", "/verwaltung");
        this.plugin = plugin;
        this.messageManager = new MessageManager();
        this.prefix = messageManager.getPrefix("essentials");
        setPermission("siedler.admin");
        setPermissionMessage(messageManager.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().then(RouteNode.literal("help").exec(context -> {
            sendHelp(context.getSender());
            return CommandResult.success();
        }));
        tree.getRoot().then(RouteNode.literal("menu").exec(context -> open(context.getSender())));
        tree.getRoot().exec(context -> open(context.getSender()));
    }

    private CommandResult open(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.player-required"));
            return CommandResult.fail("Spieler erforderlich");
        }
        openMainMenu(player);
        return CommandResult.success();
    }

    private void openMainMenu(Player player) {
        int online = plugin.getServer().getOnlinePlayers().size();
        new SimpleForm(
                messageManager.getMessage("messages.essentials.management-title"),
                messageManager.getMessage("messages.essentials.management-header").replace("{online}", String.valueOf(online)))
                .addButton(messageManager.getMessage("messages.essentials.management-players"), ignored -> openPlayerList(player))
                .addButton(messageManager.getMessage("messages.essentials.management-close"))
                .send(player);
    }

    private void openPlayerList(Player admin) {
        Map<?, Player> players = plugin.getServer().getOnlinePlayers();
        SimpleForm form = new SimpleForm(
                messageManager.getMessage("messages.essentials.management-players-title"),
                messageManager.getMessage("messages.essentials.management-players-header").replace("{online}", String.valueOf(players.size())));
        for (Player target : players.values()) {
            form.addButton(target.getName(), ignored -> openPlayerInfo(admin, target));
        }
        form.addButton(messageManager.getMessage("messages.essentials.management-back"), ignored -> openMainMenu(admin));
        form.send(admin);
    }

    private void openPlayerInfo(Player admin, Player target) {
        String info = messageManager.getMessage("messages.essentials.management-player-info")
                .replace("{name}", target.getName())
                .replace("{uuid}", target.getUniqueId().toString())
                .replace("{world}", target.getLevel() == null ? "-" : target.getLevel().getName())
                .replace("{x}", String.valueOf((int) target.getFloorX()))
                .replace("{y}", String.valueOf((int) target.getFloorY()))
                .replace("{z}", String.valueOf((int) target.getFloorZ()));
        new SimpleForm(messageManager.getMessage("messages.essentials.management-player-title"), info)
                .addButton(messageManager.getMessage("messages.essentials.management-back"), ignored -> openPlayerList(admin))
                .send(admin);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-help"));
    }
}
