package me.itut.lanitium.value.parsing;

import carpet.script.Context;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import com.mojang.brigadier.StringReader;
import me.itut.lanitium.value.ObjectFunctionValue;
import me.itut.lanitium.value.SimpleFunctionValue;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.SuggestionSupplier;

import java.util.List;
import java.util.stream.Stream;

public class SuggestionSupplierValue extends ObjectFunctionValue<SuggestionSupplier<StringReader>> {
    protected SuggestionSupplierValue(SuggestionSupplier<StringReader> value) {
        super(value, value instanceof Fn fn ? fn.fn : new SimpleFunctionValue(1, 1, (c, t, e, tok, lv) -> ListValue.wrap(value.possibleValues(StateValue.from(lv.getFirst())).map(StringValue::of))));
    }

    protected SuggestionSupplierValue(SuggestionSupplierValue self) {
        super(self.value, self);
    }

    public static Value of(SuggestionSupplier<StringReader> value) {
        return value != null ? new SuggestionSupplierValue(value) : Value.NULL;
    }

    public static SuggestionSupplier<StringReader> from(Context c, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case SuggestionSupplierValue sup -> sup.value;
            case ObjectFunctionValue<?> ignored -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to parsing_suggestion_supplier");
            case FunctionValue fn -> new Fn(c, new SimpleFunctionValue(1, 1, (cc, t, e, tok, lv) -> fn.execute(cc, t, e, tok, lv, null).evalValue(c, t)));
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to parsing_suggestion_supplier");
        };
    }

    @Override
    public String getTypeString() {
        return "parsing_suggestion_supplier";
    }

    @Override
    protected Value clone() {
        return new SuggestionSupplierValue(this);
    }

    private record Fn(Context c, SimpleFunctionValue fn) implements SuggestionSupplier<StringReader> {
        @Override
        public Stream<String> possibleValues(ParseState<StringReader> state) {
            return switch (fn.callInContext(c, Context.NONE, List.of(StateValue.of(c, state))).evalValue(c)) {
                case NullValue ignored -> Stream.of();
                case AbstractListValue list -> list.unpack().stream().map(Value::getString);
                case Value v -> Stream.of(v.getString());
            };
        }
    }
}
