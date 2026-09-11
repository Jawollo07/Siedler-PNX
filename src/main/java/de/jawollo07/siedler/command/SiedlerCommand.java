package de.mcjj.siedler.command;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.utils.TextFormat;
import de.mcjj.siedler.SiedlerPlugin;

/** Base /siedler command used as the migration entry point. */
public final class SiedlerCommand extends Command {
    private final SiedlerPlugin plugin;

    public SiedlerCommand(SiedlerPlugin plugin) {
        super("siedler", "Siedler 2.0 main command", "/siedler");
        this.plugin = plugin;
        setPermission("siedler.command");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        sender.sendMessage(TextFormat.GOLD + "Siedler 2.0" + TextFormat.GRAY + " – PowerNukkitX foundation online.");
        sender.sendMessage(TextFormat.GRAY + "Gameplay modules are being migrated from Siedler 1.x.");
        return true;
    }
}
