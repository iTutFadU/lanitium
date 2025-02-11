package me.itut.lanitium;

import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ContextValue extends Value implements WithValue {
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

    @Override
    public LazyValue with(LazyValue arg) {
        Value output = arg.evalValue(context);
        return (c, t) -> output;
    }
}
