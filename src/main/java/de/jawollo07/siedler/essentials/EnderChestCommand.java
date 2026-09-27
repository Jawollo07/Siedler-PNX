package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.blockentity.BlockEntity;
import org.powernukkitx.blockentity.BlockEntityEnderChest;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandResult;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.inventory.HumanEnderChestInventory;
import org.powernukkitx.level.Level;
import org.powernukkitx.level.format.IChunk;
import org.powernukkitx.math.Vector3;
import org.powernukkitx.nbt.tag.CompoundTag;
import org.powernukkitx.command.route.RouteTree;

/**
 * Opens the player's persistent native Ender Chest inventory.
 *
 * PowerNukkitX's HumanEnderChestInventory needs a BlockEntityEnderChest
 * assigned before addWindow() can send the container-open packet. A command
 * has no physical Ender Chest block to provide that entity, so we create a
 * temporary, non-world-block-backed Ender Chest block entity at a free
 * position and remove it again one tick later. The inventory itself remains
 * the player's native persistent Ender Chest inventory.
 */
public final class EnderChestCommand extends Command {

    private final SiedlerPlugin plugin;
    private final MessageManager messageManager;

    public EnderChestCommand(SiedlerPlugin plugin) {
        super("ec", "Öffnet deine persönliche Enderchest", "/ec");
        this.plugin = plugin;
        this.messageManager = new MessageManager();
        setPermission("siedler.command.ec");
        setPermissionMessage(messageManager.getCommandMessage("no-permission"));
        enableCommandTree();
    }

    @Override
    protected void buildCommandTree(RouteTree tree) {
        tree.getRoot().exec(context -> {
            CommandSender sender = context.getSender();

            if (!(sender instanceof Player player)) {
                sender.sendMessage(messageManager.getCommandMessage("only-player-command"));
                return CommandResult.success();
            }

            if (player.getEnderChestOpen()) {
                player.sendMessage(messageManager.getMessage("messages.essentials.enderchest-already-open"));
                return CommandResult.success();
            }

            try {
                HumanEnderChestInventory inventory = player.getEnderChestInventory();
                Level level = player.getLevel();

                int baseX = (int) Math.floor(player.getX());
                int baseY = (int) Math.floor(player.getY()) + 2;
                int baseZ = (int) Math.floor(player.getZ());

                BlockEntityEnderChest virtualChest = null;

                // Find a free block-entity position close to the player.
                for (int offset = 0; offset < 16 && virtualChest == null; offset++) {
                    int x = baseX + offset;
                    int z = baseZ;

                    if (level.getBlockEntity(new Vector3(x, baseY, z)) != null) {
                        continue;
                    }

                    IChunk chunk = level.getChunk(x >> 4, z >> 4);
                    if (chunk == null) {
                        continue;
                    }

                    CompoundTag nbt = new CompoundTag()
                            .putString("id", BlockEntity.ENDER_CHEST)
                            .putInt("x", x)
                            .putInt("y", baseY)
                            .putInt("z", z);

                    virtualChest = new BlockEntityEnderChest(chunk, nbt);
                }

                if (virtualChest == null) {
                    throw new IllegalStateException("Kein freier Platz für die virtuelle Enderchest gefunden.");
                }

                inventory.setBlockEntityEnderChest(player, virtualChest);
                int windowId = player.addWindow(inventory);
                if (windowId == -1) {
                    // addWindow() can fail when another temporary window is open.
                    // Reset the native EnderChest state so the next /ec is not
                    // incorrectly rejected as "already open".
                    inventory.setBlockEntityEnderChest(player, null);
                    throw new IllegalStateException("Enderchest-Fenster konnte nicht geöffnet werden.");
                }

                final BlockEntityEnderChest entityToRemove = virtualChest;
                plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> {
                    try {
                        entityToRemove.close();
                    } catch (Exception ignored) {
                        // The inventory keeps the entity reference until close;
                        // only its temporary world registration is removed.
                    }
                }, 1);
            } catch (Exception exception) {
                player.sendMessage(messageManager.getMessage("messages.essentials.enderchest-error"));
                plugin.getLogger().warning(
                        "Enderchest konnte für " + player.getName() + " nicht geöffnet werden: "
                                + exception.getMessage()
                );
            }

            return CommandResult.success();
        });
    }
}
