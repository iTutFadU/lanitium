package me.itut.lanitium.value;

import carpet.script.CarpetContext;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.Value;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Objects;

public abstract class ObjectFunctionValue<T> extends SimpleFunctionValue {
    public final CarpetContext context;
    public final T value;
    
    protected ObjectFunctionValue(CarpetContext context, T value, SimpleFunctionValue fn) {
        super(fn.minParams, fn.maxParams, fn.body);
        this.context = context;
        this.value = value;
    }

    @Override
    public abstract String getTypeString();

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ObjectFunctionValue<?> b && Objects.equals(value, b.value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public int compareTo(Value o) {
        if (!(value instanceof Comparable a)) return super.compareTo(o);
        try {
            if (o instanceof ObjectValue<?> ob && ob.value instanceof Comparable b) return a.compareTo(b);
            if (o instanceof ObjectFunctionValue<?> ob && ob.value instanceof Comparable b) return a.compareTo(b);
        } catch (ClassCastException ignored) {}
        return super.compareTo(o);
    }

    @Override
    public Tag toTag(boolean force, RegistryAccess regs) {
        if (force) return StringTag.valueOf(getString());
        throw new NBTSerializableValue.IncompatibleTypeException(this);
    }

    @Override
    public String getString() {
        return getTypeString() + "@" + Integer.toHexString(hashCode());
    }
}
