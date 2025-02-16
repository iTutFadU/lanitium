package me.itut.lanitium.value;

import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.Throwables;
import carpet.script.value.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.Tag;

import java.util.List;
import java.util.Objects;

import static carpet.script.exception.Throwables.THROWN_EXCEPTION_TYPE;
import static carpet.script.exception.Throwables.register;

public class Lazy extends Value implements WithValue {
    public static Throwables
        LAZY_EXCEPTION = register("lazy_exception", THROWN_EXCEPTION_TYPE),
        BREAK_ERROR = register("break_error", LAZY_EXCEPTION),
        CONTINUE_ERROR = register("continue_error", LAZY_EXCEPTION),
        RETURN_ERROR = register("return_error", LAZY_EXCEPTION);

    public final Context context;
    public final Context.Type type;
    public final LazyValue value;

    public Lazy(Context context, Context.Type type, LazyValue value) {
        this.context = context;
        this.type = type;
        this.value = value;
    }

    public Value eval(Context c, Context.Type t) {
        LazyValue initial = c.getVariable("@");
        c.setVariable("@", value);
        try {
            return value.evalValue(c, t);
        } finally {
            if (initial != null)
                c.setVariable("@", initial);
            else
                c.delVariable("@");
        }
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
            case "context" -> new ContextValue(context);
            case "type" -> StringValue.of(type.name().toLowerCase());
            default -> throw new InternalExpressionException("Unknown lazy feature: " + what);
        };
    }

    @Override
    public String getString() {
        return "lazy:" + type.name().toLowerCase() + "@" + Integer.toHexString(hashCode());
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
        return "lazy";
    }

    @Override
    public Lazy deepcopy() {
        return new Lazy(context.duplicate(), type, value);
    }

    @Override
    public LazyValue with(LazyValue arg) {
        Value output = arg.evalValue(context, type);
        return (c, t) -> output;
    }

    @Override
    public int hashCode() {
        return Objects.hash(context, type, value);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Lazy c && context.equals(c.context) && type.equals(c.type) && value.equals(c.value);
    }
}
