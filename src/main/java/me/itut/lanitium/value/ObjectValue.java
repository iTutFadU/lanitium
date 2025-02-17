package me.itut.lanitium.value;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.Value;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.Tag;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class ObjectValue<T> extends Value {
    public final CarpetContext context;
    public final T value;

    protected ObjectValue(CarpetContext context, T value) {
        this.context = context;
        this.value = value;
    }

    @Override
    public Value in(Value args) {
        List<Value> values = Util.listFrom(args);
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
        return o instanceof ObjectValue<?> b && Objects.equals(value, b.value);
    }

    @Override
    public Tag toTag(boolean force, RegistryAccess regs) {
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

    protected void checkArguments(String what, Value[] more, int min, int max) throws InternalExpressionException {
        if (more.length < min || max >= 0 && more.length > max) throw new InternalExpressionException(getTypeString() + "~'" + what + "' expected " + (min == max ? min == 0 ? "no" : "" + min : max < 0 ? "at least " + min : min == 0 ? "at most " + max : "from " + min + " to " + max) + " arguments, got " + more.length);
    }

    protected void checkArguments(String what, Value[] more, int amount) throws InternalExpressionException {
        checkArguments(what, more, amount, amount);
    }

    protected <A> A checkArguments(String what, Value[] more, int min, int max, Supplier<A> result) {
        checkArguments(what, more, min, max);
        return result.get();
    }

    protected <A> A checkArguments(String what, Value[] more, int amount, Supplier<A> result) {
        checkArguments(what, more, amount);
        return result.get();
    }
}
