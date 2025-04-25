package me.itut.lanitium.value;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.Value;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.List;
import java.util.Objects;

public abstract class ObjectValue<T> extends Value {
    public final CarpetContext context;
    public final T value;

    protected ObjectValue(CarpetContext context, T value) {
        this.context = context;
        this.value = value;
    }

    @Override
    public Value in(Value args) {
        List<Value> values = ValueConversions.listFrom(args);
        if (values.isEmpty()) return get("null");
        return get(values.getFirst().getString(), values.subList(1, values.size()).toArray(Value[]::new));
    }

    public abstract Value get(String what, Value... more);

    protected Value unknownFeature(String what) throws InternalExpressionException {
        throw new InternalExpressionException("Unknown " + getTypeString() + " feature: " + what);
    }

    @Override
    public abstract String getTypeString();

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ObjectValue<?> b && Objects.equals(value, b.value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public int compareTo(Value o) {
        if (!(value instanceof Comparable a)) return super.compareTo(o);
        try {
            if (o instanceof ObjectValue<?> ob && ob.value instanceof Comparable b)
                return a.compareTo(b);
            if (o instanceof ObjectFunctionValue<?> ob && ob.value instanceof Comparable b)
                return a.compareTo(b);
        } catch (ClassCastException e) {
            throw new RuntimeException(e);
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
    public boolean getBoolean() {
        return true;
    }

    public static void checkArguments(String fn, int length, int min, int max) throws InternalExpressionException {
        assert min >= 0 && (max < 0 || min <= max) : "Why.";
        if (length < min || max >= 0 && length > max) throw new InternalExpressionException(fn + " expected " + (min == max ? min == 0 ? "no" : min : max < 0 ? "at least " + min : min == 0 ? "at most " + max : "from " + min + " to " + max) + " argument" + ((max != 1 ? min : max) != 1 ? "s" : "") + ", got " + length);
    }

    public static void checkArguments(String fn, int length, int amount) throws InternalExpressionException {
        checkArguments(fn, length, amount, amount);
    }

    protected void checkArguments(String what, Value[] more, int min, int max) throws InternalExpressionException {
        checkArguments(getTypeString() + "~'" + what + "'", more.length, min, max);
    }

    protected void checkArguments(String what, Value[] more, int amount) throws InternalExpressionException {
        checkArguments(what, more, amount, amount);
    }
}
