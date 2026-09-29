package me.itut.lanitium.mixin.carpet;

import carpet.script.CarpetScriptServer;
import carpet.script.Context;
import carpet.script.value.*;
import me.itut.lanitium.internal.carpet.EntityValueInterface;
import me.itut.lanitium.value.FeatureMethodsValue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

@Mixin(value = EntityValue.class, remap = false)
public abstract class EntityValueMixin implements FeatureMethodsValue, EntityValueInterface {
    @Shadow @Final private static Map<String, BiConsumer<Entity, Value>> featureModifiers;

    @Override
    public Map<String, BiConsumer<Entity, Value>> lanitium$featureModifiers() {
        return featureModifiers;
    }

    @Override
    public Value lanitium$feature(Context.Type type, Context ctx, String what, Value... more) {
        EntityValue entity = (EntityValue)(Object)this;

        if (type == Context.LVALUE) {
            List<Value> prefix = List.of(more);
            return new LContainerValue(new ContainerValueInterface() {
                @Override
                public boolean put(Value where, Value value) {
                    List<Value> arg = new ArrayList<>(prefix);
                    if (value instanceof ListValue list) arg.addAll(list.getItems());
                    else arg.add(value);
                    entity.set(what, switch (arg.size()) {
                        case 0 -> null;
                        case 1 -> arg.getFirst();
                        default -> ListValue.wrap(arg);
                    });
                    return true;
                }

                @Override
                public Value get(Value where) {
                    return entity.get(what, switch (more.length) {
                        case 0 -> null;
                        case 1 -> more[0];
                        default -> ListValue.of(more);
                    });
                }

                @Override
                public boolean has(Value where) {
                    return EntityValueMixin.this.lanitium$featureModifiers().containsKey(what);
                }

                @Override
                public boolean delete(Value where) {
                    return false;
                }
            }, null);
        }

        return entity.get(what, switch (more.length) {
            case 0 -> null;
            case 1 -> more[0];
            default -> ListValue.of(more);
        });
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
