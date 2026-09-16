package de.jawollo07.siedler.team;

import de.jawollo07.siedler.SiedlerPlugin;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandSender;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

public class RelationCommands extends Command {
    private final SiedlerPlugin plugin;
    private final Relations relations;

    public RelationCommands(SiedlerPlugin plugin) {
        super("diplomatie", "Ändert die Diplomatien zwischen Teams", "/diplomatie");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.relations = new Relations(plugin);
        setPermission("siedler.command.diplomatie");
        setPermission("siedler.basic");
        plugin.getLogger().info("[RelationCommands] Diplomacy command initialized.");
    }

    public String help(CommandSender sender) {
        sender.sendMessage("§6§lSiedler §7- §fDiplomatie Management");
        sender.sendMessage("§7/diplomatie help §8- §fZeigt diese Hilfe an");
        sender.sendMessage("§7/diplomatie set <anderes Team> <friendly|allied|neutral|hostile|enemy> §8- §fÄndert die Diplomatie von deinem Team");
        sender.sendMessage("§7/diplomatie show [Team] §8- §fZeigt die aktuellen Diplomatien an");
        if (sender.hasPermission("siedler.admin")) {
            sender.sendMessage("§7/diplomatie admin set <team> <team> <friendly|allied|neutral|hostile|enemy> §8- §fÄndert die Diplomatie zwischen zwei Teams");
            sender.sendMessage("§7/diplomatie admin list §8- §fZeigt alle Team-Beziehungen an");
        }
        return "help";
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        plugin.getLogger().info("[RelationCommands] Command executed: " + commandLabel + " by " + sender.getName() + " args=" + String.join(" ", args));

        if (args.length == 0) {
            return help(sender).equals("help");
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "help":
                return help(sender).equals("help");
            case "set":
                return setRelation(sender, args);
            case "show":
                return showRelations(sender, args);
            case "list":
                return adminCommand(sender, new String[]{"admin", "list"});
            case "admin":
                return adminCommand(sender, args);
            default:
                return help(sender).equals("help");
        }
    }

    private boolean setRelation(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getLogger().warning("[RelationCommands] /diplomatie set called by non-player sender: " + sender.getName());
            sender.sendMessage("§cDieser Befehl kann nur von einem Spieler verwendet werden.");
            return false;
        }
        if (args.length < 3) {
            plugin.getLogger().warning("[RelationCommands] Missing arguments for /diplomatie set by " + sender.getName());
            sender.sendMessage("§cVerwendung: /diplomatie set <anderes Team> <friendly|allied|neutral|hostile|enemy>");
            return false;
        }

        String ownTeam = resolvePlayerTeamName(player);
        if (ownTeam == null) {
            plugin.getLogger().warning("[RelationCommands] Player " + player.getName() + " tried to change diplomacy without a team");
            sender.sendMessage("§cDu bist in keinem Team und kannst deshalb keine Diplomatie ändern.");
            return false;
        }

        String targetTeam = args[1];
        String relation = args[2];

        if (!Relations.isValidRelation(relation)) {
            plugin.getLogger().warning("[RelationCommands] Invalid relation input '" + relation + "' from " + player.getName());
            sender.sendMessage("§cUngültige Beziehung. Gültig: friendly|allied|neutral|hostile|enemy");
            return false;
        }

