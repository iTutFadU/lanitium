package me.itut.lanitium.value;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;

public class ContextValue extends ObjectValue<CarpetContext> implements WithValue {
    protected ContextValue(CarpetContext value) {
        super(value, value);
    }

    public static Value of(Context value) {
        return value != null ? new ContextValue((CarpetContext)value) : Value.NULL;
    }

    public static CarpetContext from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case ContextValue v -> v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to context");
        };
    }

    public static CarpetContext fromOrCurrent(CarpetContext context, Value value) {
        CarpetContext output = from(value);
        return output != null ? output : context;
    }

    @Override
    public String getTypeString() {
        return "context";
    }

    @Override
    public ContextValue deepcopy() {
        return new ContextValue(value.duplicate());
    }

    public Value get(String what, Value... more) {
        return switch (what) {
            case "strict" -> BooleanValue.of(value.host.strict);
            default -> unknownFeature(what);
        };
    }

    @Override
    public LazyValue with(LazyValue arg) {
        Value output = arg.evalValue(value);
        return (c, t) -> output;
    }
}
