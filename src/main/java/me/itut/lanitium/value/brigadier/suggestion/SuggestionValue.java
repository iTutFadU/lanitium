package me.itut.lanitium.value.brigadier.suggestion;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.NullValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.suggestion.IntegerSuggestion;
import com.mojang.brigadier.suggestion.Suggestion;
import me.itut.lanitium.value.ObjectValue;
import me.itut.lanitium.value.ValueConversions;

public class SuggestionValue extends ObjectValue<Suggestion> {
    private SuggestionValue(CarpetContext context, Suggestion value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, Suggestion value) {
        return value != null ? new SuggestionValue(context, value) : Value.NULL;
    }

    public static Suggestion from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case SuggestionValue v -> v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to suggestion");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "range" -> {
                checkArguments(what, more, 0);
                yield ValueConversions.range(value.getRange());
            }
            case "text" -> {
                checkArguments(what, more, 0);
                yield StringValue.of(value.getText());
            }
            case "tooltip" -> {
                checkArguments(what, more, 0);
                Message tooltip = value.getTooltip();
                yield tooltip != null ? StringValue.of(tooltip.getString()) : Value.NULL;
            }
            case "apply" -> {
                checkArguments(what, more, 1);
                yield StringValue.of(value.apply(more[0].getString()));
            }
            case "expand" -> {
                checkArguments(what, more, 2);
                yield of(context, value.expand(more[0].getString(), ValueConversions.toRange(more[1])));
            }
            case "value" -> {
                checkArguments(what, more, 0);
                yield value instanceof IntegerSuggestion v ? NumericValue.of(v.getValue()) : Value.NULL;
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "suggestion";
    }
}
