package me.itut.lanitium.value;

import carpet.script.CarpetContext;
import carpet.script.LazyValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.Value;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.Tag;

import java.util.List;
import java.util.Objects;

public abstract class ObjectFunctionValue<T> extends SimpleFunctionValue {
    public final CarpetContext context;
    public final T value;
    
    protected ObjectFunctionValue(CarpetContext context, T value, LazyValue body, List<String> args, String varArgs) {
        super(body, args, varArgs);
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
        return o instanceof ObjectFunctionValue<?> b && Objects.equals(value, b.value);
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
        throw new NBTSerializableValue.IncompatibleTypeException(this);
    }

    @Override
    public String getString() {
        return getTypeString() + "@" + Integer.toHexString(hashCode());
    }
}
