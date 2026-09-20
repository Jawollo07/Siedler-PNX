package de.jawollo07.siedler.eco;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import de.jawollo07.siedler.eco.EcoManager;
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
        super("eco", "Verwaltet das Geld", "Verwendung /eco ");
        this.messageManager = new MessageManager();
        this.teamManager = new TeamManager(plugin);
        this.ecoManager = new EcoManager(plugin);
        this.prefix = messageManager.getPrefix("eco");
        this.setPermission("siedler.command.eco");
        this.setPermission("siedler.admin");
        this.setPermissionMessage(messageManager.getCommandMessage("no-permission"));
        this.enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().then(
            RouteNode.literal("help")
                .exec(context -> {
                    // sendHelp(context);
                    return CommandResult.success();
                })
        );
        tree.getRoot().then(
            RouteNode.literal("show")
            .then(
                RouteNode.argument("team", new StringNode())
                    .exec(context -> {
                        try {
                            String team_name = context.getArg("team");
                            Team team = teamManager.getTeamByName(team_name);
                            String team_id = team.id();
                            Integer balance = ecoManager.getMoney(team_id);
                            String ecoSymbol = ecoManager.getCurrency("s");
                            String message = prefix + "Der Kontostand von: " + team_name + " ist: " + balance + ecoSymbol ;
                            context.getSender().sendMessage(message);
                        }
                        catch(Exception exception) {
                            String e = exception.toString();
                            return CommandResult.fail(e);
                        }
                        return CommandResult.success();
                    })   
            )
        );
        tree.getRoot().then(
            RouteNode.literal("set")
            .then(
                RouteNode.argument("team", new StringNode())
                    .then(
                        RouteNode.argument("balance", new IntNode())
                            .exec(context -> {
                                try {
                                    String team_name = context.getArg("team");
                                    Integer balance = context.getArg("balance");
                                    Team team = teamManager.getTeamByName(team_name);
                                    String team_id = team.id();
                                    String ecoSymbol = ecoManager.getCurrency("s");
                                    ecoManager.setMoney(team_id, balance);
                                    String message = prefix + "Der Kontostand von: " + team_name + " wurde auf " + balance + ecoSymbol + " gesetzt";
                                    context.getSender().sendMessage(message);
                                    return CommandResult.success();
                                } catch (Exception e) {
                                    return CommandResult.fail(e.toString());
                                }
                            })
                    )
            )
        );
        tree.getRoot().then(
            RouteNode.literal("add")
            .then(
                RouteNode.argument("team", new StringNode())
                    .then(
                        RouteNode.argument("balance", new IntNode())
                            .exec(context -> {
                                try {
                                    String team_name = context.getArg("team");
                                    Integer balance = context.getArg("balance");
                                    Team team = teamManager.getTeamByName(team_name);
                                    String team_id = team.id();
                                    String ecoSymbol = ecoManager.getCurrency("s");
                                    ecoManager.addMoney(team_id, balance);
                                    String message = prefix + "Dem Kontostand von: " + team_name + " wurden " + balance + ecoSymbol + " hinzugefügt";
                                    context.getSender().sendMessage(message);
                                    return CommandResult.success();
                                } catch (Exception e) {
                                    return CommandResult.fail(e.toString());
                                }
                            })
                    )
            )
        );
        tree.getRoot().then(
            RouteNode.literal("remove")
            .then(
                RouteNode.argument("team", new StringNode())
                    .then(
                        RouteNode.argument("balance", new IntNode())
                            .exec(context -> {
                                try {
                                    String team_name = context.getArg("team");
                                    Integer balance = context.getArg("balance");
                                    Team team = teamManager.getTeamByName(team_name);
                                    String team_id = team.id();
                                    String ecoSymbol = ecoManager.getCurrency("s");
                                    ecoManager.removeMoney(team_id, balance);
                                    String message = prefix + "Dem Kontostand von: " + team_name + " wurden " + balance + ecoSymbol + " abgezogen";
                                    context.getSender().sendMessage(message);
                                    return CommandResult.success();
                                } catch (Exception e) {
                                    return CommandResult.fail(e.toString());
                                }
                            })
                    )
            )
        );
    }
}
