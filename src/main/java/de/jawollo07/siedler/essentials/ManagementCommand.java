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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ManagementCommand extends Command {
    private final SiedlerPlugin plugin;
    private final MessageManager messageManager;
    private final String prefix;
    private final ModerationManager moderationManager;
    private final TeamManager teamManager;
    private final DeathManager deathManager;

    public ManagementCommand(SiedlerPlugin plugin) {
        super("verwaltung", "Öffnet die Siedler-Verwaltung", "/verwaltung");
        this.plugin = plugin;
        this.messageManager = new MessageManager();
        this.prefix = messageManager.getPrefix("essentials");
        this.moderationManager = new ModerationManager(plugin);
        this.teamManager = new TeamManager(plugin);
        this.deathManager = new DeathManager(plugin);
        setPermission("siedler.admin");
        setPermissionMessage(messageManager.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().then(RouteNode.literal("help").exec(context -> {
            sendHelp(context.getSender());
            return CommandResult.success();
        }));
        tree.getRoot().then(RouteNode.literal("menu").exec(context -> open(context.getSender())));
        tree.getRoot().exec(context -> open(context.getSender()));
    }

    private CommandResult open(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.player-required"));
            return CommandResult.fail("Spieler erforderlich");
        }
        openMainMenu(player);
        return CommandResult.success();
    }

    private void openMainMenu(Player admin) {
        int online = plugin.getServer().getOnlinePlayers().size();
        new SimpleForm(
                messageManager.getMessage("messages.essentials.management-title"),
                messageManager.getMessage("messages.essentials.management-header")
                        .replace("{online}", String.valueOf(online)))
                .addButton(messageManager.getMessage("messages.essentials.management-players"), ignored -> openPlayerList(admin))
                .addButton(messageManager.getMessage("messages.essentials.management-online-actions"), ignored -> openOnlineActions(admin))
                .addButton(messageManager.getMessage("messages.essentials.management-close"))
                .send(admin);
    }

    private void openOnlineActions(Player admin) {
        Map<?, Player> players = plugin.getServer().getOnlinePlayers();
        SimpleForm form = new SimpleForm(
                messageManager.getMessage("messages.essentials.management-actions-title"),
                messageManager.getMessage("messages.essentials.management-actions-header")
                        .replace("{online}", String.valueOf(players.size())));
        for (Player target : players.values()) {
            form.addButton(target.getName(), ignored -> openPlayerInfo(admin, target));
        }
        form.addButton(messageManager.getMessage("messages.essentials.management-back"), ignored -> openMainMenu(admin));
        form.send(admin);
    }

    private void openPlayerList(Player admin) {
        openOnlineActions(admin);
    }

    private void openPlayerInfo(Player admin, Player target) {
        String playerId = target.getUniqueId().toString();
        String team = "Kein Team";
        String ban = "Kein aktiver Bann";
        int history = 0;
        int deathHistory = 0;
        try {
            Team targetTeam = teamManager.getTeamForPlayer(playerId);
            if (targetTeam != null) team = targetTeam.name();
            ModerationManager.Punishment activeBan = moderationManager.getActiveBan(playerId);
            if (activeBan != null) {
                ban = activeBan.type() + ": " + activeBan.reason();
            }
            history = moderationManager.getHistory(playerId).size();
            deathHistory = deathManager.getDeathHistory(playerId).size();
        } catch (Exception exception) {
            plugin.getLogger().warning("Verwaltungsdaten konnten nicht geladen werden: " + exception.getMessage());
        }

        String info = messageManager.getMessage("messages.essentials.management-player-info")
                .replace("{name}", target.getName())
                .replace("{uuid}", playerId)
                .replace("{world}", target.getLevel() == null ? "-" : target.getLevel().getName())
                .replace("{x}", String.valueOf((int) target.getFloorX()))
                .replace("{y}", String.valueOf((int) target.getFloorY()))
                .replace("{z}", String.valueOf((int) target.getFloorZ()))
                .replace("{team}", team)
                .replace("{ban}", ban)
                .replace("{history}", String.valueOf(history))
                .replace("{deathHistory}", String.valueOf(deathHistory));

        new SimpleForm(
                messageManager.getMessage("messages.essentials.management-player-title"),
                info)
                .addButton(messageManager.getMessage("messages.essentials.management-moderate"),
                        ignored -> openModerationMenu(admin, target))
                .addButton(messageManager.getMessage("messages.essentials.management-history"),
                        ignored -> openHistory(admin, target))
                .addButton(messageManager.getMessage("messages.essentials.management-death-history"),
                        ignored -> openDeathHistory(admin, target))
                .addButton(messageManager.getMessage("messages.essentials.management-back"),
                        ignored -> openOnlineActions(admin))
                .send(admin);
    }

    private void openDeathHistory(Player admin, Player target) {
        try {
            List<DeathManager.DeathPoint> history =
                    deathManager.getDeathHistory(target.getUniqueId().toString());

            SimpleForm form = new SimpleForm(
                    messageManager.getMessage("messages.essentials.management-death-history-title"),
                    messageManager.getMessage("messages.essentials.management-death-history-header")
                            .replace("{player}", target.getName())
                            .replace("{count}", String.valueOf(history.size())));

            if (history.isEmpty()) {
                form.addButton(messageManager.getMessage("messages.essentials.management-death-history-empty"));
            } else {
                for (DeathManager.DeathPoint point : history) {
                    form.addButton(formatDeathHistory(target.getName(), point),
                            ignored -> openDeathHistoryEntry(admin, target, point));
                }
            }

            form.addButton(messageManager.getMessage("messages.essentials.management-back"),
                    ignored -> openPlayerInfo(admin, target));
            form.send(admin);
        } catch (Exception exception) {
            admin.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-action-error")
                    .replace("{error}", exception.getMessage() == null ? "Unbekannter Fehler" : exception.getMessage()));
        }
    }

    private void openDeathHistoryEntry(Player admin, Player target, DeathManager.DeathPoint point) {
        String created = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(point.createdAt()));
        String details = "§7Spieler: §f" + target.getName()
                + "\n§7Datum: §f" + created
                + "\n§7Welt: §f" + point.world()
                + "\n§7Position: §f" + formatCoordinate(point.x())
                + " / " + formatCoordinate(point.y())
                + " / " + formatCoordinate(point.z())
                + "\n§7Rotation: §f" + formatCoordinate(point.yaw())
                + " / " + formatCoordinate(point.pitch());

        new SimpleForm(
                messageManager.getMessage("messages.essentials.management-death-history-entry-title"),
                details)
                .addButton("§eInventar anzeigen",
                        ignored -> openDeathInventory(admin, target, point))
                .addButton(messageManager.getMessage("messages.essentials.management-back"),
                        ignored -> openDeathHistory(admin, target))
                .send(admin);
    }

    private void openDeathInventory(Player admin, Player target, DeathManager.DeathPoint point) {
        String inventory = point.inventoryData();
        StringBuilder details = new StringBuilder("§7Spieler: §f")
                .append(target.getName())
                .append("\n§7Todespunkt: §f")
                .append(formatCoordinate(point.x())).append(" ")
                .append(formatCoordinate(point.y())).append(" ")
                .append(formatCoordinate(point.z()))
                .append("\n\n");

        if (inventory == null || inventory.isBlank()) {
            details.append("§7Kein Inventar-Snapshot gespeichert.");
        } else if (inventory.startsWith("inventory_error=")) {
            details.append("§cInventar konnte nicht vollständig erfasst werden:\n§7")
                    .append(unescapeInventoryValue(inventory.substring("inventory_error=".length())));
        } else {
            String[] lines = inventory.split("\\\\n");
            for (String line : lines) {
                if (line.isBlank()) continue;
                String[] parts = line.split("\\\\|", -1);
                if (parts.length < 2) {
                    details.append("§7").append(unescapeInventoryValue(line)).append("\n");
                    continue;
                }

                String section = parts[0];
                String slot = value(parts, "slot");
                String name = value(parts, "name");
                String id = value(parts, "id");
                String count = value(parts, "count");
                String damage = value(parts, "damage");

                details.append("§e").append(section)
                        .append(" §8Slot ").append(slot)
                        .append(": §f").append(name.isBlank() ? id : name)
                        .append(" §7x").append(count);
                if (!damage.isBlank() && !"0".equals(damage)) {
                    details.append(" §8(Damage ").append(damage).append(")");
                }
                details.append("\n");
            }
        }

        new SimpleForm(
                "§eInventar beim Tod",
                details.toString())
                .addButton(messageManager.getMessage("messages.essentials.management-back"),
                        ignored -> openDeathHistoryEntry(admin, target, point))
                .send(admin);
    }

    private String value(String[] parts, String key) {
        String prefix = key + "=";
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].startsWith(prefix)) {
                return unescapeInventoryValue(parts[i].substring(prefix.length()));
            }
        }
        return "";
    }

    private String unescapeInventoryValue(String value) {
        return value.replace("\\\\|", "|").replace("\\\\n", "\n").replace("\\\\\\\\", "\\");
    }

    private String formatDeathHistory(String playerName, DeathManager.DeathPoint point) {
        String date = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(point.createdAt()));
        return "§f" + playerName + " §8(" + date + ")\n"
                + "§7" + point.world() + " §8• §f"
                + formatCoordinate(point.x()) + " "
                + formatCoordinate(point.y()) + " "
                + formatCoordinate(point.z());
    }

    private String formatCoordinate(double value) {
        if (value == Math.rint(value)) return String.valueOf((long) value);
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    private void openModerationMenu(Player admin, Player target) {
        try {
            ModerationManager.Punishment activeBan = moderationManager.getActiveBan(target.getUniqueId().toString());
            SimpleForm form = new SimpleForm(
                    messageManager.getMessage("messages.essentials.management-moderation-title"),
                    messageManager.getMessage("messages.essentials.management-moderation-header")
                            .replace("{player}", target.getName())
                            .replace("{status}", activeBan == null ? "§aKein aktiver Bann" : "§c" + activeBan.type()));
            form.addButton("§e⚠ Verwarnen", ignored -> openReasonMenu(admin, target, "warn"));
            form.addButton("§6⛔ Kicken", ignored -> openReasonMenu(admin, target, "kick"));
            form.addButton("§c🔨 Permanent bannen", ignored -> openReasonMenu(admin, target, "ban"));
            form.addButton("§c⏱ 30 Minuten bannen", ignored -> openReasonMenu(admin, target, "tempban30"));
            form.addButton("§c⏱ 2 Stunden bannen", ignored -> openReasonMenu(admin, target, "tempban120"));
            form.addButton("§c⏱ 24 Stunden bannen", ignored -> openReasonMenu(admin, target, "tempban1440"));
            if (activeBan != null) {
                form.addButton("§a✓ Bann aufheben", ignored -> confirmUnban(admin, target));
            }
            form.addButton(messageManager.getMessage("messages.essentials.management-back"),
                    ignored -> openPlayerInfo(admin, target));
            form.send(admin);
        } catch (Exception exception) {
            admin.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-action-error")
                    .replace("{error}", exception.getMessage() == null ? "Unbekannter Fehler" : exception.getMessage()));
        }
    }

    private void openReasonMenu(Player admin, Player target, String action) {
        String title = messageManager.getMessage("messages.essentials.management-reason-title");
        SimpleForm form = new SimpleForm(title, "§7Spieler: §f" + target.getName() + "\n§7Wähle einen Grund:");
        form.addButton("Verstoß gegen Regeln", ignored -> confirmPunishment(admin, target, action, "Verstoß gegen die Serverregeln"));
        form.addButton("Belästigung / Beleidigung", ignored -> confirmPunishment(admin, target, action, "Belästigung / Beleidigung"));
        form.addButton("Cheating / Exploiting", ignored -> confirmPunishment(admin, target, action, "Cheating / Exploiting"));
        form.addButton("Unangemessenes Verhalten", ignored -> confirmPunishment(admin, target, action, "Unangemessenes Verhalten"));
        form.addButton("Sonstiger Regelverstoß", ignored -> confirmPunishment(admin, target, action, "Sonstiger Regelverstoß"));
        form.addButton(messageManager.getMessage("messages.essentials.management-back"),
                ignored -> openModerationMenu(admin, target));
        form.send(admin);
    }

    private void confirmPunishment(Player admin, Player target, String action, String reason) {
        String label = actionLabel(action);
        new SimpleForm(
                messageManager.getMessage("messages.essentials.management-confirm-title"),
                "§7Spieler: §f" + target.getName()
                        + "\n§7Maßnahme: §f" + label
                        + "\n§7Grund: §f" + reason
                        + "\n\n§cDiese Aktion kann den Spieler sofort vom Server trennen.")
                .addButton("§c✓ Bestätigen", ignored -> executePunishment(admin, target, action, reason))
                .addButton("§7Abbrechen", ignored -> openModerationMenu(admin, target))
                .send(admin);
    }

    private void executePunishment(Player admin, Player target, String action, String reason) {
        try {
            String moderatorId = admin.getUniqueId().toString();
            String moderatorName = admin.getName();
            String playerId = target.getUniqueId().toString();
            if ("warn".equals(action)) {
                moderationManager.warn(playerId, target.getName(), reason, moderatorId, moderatorName);
            } else if ("kick".equals(action)) {
                moderationManager.kick(playerId, target.getName(), reason, moderatorId, moderatorName);
            } else if ("ban".equals(action)) {
                moderationManager.ban(playerId, target.getName(), reason, moderatorId, moderatorName);
            } else {
                long minutes = Long.parseLong(action.substring("tempban".length()));
                moderationManager.tempBan(playerId, target.getName(), reason, moderatorId, moderatorName, minutes * 60_000L);
            }
            admin.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-action-success")
                    .replace("{type}", actionLabel(action)).replace("{player}", target.getName()));
            openPlayerInfo(admin, target);
        } catch (Exception exception) {
            admin.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-action-error")
                    .replace("{error}", exception.getMessage() == null ? "Unbekannter Fehler" : exception.getMessage()));
        }
    }

    private void confirmUnban(Player admin, Player target) {
        new SimpleForm(
                messageManager.getMessage("messages.essentials.management-confirm-title"),
                "§7Spieler: §f" + target.getName() + "\n§7Aktion: §aBann aufheben\n\n§7Möchtest du den aktiven Bann wirklich aufheben?")
                .addButton("§a✓ Bann aufheben", ignored -> executeUnban(admin, target))
                .addButton("§7Abbrechen", ignored -> openModerationMenu(admin, target))
                .send(admin);
    }

    private void executeUnban(Player admin, Player target) {
        try {
            boolean changed = moderationManager.unban(target.getUniqueId().toString(),
                    admin.getUniqueId().toString(), "Unban durch Verwaltung");
            if (!changed) {
                admin.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-no-active-ban"));
            } else {
                admin.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-action-success")
                        .replace("{type}", "Bann aufgehoben").replace("{player}", target.getName()));
            }
            openPlayerInfo(admin, target);
        } catch (Exception exception) {
            admin.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-action-error")
                    .replace("{error}", exception.getMessage() == null ? "Unbekannter Fehler" : exception.getMessage()));
        }
    }

    private void openHistory(Player admin, Player target) {
        try {
            List<ModerationManager.Punishment> history = moderationManager.getHistory(target.getUniqueId().toString());
            SimpleForm form = new SimpleForm(
                    messageManager.getMessage("messages.essentials.management-history-title"),
                    "§7Spieler: §f" + target.getName() + "\n§7Einträge: §f" + history.size());
            if (history.isEmpty()) {
                form.addButton("§7Keine Maßnahmen vorhanden");
            } else {
                for (ModerationManager.Punishment punishment : history) {
                    form.addButton(formatHistory(punishment), ignored -> openHistoryEntry(admin, target, punishment));
                }
            }
            form.addButton(messageManager.getMessage("messages.essentials.management-back"),
                    ignored -> openPlayerInfo(admin, target));
            form.send(admin);
        } catch (Exception exception) {
            admin.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-action-error")
                    .replace("{error}", exception.getMessage() == null ? "Unbekannter Fehler" : exception.getMessage()));
        }
    }

    private void openHistoryEntry(Player admin, Player target, ModerationManager.Punishment punishment) {
        String created = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(punishment.createdAt()));
        String expires = punishment.expiresAt() == null ? "dauerhaft" :
                new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(punishment.expiresAt()));
        String details = "§7Typ: §f" + punishment.type()
                + "\n§7Grund: §f" + punishment.reason()
                + "\n§7Moderator: §f" + (punishment.moderatorName() == null ? "Console" : punishment.moderatorName())
                + "\n§7Erstellt: §f" + created
                + "\n§7Ablauf: §f" + expires
                + "\n§7Status: §f" + (punishment.active() ? "Aktiv" : "Beendet");
        new SimpleForm(
                messageManager.getMessage("messages.essentials.management-history-entry-title"),
                details)
                .addButton(messageManager.getMessage("messages.essentials.management-back"),
                        ignored -> openHistory(admin, target))
                .send(admin);
    }

    private String formatHistory(ModerationManager.Punishment p) {
        String status = p.active() ? "§cAKTIV" : "§7beendet";
        String date = new SimpleDateFormat("dd.MM HH:mm").format(new Date(p.createdAt()));
        return status + " §f" + p.type() + " §8• §7" + date + "\n§8" + p.reason();
    }

    private String actionLabel(String action) {
        return switch (action) {
            case "warn" -> "Verwarnung";
            case "kick" -> "Kick";
            case "ban" -> "Permanenter Bann";
            case "tempban30" -> "Bann (30 Minuten)";
            case "tempban120" -> "Bann (2 Stunden)";
            case "tempban1440" -> "Bann (24 Stunden)";
            default -> action;
        };
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-help"));
    }
}
