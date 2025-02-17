package me.itut.lanitium.value.brigadier;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.ListValue;
import carpet.script.value.NullValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.itut.lanitium.Conversions;
import me.itut.lanitium.value.ContextValue;
import me.itut.lanitium.value.ObjectValue;

public class ArgumentTypeValue<T> extends ObjectValue<ArgumentType<T>> {
    protected ArgumentTypeValue(CarpetContext context, ArgumentType<T> value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, ArgumentType<?> value) {
        return value != null ? new ArgumentTypeValue<>(context, value) : Value.NULL;
    }

    public static ArgumentType<?> from(CarpetContext context, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case ArgumentTypeValue<?> v -> v.value;
            // TODO: Add argument types
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to argument_type");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "parse" -> {
                checkArguments(what, more, 1, 2);
                try {
                    if (more.length > 1)
                        yield Conversions.from(value.parse(new StringReader(more[0].getString()), ContextValue.fromOrCurrent(context, more[1]).source()));
                    yield Conversions.from(value.parse(new StringReader(more[0].getString())));
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(context, e);
                }
            }
            case "list_suggestions" -> checkArguments(what, more, 2, () -> SuggestionsFuture.of(context, value.listSuggestions(CommandContextValue.from(context, more[0]), SuggestionsBuilderValue.from(context, more[1]))));
            case "examples" -> checkArguments(what, more, 0, () -> ListValue.wrap(value.getExamples().stream().map(StringValue::of)));
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "argument_type";
    }
}
