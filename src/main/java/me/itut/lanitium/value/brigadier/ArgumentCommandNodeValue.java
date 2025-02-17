package me.itut.lanitium.value.brigadier;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.BooleanValue;
import carpet.script.value.NullValue;
import carpet.script.value.Value;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import net.minecraft.commands.CommandSourceStack;

public class ArgumentCommandNodeValue<T> extends CommandNodeValue {
    protected ArgumentCommandNodeValue(CarpetContext context, ArgumentCommandNode<CommandSourceStack, T> value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, ArgumentCommandNode<CommandSourceStack, ?> value) {
        return value != null ? new ArgumentCommandNodeValue<>(context, value) : Value.NULL;
    }

    public static ArgumentCommandNode<CommandSourceStack, ?> from(CarpetContext context, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case ArgumentCommandNodeValue<?> v -> (ArgumentCommandNode<CommandSourceStack, ?>)v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to argument_command_node");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        final ArgumentCommandNode<CommandSourceStack, T> value = (ArgumentCommandNode<CommandSourceStack, T>)this.value;
        return switch (what) {
            case "type" -> checkArguments(what, more, 0, () -> ArgumentTypeValue.of(context, value.getType()));
            case "custom_suggestions" -> checkArguments(what, more, 0, () -> SuggestionProviderValue.of(context, value.getCustomSuggestions()));
            case "valid_input" -> checkArguments(what, more, 1, () -> BooleanValue.of(value.isValidInput(more[0].getString())));
            default -> super.get(what, more);
        };
    }

    @Override
    public String getTypeString() {
        return "argument_command_node";
    }
}
