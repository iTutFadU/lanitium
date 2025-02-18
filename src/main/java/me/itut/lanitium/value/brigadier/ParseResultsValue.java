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

import java.util.HashMap;
import java.util.Map;

public class ParseResultsValue extends ObjectValue<ParseResults<CommandSourceStack>> {
    protected ParseResultsValue(CarpetContext context, ParseResults<CommandSourceStack> value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, ParseResults<CommandSourceStack> value) {
        return value != null ? new ParseResultsValue(context, value) : Value.NULL;
    }

    public static ParseResults<CommandSourceStack> from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case ParseResultsValue v -> v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to parse_results");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "context" -> {
                checkArguments(what, more, 0);
                yield CommandContextBuilderValue.of(context, value.getContext());
            }
            case "string" -> {
                checkArguments(what, more, 0);
                yield StringValue.of(value.getReader().getString());
            }
            case "errors" -> {
                checkArguments(what, more, 0);
                Map<Value, Value> map = new HashMap<>();
                value.getExceptions().forEach((k, v) -> map.put(CommandNodeValue.of(context, k), new CommandSyntaxError(context, v)));
                yield MapValue.wrap(map);
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "parse_results";
    }
}
