package me.itut.lanitium.value.brigadier.context;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.NullValue;
import carpet.script.value.Value;
import com.mojang.brigadier.context.ParsedCommandNode;
import me.itut.lanitium.value.ObjectValue;
import me.itut.lanitium.value.ValueConversions;
import me.itut.lanitium.value.brigadier.tree.CommandNodeValue;
import net.minecraft.commands.CommandSourceStack;

public class ParsedCommandNodeValue extends ObjectValue<ParsedCommandNode<CommandSourceStack>> {
    protected ParsedCommandNodeValue(CarpetContext context, ParsedCommandNode<CommandSourceStack> value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, ParsedCommandNode<CommandSourceStack> value) {
        return value != null ? new ParsedCommandNodeValue(context, value) : Value.NULL;
    }

    public static ParsedCommandNode<CommandSourceStack> from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case ParsedCommandNodeValue v -> v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to parsed_command_node");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "node" -> {
                checkArguments(what, more, 0);
                yield CommandNodeValue.of(context, value.getNode());
            }
            case "range" -> {
                checkArguments(what, more, 0);
                yield ValueConversions.range(value.getRange());
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "parsed_command_node";
    }
}