        try {
            boolean changed = relations.setTeamRelation(ownTeam, targetTeam, relation);
            if (changed) {
                plugin.getLogger().info("[RelationCommands] " + player.getName() + " changed relation " + ownTeam + " -> " + targetTeam + " = " + relation);
                sender.sendMessage("§aBeziehung zwischen §f" + ownTeam + " §aund §f" + targetTeam + " §aauf §f" + relation + " §agesetzt.");
                return true;
            }
            plugin.getLogger().warning("[RelationCommands] Failed to update relation from " + player.getName() + ": " + ownTeam + " -> " + targetTeam + " = " + relation);
            sender.sendMessage("§cDie Beziehung konnte nicht gesetzt werden. Prüfe Teamnamen und Team-Existenz.");
            return false;
        } catch (SQLException e) {
            plugin.getLogger().info("[RelationCommands] DB error while setting relation", e);
            sender.sendMessage("§cFehler beim Speichern der Diplomatie: " + e.getMessage());
            return false;
        }
    }

    private boolean showRelations(CommandSender sender, String[] args) {
        String teamName = args.length >= 2 ? args[1] : resolvePlayerTeamName(sender);
        if (teamName == null) {
            sender.sendMessage("§cKein Team angegeben und du bist in keinem Team.");
            return false;
        }

        try {
            Map<String, String> relationsForTeam = relations.getRelationsForTeamByName(teamName);
            if (relationsForTeam.isEmpty()) {
                sender.sendMessage("§7Für das Team §f" + teamName + " §7sind noch keine Beziehungen eingetragen.");
                return true;
            }

            sender.sendMessage("§6Beziehungen für Team §f" + teamName + "§6:");
            for (Map.Entry<String, String> entry : relationsForTeam.entrySet()) {
                sender.sendMessage("§7- " + entry.getKey() + " §8→ §f" + entry.getValue());
            }
            return true;
        } catch (SQLException e) {
            sender.sendMessage("§cFehler beim Laden der Beziehungen: " + e.getMessage());
            return false;
        }
    }

    private boolean adminCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("siedler.admin")) {
            sender.sendMessage("§cDu hast keine Rechte für Admin-Diplomatiebefehle.");
            return false;
        }
        if (args.length < 2) {
            return help(sender).equals("help");
        }

        String subAction = args[1].toLowerCase();
        switch (subAction) {
            case "set":
                if (args.length < 5) {
                    sender.sendMessage("§cVerwendung: /diplomatie admin set <team1> <team2> <friendly|allied|neutral|hostile|enemy>");
                    return false;
                }
                try {
                    String teamA = args[2];
                    String teamB = args[3];
                    String relation = args[4];
                    boolean changed = relations.setTeamRelation(teamA, teamB, relation);
                    if (changed) {
                        sender.sendMessage("§aBeziehung zwischen §f" + teamA + " §aund §f" + teamB + " §aauf §f" + relation + " §agesetzt.");
                        return true;
                    }
                    sender.sendMessage("§cDie Beziehung konnte nicht gesetzt werden.");
                    return false;
                } catch (SQLException e) {
                    sender.sendMessage("§cFehler beim Admin-Setzen: " + e.getMessage());
                    return false;
                }
            case "show":
            case "list":
                if (args.length >= 3) {
                    return showRelations(sender, new String[]{"show", args[2]});
                }
                try {
                    TeamManager teamManager = new TeamManager(plugin);
                    for (Team team : teamManager.getTeams()) {
                        sender.sendMessage("§6Team: §f" + team.name());
                        Map<String, String> entries = relations.getRelationsForTeamByName(team.name());
                        if (entries.isEmpty()) {
                            sender.sendMessage("§7- keine Beziehungen");
                            continue;
                        }
                        for (Map.Entry<String, String> entry : entries.entrySet()) {
                            sender.sendMessage("§7- " + entry.getKey() + " §8→ §f" + entry.getValue());
                        }
                    }
                    return true;
                } catch (SQLException e) {
                    sender.sendMessage("§cFehler beim Laden der Diplomatie-Liste: " + e.getMessage());
                    return false;
                }
            default:
                return help(sender).equals("help");
        }
    }

    private String resolvePlayerTeamName(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return null;
        }
        return resolvePlayerTeamName(player);
    }

    private String resolvePlayerTeamName(Player player) {
        String sql = "SELECT t.name FROM players p LEFT JOIN teams t ON t.id = p.team_id WHERE p.last_name = ?";
        try (Connection connection = plugin.getStorage().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, player.getName());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("name");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not resolve player team for " + player.getName() + ": " + e.getMessage());
        }
        return null;
    }
}
