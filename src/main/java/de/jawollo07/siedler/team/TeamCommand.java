package de.jawollo07.siedler.team;

import java.util.List;

import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandSender;

import de.jawollo07.siedler.SiedlerPlugin;

public final class TeamCommand extends Command {
    private final SiedlerPlugin plugin;
    public TeamCommand() {
        super("team", "Manage teams", "/team");
        this.plugin = SiedlerPlugin.getInstance();
        setPermission("siedler.command.team");
    }
    public String help(CommandSender sender) {
        sender.sendMessage("§6§lSiedler 2.0 §7- §fTeam Management");
        sender.sendMessage("§7/team create <name> <color> §8- §fCreate a new team");
        sender.sendMessage("§7/team delete <name> §8- §fDelete a team");
        sender.sendMessage("§7/team add <player> <team> §8- §fAdd a player to a team");
        sender.sendMessage("§7/team remove <player> §8- §fRemove a player from their team");
        sender.sendMessage("§7/team list §8- §fList all teams");
        sender.sendMessage("§7/team info <team> §8- §fGet information about a team");
        sender.sendMessage("§7/team setcolor <team> <color> §8- §fSet the color of a team");
        sender.sendMessage("§7/team help <team> §8- §fGet help about a team");
        return "help";
    }
    @Override 
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (args.length == 0) {
            return help(sender).equals("help");
        }
        switch (args[0].toLowerCase()) {
            case "create":
                return new TeamCreateCommand(plugin).execute(sender, commandLabel, args);
            case "delete":
                return new TeamDeleteCommand(plugin).execute(sender, commandLabel, args);
            case "add":
                return new TeamAddCommand(plugin).execute(sender, commandLabel, args);
            case "remove":
                return new TeamRemoveCommand(plugin).execute(sender, commandLabel, args);
            case "list":
                return new TeamListCommand(plugin).execute(sender, commandLabel, args);
            case "info":
                return new TeamInfoCommand(plugin).execute(sender, commandLabel, args);
            case "setcolor":
                return new TeamSetColorCommand(plugin).execute(sender, commandLabel, args);
            default:
                return help(sender).equals("help");
        }
    }
    public final class TeamCreateCommand {
        private final SiedlerPlugin plugin;
        public TeamCreateCommand(SiedlerPlugin plugin) {
            this.plugin = plugin;
            setPermission("siedler.command.team.create");
            setPermission("siedler.admin");
        }
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /team create <name> <color>");
                return false;
            }
            String name = args[1];
            String color = args[2];
            try {
                TeamManager teamManager = new TeamManager(plugin);
                Team team = teamManager.createTeam(name, color);
                sender.sendMessage("§aTeam '" + team.name() + "' created with color '" + team.color() + "'.");
                return true;
            } catch (Exception e) {
                sender.sendMessage("§cError creating team: " + e.getMessage());
                return false;
            }
        }
    }
    public final class TeamDeleteCommand {
        private final SiedlerPlugin plugin;
        public TeamDeleteCommand(SiedlerPlugin plugin) {
            this.plugin = plugin;
            setPermission("siedler.command.team.delete");
            setPermission("siedler.admin");
        }
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /team delete <name>");
                return false;
            }
            String name = args[1];
            try {
                TeamManager teamManager = new TeamManager(plugin);
                boolean deleted = teamManager.deleteTeam(name);
                if (deleted) {
                    sender.sendMessage("§aTeam '" + name + "' deleted.");
                } else {
                    sender.sendMessage("§cTeam '" + name + "' not found.");
                }
                return true;
            } catch (Exception e) {
                sender.sendMessage("§cError deleting team: " + e.getMessage());
                return false;
            }
        }
    }
    public final class TeamAddCommand {
        private final SiedlerPlugin plugin;
        public TeamAddCommand(SiedlerPlugin plugin) {
            this.plugin = plugin;
            setPermission("siedler.command.team.add");
            setPermission("siedler.admin");
        }
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /team add <player> <team>");
                return false;
            }
            String playerName = args[1];
            String teamName = args[2];
            try {
                TeamManager teamManager = new TeamManager(plugin);
                boolean added = teamManager.addPlayerToTeam(playerName, teamName);
                if (added) {
                    sender.sendMessage("§aPlayer '" + playerName + "' added to team '" + teamName + "'.");
                } else {
                    sender.sendMessage("§cPlayer '" + playerName + "' or team '" + teamName + "' not found.");
                }
                return true;
            } catch (Exception e) {
                sender.sendMessage("§cError adding player to team: " + e.getMessage());
                return false;
            }
        }
    }
    public final class TeamRemoveCommand {
        private final SiedlerPlugin plugin;
        public TeamRemoveCommand(SiedlerPlugin plugin) {
            this.plugin = plugin;
            setPermission("siedler.command.team.remove");
            setPermission("siedler.admin");
        }
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /team remove <player>");
                return false;
            }
            String playerName = args[1];
            try {
                TeamManager teamManager = new TeamManager(plugin);
                boolean removed = teamManager.removePlayerFromTeam(playerName);
                if (removed) {
                    sender.sendMessage("§aPlayer '" + playerName + "' removed from their team.");
                } else {
                    sender.sendMessage("§cPlayer '" + playerName + "' not found or not in a team.");
                }
                return true;
            } catch (Exception e) {
                sender.sendMessage("§cError removing player from team: " + e.getMessage());
                return false;
            }
        }
    }
    public final class TeamListCommand {
        private final SiedlerPlugin plugin;
        public TeamListCommand(SiedlerPlugin plugin) {
            this.plugin = plugin;
            setPermission("siedler.command.team.list");
            setPermission("siedler.admin");
            setPermission("siedler.basic");
        }
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            try {
                TeamManager teamManager = new TeamManager(plugin);
                List<Team> teams = teamManager.getTeams();
                if (teams.isEmpty()) {
                    sender.sendMessage("§cNo teams found.");
                } else {
                    sender.sendMessage("§6Teams:");
                    for (Team team : teams) {
                        sender.sendMessage("§7- §f" + team.name() + " §8(§f" + team.color() + "§8)");
                    }
                }
                return true;
            } catch (Exception e) {
                sender.sendMessage("§cError listing teams: " + e.getMessage());
                return false;
            }
        }
    }
    public final class TeamInfoCommand {
        private final SiedlerPlugin plugin;
        public TeamInfoCommand(SiedlerPlugin plugin) {
            this.plugin = plugin;
            setPermission("siedler.command.team.info");
            setPermission("siedler.admin");
            setPermission("siedler.basic");
        }
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /team info <team>");
                return false;
            }
            String teamName = args[1];
            try {
                TeamManager teamManager = new TeamManager(plugin);
                Team team = teamManager.getTeamByName(teamName);
                if (team == null) {
                    sender.sendMessage("§cTeam '" + teamName + "' not found.");
                    return false;
                }
                sender.sendMessage("§6Team Info:");
                sender.sendMessage("§7- §fName: §f" + team.name());
                sender.sendMessage("§7- §fColor: §f" + team.color());
                sender.sendMessage("§7- §fTax Bonus: §f" + team.taxBonus());
                sender.sendMessage("§7- §fEliminated: §f" + (team.eliminated() == 1 ? "Yes" : "No"));
                sender.sendMessage("§7- §fBalance: §f" + team.balance());
                return true;
            } catch (Exception e) {
                sender.sendMessage("§cError retrieving team info: " + e.getMessage());
                return false;
            }
        }
    }
    public final class TeamSetColorCommand {
        private final SiedlerPlugin plugin;
        public TeamSetColorCommand(SiedlerPlugin plugin) {
            this.plugin = plugin;
            setPermission("siedler.command.team.setcolor");
            setPermission("siedler.admin");
        }
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /team setcolor <team> <color>");
                return false;
            }
            String teamName = args[1];
            String color = args[2];
            try {
                TeamManager teamManager = new TeamManager(plugin);
                boolean updated = teamManager.setTeamColor(teamName, color);
                if (updated) {
                    sender.sendMessage("§aTeam '" + teamName + "' color set to '" + color + "'.");
                } else {
                    sender.sendMessage("§cTeam '" + teamName + "' not found.");
                }
                return true;
            } catch (Exception e) {
                sender.sendMessage("§cError setting team color: " + e.getMessage());
                return false;
            }
        }
    }
}