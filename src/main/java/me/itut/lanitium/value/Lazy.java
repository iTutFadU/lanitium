package me.itut.lanitium.value;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.Throwables;
import carpet.script.value.*;

import java.util.List;
import java.util.Objects;

import static carpet.script.exception.Throwables.THROWN_EXCEPTION_TYPE;
import static carpet.script.exception.Throwables.register;

public class Lazy extends ContextValue implements WithValue {
    public static final Throwables
        LAZY_EXCEPTION = register("lazy_exception", THROWN_EXCEPTION_TYPE),
        BREAK_ERROR = register("break_error", LAZY_EXCEPTION),
        CONTINUE_ERROR = register("continue_error", LAZY_EXCEPTION),
        RETURN_ERROR = register("return_error", LAZY_EXCEPTION);

    public final Context.Type type;
    public final LazyValue lazy;

    public Lazy(Context context, Context.Type type, LazyValue lazy) {
        super((CarpetContext)context);
        this.type = type;
        this.lazy = lazy;
    }

    @SuppressWarnings("ConstantValue")
    public Value eval(Context c, Context.Type t) {
        LazyValue initial = c.getVariable("@");
        c.setVariable("@", lazy);
        try {
            return lazy.evalValue(c, t);
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
            case "context" -> checkArguments(what, more, 0, () -> this);
            case "type" -> checkArguments(what, more, 0, () -> StringValue.of(type.name().toLowerCase()));
            default -> throw new InternalExpressionException("Unknown lazy feature: " + what);
        };
    }

    @Override
    public String getString() {
        return "lazy:" + type.name().toLowerCase() + "@" + Integer.toHexString(hashCode());
    }

    @Override
    public String getTypeString() {
        return "lazy";
    }

    @Override
    public Lazy deepcopy() {
        return new Lazy(value.duplicate(), type, lazy);
    }

    @Override
    public LazyValue with(LazyValue arg) {
        Value output = arg.evalValue(value, type);
        return (c, t) -> output;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, type, lazy);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Lazy c && value.equals(c.value) && type.equals(c.type) && lazy.equals(c.lazy);
    }
}
