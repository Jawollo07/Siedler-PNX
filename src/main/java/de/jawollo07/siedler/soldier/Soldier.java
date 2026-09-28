package de.jawollo07.siedler.soldier;

import org.jetbrains.annotations.NotNull;
import org.powernukkitx.entity.Entity;
import org.powernukkitx.entity.custom.CustomEntity;
import org.powernukkitx.entity.custom.CustomEntityDefinition;
import org.powernukkitx.entity.custom.CustomEntityDefinition.SimpleBuilder;
import org.powernukkitx.level.format.IChunk;
import org.powernukkitx.nbt.tag.CompoundTag;
import org.powernukkitx.nbt.tag.CompoundTagView;

public class Soldier extends Entity implements CustomEntity {
    public static final String IDENTIFIER = "siedler:soldier";
    public static Integer HEALTH = 20;
    public static float SPEED = 20;
    public Soldier(IChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }
    @Override
    public @NotNull String getIdentifier() {
        return IDENTIFIER;
    }
    @Override
    public String getOriginalName() {
        return "soldier";
    }
    public static CustomEntityDefinition definition() {
        return CustomEntityDefinition.simpleBuilder(IDENTIFIER)
            .eid(IDENTIFIER)
            .hasSpawnEgg(true)
            .isSummonable(true)
            .originalName("Soldat")
            .health(HEALTH)
            .movement(SPEED)
            .typeFamily("soldier")
            .isPersistent(true)
            .build();
    } 
}
