package de.jawollo07.siedler.team;

import java.sql.SQLException;
import java.util.List;

import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;
import org.powernukkitx.command.route.node.RouteNode;
import org.powernukkitx.command.tree.node.StringNode;
import org.powernukkitx.Player;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;

/** Team management commands, registered through the PowerNukkitX RouteTree API. */
public final class TeamCommand extends Command {
    private final SiedlerPlugin plugin;
    private final TeamManager teamManager;
    private final MessageManager messages = new MessageManager();
    private final String prefix;

    public TeamCommand() {
        super("team", "Manage teams", "/team help");
        this.plugin = SiedlerPlugin.getInstance();
        this.teamManager = new TeamManager(plugin);
        this.prefix = messages.getPrefix("team");
        setPermission("siedler.command.team");
        setPermissionMessage(messages.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().then(RouteNode.literal("help").exec(context -> {
            sendHelp(context.getSender());
            return CommandResult.success();
        }));

        tree.getRoot().then(RouteNode.literal("list").exec(context -> {
            list(context.getSender());
            return CommandResult.success();
        }));

        tree.getRoot().then(RouteNode.literal("info")
                .then(RouteNode.argument("name", new StringNode()).exec(context -> {
                    info(context.getSender(), context.getArg("name"));
                    return CommandResult.success();
                })));

        RouteNode admin = RouteNode.literal("admin")
                .permission("siedler.admin", messages.getCommandMessage("no-permission"));

        admin.then(RouteNode.literal("help").exec(context -> {
            sendAdminHelp(context.getSender());
            return CommandResult.success();
        }));
        admin.then(RouteNode.literal("create")
                .then(RouteNode.argument("name", new StringNode())
                        .then(RouteNode.argument("color", new StringNode()).exec(context -> {
                            create(context.getSender(), context.getArg("name"), context.getArg("color"));
                            return CommandResult.success();
                        }))));
        admin.then(RouteNode.literal("delete")
                .then(RouteNode.argument("name", new StringNode()).exec(context -> {
                    delete(context.getSender(), context.getArg("name"));
                    return CommandResult.success();
                })));
        admin.then(RouteNode.literal("add")
                .then(RouteNode.argument("player", new StringNode())
                        .then(RouteNode.argument("team", new StringNode()).exec(context -> {
                            add(context.getSender(), context.getArg("player"), context.getArg("team"));
                            return CommandResult.success();
                        }))));
        admin.then(RouteNode.literal("remove")
                .then(RouteNode.argument("player", new StringNode()).exec(context -> {
                    remove(context.getSender(), context.getArg("player"));
                    return CommandResult.success();
                })));
        admin.then(RouteNode.literal("setcolor")
                .then(RouteNode.argument("name", new StringNode())
                        .then(RouteNode.argument("color", new StringNode()).exec(context -> {
                            setColor(context.getSender(), context.getArg("name"), context.getArg("color"));
                            return CommandResult.success();
                        }))));
        tree.getRoot().then(admin);
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(prefix + "§eTeam-Admin-Befehle:");
        sender.sendMessage("§7/team admin create <Name> <Farbe> §8- §fErstellt ein Team");
        sender.sendMessage("§7/team admin delete <Name> §8- §fLöscht ein Team");
        sender.sendMessage("§7/team admin add <Spieler> <Team> §8- §fFügt einen Spieler hinzu");
        sender.sendMessage("§7/team admin remove <Spieler> §8- §fEntfernt einen Spieler");
        sender.sendMessage("§7/team admin setcolor <Name> <Farbe> §8- §fÄndert die Teamfarbe");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(prefix + "§eTeam-Befehle:");
        sender.sendMessage("§7/team create <Name> <Farbe> §8- §fErstellt ein Team");
        sender.sendMessage("§7/team delete <Name> §8- §fLöscht ein Team");
        sender.sendMessage("§7/team add <Spieler> <Team> §8- §fFügt einen Online-Spieler hinzu");
        sender.sendMessage("§7/team remove <Spieler> §8- §fEntfernt einen Spieler");
        sender.sendMessage("§7/team list §8- §fListet Teams auf");
        sender.sendMessage("§7/team info <Name> §8- §fZeigt Team-Informationen");
        sender.sendMessage("§7/team setcolor <Name> <Farbe> §8- §fÄndert die Teamfarbe");
    }

    private void create(CommandSender sender, String name, String color) {
        try {
            Team team = teamManager.createTeam(name, color);
            sender.sendMessage(prefix + "Team §f" + team.name() + "§a wurde mit der Farbe §f" + team.color() + "§a erstellt.");
        } catch (Exception e) {
            sender.sendMessage(prefix + "§cTeam konnte nicht erstellt werden: " + e.getMessage());
        }
    }

    private void delete(CommandSender sender, String name) {
        try {
            teamManager.deleteTeam(name);
            sender.sendMessage(prefix + "§aTeam §f" + name + "§a wurde gelöscht.");
        } catch (Exception e) {
            sender.sendMessage(prefix + "§cTeam konnte nicht gelöscht werden: " + e.getMessage());
        }
    }

    private void add(CommandSender sender, String player, String team) {
        try {
            teamManager.addPlayerToTeam(player, team);
            sender.sendMessage(prefix + "§aSpieler §f" + player + "§a wurde Team §f" + team + "§a hinzugefügt.");
        } catch (Exception e) {
            sender.sendMessage(prefix + "§cSpieler konnte nicht hinzugefügt werden: " + e.getMessage());
        }
    }

    private void remove(CommandSender sender, String player) {
        try {
            teamManager.removePlayerFromTeam(player);
            sender.sendMessage(prefix + "§aSpieler §f" + player + "§a wurde aus seinem Team entfernt.");
        } catch (Exception e) {
            sender.sendMessage(prefix + "§cSpieler konnte nicht entfernt werden: " + e.getMessage());
        }
    }

    private void list(CommandSender sender) {
        try {
            List<Team> teams = teamManager.getTeams();
            if (teams.isEmpty()) {
                sender.sendMessage(prefix + "§7Es sind keine Teams vorhanden.");
                return;
            }
            sender.sendMessage(prefix + "§eTeams:");
            for (Team team : teams) {
                sender.sendMessage("§8- §f" + team.name() + " §8(§f" + team.color() + "§8)");
            }
        } catch (SQLException e) {
            sender.sendMessage(prefix + "§cTeams konnten nicht geladen werden.");
            plugin.getLogger().warning("Could not list teams: " + e.getMessage());
        }
    }

    private void info(CommandSender sender, String name) {
        try {
            Team team = teamManager.getTeamByName(name);
            sender.sendMessage(prefix + "§eTeam-Informationen:");
            sender.sendMessage("§7Name: §f" + team.name());
            sender.sendMessage("§7Farbe: §f" + team.color());
            sender.sendMessage("§7Steuerbonus: §f" + team.taxBonus());
            sender.sendMessage("§7Eliminiert: §f" + (team.eliminated() == 1 ? "Ja" : "Nein"));
            sender.sendMessage("§7Kontostand: §f" + team.balance());
        } catch (SQLException e) {
            sender.sendMessage(prefix + "§cTeam §f" + name + "§c wurde nicht gefunden.");
        }
    }

    private void setColor(CommandSender sender, String name, String color) {
        try {
            teamManager.setTeamColor(name, color);
            sender.sendMessage(prefix + "§aDie Farbe von Team §f" + name + "§a wurde auf §f" + color + "§a gesetzt.");
        } catch (Exception e) {
            sender.sendMessage(prefix + "§cTeamfarbe konnte nicht geändert werden: " + e.getMessage());
        }
    }
}
