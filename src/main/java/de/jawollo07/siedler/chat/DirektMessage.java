package de.jawollo07.siedler.chat;
 
import de.jawollo07.siedler.SiedlerPlugin;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandSender;

public class DirektMessage extends Command {
    private final SiedlerPlugin plugin;
    private final ChatLogger chatLogger;

    public DirektMessage(SiedlerPlugin plugin) {
        super("dm", "Sendet eine Direktnachricht an einen Spieler", "/dm <Spieler> <Nachricht>");
        this.setPermission("siedler.command.dm");
        this.setPermission("siedler.basic");
        this.plugin = plugin;
        this.chatLogger = new ChatLogger(plugin.getStorage());
    }
    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cVerwendung: /dm <Spieler> <Nachricht>");
            return false;
        }
        if (!(sender instanceof Player senderPlayer)) {
            sender.sendMessage("§cDieser Befehl kann nur von einem Spieler verwendet werden.");
            return false;
        }

        String targetPlayerName = args[0];
        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
            plugin.getServer().getOnlinePlayers().values().stream()
                    .filter(player -> player.getName().equalsIgnoreCase(targetPlayerName))
                    .findFirst()
                    .ifPresentOrElse(targetPlayer -> {
                        targetPlayer.sendMessage("§7[DM] §e" + sender.getName() + " §7-> §eDir: §f" + message);
                        sender.sendMessage("§7[DM] §eDu §7-> §e" + targetPlayer.getName() + ": §f" + message);
                        chatLogger.saveChatMessage(
                                senderPlayer.getUniqueId().toString(),
                                senderPlayer.getName(),
                                senderPlayer.getLevel().getName(),
                                targetPlayer.getName(),
                                senderPlayer.getX(),
                                senderPlayer.getY(),
                                senderPlayer.getZ(),
                                message
                        );
                    }, () -> {
                        sender.sendMessage("§cSpieler " + targetPlayerName + " ist nicht online.");
                    });
        });

        return true;
    }
}
