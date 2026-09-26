package de.jawollo07.siedler.tournament;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;
import org.powernukkitx.command.route.node.RouteNode;

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
        }
        return CommandResult.success();
    }
    private void reply(CommandSender s, boolean ok, String yes, String no) {
        s.sendMessage(messages.getMessage("messages.tournament." + (ok ? yes : no)));
    }
    private void help(CommandSender s) { s.sendMessage(messages.getMessage("messages.tournament.help")); }
    private void adminHelp(CommandSender s) { s.sendMessage(messages.getMessage("messages.tournament.admin-help")); }
}