package me.itut.lanitium.value.parsing;

import carpet.script.exception.InternalExpressionException;
import carpet.script.value.NullValue;
import carpet.script.value.NumericValue;
import carpet.script.value.Value;
import me.itut.lanitium.value.ObjectValue;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.Scope;

import java.util.Arrays;
import java.util.Objects;

public class ScopeValue extends ObjectValue<Scope> {
    protected ScopeValue(Scope value) {
        super(value);
    }

    public static Value of(Scope value) {
        return value != null ? new ScopeValue(value) : Value.NULL;
    }

    public static Scope from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case ScopeValue scope -> scope.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to parsing_scope");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "value_index_for_any" -> {
                Atom<?>[] atoms = Arrays.stream(more).map(AtomValue::from).toArray(Atom<?>[]::new);
                int index = value.valueIndexForAny(atoms);
                yield index < 0 ? Value.NULL : NumericValue.of(index);
            }
            case "push_frame" -> {
                checkArguments(what, more, 0);
                value.pushFrame();
                yield Value.NULL;
            }
            case "pop_frame" -> {
                checkArguments(what, more, 0);
                value.popFrame();
                yield Value.NULL;
            }
            case "split_frame" -> {
                checkArguments(what, more, 0);
                value.splitFrame();
                yield Value.NULL;
            }
            case "clear_frame_values" -> {
                checkArguments(what, more, 0);
                value.clearFrameValues();
                yield Value.NULL;
            }
            case "merge_frame" -> {
                checkArguments(what, more, 0);
                value.mergeFrame();
                yield Value.NULL;
            }
            case "put" -> {
                checkArguments(what, more, 2);
                value.put(AtomValue.from(more[0]), more[1]);
                yield Value.NULL;
            }
            case "get" -> {
                checkArguments(what, more, 1, 2);
                yield Objects.requireNonNullElse(value.get(AtomValue.from(more[0])), more.length == 2 ? more[1] : Value.NULL);
            }
            case "get_any" -> Objects.requireNonNullElse((Value)value.getAny(Arrays.stream(more).map(AtomValue::from).toArray(Atom[]::new)), Value.NULL);
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "parsing_scope";
    }

    @Override
    public String getString() {
        return value.toString();
    }
}
