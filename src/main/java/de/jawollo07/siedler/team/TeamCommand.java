package de.jawollo07.siedler.team;

import java.util.List;

import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandSender;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;

public final class TeamCommand extends Command {
    private final SiedlerPlugin plugin;
    private final MessageManager messageManager;
    private final String prefix;

    public TeamCommand() {
        super("team", "Manage teams", "/team");
        this.plugin = SiedlerPlugin.getInstance();
        this.messageManager = new MessageManager();
        this.prefix = messageManager.getPrefix("team");
        setPermission("siedler.command.team");
    }
    public String help(CommandSender sender) {
        sender.sendMessage(prefix + messageManager.getMessage("team-command", "help.1"));
        sender.sendMessage(prefix + messageManager.getMessage("team-command", "help.2"));
        sender.sendMessage(prefix + messageManager.getMessage("team-command", "help.3"));
        sender.sendMessage(prefix + messageManager.getMessage("team-command", "help.4"));
        sender.sendMessage(prefix + messageManager.getMessage("team-command", "help.5"));
        sender.sendMessage(prefix + messageManager.getMessage("team-command", "help.6"));
        sender.sendMessage(prefix + messageManager.getMessage("team-command", "help.7"));
        sender.sendMessage(prefix + messageManager.getMessage("team-command", "help.8"));
        sender.sendMessage(prefix + messageManager.getMessage("team-command", "help.9"));
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
                sender.sendMessage(prefix + messageManager.getMessage("team-command", "usage.create"));
                return false;
            }
            String name = args[1];
            String color = args[2];
            try {
                TeamManager teamManager = new TeamManager(plugin);
                Team team = teamManager.createTeam(name, color);
                sender.sendMessage(prefix + messageManager.getMessage("team-command", "create.success") + team.name() + "' created with color '" + team.color() + "'.");
                return true;
            } catch (Exception e) {
                sender.sendMessage(prefix + messageManager.getMessage("team-command", "create.error") + e.getMessage());
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
                sender.sendMessage(prefix + messageManager.getMessage("team-command", "usage.delete"));
                return false;
            }
            String name = args[1];
            try {
                TeamManager teamManager = new TeamManager(plugin);
                boolean deleted = teamManager.deleteTeam(name);
                if (deleted) {
                    sender.sendMessage(prefix + messageManager.getMessage("team-command", "delete.success") + name + "' deleted.");
                } else {
                    sender.sendMessage(prefix + messageManager.getMessage("team-command", "team-not-found") + name + "' not found.");
                }
                return true;
            } catch (Exception e) {
                sender.sendMessage(prefix + messageManager.getMessage("team-command", "delete.error") + e.getMessage());
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
                sender.sendMessage(prefix + messageManager.getMessage("team-command", "usage.add"));
                return false;
            }
            String playerName = args[1];
            String teamName = args[2];
            try {
                TeamManager teamManager = new TeamManager(plugin);
                boolean added = teamManager.addPlayerToTeam(playerName, teamName);
                if (added) {
                    sender.sendMessage(prefix + messageManager.getMessage("team-command", "add.success") + playerName + "' added to team '" + teamName + "'.");
                } else {
                    sender.sendMessage(prefix + messageManager.getMessage("team-command", "add.not-found") + playerName + "' or team '" + teamName + "' not found.");
                }
                return true;
            } catch (Exception e) {
                sender.sendMessage(prefix + messageManager.getMessage("team-command", "add.error") + e.getMessage());
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
                sender.sendMessage(prefix + messageManager.getMessage("team-command", "usage.remove"));
                return false;
            }
            String playerName = args[1];
            try {
                TeamManager teamManager = new TeamManager(plugin);
                boolean removed = teamManager.removePlayerFromTeam(playerName);
                if (removed) {
                    sender.sendMessage(prefix + messageManager.getMessage("team-command", "remove.success") + playerName + "' removed from their team.");
                } else {
                    sender.sendMessage(prefix + messageManager.getMessage("team-command", "remove.not-found") + playerName + "' not found or not in a team.");
                }
                return true;
            } catch (Exception e) {
                sender.sendMessage(prefix + messageManager.getMessage("team-command", "remove.error") + e.getMessage());
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
                    sender.sendMessage(prefix + messageManager.getMessage("team-command", "list.empty"));
                } else {
                    sender.sendMessage(prefix + messageManager.getMessage("team-command", "list.header"));
                    for (Team team : teams) {
                        sender.sendMessage(prefix + messageManager.getMessage("team-command", "list.entry") + team.name() + " §8(§f" + team.color() + "§8)");
                    }
                }
                return true;
            } catch (Exception e) {
                sender.sendMessage(prefix + messageManager.getMessage("team-command", "list.error") + e.getMessage());
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
                sender.sendMessage(prefix + messageManager.getMessage("team-command", "usage.info"));
                return false;
            }
            String teamName = args[1];
            try {
                TeamManager teamManager = new TeamManager(plugin);
                Team team = teamManager.getTeamByName(teamName);
                if (team == null) {
                    sender.sendMessage(prefix + messageManager.getMessage("team-command", "team-not-found") + teamName + "' not found.");
                    return false;
                }
                sender.sendMessage(prefix + messageManager.getMessage("team-command", "info.header"));
                sender.sendMessage(prefix + messageManager.getMessage("team-command", "info.name") + team.name());
                sender.sendMessage(prefix + messageManager.getMessage("team-command", "info.color") + team.color());
                sender.sendMessage(prefix + messageManager.getMessage("team-command", "info.tax-bonus") + team.taxBonus());
                sender.sendMessage(prefix + messageManager.getMessage("team-command", "info.eliminated") + (team.eliminated() == 1 ? "Yes" : "No"));
                sender.sendMessage(prefix + messageManager.getMessage("team-command", "info.balance") + team.balance());
                return true;
            } catch (Exception e) {
                sender.sendMessage(prefix + messageManager.getMessage("team-command", "info.error") + e.getMessage());
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
                sender.sendMessage(prefix + messageManager.getMessage("team-command", "usage.setcolor"));
                return false;
            }
            String teamName = args[1];
            String color = args[2];
            try {
                TeamManager teamManager = new TeamManager(plugin);
                boolean updated = teamManager.setTeamColor(teamName, color);
                if (updated) {
                    sender.sendMessage(prefix + messageManager.getMessage("team-command", "setcolor.success") + teamName + "' color set to '" + color + "'.");
                } else {
                    sender.sendMessage(prefix + messageManager.getMessage("team-command", "team-not-found") + teamName + "' not found.");
                }
                return true;
            } catch (Exception e) {
                sender.sendMessage(prefix + messageManager.getMessage("team-command", "setcolor.error") + e.getMessage());
                return false;
            }
        }
    }
}