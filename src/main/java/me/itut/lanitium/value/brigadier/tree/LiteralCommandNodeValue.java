package me.itut.lanitium.value.brigadier.tree;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.BooleanValue;
import carpet.script.value.NullValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;

public class LiteralCommandNodeValue extends CommandNodeValue {
    protected LiteralCommandNodeValue(CarpetContext context, LiteralCommandNode<CommandSourceStack> value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, LiteralCommandNode<CommandSourceStack> value) {
        return value != null ? new LiteralCommandNodeValue(context, value) : Value.NULL;
    }

    public static LiteralCommandNode<CommandSourceStack> from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case LiteralCommandNodeValue v -> (LiteralCommandNode<CommandSourceStack>)v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to literal_command_node");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        final LiteralCommandNode<CommandSourceStack> value = (LiteralCommandNode<CommandSourceStack>)this.value;
        return switch (what) {
            case "literal" -> {
                checkArguments(what, more, 0);
                yield StringValue.of(value.getLiteral());
            }
            case "valid_input" -> {
                checkArguments(what, more, 1);
                yield BooleanValue.of(value.isValidInput(more[0].getString()));
            }
            default -> super.get(what, more);
        };
    }

    @Override
    public String getTypeString() {
        return "literal_command_node";
    }
}
