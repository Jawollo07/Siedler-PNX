package de.jawollo07.siedler.essentials;

import org.powernukkitx.Player;
import org.powernukkitx.inventory.ContainerInventory;
import org.powernukkitx.inventory.InventoryHolder;
import org.powernukkitx.item.Item;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerEnumName;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;

import java.util.Map;

public final class TeamEnderChestInventory extends ContainerInventory {

    private final String teamId;
    private final TeamEnderChestManager manager;

    public TeamEnderChestInventory(Player holder, String teamId, TeamEnderChestManager manager) {
        super(holder, ContainerType.CONTAINER, 54);
        this.teamId = teamId;
        this.manager = manager;
    }

    public String getTeamId() {
        return teamId;
    }

    @Override
    public void init() {
        Map<Integer, ContainerEnumName> map = super.slotTypeMap();
        for (int i = 0; i < getSize(); i++) {
            map.put(i, ContainerEnumName.LEVEL_ENTITY_CONTAINER);
        }
    }

    @Override
    public InventoryHolder getHolder() {
        return super.getHolder();
    }

    @Override
    public void onSlotChange(int index, Item before, boolean send) {
        super.onSlotChange(index, before, send);
        manager.saveSlot(this, index);
    }
}
