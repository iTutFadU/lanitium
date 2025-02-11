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
import java.util.List;

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
        String keyName = key.getString();
        return context.variables.put(keyName, (c, t) -> lazy.value.evalValue(c, t).reboundedTo(keyName)) != null;
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
    public int length() {
        return context.variables.size();
    }

    @Override
    public Iterator<Value> iterator() {
        return new ArrayList<>(context.variables.keySet().stream().map(StringValue::of).toList()).iterator();
    }

    @Override
    public Value in(Value args) {
        if (args instanceof ListValue lv) {
            List<Value> values = lv.getItems();
            String what = values.getFirst().getString();
            return get(what, values.subList(1, values.size()).toArray(new Value[0]));
        } else {
            return get(args.getString());
        }
    }

    public Value get(String what, Value... more) {
        return switch (what) {
            case "strict" -> BooleanValue.of(context.host.strict);
            default -> throw new InternalExpressionException("Unknown context feature: " + what);
        };
    }
}
