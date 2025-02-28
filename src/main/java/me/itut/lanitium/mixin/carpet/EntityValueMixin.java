package me.itut.lanitium.mixin.carpet;

import carpet.script.value.EntityValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.Value;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

@Mixin(value = EntityValue.class, remap = false)
public abstract class EntityValueMixin {
    @Shadow @Final private static Map<String, BiConsumer<Entity, Value>> featureModifiers;

    static {
        // No !player check
        featureModifiers.put("nbt", (e, v) -> {
            UUID uUID = e.getUUID();
            Value tagValue = NBTSerializableValue.fromValue(v);
            if (tagValue instanceof NBTSerializableValue nbtsv)
            {
                e.load(nbtsv.getCompoundTag());
                e.setUUID(uUID);
            }
        });
        featureModifiers.put("nbt_merge", (e, v) -> {
            UUID uUID = e.getUUID();
            Value tagValue = NBTSerializableValue.fromValue(v);
            if (tagValue instanceof NBTSerializableValue nbtsv)
            {
                CompoundTag compound = e.saveWithoutId((new CompoundTag()));
                compound.merge(nbtsv.getCompoundTag());
                e.load(compound);
                e.setUUID(uUID);
            }
        });
    }
}
