package me.itut.lanitium.value.brigadier.suggestion;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.BooleanValue;
import carpet.script.value.NullValue;
import carpet.script.value.Value;
import com.mojang.brigadier.suggestion.Suggestions;
import me.itut.lanitium.value.ObjectValue;

import java.util.concurrent.CompletableFuture;

public class SuggestionsFuture extends ObjectValue<CompletableFuture<Suggestions>> {
    protected SuggestionsFuture(CarpetContext context, CompletableFuture<Suggestions> value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, CompletableFuture<Suggestions> value) {
        return value != null ? new SuggestionsFuture(context, value) : Value.NULL;
    }

    public static CompletableFuture<Suggestions> from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case SuggestionsFuture v -> v.value;
            case SuggestionsValue v -> CompletableFuture.completedFuture(v.value);
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to suggestions_future");
        };
    }

    public static SuggestionsFuture empty(CarpetContext context) {
        return new SuggestionsFuture(context, Suggestions.empty());
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "done" -> {
                checkArguments(what, more, 0);
                yield BooleanValue.of(value.isDone());
            }
            case "cancelled" -> {
                checkArguments(what, more, 0);
                yield BooleanValue.of(value.isCancelled());
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "suggestions_future";
    }
}
