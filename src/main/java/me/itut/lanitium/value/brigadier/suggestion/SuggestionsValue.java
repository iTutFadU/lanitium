package me.itut.lanitium.value.brigadier.suggestion;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.ListValue;
import carpet.script.value.NullValue;
import carpet.script.value.Value;
import com.mojang.brigadier.suggestion.Suggestions;
import me.itut.lanitium.value.ObjectValue;
import me.itut.lanitium.value.ValueConversions;

public class SuggestionsValue extends ObjectValue<Suggestions> {
    protected SuggestionsValue(CarpetContext context, Suggestions value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, Suggestions value) {
        return value != null ? new SuggestionsValue(context, value) : Value.NULL;
    }

    public static Suggestions from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case SuggestionsValue v -> v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to suggestions");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "range" -> {
                checkArguments(what, more, 0);
                yield ValueConversions.range(value.getRange());
            }
            case "list" -> {
                checkArguments(what, more, 0);
                yield ListValue.wrap(value.getList().stream().map(v -> SuggestionValue.of(context, v)));
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "suggestions";
    }
}
