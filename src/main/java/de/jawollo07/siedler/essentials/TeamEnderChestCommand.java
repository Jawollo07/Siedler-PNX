package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import de.jawollo07.siedler.team.Team;
import de.jawollo07.siedler.team.TeamManager;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;

import java.sql.SQLException;

public final class TeamEnderChestCommand extends Command {

    private final SiedlerPlugin plugin;
    private final MessageManager messageManager;
    private final TeamManager teamManager;
    private final TeamEnderChestManager teamEnderChestManager;

    public TeamEnderChestCommand(SiedlerPlugin plugin, TeamEnderChestManager teamEnderChestManager) {
        super("tec", "Öffnet die gemeinsame Team-Enderchest", "/tec");
        this.plugin = plugin;
        this.messageManager = new MessageManager();
        this.teamManager = new TeamManager(plugin);
        this.teamEnderChestManager = teamEnderChestManager;
        setPermission("siedler.command.tec");
        setPermissionMessage(messageManager.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().exec(context -> {
            CommandSender sender = context.getSender();

            if (!(sender instanceof Player player)) {
                sender.sendMessage(messageManager.getCommandMessage("only-player-command"));
                return CommandResult.success();
            }

            try {
                Team team = teamManager.getTeamForPlayer(player.getUniqueId().toString());
                if (team == null) {
                    player.sendMessage(messageManager.getMessage("messages.essentials.team-enderchest-no-team"));
                    return CommandResult.success();
                }

                TeamEnderChestInventory inventory = teamEnderChestManager.getOrCreate(player, team.id());
                int windowId = player.addWindow(inventory);
                if (windowId == -1) {
                    player.sendMessage(messageManager.getMessage("messages.essentials.team-enderchest-already-open"));
                }
            } catch (SQLException exception) {
                plugin.getLogger().warning("Team-Enderchest konnte nicht geöffnet werden: " + exception.getMessage());
                player.sendMessage(messageManager.getMessage("messages.essentials.team-enderchest-error"));
            }

            return CommandResult.success();
        });
    }
}
