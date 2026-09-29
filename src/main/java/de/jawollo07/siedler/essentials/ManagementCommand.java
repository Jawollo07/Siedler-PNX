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
import org.powernukkitx.command.node.StringNode;
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
    private final TeamEnderChestManager teamEnderChestManager;
    private final InventorySnapshotManager inventorySnapshotManager;

    public ManagementCommand(SiedlerPlugin plugin, ModerationManager moderationManager) {
        super("verwaltung", "Öffnet die Siedler-Verwaltung", "/verwaltung");
        this.plugin = plugin;
        this.messageManager = new MessageManager();
        this.prefix = messageManager.getPrefix("essentials");
        this.moderationManager = moderationManager == null ? new ModerationManager(plugin) : moderationManager;
        this.teamManager = new TeamManager(plugin);
        this.deathManager = new DeathManager(plugin);
        this.teamEnderChestManager = new TeamEnderChestManager(plugin);
        this.inventorySnapshotManager = new InventorySnapshotManager(plugin);
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

        tree.getRoot().then(RouteNode.literal("player")
                .then(RouteNode.argument("name", new StringNode()).exec(context -> {
                    showPlayerInfo(context.getSender(), context.getArg("name"));
                    return CommandResult.success();
                })));
        tree.getRoot().then(RouteNode.literal("warn")
                .then(RouteNode.argument("name", new StringNode())
                        .then(RouteNode.argument("reason", new StringNode()).exec(context -> {
                            runModeration(context.getSender(), context.getArg("name"), "warn", context.getArg("reason"));
                            return CommandResult.success();
                        }))));
        tree.getRoot().then(RouteNode.literal("kick")
                .then(RouteNode.argument("name", new StringNode())
                        .then(RouteNode.argument("reason", new StringNode()).exec(context -> {
                            runModeration(context.getSender(), context.getArg("name"), "kick", context.getArg("reason"));
                            return CommandResult.success();
                        }))));
        tree.getRoot().then(RouteNode.literal("ban")
                .then(RouteNode.argument("name", new StringNode())
                        .then(RouteNode.argument("reason", new StringNode()).exec(context -> {
                            runModeration(context.getSender(), context.getArg("name"), "ban", context.getArg("reason"));
                            return CommandResult.success();
                        }))));
        tree.getRoot().then(RouteNode.literal("tempban")
                .then(RouteNode.argument("name", new StringNode())
                        .then(RouteNode.argument("minutes", new StringNode())
                                .then(RouteNode.argument("reason", new StringNode()).exec(context -> {
                                    runModeration(context.getSender(), context.getArg("name"),
                                            "tempban" + context.getArg("minutes"), context.getArg("reason"));
                                    return CommandResult.success();
                                })))));
        tree.getRoot().then(RouteNode.literal("unban")
                .then(RouteNode.argument("name", new StringNode()).exec(context -> {
                    runUnban(context.getSender(), context.getArg("name"));
                    return CommandResult.success();
                })));
        tree.getRoot().then(RouteNode.literal("history")
                .then(RouteNode.argument("name", new StringNode()).exec(context -> {
                    showHistory(context.getSender(), context.getArg("name"));
                    return CommandResult.success();
                })));
        tree.getRoot().then(RouteNode.literal("deathhistory")
                .then(RouteNode.argument("name", new StringNode()).exec(context -> {
                    showDeathHistory(context.getSender(), context.getArg("name"));
                    return CommandResult.success();
                })));
        tree.getRoot().then(RouteNode.literal("snapshots")
                .then(RouteNode.argument("name", new StringNode()).exec(context -> {
                    showSnapshots(context.getSender(), context.getArg("name"));
                    return CommandResult.success();
                })));
        tree.getRoot().then(RouteNode.literal("ec")
                .then(RouteNode.literal("player")
                        .then(RouteNode.argument("name", new StringNode()).exec(context -> {
                            openPlayerEnderChestCommand(context.getSender(), context.getArg("name"));
                            return CommandResult.success();
                        })))
                .then(RouteNode.literal("team")
                        .then(RouteNode.argument("team", new StringNode()).exec(context -> {
                            openTeamEnderChestCommand(context.getSender(), context.getArg("team"));
                            return CommandResult.success();
                        }))));

        tree.getRoot().exec(context -> open(context.getSender()));
    }

    private Player findOnlinePlayer(String name) {
        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            if (player.getName().equalsIgnoreCase(name)) return player;
        }
        return null;
    }

    private void showPlayerInfo(CommandSender sender, String name) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.player-required"));
            return;
        }
        Player target = findOnlinePlayer(name);
        if (target == null) {
            sender.sendMessage(prefix + "§cDer Spieler muss für /verwaltung player aktuell online sein.");
            return;
        }
        openPlayerInfo(admin, target);
    }

    private void runModeration(CommandSender sender, String name, String action, String reason) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.player-required"));
            return;
        }
        try {
            String playerId = moderationManager.findPlayerIdByName(name);
            Player target = findOnlinePlayer(name);
            if (playerId == null) {
                if (target == null) {
                    sender.sendMessage(prefix + "§cSpieler wurde noch nicht in der Siedler-Datenbank gefunden.");
                    return;
                }
                playerId = target.getUniqueId().toString();
            }
            String playerName = target == null ? name : target.getName();
            String moderatorId = admin.getUniqueId().toString();
            String moderatorName = admin.getName();

            if ("warn".equals(action)) {
                moderationManager.warn(playerId, playerName, reason, moderatorId, moderatorName);
            } else if ("kick".equals(action)) {
                moderationManager.kick(playerId, playerName, reason, moderatorId, moderatorName);
            } else if ("ban".equals(action)) {
                moderationManager.ban(playerId, playerName, reason, moderatorId, moderatorName);
            } else if (action.startsWith("tempban")) {
                long minutes = Long.parseLong(action.substring("tempban".length()));
                if (minutes <= 0) throw new IllegalArgumentException("Die Dauer muss größer als 0 sein.");
                moderationManager.tempBan(playerId, playerName, reason, moderatorId, moderatorName, minutes * 60_000L);
            }
            sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-action-success")
                    .replace("{type}", actionLabel(action)).replace("{player}", playerName));
        } catch (Exception exception) {
            sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-action-error")
                    .replace("{error}", exception.getMessage() == null ? messageManager.getMessage("messages.essentials.error-unknown") : exception.getMessage()));
        }
    }

    private void runUnban(CommandSender sender, String name) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.player-required"));
            return;
        }
        try {
            String playerId = moderationManager.findPlayerIdByName(name);
            if (playerId == null) {
                sender.sendMessage(prefix + "§cSpieler wurde nicht gefunden.");
                return;
            }
            boolean changed = moderationManager.unban(playerId, admin.getUniqueId().toString(), "Unban per Verwaltung-Command");
            sender.sendMessage(prefix + (changed
                    ? "§aSpieler §e" + name + " §awurde entbannt."
                    : "§e" + name + " §7hat keine aktive Sperre."));
        } catch (Exception exception) {
            sender.sendMessage(prefix + "§cUnban fehlgeschlagen: " + exception.getMessage());
        }
    }

    private void showHistory(CommandSender sender, String name) {
        try {
            String id = moderationManager.findPlayerIdByName(name);
            if (id == null) { sender.sendMessage(prefix + "§cSpieler nicht gefunden."); return; }
            List<ModerationManager.Punishment> history = moderationManager.getHistory(id);
            sender.sendMessage(prefix + "§6Moderationsverlauf §7(" + name + "):");
            if (history.isEmpty()) { sender.sendMessage("§7Keine Einträge."); return; }
            for (ModerationManager.Punishment p : history) {
                String date = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(p.createdAt()));
                sender.sendMessage("§8- §e" + p.type() + " §7" + date + " §f" + p.reason()
                        + " §8[" + (p.active() ? "aktiv" : "inaktiv") + "]");
            }
        } catch (Exception exception) {
            sender.sendMessage(prefix + "§cVerlauf konnte nicht geladen werden: " + exception.getMessage());
        }
    }

    private void showDeathHistory(CommandSender sender, String name) {
        try {
            String id = moderationManager.findPlayerIdByName(name);
            if (id == null) { sender.sendMessage(prefix + "§cSpieler nicht gefunden."); return; }
            List<DeathManager.DeathPoint> history = deathManager.getDeathHistory(id);
            sender.sendMessage(prefix + "§6Todeshistorie §7(" + name + "):");
            if (history.isEmpty()) { sender.sendMessage("§7Keine Einträge."); return; }
            for (DeathManager.DeathPoint p : history) {
                String date = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(p.createdAt()));
                sender.sendMessage("§8- §e" + date + " §7" + p.world() + " §f"
                        + formatCoordinate(p.x()) + ", " + formatCoordinate(p.y()) + ", " + formatCoordinate(p.z()));
            }
        } catch (Exception exception) {
            sender.sendMessage(prefix + "§cTodeshistorie konnte nicht geladen werden: " + exception.getMessage());
        }
    }

    private void showSnapshots(CommandSender sender, String name) {
        try {
            String id = moderationManager.findPlayerIdByName(name);
            if (id == null) { sender.sendMessage(prefix + "§cSpieler nicht gefunden."); return; }
            List<InventorySnapshotManager.InventorySnapshot> snapshots = inventorySnapshotManager.getHistory(id, 50);
            sender.sendMessage(prefix + "§6Inventar-Snapshots §7(" + name + "):");
            if (snapshots.isEmpty()) { sender.sendMessage("§7Keine Snapshots."); return; }
            for (InventorySnapshotManager.InventorySnapshot s : snapshots) {
                String date = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(s.capturedAt()));
                sender.sendMessage("§8- §e" + date + " §7" + s.reason() + " §8@ " + s.world()
                        + " " + formatCoordinate(s.x()) + "," + formatCoordinate(s.y()) + "," + formatCoordinate(s.z()));
            }
        } catch (Exception exception) {
            sender.sendMessage(prefix + "§cSnapshots konnten nicht geladen werden: " + exception.getMessage());
        }
    }

    private void showSnapshotDetail(CommandSender sender, String name, String indexText) {
        try {
            int index = Integer.parseInt(indexText) - 1;
            String id = moderationManager.findPlayerIdByName(name);
            if (id == null) { sender.sendMessage(prefix + "§cSpieler nicht gefunden."); return; }
            List<InventorySnapshotManager.InventorySnapshot> snapshots = inventorySnapshotManager.getHistory(id, 50);
            if (index < 0 || index >= snapshots.size()) { sender.sendMessage(prefix + "§cSnapshot-Index ungültig."); return; }
            InventorySnapshotManager.InventorySnapshot s = snapshots.get(index);
            sender.sendMessage(prefix + "§6Snapshot §e#" + (index + 1) + " §7(" + s.reason() + ")");
            sender.sendMessage("§7Zeit: §f" + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(s.capturedAt())));
            sender.sendMessage("§7Position: §f" + s.world() + " " + formatCoordinate(s.x()) + "," + formatCoordinate(s.y()) + "," + formatCoordinate(s.z()));
            sendStoredInventory(sender, s.inventoryData());
        } catch (NumberFormatException exception) {
            sender.sendMessage(prefix + "§cDer Index muss eine Zahl sein.");
        } catch (Exception exception) {
            sender.sendMessage(prefix + "§cSnapshot konnte nicht geladen werden: " + exception.getMessage());
        }
    }

    private void showDeathDetail(CommandSender sender, String name, String indexText) {
        try {
            int index = Integer.parseInt(indexText) - 1;
            String id = moderationManager.findPlayerIdByName(name);
            if (id == null) { sender.sendMessage(prefix + "§cSpieler nicht gefunden."); return; }
            List<DeathManager.DeathPoint> history = deathManager.getDeathHistory(id);
            if (index < 0 || index >= history.size()) { sender.sendMessage(prefix + "§cTodeshistorie-Index ungültig."); return; }
            DeathManager.DeathPoint p = history.get(index);
            sender.sendMessage(prefix + "§6Tod §e#" + (index + 1));
            sender.sendMessage("§7Zeit: §f" + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(p.createdAt())));
            sender.sendMessage("§7Position: §f" + p.world() + " " + formatCoordinate(p.x()) + "," + formatCoordinate(p.y()) + "," + formatCoordinate(p.z()));
            sender.sendMessage("§7Rotation: §f" + formatCoordinate(p.yaw()) + " / " + formatCoordinate(p.pitch()));
            sendStoredInventory(sender, p.inventoryData());
        } catch (NumberFormatException exception) {
            sender.sendMessage(prefix + "§cDer Index muss eine Zahl sein.");
        } catch (Exception exception) {
            sender.sendMessage(prefix + "§cTodesdaten konnten nicht geladen werden: " + exception.getMessage());
        }
    }

    private void sendStoredInventory(CommandSender sender, String inventory) {
        if (inventory == null || inventory.isBlank()) {
            sender.sendMessage("§7Kein gespeichertes Inventar.");
            return;
        }
        for (String line : inventory.split("\\\\n")) {
            if (!line.isBlank()) sender.sendMessage("§8- §f" + unescapeInventoryValue(line));
        }
    }

    private void openPlayerEnderChestCommand(CommandSender sender, String name) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.player-required"));
            return;
        }
        Player target = findOnlinePlayer(name);
        if (target == null) {
            sender.sendMessage(prefix + "§cDer Zielspieler muss online sein.");
            return;
        }
        int windowId = admin.addWindow(target.getEnderChestInventory());
        if (windowId == -1) {
            sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-enderchest-open-error"));
        }
    }

    private void openTeamEnderChestCommand(CommandSender sender, String teamName) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.player-required"));
            return;
        }
        try {
            Team team = teamManager.getTeamByName(teamName);
            if (team == null) {
                sender.sendMessage(prefix + "§cTeam nicht gefunden: " + teamName);
                return;
            }
            TeamEnderChestInventory inventory = teamEnderChestManager.getOrCreate(admin, team.id());
            int windowId = admin.addWindow(inventory);
            if (windowId == -1) {
                sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-enderchest-open-error"));
            }
        } catch (Exception exception) {
            sender.sendMessage(prefix + "§cTeam-Enderchest konnte nicht geöffnet werden: " + exception.getMessage());
        }
    }

    private CommandResult open(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.player-required"));
            return CommandResult.fail(messageManager.getMessage("messages.essentials.player-required"));
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
                .addButton(messageManager.getMessage("messages.essentials.management-enderchests"), ignored -> openEnderChestManagement(admin))
                .addButton(messageManager.getMessage("messages.essentials.management-close"))
                .send(admin);
    }

    private void openEnderChestManagement(Player admin) {
        new SimpleForm(
                messageManager.getMessage("messages.essentials.management-enderchests-title"),
                messageManager.getMessage("messages.essentials.management-enderchests-header"))
                .addButton(messageManager.getMessage("messages.essentials.management-enderchest-player"),
                        ignored -> openPlayerEnderChestList(admin))
                .addButton(messageManager.getMessage("messages.essentials.management-enderchest-team"),
                        ignored -> openTeamEnderChestList(admin))
                .addButton(messageManager.getMessage("messages.essentials.management-back"),
                        ignored -> openMainMenu(admin))
                .send(admin);
    }

    private void openPlayerEnderChestList(Player admin) {
        Map<?, Player> players = plugin.getServer().getOnlinePlayers();
        SimpleForm form = new SimpleForm(
                messageManager.getMessage("messages.essentials.management-enderchest-player-title"),
                messageManager.getMessage("messages.essentials.management-enderchest-player-header"));
        for (Player target : players.values()) {
            form.addButton(target.getName(), ignored -> {
                int windowId = admin.addWindow(target.getEnderChestInventory());
                if (windowId == -1) {
                    admin.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-enderchest-open-error"));
                }
            });
        }
        form.addButton(messageManager.getMessage("messages.essentials.management-back"),
                ignored -> openEnderChestManagement(admin));
        form.send(admin);
    }

    private void openTeamEnderChestList(Player admin) {
        try {
            List<Team> teams = teamManager.getTeams();
            SimpleForm form = new SimpleForm(
                    messageManager.getMessage("messages.essentials.management-enderchest-team-title"),
                    messageManager.getMessage("messages.essentials.management-enderchest-team-header"));
            for (Team team : teams) {
                form.addButton(team.name(), ignored -> openTeamEnderChest(admin, team));
            }
            form.addButton(messageManager.getMessage("messages.essentials.management-back"),
                    ignored -> openEnderChestManagement(admin));
            form.send(admin);
        } catch (Exception exception) {
            admin.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-enderchest-open-error")
                    .replace("{error}", exception.getMessage() == null ? "Unbekannter Fehler" : exception.getMessage()));
        }
    }

    private void openTeamEnderChest(Player admin, Team team) {
        try {
            TeamEnderChestInventory inventory = teamEnderChestManager.getOrCreate(admin, team.id());
            int windowId = admin.addWindow(inventory);
            if (windowId == -1) {
                admin.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-enderchest-open-error"));
            }
        } catch (Exception exception) {
            admin.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-enderchest-open-error")
                    .replace("{error}", exception.getMessage() == null ? "Unbekannter Fehler" : exception.getMessage()));
        }
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
        String team = messageManager.getMessage("messages.essentials.management-no-team");
        String ban = messageManager.getMessage("messages.essentials.management-status-no-ban");
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
            plugin.getLogger().warning(messageManager.getMessage("messages.essentials.management-refresh-error").replace("{error}", exception.getMessage() == null ? messageManager.getMessage("messages.essentials.error-unknown") : exception.getMessage()));
        }

        String info = messageManager.getMessage("messages.essentials.management-player-info")
                .replace("{name}", target.getName())
                .replace("{uuid}", playerId)
                .replace("{world}", target.getLevel() == null ? messageManager.getMessage("messages.essentials.error-unknown") : target.getLevel().getName())
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
                .addButton(messageManager.getMessage("messages.essentials.management-inventory-snapshots"),
                        ignored -> openInventorySnapshots(admin, target))
                .addButton(messageManager.getMessage("messages.essentials.management-back"),
                        ignored -> openOnlineActions(admin))
                .send(admin);
    }

    private void openInventorySnapshots(Player admin, Player target) {
        try {
            List<InventorySnapshotManager.InventorySnapshot> snapshots =
                    inventorySnapshotManager.getHistory(target.getUniqueId().toString(), 50);

            SimpleForm form = new SimpleForm(
                    messageManager.getMessage("messages.essentials.management-inventory-snapshots-title"),
                    messageManager.getMessage("messages.essentials.management-inventory-snapshots-header")
                            .replace("{player}", target.getName())
                            .replace("{count}", String.valueOf(snapshots.size())));

            if (snapshots.isEmpty()) {
                form.addButton(messageManager.getMessage("messages.essentials.management-inventory-snapshots-empty"));
            } else {
                for (InventorySnapshotManager.InventorySnapshot snapshot : snapshots) {
                    form.addButton(formatSnapshot(snapshot),
                            ignored -> openInventorySnapshot(admin, target, snapshot));
                }
            }

            form.addButton(messageManager.getMessage("messages.essentials.management-back"),
                    ignored -> openPlayerInfo(admin, target));
            form.send(admin);
        } catch (Exception exception) {
            admin.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-action-error")
                    .replace("{error}", exception.getMessage() == null
                            ? messageManager.getMessage("messages.essentials.error-unknown")
                            : exception.getMessage()));
        }
    }

    private void openInventorySnapshot(Player admin, Player target,
                                       InventorySnapshotManager.InventorySnapshot snapshot) {
        String date = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
                .format(new Date(snapshot.capturedAt()));
        String data = snapshot.inventoryData();
        int itemLines = 0;
        if (data != null && !data.isBlank()) {
            for (String line : data.split("\\n")) {
                if (!line.isBlank() && !line.startsWith("inventory_error=")
                        && !line.contains("|error=")) {
                    itemLines++;
                }
            }
        }

        String details = messageManager.getMessage("messages.essentials.management-inventory-snapshot-detail")
                .replace("{player}", target.getName())
                .replace("{reason}", snapshot.reason())
                .replace("{date}", date)
                .replace("{world}", snapshot.world() == null ? "-" : snapshot.world())
                .replace("{x}", formatCoordinate(snapshot.x() == null ? 0 : snapshot.x()))
                .replace("{y}", formatCoordinate(snapshot.y() == null ? 0 : snapshot.y()))
                .replace("{z}", formatCoordinate(snapshot.z() == null ? 0 : snapshot.z()))
                .replace("{items}", String.valueOf(itemLines));

        new SimpleForm(
                messageManager.getMessage("messages.essentials.management-inventory-snapshot-detail-title"),
                details)
                .addButton(messageManager.getMessage("messages.essentials.management-back"),
                        ignored -> openInventorySnapshots(admin, target))
                .send(admin);
    }

    private String formatSnapshot(InventorySnapshotManager.InventorySnapshot snapshot) {
        String date = new SimpleDateFormat("dd.MM HH:mm:ss").format(new Date(snapshot.capturedAt()));
        return "§e" + snapshot.reason() + " §8• §7" + date;
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
                    .replace("{error}", exception.getMessage() == null ? messageManager.getMessage("messages.essentials.error-unknown") : exception.getMessage()));
        }
    }

    private void openDeathHistoryEntry(Player admin, Player target, DeathManager.DeathPoint point) {
        String created = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(point.createdAt()));
        String details = messageManager.getMessage("messages.essentials.management-death-history-entry")
                .replace("{player}", target.getName())
                .replace("{date}", created)
                .replace("{world}", point.world())
                .replace("{x}", formatCoordinate(point.x()))
                .replace("{y}", formatCoordinate(point.y()))
                .replace("{z}", formatCoordinate(point.z()))
                .replace("{yaw}", formatCoordinate(point.yaw()))
                .replace("{pitch}", formatCoordinate(point.pitch()));

        new SimpleForm(
                messageManager.getMessage("messages.essentials.management-death-history-entry-title"),
                details)
                .addButton(messageManager.getMessage("messages.essentials.management-death-inventory-button"),
                        ignored -> openDeathInventory(admin, target, point))
                .addButton(messageManager.getMessage("messages.essentials.management-back"),
                        ignored -> openDeathHistory(admin, target))
                .send(admin);
    }

    private void openDeathInventory(Player admin, Player target, DeathManager.DeathPoint point) {
        String inventory = point.inventoryData();
        StringBuilder details = new StringBuilder(messageManager.getMessage("messages.essentials.management-death-inventory-header")
                .replace("{player}", target.getName())
                .replace("{x}", formatCoordinate(point.x()))
                .replace("{y}", formatCoordinate(point.y()))
                .replace("{z}", formatCoordinate(point.z())));

        if (inventory == null || inventory.isBlank()) {
            details.append(messageManager.getMessage("messages.essentials.management-death-inventory-empty"));
        } else if (inventory.startsWith("inventory_error=")) {
            details.append(messageManager.getMessage("messages.essentials.management-death-inventory-error"))
                    .append(unescapeInventoryValue(inventory.substring("inventory_error=".length())));
        } else {
            String[] lines = inventory.split("\\\\n");
            for (String line : lines) {
                if (line.isBlank()) continue;
                String[] parts = line.split("\\\\|", -1);
                if (parts.length < 2) {
                    details.append(messageManager.getMessage("messages.essentials.management-death-inventory-raw").replace("{line}", unescapeInventoryValue(line))).append("\n");
                    continue;
                }

                String section = parts[0];
                String slot = value(parts, "slot");
                String name = value(parts, "name");
                String id = value(parts, "id");
                String count = value(parts, "count");
                String damage = value(parts, "damage");

                details.append(messageManager.getMessage("messages.essentials.management-death-inventory-item")
                        .replace("{section}", section)
                        .replace("{slot}", slot)
                        .replace("{name}", name.isBlank() ? id : name)
                        .replace("{count}", count));
                if (!damage.isBlank() && !"0".equals(damage)) {
                    details.append(messageManager.getMessage("messages.essentials.management-death-inventory-damage").replace("{damage}", damage));
                }
                details.append("\n");
            }
        }

        new SimpleForm(
                messageManager.getMessage("messages.essentials.management-death-inventory-title"),
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
        return messageManager.getMessage("messages.essentials.management-death-history-entry-button")
                .replace("{player}", playerName)
                .replace("{date}", date)
                .replace("{world}", point.world())
                .replace("{x}", formatCoordinate(point.x()))
                .replace("{y}", formatCoordinate(point.y()))
                .replace("{z}", formatCoordinate(point.z()));
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
                            .replace("{status}", activeBan == null ? messageManager.getMessage("messages.essentials.management-status-no-ban") : messageManager.getMessage("messages.essentials.management-status-ban").replace("{type}", activeBan.type())));
            form.addButton(messageManager.getMessage("messages.essentials.management-action-warn"), ignored -> openReasonMenu(admin, target, "warn"));
            form.addButton(messageManager.getMessage("messages.essentials.management-action-kick"), ignored -> openReasonMenu(admin, target, "kick"));
            form.addButton(messageManager.getMessage("messages.essentials.management-action-ban"), ignored -> openReasonMenu(admin, target, "ban"));
            form.addButton(messageManager.getMessage("messages.essentials.management-action-tempban-30"), ignored -> openReasonMenu(admin, target, "tempban30"));
            form.addButton(messageManager.getMessage("messages.essentials.management-action-tempban-120"), ignored -> openReasonMenu(admin, target, "tempban120"));
            form.addButton(messageManager.getMessage("messages.essentials.management-action-tempban-1440"), ignored -> openReasonMenu(admin, target, "tempban1440"));
            if (activeBan != null) {
                form.addButton(messageManager.getMessage("messages.essentials.management-action-unban"), ignored -> confirmUnban(admin, target));
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
        SimpleForm form = new SimpleForm(title, messageManager.getMessage("messages.essentials.management-reason-header").replace("{player}", target.getName()));
        form.addButton(messageManager.getMessage("messages.essentials.management-reason-rules"), ignored -> confirmPunishment(admin, target, action, messageManager.getMessage("messages.essentials.management-reason-rules")));
        form.addButton(messageManager.getMessage("messages.essentials.management-reason-harassment"), ignored -> confirmPunishment(admin, target, action, messageManager.getMessage("messages.essentials.management-reason-harassment")));
        form.addButton(messageManager.getMessage("messages.essentials.management-reason-cheating"), ignored -> confirmPunishment(admin, target, action, messageManager.getMessage("messages.essentials.management-reason-cheating")));
        form.addButton(messageManager.getMessage("messages.essentials.management-reason-behavior"), ignored -> confirmPunishment(admin, target, action, messageManager.getMessage("messages.essentials.management-reason-behavior")));
        form.addButton(messageManager.getMessage("messages.essentials.management-reason-other"), ignored -> confirmPunishment(admin, target, action, messageManager.getMessage("messages.essentials.management-reason-other")));
        form.addButton(messageManager.getMessage("messages.essentials.management-back"),
                ignored -> openModerationMenu(admin, target));
        form.send(admin);
    }

    private void confirmPunishment(Player admin, Player target, String action, String reason) {
        String label = actionLabel(action);
        new SimpleForm(
                messageManager.getMessage("messages.essentials.management-confirm-title"),
                messageManager.getMessage("messages.essentials.management-confirm-header")
                        .replace("{player}", target.getName())
                        .replace("{type}", label)
                        .replace("{reason}", reason))
                .addButton(messageManager.getMessage("messages.essentials.management-confirm-action"), ignored -> executePunishment(admin, target, action, reason))
                .addButton(messageManager.getMessage("messages.essentials.management-confirm-cancel"), ignored -> openModerationMenu(admin, target))
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
                messageManager.getMessage("messages.essentials.management-unban-confirm-title"),
                messageManager.getMessage("messages.essentials.management-unban-confirm-header").replace("{player}", target.getName()))
                .addButton(messageManager.getMessage("messages.essentials.management-confirm-unban"), ignored -> executeUnban(admin, target))
                .addButton(messageManager.getMessage("messages.essentials.management-confirm-cancel"), ignored -> openModerationMenu(admin, target))
                .send(admin);
    }

    private void executeUnban(Player admin, Player target) {
        try {
            boolean changed = moderationManager.unban(target.getUniqueId().toString(),
                    admin.getUniqueId().toString(), messageManager.getMessage("messages.essentials.management-unban-reason"));
            if (!changed) {
                admin.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-no-active-ban"));
            } else {
                admin.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-action-success")
                        .replace("{type}", messageManager.getMessage("messages.essentials.management-action-unban-label")).replace("{player}", target.getName()));
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
                    messageManager.getMessage("messages.essentials.management-history-header")
                            .replace("{player}", target.getName())
                            .replace("{count}", String.valueOf(history.size())));
            if (history.isEmpty()) {
                form.addButton(messageManager.getMessage("messages.essentials.management-no-history"));
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
        String expires = punishment.expiresAt() == null ? messageManager.getMessage("messages.essentials.management-history-permanent") :
                new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(punishment.expiresAt()));
        String details = messageManager.getMessage("messages.essentials.management-history-entry")
                .replace("{type}", punishment.type())
                .replace("{reason}", punishment.reason())
                .replace("{moderator}", punishment.moderatorName() == null ? messageManager.getMessage("messages.essentials.management-history-console") : punishment.moderatorName())
                .replace("{created}", created)
                .replace("{expires}", expires)
                .replace("{status}", punishment.active() ? messageManager.getMessage("messages.essentials.management-history-active") : messageManager.getMessage("messages.essentials.management-history-inactive"));
        new SimpleForm(
                messageManager.getMessage("messages.essentials.management-history-entry-title"),
                details)
                .addButton(messageManager.getMessage("messages.essentials.management-back"),
                        ignored -> openHistory(admin, target))
                .send(admin);
    }

    private String formatHistory(ModerationManager.Punishment p) {
        String status = p.active() ? messageManager.getMessage("messages.essentials.management-history-active") : messageManager.getMessage("messages.essentials.management-history-inactive");
        String date = new SimpleDateFormat("dd.MM HH:mm").format(new Date(p.createdAt()));
        return status + " §f" + p.type() + " §8• §7" + date + "\n§8" + p.reason();
    }

    private String actionLabel(String action) {
        return switch (action) {
            case "warn" -> messageManager.getMessage("messages.essentials.management-label-warn");
            case "kick" -> messageManager.getMessage("messages.essentials.management-label-kick");
            case "ban" -> messageManager.getMessage("messages.essentials.management-label-ban");
            case "tempban30" -> messageManager.getMessage("messages.essentials.management-label-tempban-30");
            case "tempban120" -> messageManager.getMessage("messages.essentials.management-label-tempban-120");
            case "tempban1440" -> messageManager.getMessage("messages.essentials.management-label-tempban-1440");
            default -> action;
        };
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(prefix + messageManager.getMessage("messages.essentials.management-help"));
    }
}
        tree.getRoot().then(RouteNode.literal("snapshot")
                .then(RouteNode.argument("name", new StringNode())
                        .then(RouteNode.argument("index", new StringNode()).exec(context -> {
                            showSnapshotDetail(context.getSender(), context.getArg("name"), context.getArg("index"));
                            return CommandResult.success();
                        }))));
        tree.getRoot().then(RouteNode.literal("death")
                .then(RouteNode.argument("name", new StringNode())
                        .then(RouteNode.argument("index", new StringNode()).exec(context -> {
                            showDeathDetail(context.getSender(), context.getArg("name"), context.getArg("index"));
                            return CommandResult.success();
                        }))));
