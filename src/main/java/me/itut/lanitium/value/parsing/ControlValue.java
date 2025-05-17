package me.itut.lanitium.value.parsing;

import carpet.script.exception.InternalExpressionException;
import carpet.script.value.BooleanValue;
import carpet.script.value.NullValue;
import carpet.script.value.Value;
import me.itut.lanitium.value.ObjectValue;
import net.minecraft.util.parsing.packrat.Control;

public class ControlValue extends ObjectValue<Control> {
    public static final ControlValue UNBOUND = new ControlValue(Control.UNBOUND);

    protected ControlValue(Control value) {
        super(value);
    }

    public static Value of(Control value) {
        return value != null ? new ControlValue(value) : Value.NULL;
    }

    public static Control from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case ControlValue control -> control.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to parsing_control");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "cut" -> {
                checkArguments(what, more, 0);
                value.cut();
                yield Value.NULL;
            }
            case "has_cut" -> {
                checkArguments(what, more, 0);
                yield BooleanValue.of(value.hasCut());
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "parsing_control";
    }

    @Override
    public boolean getBoolean() {
        return value.hasCut();
    }
}
