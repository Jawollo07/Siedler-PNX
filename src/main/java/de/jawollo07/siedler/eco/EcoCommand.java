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
    private final TaxManager taxManager;
    private final String prefix;

    public EcoCommand(SiedlerPlugin plugin) {
        super("eco", "Verwaltet das Geld", "Verwendung /eco");
        this.messageManager = new MessageManager();
        this.teamManager = new TeamManager(plugin);
        this.ecoManager = new EcoManager(plugin);
        this.taxManager = new TaxManager(plugin);
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
                            context.getSender().sendMessage(prefix + "/eco stats [Team]");
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

        RouteNode stats = RouteNode.literal("stats");

        stats.exec(context -> {
            if (!(context.getSender() instanceof Player player)) {
                context.getSender().sendMessage(prefix + "Die Steuer-Statistik als UI kann nur ein Spieler öffnen.");
                return CommandResult.fail("Spieler erforderlich.");
            }

            try {
                Team team = teamManager.getTeamForPlayer(player.getUniqueId().toString());
                if (team == null) {
                    context.getSender().sendMessage(prefix + "Du bist keinem Team zugeordnet.");
                    return CommandResult.fail("Spieler ist keinem Team zugeordnet.");
                }
                sendTaxStatistics(player, team);
                return CommandResult.success();
            } catch (Exception exception) {
                return CommandResult.fail(exception.getMessage());
            }
        });

        stats.then(RouteNode.argument("team", new StringNode()).exec(context -> {
            try {
                Team team = teamManager.getTeamByName(context.getArg("team"));
                if (context.getSender() instanceof Player player) {
                    sendTaxStatistics(player, team);
                } else {
                    sendTaxStatistics(context.getSender(), team);
                }
                return CommandResult.success();
            } catch (Exception exception) {
                return CommandResult.fail(exception.getMessage());
            }
        }));

        tree.getRoot().then(stats);

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

    private void sendTaxStatistics(Player player, Team team) throws Exception {
        TaxManager.TaxStatistics stats = taxManager.getStatistics(team.id());
        String symbol = ecoManager.getCurrency("s");

        SimpleForm form = new SimpleForm("Steuer-Statistik: " + team.name(),
                buildStatisticsText(team, stats, symbol))
                .addButton("Aktualisieren", ignored -> {
                    try {
                        sendTaxStatistics(player, team);
                    } catch (Exception exception) {
                        player.sendMessage(prefix + "Fehler beim Aktualisieren: " + exception.getMessage());
                    }
                })
                .addButton("Schließen");

        form.send(player);
    }

    private void sendTaxStatistics(org.powernukkitx.command.CommandSender sender, Team team) throws Exception {
        TaxManager.TaxStatistics stats = taxManager.getStatistics(team.id());
        sender.sendMessage(buildStatisticsText(team, stats, ecoManager.getCurrency("s")));
    }

    private String buildStatisticsText(Team team, TaxManager.TaxStatistics stats, String symbol) {
        String last = stats.lastTaxTimestamp() == null
                ? "Noch keine Steuererhebung"
                : formatTimestamp(stats.lastTaxTimestamp());

        return "§6Steuerübersicht§r\n"
                + "Team: " + team.name() + "\n"
                + "TaxBonus: " + Math.max(1, team.taxBonus()) + "\n\n"
                + "§eGesamteinnahmen:§r " + stats.totalCoins() + symbol + "\n"
                + "Erfolgreiche Erhebungen: " + stats.successfulCycles() + "\n"
                + "Fehlgeschlagene Erhebungen: " + stats.failedCycles() + "\n"
                + "Erfasste Dorfbewohner: " + stats.totalVillagers() + "\n\n"
                + "§eLetzte Erhebung§r\n"
                + "Zeit: " + last + "\n"
                + "Dorfbewohner: " + stats.lastVillagers() + "\n"
                + "TaxBonus: " + stats.lastTaxBonus() + "\n"
                + "Betrag: " + stats.lastAmount() + symbol + "\n"
                + "Ergebnis: " + (stats.lastReason() == null ? "-" : stats.lastReason());
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        format.setTimeZone(TimeZone.getDefault());
        return format.format(new Date(timestamp));
    }
}

