package me.itut.lanitium.value.parsing;

import carpet.script.exception.InternalExpressionException;
import carpet.script.value.NullValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import me.itut.lanitium.value.ObjectValue;
import net.minecraft.util.parsing.packrat.Atom;

public class AtomValue extends ObjectValue<Atom<Value>> {
    protected AtomValue(Atom<Value> value) {
        super(value);
    }

    public static Value of(Atom<Value> value) {
        return value != null ? new AtomValue(value) : Value.NULL;
    }

    public static Atom<Value> from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case AtomValue atom -> atom.value;
            case StringValue str -> Atom.of(str.getString());
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to parsing_atom");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        if (what.equals("name")) {
            checkArguments(what, more, 0);
            return StringValue.of(value.name());
        }
        return unknownFeature(what);
    }

    @Override
    public String getTypeString() {
        return "parsing_atom";
    }

    @Override
    public String getString() {
        return value.toString();
    }
}
