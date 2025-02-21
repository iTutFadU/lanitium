package me.itut.lanitium.value.brigadier;

import carpet.fakes.CommandDispatcherInterface;
import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.itut.lanitium.value.ContextValue;
import me.itut.lanitium.value.ObjectValue;
import me.itut.lanitium.value.StringReaderValue;
import me.itut.lanitium.value.ValueConversions;
import me.itut.lanitium.value.brigadier.builder.LiteralArgumentBuilderValue;
import me.itut.lanitium.value.brigadier.function.AmbiguityConsumerValue;
import me.itut.lanitium.value.brigadier.function.ResultConsumerValue;
import me.itut.lanitium.value.brigadier.suggestion.SuggestionsFuture;
import me.itut.lanitium.value.brigadier.tree.CommandNodeValue;
import me.itut.lanitium.value.brigadier.tree.LiteralCommandNodeValue;
import me.itut.lanitium.value.brigadier.tree.RootCommandNodeValue;
import net.minecraft.commands.CommandSourceStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CommandDispatcherValue extends ObjectValue<CommandDispatcher<CommandSourceStack>> {
    protected CommandDispatcherValue(CarpetContext context, CommandDispatcher<CommandSourceStack> value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, CommandDispatcher<CommandSourceStack> value) {
        return value != null ? new CommandDispatcherValue(context, value) : Value.NULL;
    }

    public static CommandDispatcher<CommandSourceStack> from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case CommandDispatcherValue v -> v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to command_dispatcher");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "register" -> {
                checkArguments(what, more, 1);
                yield LiteralCommandNodeValue.of(context, value.register(LiteralArgumentBuilderValue.from(more[0])));
            }
            case "set_consumer" -> {
                checkArguments(what, more, 1);
                value.setConsumer(ResultConsumerValue.from(context, more[0]));
                yield Value.NULL;
            }
            case "execute" -> {
                checkArguments(what, more, 1, 2);
                try {
                    if (more.length > 1) yield NumericValue.of(value.execute(more[0].getString(), ContextValue.fromOrCurrent(context, more[1]).source()));
                    yield NumericValue.of(value.execute(ParseResultsValue.from(more[0])));
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(context, e);
                }
            }
            case "parse" -> {
                checkArguments(what, more, 2);
                yield ParseResultsValue.of(context, value.parse(StringReaderValue.from(more[0]), ContextValue.fromOrCurrent(context, more[1]).source()));
            }
            case "all_usage" -> {
                checkArguments(what, more, 3);
                yield ListValue.wrap(Arrays.stream(value.getAllUsage(CommandNodeValue.from(more[0]), ContextValue.fromOrCurrent(context, more[1]).source(), more[2].getBoolean())).map(StringValue::of));
            }
            case "smart_usage" -> {
                checkArguments(what, more, 2);
                Map<Value, Value> map = new HashMap<>();
                value.getSmartUsage(CommandNodeValue.from(more[0]), ContextValue.fromOrCurrent(context, more[1]).source()).forEach((k, v) -> map.put(CommandNodeValue.of(context, k), StringValue.of(v)));
                yield MapValue.wrap(map);
            }
            case "completion_suggestions" -> {
                checkArguments(what, more, 1, 2);
                yield SuggestionsFuture.of(context, more.length > 1 ? value.getCompletionSuggestions(ParseResultsValue.from(more[0]), NumericValue.asNumber(more[1]).getInt()) : value.getCompletionSuggestions(ParseResultsValue.from(more[0])));
            }
            case "root" -> {
                checkArguments(what, more, 0);
                yield RootCommandNodeValue.of(context, value.getRoot());
            }
            case "path" -> {
                checkArguments(what, more, 1);
                yield ListValue.wrap(value.getPath(CommandNodeValue.from(more[0])).stream().map(StringValue::of));
            }
            case "find_node" -> {
                checkArguments(what, more, 1);
                yield CommandNodeValue.of(context, value.findNode(ValueConversions.listFrom(more[0]).stream().map(Value::getString).toList()));
            }
            case "find_ambiguities" -> {
                checkArguments(what, more, 1);
                value.findAmbiguities(AmbiguityConsumerValue.from(context, more[0]));
                yield Value.NULL;
            }
            case "unregister" -> {
                checkArguments(what, more, 1);
                ((CommandDispatcherInterface)value).carpet$unregister(more[0].getString());
                yield Value.NULL;
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "command_dispatcher";
    }
}
