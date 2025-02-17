package me.itut.lanitium.value.brigadier;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import com.mojang.brigadier.context.CommandContext;
import me.itut.lanitium.Conversions;
import me.itut.lanitium.value.ContextValue;
import me.itut.lanitium.value.ObjectValue;
import me.itut.lanitium.value.Util;
import net.minecraft.commands.CommandSourceStack;

public class CommandContextValue extends ObjectValue<CommandContext<CommandSourceStack>> {
    protected CommandContextValue(CarpetContext context, CommandContext<CommandSourceStack> value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, CommandContext<CommandSourceStack> value) {
        return value != null ? new CommandContextValue(context, value) : Value.NULL;
    }

    public static CommandContext<CommandSourceStack> from(CarpetContext context, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case CommandContextValue v -> v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to command_context");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "copy_for" -> checkArguments(what, more, 1, () -> of(context, value.copyFor((ContextValue.fromOrCurrent(context, more[0])).source())));
            case "child" -> checkArguments(what, more, 0, () -> of(context, value.getChild()));
            case "last_child" -> checkArguments(what, more, 0, () -> of(context, value.getLastChild()));
            case "command" -> checkArguments(what, more, 0, () -> CommandValue.of(context, value.getCommand()));
            case "source" -> checkArguments(what, more, 1, () -> Util.source(ContextValue.fromOrCurrent(context, more[0]), value.getSource()));
            case "argument" -> checkArguments(what, more, 1, () -> Conversions.from(value.getArgument(more[0].getString(), Object.class)));
            case "redirect_modifier" -> checkArguments(what, more, 0, () -> RedirectModifierValue.of(context, value.getRedirectModifier()));
            case "range" -> checkArguments(what, more, 0, () -> Util.range(value.getRange()));
            case "input" -> checkArguments(what, more, 0, () -> StringValue.of(value.getInput()));
            case "root_node" -> checkArguments(what, more, 0, () -> CommandNodeValue.of(context, value.getRootNode()));
            case "nodes" -> checkArguments(what, more, 0, () -> ListValue.wrap(value.getNodes().stream().map(v -> ParsedCommandNodeValue.of(context, v))));
            case "forks" -> checkArguments(what, more, 0, () -> BooleanValue.of(value.isForked()));
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "command_context";
    }
}
