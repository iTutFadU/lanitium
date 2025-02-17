package me.itut.lanitium.value.brigadier;

import carpet.script.CarpetContext;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.mojang.brigadier.suggestion.Suggestion;
import me.itut.lanitium.value.ObjectValue;
import me.itut.lanitium.value.Util;

public class SuggestionValue extends ObjectValue<Suggestion> {
    private SuggestionValue(CarpetContext context, Suggestion value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, Suggestion value) {
        return value != null ? new SuggestionValue(context, value) : Value.NULL;
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "range" -> checkArguments(what, more, 0, () -> Util.range(value.getRange()));
            case "text" -> checkArguments(what, more, 0, () -> StringValue.of(value.getText()));
            case "tooltip" -> checkArguments(what, more, 0, () -> StringValue.of(value.getTooltip().getString()));
            case "apply" -> checkArguments(what, more, 1, () -> StringValue.of(value.apply(more[0].getString())));
            case "expand" -> checkArguments(what, more, 2, () -> of(context, value.expand(more[0].getString(), Util.toRange(more[1]))));
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "suggestion";
    }
}
