package me.itut.lanitium.value;

import carpet.script.exception.InternalExpressionException;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.Value;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Objects;

public abstract class ObjectFunctionValue<T> extends SimpleFunctionValue {
    public final T value;
    
    protected ObjectFunctionValue(T value, SimpleFunctionValue fn) {
        super(fn.minParams, fn.maxParams, fn.body);
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
            if (o instanceof ObjectFunctionValue<?> b)
                return a.compareTo(b.value);
        } catch (ClassCastException e) {
            throw new InternalExpressionException("Cannot compare " + getTypeString() + " to " + o.getTypeString());
        }
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

    @Override
    protected abstract Value clone();
}
