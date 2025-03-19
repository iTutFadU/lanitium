package me.itut.lanitium.internal.carpet;

import carpet.script.value.NBTSerializableValue;
import carpet.script.value.Value;
import net.minecraft.nbt.Tag;

public interface NBTSerializableValueInterface {
    NBTSerializableValue instance = new NBTSerializableValue((Tag)null);
    Value lanitium$decodeTag(Tag tag);

    static Value decodeTag(Tag tag) {
        return ((NBTSerializableValueInterface)instance).lanitium$decodeTag(tag);
    }
}
