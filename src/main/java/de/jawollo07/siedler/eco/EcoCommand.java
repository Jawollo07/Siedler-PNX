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
import org.powernukkitx.Player;
import org.powernukkitx.form.window.SimpleForm;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

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
                            context.getSender().sendMessage(messageManager.getMessage("messages.eco.help.show"));
                            context.getSender().sendMessage(messageManager.getMessage("messages.eco.help.stats"));
                            context.getSender().sendMessage(messageManager.getMessage("messages.eco.help.admin-set"));
                            context.getSender().sendMessage(messageManager.getMessage("messages.eco.help.admin-add"));
                            context.getSender().sendMessage(messageManager.getMessage("messages.eco.help.admin-remove"));
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
                                        prefix + format(messageManager.getMessage("messages.eco.balance"), "team", team.name(), "balance", String(balance), "symbol", symbol)
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
                context.getSender().sendMessage(prefix + messageManager.getMessage("messages.eco.stats-player-only"));
                return CommandResult.fail(messageManager.getMessage("messages.eco.player-required"));
            }

            try {
                Team team = teamManager.getTeamForPlayer(player.getUniqueId().toString());
                if (team == null) {
                    context.getSender().sendMessage(prefix + messageManager.getMessage("messages.eco.no-team"));
                    return CommandResult.fail(messageManager.getMessage("messages.eco.no-team-error"));
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
                            context.getSender().sendMessage(messageManager.getMessage("messages.eco.help.admin-set"));
                            context.getSender().sendMessage(messageManager.getMessage("messages.eco.help.admin-add"));
                            context.getSender().sendMessage(messageManager.getMessage("messages.eco.help.admin-remove"));
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
                                                prefix + format(messageManager.getMessage("messages.eco.balance-set"), "team", team.name(), "amount", String(balance), "symbol", symbol)
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
                                                prefix + format(messageManager.getMessage("messages.eco.balance-added"), "team", team.name(), "amount", String(amount), "symbol", symbol)
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
                                                prefix + format(messageManager.getMessage("messages.eco.balance-removed"), "team", team.name(), "amount", String(amount), "symbol", symbol)
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

        SimpleForm form = new SimpleForm(format(messageManager.getMessage("messages.eco.stats-title"), "team", team.name()),
                buildStatisticsText(team, stats, symbol))
                .addButton(messageManager.getMessage("messages.eco.stats-refresh"), ignored -> {
                    try {
                        sendTaxStatistics(player, team);
                    } catch (Exception exception) {
                        player.sendMessage(prefix + format(messageManager.getMessage("messages.eco.stats-refresh-error"), "error", exception.getMessage()));
                    }
                })
                .addButton(messageManager.getMessage("messages.eco.stats-close"));

        form.send(player);
    }

    private void sendTaxStatistics(org.powernukkitx.command.CommandSender sender, Team team) throws Exception {
        TaxManager.TaxStatistics stats = taxManager.getStatistics(team.id());
        sender.sendMessage(buildStatisticsText(team, stats, ecoManager.getCurrency("s")));
    }

    private String buildStatisticsText(Team team, TaxManager.TaxStatistics stats, String symbol) {
        String last = stats.lastTaxTimestamp() == null
                ? messageManager.getMessage("messages.eco.stats-last-none")
                : format(messageManager.getMessage("messages.eco.stats-last-time"), "time", formatTimestamp(stats.lastTaxTimestamp()));
        return messageManager.getMessage("messages.eco.stats-header") + "\n"
                + format(messageManager.getMessage("messages.eco.stats-team"), "team", team.name()) + "\n"
                + format(messageManager.getMessage("messages.eco.stats-bonus"), "bonus", String.valueOf(Math.max(1, team.taxBonus()))) + "\n\n"
                + format(messageManager.getMessage("messages.eco.stats-total"), "amount", String.valueOf(stats.totalCoins()), "symbol", symbol) + "\n"
                + format(messageManager.getMessage("messages.eco.stats-successful"), "amount", String.valueOf(stats.successfulCycles())) + "\n"
                + format(messageManager.getMessage("messages.eco.stats-failed"), "amount", String.valueOf(stats.failedCycles())) + "\n"
                + format(messageManager.getMessage("messages.eco.stats-villagers"), "amount", String.valueOf(stats.totalVillagers())) + "\n\n"
                + messageManager.getMessage("messages.eco.stats-last-header") + "\n"
                + last + "\n"
                + format(messageManager.getMessage("messages.eco.stats-last-villagers"), "amount", String.valueOf(stats.lastVillagers())) + "\n"
                + format(messageManager.getMessage("messages.eco.stats-last-bonus"), "amount", String.valueOf(stats.lastTaxBonus())) + "\n"
                + format(messageManager.getMessage("messages.eco.stats-last-amount"), "amount", String.valueOf(stats.lastAmount()), "symbol", symbol) + "\n"
                + format(messageManager.getMessage("messages.eco.stats-last-result"), "result", stats.lastReason() == null ? "-" : stats.lastReason());
    }

    private String format(String template, String... replacements) {
        String result = template;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            result = result.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return result;
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        format.setTimeZone(TimeZone.getDefault());
        return format.format(new Date(timestamp));
    }
}

