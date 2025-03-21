package me.itut.lanitium.value;

import carpet.script.value.NBTSerializableValue;
import carpet.script.value.Value;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class Symbol extends Value {
    private final Object symbol = new Object();

    @Override
    public String getString() {
        return "symbol@" + Integer.toHexString(symbol.hashCode());
    }

    @Override
    public boolean getBoolean() {
        return true;
    }

    @Override
    public Tag toTag(boolean force, RegistryAccess registryAccess) {
        if (force) return StringTag.valueOf(getString());
        throw new NBTSerializableValue.IncompatibleTypeException(this);
    }

    @Override
    public String getTypeString() {
        return "symbol";
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Symbol s && symbol == s.symbol;
    }

    @Override
    public int hashCode() {
        return symbol.hashCode();
    }
}
