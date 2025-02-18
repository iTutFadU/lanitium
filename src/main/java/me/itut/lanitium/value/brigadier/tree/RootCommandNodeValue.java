package me.itut.lanitium.value.brigadier.tree;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.NullValue;
import carpet.script.value.Value;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.CommandSourceStack;

public class RootCommandNodeValue extends CommandNodeValue {
    protected RootCommandNodeValue(CarpetContext context, RootCommandNode<CommandSourceStack> value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, RootCommandNode<CommandSourceStack> value) {
        return value != null ? new RootCommandNodeValue(context, value) : Value.NULL;
    }

    public static RootCommandNode<CommandSourceStack> from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case RootCommandNodeValue v -> (RootCommandNode<CommandSourceStack>)v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to root_command_node");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "create_builder" -> unknownFeature(what);
            default -> super.get(what, more);
        };
    }

    @Override
    public String getTypeString() {
        return "root_command_node";
    }
}
