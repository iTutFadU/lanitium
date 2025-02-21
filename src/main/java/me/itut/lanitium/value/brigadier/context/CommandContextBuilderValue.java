package me.itut.lanitium.value.brigadier.context;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import com.mojang.brigadier.context.CommandContextBuilder;
import me.itut.lanitium.value.ContextValue;
import me.itut.lanitium.value.ObjectValue;
import me.itut.lanitium.value.ValueConversions;
import me.itut.lanitium.value.brigadier.CommandDispatcherValue;
import me.itut.lanitium.value.brigadier.function.CommandValue;
import me.itut.lanitium.value.brigadier.tree.CommandNodeValue;
import net.minecraft.commands.CommandSourceStack;

import java.util.HashMap;
import java.util.Map;

public class CommandContextBuilderValue extends ObjectValue<CommandContextBuilder<CommandSourceStack>> {
    protected CommandContextBuilderValue(CarpetContext context, CommandContextBuilder<CommandSourceStack> value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, CommandContextBuilder<CommandSourceStack> value) {
        return value != null ? new CommandContextBuilderValue(context, value) : Value.NULL;
    }

    public static CommandContextBuilder<CommandSourceStack> from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case CommandContextBuilderValue v -> v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to command_context_builder");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "with_source" -> {
                checkArguments(what, more, 1);
                value.withSource(ContextValue.fromOrCurrent(context, more[0]).source());
                yield this;
            }
            case "source" -> {
                checkArguments(what, more, 0);
                yield ValueConversions.source(context, value.getSource());
            }
            case "root_node" -> {
                checkArguments(what, more, 0);
                yield CommandNodeValue.of(context, value.getRootNode());
            }
            case "with_argument" -> {
                checkArguments(what, more, 2);
                value.withArgument(more[0].getString(), ParsedArgumentValue.from(more[1]));
                yield this;
            }
            case "arguments" -> {
                checkArguments(what, more, 0);
                Map<Value, Value> map = new HashMap<>();
                value.getArguments().forEach((k, v) -> map.put(StringValue.of(k), ParsedArgumentValue.of(context, v)));
                yield MapValue.wrap(map);
            }
            case "with_command" -> {
                checkArguments(what, more, 1);
                value.withCommand(CommandValue.from(context, more[0]));
                yield this;
            }
            case "with_node" -> {
                checkArguments(what, more, 2);
                value.withNode(CommandNodeValue.from(more[0]), ValueConversions.toRange(more[1]));
                yield this;
            }
            case "copy" -> {
                checkArguments(what, more, 0);
                yield of(context, value.copy());
            }
            case "with_child" -> {
                checkArguments(what, more, 1);
                value.withChild(CommandContextBuilderValue.from(more[0]));
                yield this;
            }
            case "child" -> {
                checkArguments(what, more, 0);
                yield of(context, value.getChild());
            }
            case "last_child" -> {
                checkArguments(what, more, 0);
                yield of(context, value.getLastChild());
            }
            case "build" -> {
                checkArguments(what, more, 1);
                yield CommandContextValue.of(context, value.build(more[0].getString()));
            }
            case "dispatcher" -> {
                checkArguments(what, more, 0);
                yield CommandDispatcherValue.of(context, value.getDispatcher());
            }
            case "range" -> {
                checkArguments(what, more, 0);
                yield ValueConversions.range(value.getRange());
            }
            case "find_suggestion_context" -> {
                checkArguments(what, more, 1);
                yield SuggestionContextValue.of(context, value.findSuggestionContext(NumericValue.asNumber(more[0]).getInt()));
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "command_context_builder";
    }
}
