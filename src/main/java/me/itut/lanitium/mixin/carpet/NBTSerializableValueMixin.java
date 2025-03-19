package me.itut.lanitium.mixin.carpet;

import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.value.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.itut.lanitium.internal.carpet.NBTSerializableValueInterface;
import me.itut.lanitium.value.WithValue;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = NBTSerializableValue.class, remap = false)
public abstract class NBTSerializableValueMixin implements NBTSerializableValueInterface, WithValue {
    @Shadow
    public abstract Tag getTag();

    @Shadow
    public static Value of(Tag tag) {
        return null;
    }

    @Shadow
    private static Value decodeSimpleTag(Tag t) {
        return null;
    }

    @Shadow
    private static NbtPathArgument.NbtPath cachePath(String arg) {
        return null;
    }

    @Shadow
    private static Value decodeTag(Tag t) {
        return null;
    }

    @Override
    public Value lanitium$decodeTag(Tag tag) {
        return decodeTag(tag);
    }

    @Override
    public LazyValue with(Context c, Context.Type t, LazyValue arg) {
        Value value = arg.evalValue(c, t), output;
        if (value.isNull()) {
            output = switch (getTag()) {
                case CompoundTag compound -> {
                    Map<Value, Value> map = new HashMap<>(compound.size());
                    compound.getAllKeys().forEach(k -> map.put(StringValue.of(k), of(compound.get(k))));
                    yield MapValue.wrap(map);
                }
                case ListTag list -> ListValue.wrap(list.stream().map(NBTSerializableValue::of));
                case Tag tag -> decodeSimpleTag(tag);
            };
        } else try {
            output = ListValue.wrap(cachePath(value.getString()).get(getTag()).stream().map(NBTSerializableValue::of));
        } catch (CommandSyntaxException ignored) {
            output = ListValue.of();
        }
        Value finalOutput = output;
        return (cc, tt) -> finalOutput;
    }
}
