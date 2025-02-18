package me.itut.lanitium.value.brigadier;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.NullValue;
import carpet.script.value.Value;
import com.mojang.brigadier.context.ParsedArgument;
import me.itut.lanitium.Conversions;
import me.itut.lanitium.value.ObjectValue;
import me.itut.lanitium.value.Util;
import net.minecraft.commands.CommandSourceStack;

public class ParsedArgumentValue<T> extends ObjectValue<ParsedArgument<CommandSourceStack, T>> {
    protected ParsedArgumentValue(CarpetContext context, ParsedArgument<CommandSourceStack, T> value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, ParsedArgument<CommandSourceStack, ?> value) {
        return value != null ? new ParsedArgumentValue<>(context, value) : Value.NULL;
    }

    public static ParsedArgument<CommandSourceStack, ?> from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case ParsedArgumentValue<?> v -> v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to parsed_argument");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "range" -> {
                checkArguments(what, more, 0);
                yield Util.range(value.getRange());
            }
            case "result" -> {
                checkArguments(what, more, 0);
                yield Conversions.from(value.getResult());
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "parsed_argument";
    }
}
