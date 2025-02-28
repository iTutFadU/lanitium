package me.itut.lanitium.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(EntityDataAccessor.class)
public abstract class EntityDataAccessorMixin {
    @Shadow @Final
    private Entity entity;

    /**
     * @author iTut
     * @reason Yes players
     */
    @Overwrite
    public void setData(CompoundTag compound) {
        UUID uuid = entity.getUUID();
        entity.load(compound);
        entity.setUUID(uuid);
    }
}
