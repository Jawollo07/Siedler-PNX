package de.jawollo07.siedler.eco;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import de.jawollo07.siedler.team.Team;
import de.jawollo07.siedler.team.TeamManager;

import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.route.RouteTree;
import org.powernukkitx.command.route.node.RouteNode;
import org.powernukkitx.command.tree.node.IntNode;
import org.powernukkitx.command.tree.node.StringNode;

public class EcoCommand extends Command {
    private final MessageManager messageManager;
    private final TeamManager teamManager;
    private final EcoManager ecoManager;
    private final String prefix;

    public EcoCommand(SiedlerPlugin plugin) {
        super("eco", "Verwaltet das Geld", "Verwendung /eco");
        this.messageManager = new MessageManager();
        this.teamManager = new TeamManager(plugin);
        this.ecoManager = new EcoManager(plugin);
        this.prefix = messageManager.getPrefix("eco");

        this.setPermission("siedler.command.eco");
        this.setPermissionMessage(messageManager.getCommandMessage("no-permission"));
        this.enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().then(
                RouteNode.literal("help")
                        .exec(context -> {
                            context.getSender().sendMessage(prefix + "/eco show <Team>");
                            context.getSender().sendMessage(prefix + "/eco admin set <Team> <Betrag>");
                            context.getSender().sendMessage(prefix + "/eco admin add <Team> <Betrag>");
                            context.getSender().sendMessage(prefix + "/eco admin remove <Team> <Betrag>");
                            return CommandResult.success();
                        })
        );

        tree.getRoot().then(
                RouteNode.literal("show")
                        .then(RouteNode.argument("team", new StringNode()).exec(context -> {
                            try {
                                String teamName = context.getArg("team");
                                Team team = teamManager.getTeamByName(teamName);
                                Integer balance = ecoManager.getMoney(team.id());
                                String symbol = ecoManager.getCurrency("s");
                                context.getSender().sendMessage(
                                        prefix + "Der Kontostand von " + team.name() + " ist: "
                                                + balance + symbol
                                );
                                return CommandResult.success();
                            } catch (Exception exception) {
                                return CommandResult.fail(exception.getMessage());
                            }
                        }))
        );

        RouteNode admin = RouteNode.literal("admin")
                .permission("siedler.admin", messageManager.getCommandMessage("no-permission"));

        admin.then(
                RouteNode.literal("help")
                        .exec(context -> {
                            context.getSender().sendMessage(prefix + "/eco admin set <Team> <Betrag>");
                            context.getSender().sendMessage(prefix + "/eco admin add <Team> <Betrag>");
                            context.getSender().sendMessage(prefix + "/eco admin remove <Team> <Betrag>");
                            return CommandResult.success();
                        })
        );

        admin.then(
                RouteNode.literal("set")
                        .then(RouteNode.argument("team", new StringNode())
                                .then(RouteNode.argument("balance", new IntNode()).exec(context -> {
                                    try {
                                        String teamName = context.getArg("team");
                                        Integer balance = context.getArg("balance");
                                        Team team = teamManager.getTeamByName(teamName);
                                        ecoManager.setMoney(team.id(), balance);
                                        String symbol = ecoManager.getCurrency("s");
                                        context.getSender().sendMessage(
                                                prefix + "Der Kontostand von " + team.name()
                                                        + " wurde auf " + balance + symbol + " gesetzt."
                                        );
                                        return CommandResult.success();
                                    } catch (Exception exception) {
                                        return CommandResult.fail(exception.getMessage());
                                    }
                                })))
        );

        admin.then(
                RouteNode.literal("add")
                        .then(RouteNode.argument("team", new StringNode())
                                .then(RouteNode.argument("balance", new IntNode()).exec(context -> {
                                    try {
                                        String teamName = context.getArg("team");
                                        Integer amount = context.getArg("balance");
                                        Team team = teamManager.getTeamByName(teamName);
                                        ecoManager.addMoney(team.id(), amount);
                                        String symbol = ecoManager.getCurrency("s");
                                        context.getSender().sendMessage(
                                                prefix + "Dem Kontostand von " + team.name()
                                                        + " wurden " + amount + symbol + " hinzugefügt."
                                        );
                                        return CommandResult.success();
                                    } catch (Exception exception) {
                                        return CommandResult.fail(exception.getMessage());
                                    }
                                })))
        );

        admin.then(
                RouteNode.literal("remove")
                        .then(RouteNode.argument("team", new StringNode())
                                .then(RouteNode.argument("balance", new IntNode()).exec(context -> {
                                    try {
                                        String teamName = context.getArg("team");
                                        Integer amount = context.getArg("balance");
                                        Team team = teamManager.getTeamByName(teamName);
                                        ecoManager.removeMoney(team.id(), amount);
                                        String symbol = ecoManager.getCurrency("s");
                                        context.getSender().sendMessage(
                                                prefix + "Vom Kontostand von " + team.name()
                                                        + " wurden " + amount + symbol + " abgezogen."
                                        );
                                        return CommandResult.success();
                                    } catch (Exception exception) {
                                        return CommandResult.fail(exception.getMessage());
                                    }
                                })))
        );

        tree.getRoot().then(admin);
    }
}
