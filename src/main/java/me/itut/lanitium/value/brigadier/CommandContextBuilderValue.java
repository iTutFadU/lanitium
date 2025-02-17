package me.itut.lanitium.value.brigadier;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.MapValue;
import carpet.script.value.NullValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.mojang.brigadier.context.CommandContextBuilder;
import me.itut.lanitium.value.ContextValue;
import me.itut.lanitium.value.ObjectValue;
import me.itut.lanitium.value.Util;
import net.minecraft.commands.CommandSourceStack;

import java.util.AbstractMap;
import java.util.Map;

public class CommandContextBuilderValue extends ObjectValue<CommandContextBuilder<CommandSourceStack>> {
    protected CommandContextBuilderValue(CarpetContext context, CommandContextBuilder<CommandSourceStack> value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, CommandContextBuilder<CommandSourceStack> value) {
        return value != null ? new CommandContextBuilderValue(context, value) : Value.NULL;
    }

    public static CommandContextBuilder<CommandSourceStack> from(CarpetContext context, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case CommandContextBuilderValue v -> v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to command_context_builder");
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "with_source" -> checkArguments(what, more, 1, () -> of(context, value.withSource(ContextValue.fromOrCurrent(context, more[0]).source())));
            case "source" -> checkArguments(what, more, 0, () -> Util.source(context, value.getSource()));
            case "root_node" -> checkArguments(what, more, 0, () -> CommandNodeValue.of(context, value.getRootNode()));
            case "with_argument" -> {
                checkArguments(what, more, 2);
                value.withArgument(more[0].getString(), ParsedArgumentValue.from(context, more[1]));
                yield this;
            }
            case "arguments" -> checkArguments(what, more, 0, () -> MapValue.wrap(Map.ofEntries(value.getArguments().entrySet().stream().map(v -> new AbstractMap.SimpleEntry<>(StringValue.of(v.getKey()), ParsedArgumentValue.of(context, v.getValue()))).toArray(Map.Entry[]::new))));
            case "with_command" -> {
                checkArguments(what, more, 1);
                value.withCommand(CommandValue.from(context, more[0]));
                yield this;
            }
            case "with_node" -> {
                checkArguments(what, more, 2);
                value.withNode(CommandNodeValue.from(context, more[0]), Util.toRange(more[1]));
                yield this;
            }
            case "copy" -> checkArguments(what, more, 0, () -> of(context, value.copy()));
            case "with_child" -> {
                checkArguments(what, more, 1);
                value.withChild(CommandContextBuilderValue.from(context, more[0]));
                yield this;
            }
            case "child" -> checkArguments(what, more, 0, () -> of(context, value.getChild()));
            case "last_child" -> checkArguments(what, more, 0, () -> of(context, value.getLastChild()));
            case "build" -> checkArguments(what, more, 1, () -> CommandContextValue.of(context, value.build(more[0].getString())));
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "command_context_builder";
    }
}
