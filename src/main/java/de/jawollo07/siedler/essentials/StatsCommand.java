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
import org.powernukkitx.command.route.node.RouteNode;
import org.powernukkitx.form.window.SimpleForm;

import java.util.List;
import java.util.Locale;

public final class StatsCommand extends Command {
    private static final int PAGE_SIZE = 20;

    private final SiedlerPlugin plugin;
    private final MessageManager messageManager;
    private final String prefix;
    private final StatsManager statsManager;
    private final TeamManager teamManager;

    public StatsCommand(SiedlerPlugin plugin, StatsManager statsManager) {
        super("stats", "Zeigt Spielerstatistiken an", "/stats [admin]");
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.messageManager = new MessageManager();
        this.prefix = messageManager.getPrefix("essentials");
        this.teamManager = new TeamManager(plugin);
        setPermission("siedler.command.stats");
        setPermissionMessage(messageManager.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().exec(context -> {
            openOwnStats(context.getSender());
            return CommandResult.success();
        });

        tree.getRoot().then(RouteNode.literal("admin")
                .permission("siedler.admin", messageManager.getCommandMessage("no-permission"))
                .exec(context -> {
                    openAdminOverview(context.getSender());
                    return CommandResult.success();
                }));
    }

    private void openOwnStats(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.stats-player-required"));
            return;
        }

        try {
            StatsManager.PlayerStats stats =
                    statsManager.getStats(player.getUniqueId().toString());
            String team = "Kein Team";
            try {
                Team targetTeam = teamManager.getTeamForPlayer(player.getUniqueId().toString());
                if (targetTeam != null) {
                    team = targetTeam.name();
                }
            } catch (Exception ignored) {
                // Team information is supplementary to the statistics.
            }

            new SimpleForm(
                    messageManager.getMessage("messages.essentials.stats-title"),
                    messageManager.getMessage("messages.essentials.stats-header")
                            .replace("{player}", player.getName())
                            .replace("{team}", team)
                            .replace("{kills}", String.valueOf(stats.kills()))
                            .replace("{deaths}", String.valueOf(stats.deaths()))
                            .replace("{soldierKills}", String.valueOf(stats.soldierKills()))
                            .replace("{monsterKills}", String.valueOf(stats.monsterKills()))
                            .replace("{playtime}", formatPlaytime(stats.playtimeSeconds()))
                            .replace("{ratio}", formatRatio(stats.killDeathRatio())))
                    .addButton(messageManager.getMessage("messages.essentials.stats-close"))
                    .send(player);
        } catch (Exception e) {
            sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.stats-error")
                    .replace("{error}", error(e)));
        }
    }

    private void openAdminOverview(CommandSender sender) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.stats-player-required"));
            return;
        }

        try {
            StatsManager.GlobalStats stats = statsManager.getGlobalStats();
            new SimpleForm(
                    messageManager.getMessage("messages.essentials.stats-admin-title"),
                    messageManager.getMessage("messages.essentials.stats-admin-header")
                            .replace("{players}", String.valueOf(stats.players()))
                            .replace("{kills}", String.valueOf(stats.kills()))
                            .replace("{deaths}", String.valueOf(stats.deaths()))
                            .replace("{soldierKills}", String.valueOf(stats.soldierKills()))
                            .replace("{monsterKills}", String.valueOf(stats.monsterKills()))
                            .replace("{playtime}", formatPlaytime(stats.playtimeSeconds())))
                    .addButton(messageManager.getMessage("messages.essentials.stats-admin-players"),
                            ignored -> openPlayerList(admin, 0))
                    .addButton(messageManager.getMessage("messages.essentials.stats-close"))
                    .send(admin);
        } catch (Exception e) {
            sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.stats-error")
                    .replace("{error}", error(e)));
        }
    }

    private void openPlayerList(Player admin, int page) {
        try {
            List<StatsManager.PlayerStats> all = statsManager.getAllStats();
            int pages = Math.max(1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            int safePage = Math.max(0, Math.min(page, pages - 1));
            int from = safePage * PAGE_SIZE;
            int to = Math.min(from + PAGE_SIZE, all.size());

            SimpleForm form = new SimpleForm(
                    messageManager.getMessage("messages.essentials.stats-admin-player-list-title"),
                    messageManager.getMessage("messages.essentials.stats-admin-player-list-header")
                            .replace("{page}", String.valueOf(safePage + 1))
                            .replace("{pages}", String.valueOf(pages))
                            .replace("{count}", String.valueOf(all.size())));

            for (int i = from; i < to; i++) {
                StatsManager.PlayerStats stats = all.get(i);
                form.addButton(formatPlayerButton(stats),
                        ignored -> openPlayerDetails(admin, stats.playerId()));
            }

            if (safePage > 0) {
                form.addButton(messageManager.getMessage("messages.essentials.stats-admin-previous"),
                        ignored -> openPlayerList(admin, safePage - 1));
            }
            if (safePage + 1 < pages) {
                form.addButton(messageManager.getMessage("messages.essentials.stats-admin-next"),
                        ignored -> openPlayerList(admin, safePage + 1));
            }
            form.addButton(messageManager.getMessage("messages.essentials.management-back"),
                    ignored -> openAdminOverview(admin));
            form.send(admin);
        } catch (Exception e) {
            admin.sendMessage(prefix + messageManager.getMessage("messages.essentials.stats-error")
                    .replace("{error}", error(e)));
        }
    }

    private void openPlayerDetails(Player admin, String playerId) {
        try {
            StatsManager.PlayerStats stats = statsManager.getStats(playerId);
            String team = "Kein Team";
            try {
                Team targetTeam = teamManager.getTeamForPlayer(playerId);
                if (targetTeam != null) {
                    team = targetTeam.name();
                }
            } catch (Exception ignored) {
                // Team information is supplementary.
            }

            String header = messageManager.getMessage("messages.essentials.stats-admin-detail")
                    .replace("{player}", stats.playerName())
                    .replace("{team}", team)
                    .replace("{kills}", String.valueOf(stats.kills()))
                    .replace("{deaths}", String.valueOf(stats.deaths()))
                    .replace("{soldierKills}", String.valueOf(stats.soldierKills()))
                    .replace("{monsterKills}", String.valueOf(stats.monsterKills()))
                    .replace("{playtime}", formatPlaytime(stats.playtimeSeconds()))
                    .replace("{ratio}", formatRatio(stats.killDeathRatio()));

            new SimpleForm(
                    messageManager.getMessage("messages.essentials.stats-admin-detail-title"),
                    header)
                    .addButton(messageManager.getMessage("messages.essentials.management-back"),
                            ignored -> openPlayerList(admin, 0))
                    .send(admin);
        } catch (Exception e) {
            admin.sendMessage(prefix + messageManager.getMessage("messages.essentials.stats-error")
                    .replace("{error}", error(e)));
        }
    }

    private String formatPlayerButton(StatsManager.PlayerStats stats) {
        return "§e" + stats.playerName()
                + "\n§7Kills: §f" + stats.kills()
                + " §8• §7Tode: §f" + stats.deaths()
                + " §8• §7Spielzeit: §f" + formatPlaytime(stats.playtimeSeconds());
    }

    private String formatPlaytime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        if (days > 0) {
            return String.format(Locale.ROOT, "%dd %02dh %02dm", days, hours, minutes);
        }
        return String.format(Locale.ROOT, "%02dh %02dm", hours, minutes);
    }

    private String formatRatio(double ratio) {
        return String.format(Locale.ROOT, "%.2f", ratio);
    }

    private String error(Exception e) {
        return e.getMessage() == null ? "Unbekannter Fehler" : e.getMessage();
    }
}
