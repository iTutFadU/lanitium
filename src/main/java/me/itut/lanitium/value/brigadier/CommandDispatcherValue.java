package me.itut.lanitium.value.brigadier;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.itut.lanitium.value.ContextValue;
import me.itut.lanitium.value.ObjectValue;
import me.itut.lanitium.value.Util;
import net.minecraft.commands.CommandSourceStack;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;

public class CommandDispatcherValue extends ObjectValue<CommandDispatcher<CommandSourceStack>> {
    protected CommandDispatcherValue(CarpetContext context, CommandDispatcher<CommandSourceStack> value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, CommandDispatcher<CommandSourceStack> value) {
        return value != null ? new CommandDispatcherValue(context, value) : Value.NULL;
    }

    public static CommandDispatcher<CommandSourceStack> from(CarpetContext context, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case CommandDispatcherValue v -> v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to command_dispatcher");
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "register" -> checkArguments(what, more, 1, () -> LiteralCommandNodeValue.of(context, value.register(LiteralArgumentBuilderValue.from(context, more[0]))));
            case "set_consumer" -> {
                checkArguments(what, more, 1);
                value.setConsumer(ResultConsumerValue.from(context, more[0]));
                yield Value.NULL;
            }
            case "execute" -> {
                checkArguments(what, more, 1, 2);
                try {
                    if (more.length > 1) yield NumericValue.of(value.execute(more[0].getString(), ContextValue.fromOrCurrent(context, more[1]).source()));
                    yield NumericValue.of(value.execute(ParseResultsValue.from(context, more[0])));
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(context, e);
                }
            }
            case "parse" -> checkArguments(what, more, 2, () -> ParseResultsValue.of(context, value.parse(more[0].getString(), ContextValue.fromOrCurrent(context, more[1]).source())));
            case "all_usage" -> checkArguments(what, more, 3, () -> ListValue.wrap(Arrays.stream(value.getAllUsage(CommandNodeValue.from(context, more[0]), ContextValue.fromOrCurrent(context, more[1]).source(), more[2].getBoolean())).map(StringValue::of)));
            case "smart_usage" -> checkArguments(what, more, 2, () -> MapValue.wrap(Map.ofEntries(value.getSmartUsage(CommandNodeValue.from(context, more[0]), ContextValue.fromOrCurrent(context, more[1]).source()).entrySet().stream().map(v -> new AbstractMap.SimpleEntry<>(CommandNodeValue.of(context, v.getKey()), StringValue.of(v.getValue()))).toArray(Map.Entry[]::new))));
            case "completion_suggestions" -> checkArguments(what, more, 1, 2, () -> SuggestionsFuture.of(context, more.length > 1 ? value.getCompletionSuggestions(ParseResultsValue.from(context, more[0]), NumericValue.asNumber(more[1]).getInt()) : value.getCompletionSuggestions(ParseResultsValue.from(context, more[0]))));
            case "root" -> checkArguments(what, more, 0, () -> RootCommandNodeValue.of(context, value.getRoot()));
            case "path" -> checkArguments(what, more, 1, () -> ListValue.wrap(value.getPath(CommandNodeValue.from(context, more[0])).stream().map(StringValue::of)));
            case "find_node" -> checkArguments(what, more, 1, () -> CommandNodeValue.of(context, value.findNode(Util.listFrom(more[0]).stream().map(Value::getString).toList())));
            case "find_ambiguities" -> {
                checkArguments(what, more, 1);
                value.findAmbiguities(AmbiguityConsumerValue.from(context, more[0]));
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
