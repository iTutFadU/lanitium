package me.itut.lanitium.value.brigadier.suggestion;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.NullValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.itut.lanitium.value.ObjectValue;

public class SuggestionsBuilderValue extends ObjectValue<SuggestionsBuilder> {
    protected SuggestionsBuilderValue(CarpetContext context, SuggestionsBuilder value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, SuggestionsBuilder value) {
        return value != null ? new SuggestionsBuilderValue(context, value) : Value.NULL;
    }

    public static SuggestionsBuilder from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case SuggestionsBuilderValue v -> v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to suggestions_builder");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "input" -> {
                checkArguments(what, more, 0);
                yield StringValue.of(value.getInput());
            }
            case "start" -> {
                checkArguments(what, more, 0);
                yield NumericValue.of(value.getStart());
            }
            case "remaining" -> {
                checkArguments(what, more, 0);
                yield StringValue.of(value.getRemaining());
            }
            case "remaining_lowercase" -> {
                checkArguments(what, more, 0);
                yield StringValue.of(value.getRemainingLowerCase());
            }
            case "build" -> {
                checkArguments(what, more, 0);
                yield SuggestionsValue.of(context, value.build());
            }
            case "build_future" -> {
                checkArguments(what, more, 0);
                yield SuggestionsFuture.of(context, value.buildFuture());
            }
            case "suggest" -> {
                checkArguments(what, more, 1, 2);
                if (more.length > 1) {
                    if (more[0] instanceof NumericValue number) value.suggest(number.getInt(), () -> more[1].getString());
                    else value.suggest(more[0].getString(), () -> more[1].getString());
                } else if (more[0] instanceof NumericValue number) value.suggest(number.getInt());
                else value.suggest(more[0].getString());
                yield this;
            }
            case "add" -> {
                checkArguments(what, more, 1);
                value.add(SuggestionsBuilderValue.from(more[0]));
                yield this;
            }
            case "create_offset" -> {
                checkArguments(what, more, 1);
                yield of(context, value.createOffset(NumericValue.asNumber(more[0]).getInt()));
            }
            case "restart" -> {
                checkArguments(what, more, 0);
                yield of(context, value.restart());
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "suggestions_builder";
    }
}
