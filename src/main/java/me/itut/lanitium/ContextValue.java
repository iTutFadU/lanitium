package me.itut.lanitium;

import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;

public class ContextValue extends AbstractListValue implements ContainerValueInterface {
    public final Context context;

    public ContextValue(Context context) {
        this.context = context;
    }

    @Override
    public String getString() {
        return "context";
    }

    @Override
    public boolean getBoolean() {
        return true;
    }

    @Override
    public Tag toTag(boolean b, RegistryAccess registryAccess) {
        throw new NBTSerializableValue.IncompatibleTypeException(this);
    }

    @Override
    public String getTypeString() {
        return "context";
    }

    @Override
    public ContextValue deepcopy() {
        return new ContextValue(context.duplicate());
    }

    @Override
    public boolean put(Value key, Value value) {
        if (!(value instanceof Lazy lazy)) throw new InternalExpressionException("A variable must be set to a lazy value");
        return context.variables.put(key.getString(), lazy.value) != null;
    }

    @Override
    public Value get(Value value) {
        LazyValue v = context.variables.get(value.getString());
        return v != null ? new Lazy(context, Context.NONE, v) : NULL;
    }

    @Override
    public boolean has(Value value) {
        return context.variables.containsKey(value.getString());
    }

    @Override
    public boolean delete(Value value) {
        return context.variables.remove(value.getString()) != null;
    }

    @Override
    public Value in(Value value) {
        throw new InternalExpressionException("This might get implemented later");
    }

    @Override
    public int length() {
        return context.variables.size();
    }

    @Override
    public Iterator<Value> iterator() {
        return new ArrayList<>(context.variables.keySet().stream().map(StringValue::of).toList()).iterator();
    }
}
