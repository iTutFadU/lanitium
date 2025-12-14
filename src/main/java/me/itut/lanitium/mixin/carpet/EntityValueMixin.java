package me.itut.lanitium.mixin.carpet;

import carpet.script.CarpetScriptServer;
import carpet.script.value.EntityValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.Value;
import me.itut.lanitium.internal.carpet.EntityValueInterface;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

@Mixin(value = EntityValue.class, remap = false)
public abstract class EntityValueMixin implements EntityValueInterface {
    @Shadow @Final private static Map<String, BiConsumer<Entity, Value>> featureModifiers;

    @Override
    public Map<String, BiConsumer<Entity, Value>> lanitium$featureModifiers() {
        return featureModifiers;
    }

    static {
        // No !player check
        featureModifiers.put("nbt", (e, v) -> {
            UUID uUID = e.getUUID();
            Value tagValue = NBTSerializableValue.fromValue(v);
            if (tagValue instanceof NBTSerializableValue nbtsv) {
                try (final ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(e.problemPath(), CarpetScriptServer.LOG)) {
                    e.load(TagValueInput.create(reporter, e.registryAccess(), nbtsv.getCompoundTag()));
                }
                e.setUUID(uUID);
            }
        });
        featureModifiers.put("nbt_merge", (e, v) -> {
            UUID uUID = e.getUUID();
            Value tagValue = NBTSerializableValue.fromValue(v);
            if (tagValue instanceof NBTSerializableValue nbtsv) {
                CompoundTag nbttagcompound;
                try (final ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(e.problemPath(), CarpetScriptServer.LOG)) {
                    final TagValueOutput output = TagValueOutput.createWithContext(reporter, e.registryAccess());
                    e.saveWithoutId(output);
                    nbttagcompound = output.buildResult();
                }
                nbttagcompound.merge(nbtsv.getCompoundTag());
                try (final ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(e.problemPath(), CarpetScriptServer.LOG)) {
                    e.load(TagValueInput.create(reporter, e.registryAccess(), nbttagcompound));
                }
                e.setUUID(uUID);
            }
        });
    }
}
