package de.jawollo07.siedler.tournament;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;
import org.powernukkitx.command.route.node.RouteNode;
import org.powernukkitx.command.tree.node.MessageStringNode;

import java.util.List;

public final class TournamentCommand extends Command {
    private final TournamentManager manager;
    private final MessageManager messages = new MessageManager();

    public TournamentCommand(SiedlerPlugin plugin, TournamentManager manager) {
        super("tournament", "PvP Tournament", "/tournament help");
        this.manager = manager;
        setPermission("siedler.command.tournament");
        setPermissionMessage(messages.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().then(RouteNode.literal("help").exec(ctx -> { help(ctx.getSender()); return CommandResult.success(); }));
        tree.getRoot().then(RouteNode.literal("join").exec(ctx -> action(ctx.getSender(), 0)));
        tree.getRoot().then(RouteNode.literal("leave").exec(ctx -> action(ctx.getSender(), 1)));
        tree.getRoot().then(RouteNode.literal("status").exec(ctx -> action(ctx.getSender(), 2)));
        tree.getRoot().then(RouteNode.literal("bracket").exec(ctx -> action(ctx.getSender(), 3)));
        tree.getRoot().then(RouteNode.literal("spectate").exec(ctx -> action(ctx.getSender(), 4)));
        tree.getRoot().then(RouteNode.literal("kits").exec(ctx -> action(ctx.getSender(), 5)));
        tree.getRoot().then(RouteNode.literal("kit")
                .then(RouteNode.argument("kit", new MessageStringNode())
                        .exec(ctx -> {
                            if (!(ctx.getSender() instanceof Player p)) {
                                ctx.getSender().sendMessage(messages.getMessage("messages.tournament.player-only"));
                                return CommandResult.fail();
                            }
                            manager.selectKit(p, ctx.getArg("kit"));
                            return CommandResult.success();
                        })));

        RouteNode admin = RouteNode.literal("admin").permission("siedler.admin", messages.getCommandMessage("no-permission"));
        admin.then(RouteNode.literal("help").exec(ctx -> { adminHelp(ctx.getSender()); return CommandResult.success(); }));
        admin.then(RouteNode.literal("open").exec(ctx -> { reply(ctx.getSender(), manager.openRegistration(), "admin-opened", "admin-open-failed"); return CommandResult.success(); }));
        admin.then(RouteNode.literal("start").exec(ctx -> { reply(ctx.getSender(), manager.forceStart(), "admin-started", "admin-start-failed"); return CommandResult.success(); }));
        admin.then(RouteNode.literal("stop").exec(ctx -> { manager.stop("Admin"); ctx.getSender().sendMessage(messages.getMessage("messages.tournament.admin-stopped")); return CommandResult.success(); }));
        admin.then(RouteNode.literal("status").exec(ctx -> {
            if (ctx.getSender() instanceof Player p) manager.status(p);
            else ctx.getSender().sendMessage(messages.getMessage("messages.tournament.admin-status")
                    .replace("{state}", manager.getState().name())
                    .replace("{participants}", String.valueOf(manager.getParticipantCount()))
                    .replace("{max}", String.valueOf(manager.getMaxParticipants())));
            return CommandResult.success();
        }));

        RouteNode kitAdmin = RouteNode.literal("kit");
        kitAdmin.then(RouteNode.literal("list").exec(ctx -> { adminKitList(ctx.getSender()); return CommandResult.success(); }));
        kitAdmin.then(RouteNode.literal("create").then(RouteNode.argument("args", new MessageStringNode()).exec(ctx -> {
            return result(ctx.getSender(), manager.adminKitCreate(ctx.getArg("args").trim()), "kit-created", "kit-create-failed")
                    .replace("{kit}", first(ctx.getArg("args")));
        })));
        kitAdmin.then(RouteNode.literal("delete").then(RouteNode.argument("args", new MessageStringNode()).exec(ctx -> {
            String kit = ctx.getArg("args").trim();
            return result(ctx.getSender(), manager.adminKitDelete(kit), "kit-deleted", "kit-delete-failed").replace("{kit}", kit);
        })));
        kitAdmin.then(RouteNode.literal("clear").then(RouteNode.argument("args", new MessageStringNode()).exec(ctx -> {
            String kit = ctx.getArg("args").trim();
            return result(ctx.getSender(), manager.adminKitClear(kit), "kit-cleared", "kit-clear-failed").replace("{kit}", kit);
        })));
        kitAdmin.then(RouteNode.literal("add").then(RouteNode.argument("args", new MessageStringNode()).exec(ctx -> {
            String[] parts = splitFirst(ctx.getArg("args"));
            if (parts.length < 2) {
                ctx.getSender().sendMessage(messages.getMessage("messages.tournament.kit-add-usage"));
                return CommandResult.fail();
            }
            return result(ctx.getSender(), manager.adminKitAdd(parts[0], parts[1]), "kit-command-added", "kit-command-add-failed")
                    .replace("{kit}", parts[0]);
        })));
        kitAdmin.then(RouteNode.literal("show").then(RouteNode.argument("args", new MessageStringNode()).exec(ctx -> {
            String kit = ctx.getArg("args").trim();
            ctx.getSender().sendMessage(messages.getMessage("messages.tournament.kit-show-header").replace("{kit}", kit));
            for (String command : manager.adminKitCommands(kit))
                ctx.getSender().sendMessage(messages.getMessage("messages.tournament.kit-show-entry").replace("{command}", command));
            return CommandResult.success();
        })));
        admin.then(kitAdmin);

        RouteNode configAdmin = RouteNode.literal("config");
        configAdmin.then(RouteNode.literal("get").then(RouteNode.argument("path", new MessageStringNode()).exec(ctx -> {
            String path = ctx.getArg("path").trim();
            Object value = manager.adminConfigGet(path);
            ctx.getSender().sendMessage(messages.getMessage("messages.tournament.config-value")
                    .replace("{path}", path).replace("{value}", String.valueOf(value)));
            return CommandResult.success();
        })));
        configAdmin.then(RouteNode.literal("set").then(RouteNode.argument("args", new MessageStringNode()).exec(ctx -> {
            String[] parts = splitFirst(ctx.getArg("args"));
            if (parts.length < 2) {
                ctx.getSender().sendMessage(messages.getMessage("messages.tournament.config-set-usage"));
                return CommandResult.fail();
            }
            boolean ok = manager.adminConfigSet(parts[0], parts[1]);
            ctx.getSender().sendMessage(messages.getMessage("messages.tournament." + (ok ? "config-set" : "config-set-failed"))
                    .replace("{path}", parts[0]).replace("{value}", parts[1]));
            return ok ? CommandResult.success() : CommandResult.fail();
        })));
        configAdmin.then(RouteNode.literal("reload").exec(ctx -> {
            boolean ok = manager.adminConfigReload();
            ctx.getSender().sendMessage(messages.getMessage("messages.tournament." + (ok ? "config-reloaded" : "config-reload-failed")));
            return ok ? CommandResult.success() : CommandResult.fail();
        }));
        admin.then(configAdmin);
        tree.getRoot().then(admin);
    }

    private CommandResult action(CommandSender sender, int action) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(messages.getMessage("messages.tournament.player-only")); return CommandResult.fail();
        }
        switch (action) {
            case 0 -> manager.join(p);
            case 1 -> manager.leave(p);
            case 2 -> manager.status(p);
            case 3 -> manager.bracket(p);
            case 4 -> manager.spectate(p);
            case 5 -> manager.kits(p);
        }
        return CommandResult.success();
    }

    private void adminKitList(CommandSender sender) {
        sender.sendMessage(messages.getMessage("messages.tournament.kit-list-header"));
        List<String> kits = manager.adminKitNames();
        if (kits.isEmpty()) {
            sender.sendMessage(messages.getMessage("messages.tournament.kit-list-empty"));
            return;
        }
        for (String kit : kits)
            sender.sendMessage(messages.getMessage("messages.tournament.kit-admin-entry")
                    .replace("{kit}", kit)
                    .replace("{default}", kit.equalsIgnoreCase(manager.adminKitDefault()) ? "*" : ""));
    }

    private String result(CommandSender sender, boolean ok, String yes, String no) {
        return messages.getMessage("messages.tournament." + (ok ? yes : no));
    }

    private void reply(CommandSender s, boolean ok, String yes, String no) {
        s.sendMessage(messages.getMessage("messages.tournament." + (ok ? yes : no)));
    }

    private void help(CommandSender s) { s.sendMessage(messages.getMessage("messages.tournament.help")); }
    private void adminHelp(CommandSender s) { s.sendMessage(messages.getMessage("messages.tournament.admin-help")); }

    private static String first(String value) {
        String[] p = splitFirst(value);
        return p.length == 0 ? value.trim() : p[0];
    }

    private static String[] splitFirst(String value) {
        String trimmed = value == null ? "" : value.trim();
        int index = trimmed.indexOf(' ');
        if (index < 0) return new String[]{trimmed};
        return new String[]{trimmed.substring(0, index), trimmed.substring(index + 1).trim()};
    }
}
