package de.jawollo07.siedler.essentials;

import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.route.RouteTree;
import org.powernukkitx.command.route.node.RouteNode;

public class HomeCommands extends Command{
    public HomeCommands() {
        super("home", "Verwalte deine Homes", "/home help");
        this.setPermission("siedler.basic");
        this.enableCommandTree();    
    }
    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().then(
            RouteNode.literal("tp")
                .exec(context -> {
                    
                    return CommandResult.success();
                })
        );
        tree.getRoot().then(
            RouteNode.literal("create")
                .exec(context -> {
                    
                    return CommandResult.success();
                })
        );
        tree.getRoot().then(
            RouteNode.literal("help")
                .exec(context -> {
                    
                    return CommandResult.success();
                })
        );
    }
}
