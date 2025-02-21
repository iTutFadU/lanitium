package me.itut.lanitium.value;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.IntegerSuggestion;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.commands.CommandSourceStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Util {
    public static final Value
        EYES = StringValue.of("eyes"),
        FEET = StringValue.of("feet"),
        RANGE = StringValue.of("range"),
        LIST = StringValue.of("list"),
        TEXT = StringValue.of("text"),
        TOOLTIP = StringValue.of("tooltip"),
        VALUE = StringValue.of("value");

    public static Value range(StringRange range) {
        return range != null ? ListValue.of(NumericValue.of(range.getStart()), NumericValue.of(range.getEnd())) : Value.NULL;
    }

    public static StringRange toRange(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case NumericValue number -> StringRange.at(number.getInt());
            case AbstractListValue list -> {
                List<Value> values = list.unpack();
                yield switch (values.size()) {
                    case 1 -> StringRange.at(NumericValue.asNumber(values.getFirst()).getInt());
                    case 2 -> StringRange.between(NumericValue.asNumber(values.getFirst()).getInt(), NumericValue.asNumber(values.get(1)).getInt());
                    default -> throw new InternalExpressionException("A range must be either a list with one or two elements, or a number");
                };
            }
            default -> throw new InternalExpressionException("A range must be either a list with one or two elements, or a number");
        };
    }

    public static char toChar(Value value) {
        return switch (value) {
            case null -> '\0';
            case NullValue ignored -> '\0';
            case NumericValue c -> (char)c.getInt();
            default -> {
                String str = value.getString();
                if (str.isEmpty())
                    throw new InternalExpressionException("Empty string cannot be used as a character");
                yield str.charAt(0);
            }
        };
    }

    public static Value source(CarpetContext context, CommandSourceStack source) {
        if (source == null) return Value.NULL;
        CarpetContext copy = (CarpetContext)context.recreate();
        copy.variables = context.variables;
        copy.swapSource(source);
        return ContextValue.of(copy);
    }

    public static List<Value> listFrom(Value value) {
        return switch (value) {
            case null -> List.of();
            case NullValue ignored -> List.of();
            case AbstractListValue list -> list.unpack();
            default -> List.of(value);
        };
    }

    public static class MessageValue extends SimpleFunctionValue {
        public final Message message;

        public MessageValue(Message message) {
            super((cc, tt) -> StringValue.of(message.getString()), List.of(), null);
            this.message = message;
        }
    }

    public static Value suggestions(Suggestions suggestions) {
        Map<Value, Value> map = new HashMap<>(2) {{
            put(RANGE, range(suggestions.getRange()));
            put(LIST, ListValue.wrap(suggestions.getList().stream().map(Util::suggestion)));
        }};
        return MapValue.wrap(map);
    }

    public static Suggestions toSuggestions(String command, Value value) {
        return switch (value) {
            case null -> Suggestions.empty().resultNow();
            case NullValue ignored -> Suggestions.empty().resultNow();
            case MapValue complex -> {
                Value list = complex.get(LIST);
                if (list.isNull()) yield Suggestions.create(command, toSuggestion(command.length(), value) instanceof Suggestion suggestion ? List.of(suggestion) : List.of());
                StringRange range = toRange(complex.get(RANGE));
                if (range == null) yield toSuggestions(command, list);
                yield new Suggestions(range, listFrom(list).stream().flatMap(v -> toSuggestion(command.length(), v) instanceof Suggestion suggestion ? Stream.of(suggestion) : Stream.empty()).toList());
            }
            case AbstractListValue list -> Suggestions.create(command, list.unpack().stream().map(v -> toSuggestion(command.length(), v)).toList());
            default -> Suggestions.create(command, toSuggestion(command.length(), value) instanceof Suggestion suggestion ? List.of(suggestion) : List.of());
        };
    }

    public static Value suggestion(Suggestion suggestion) {
        Map<Value, Value> map = new HashMap<>(4) {{
            put(RANGE, range(suggestion.getRange()));
            put(TEXT, StringValue.of(suggestion.getText()));
            if (suggestion.getTooltip() instanceof Message tooltip)
                put(TOOLTIP, new SimpleFunctionValue((cc, tt) -> StringValue.of(tooltip.getString()), List.of(), null));
            if (suggestion instanceof IntegerSuggestion integerSuggestion)
                put(VALUE, NumericValue.of(integerSuggestion.getValue()));
        }};
        return MapValue.wrap(map);
    }

    public static Suggestion toSuggestion(int cursor, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case NumericValue number -> {
                int suggest = number.getInt(), length = Integer.toString(suggest).length();
                yield new IntegerSuggestion(StringRange.between(cursor, cursor + length), suggest);
            }
            case MapValue complex -> {
                StringRange range = toRange(complex.get(RANGE));
                Message tooltip = switch (complex.get(TOOLTIP)) {
                    case NullValue ignored -> null;
                    case MessageValue message -> message.message;
                    case Value v -> v::getString;
                };
                if (complex.has(VALUE))
                    yield new IntegerSuggestion(range, NumericValue.asNumber(complex.get(VALUE)).getInt(), tooltip);
                yield new Suggestion(range, complex.get(TEXT).getString(), tooltip);
            }
            default -> {
                String string = value.getString();
                yield new Suggestion(StringRange.between(cursor, cursor + string.length()), string);
            }
        };
    }
}
