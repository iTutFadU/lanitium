package me.itut.lanitium.value.brigadier;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.MapValue;
import carpet.script.value.NullValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.mojang.brigadier.ParseResults;
import me.itut.lanitium.value.ObjectValue;
import net.minecraft.commands.CommandSourceStack;

import java.util.AbstractMap;
import java.util.Map;

public class ParseResultsValue extends ObjectValue<ParseResults<CommandSourceStack>> {
    protected ParseResultsValue(CarpetContext context, ParseResults<CommandSourceStack> value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, ParseResults<CommandSourceStack> value) {
        return value != null ? new ParseResultsValue(context, value) : Value.NULL;
    }

    public static ParseResults<CommandSourceStack> from(CarpetContext context, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case ParseResultsValue v -> v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to parse_results");
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "context" -> checkArguments(what, more, 0, () -> CommandContextBuilderValue.of(context, value.getContext()));
            case "string" -> checkArguments(what, more, 0, () -> StringValue.of(value.getReader().getString()));
            case "errors" -> checkArguments(what, more, 0, () -> MapValue.wrap(Map.ofEntries(value.getExceptions().entrySet().stream().map(v -> new AbstractMap.SimpleEntry<>(CommandNodeValue.of(context, v.getKey()), new CommandSyntaxError(context, v.getValue()))).toArray(Map.Entry[]::new))));
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "parse_results";
    }
}
