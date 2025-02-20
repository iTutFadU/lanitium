package me.itut.lanitium.value.brigadier.argument;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ProcessedThrowStatement;
import carpet.script.value.*;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.itut.lanitium.Conversions;
import me.itut.lanitium.value.ContextValue;
import me.itut.lanitium.value.ObjectValue;
import me.itut.lanitium.value.Util;
import me.itut.lanitium.value.brigadier.CommandSyntaxError;
import me.itut.lanitium.value.brigadier.StringReaderValue;
import me.itut.lanitium.value.brigadier.context.CommandContextValue;
import me.itut.lanitium.value.brigadier.suggestion.SuggestionsBuilderValue;
import me.itut.lanitium.value.brigadier.suggestion.SuggestionsFuture;
import net.minecraft.commands.CommandSourceStack;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ArgumentTypeValue<T> extends ObjectValue<ArgumentType<T>> {
    protected ArgumentTypeValue(CarpetContext context, ArgumentType<T> value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, ArgumentType<?> value) {
        return value != null ? new ArgumentTypeValue<>(context, value) : Value.NULL;
    }

    public static ArgumentType<?> from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case ArgumentTypeValue<?> v -> v.value;
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
                        yield Conversions.from(context, value.parse(new StringReader(more[0].getString()), ContextValue.fromOrCurrent(context, more[1]).source()));
                    yield Conversions.from(context, value.parse(new StringReader(more[0].getString())));
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(context, e);
                }
            }
            case "list_suggestions" -> {
                checkArguments(what, more, 2);
                yield SuggestionsFuture.of(context, value.listSuggestions(CommandContextValue.from(more[0]), SuggestionsBuilderValue.from(more[1])));
            }
            case "examples" -> {
                checkArguments(what, more, 0);
                yield ListValue.wrap(value.getExamples().stream().map(StringValue::of));
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "argument_type";
    }
}
