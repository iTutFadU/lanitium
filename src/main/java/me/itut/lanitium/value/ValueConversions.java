package me.itut.lanitium.value;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.IntegerSuggestion;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ValueConversions {
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

    public static List<Value> listFrom(Value value) {
        return switch (value) {
            case null -> List.of();
            case NullValue ignored -> List.of();
            case AbstractListValue list -> list.unpack();
            default -> List.of(value);
        };
    }

    public static Value suggestions(Suggestions suggestions) {
        Map<Value, Value> map = new HashMap<>(2) {{
            put(RANGE, range(suggestions.getRange()));
            put(LIST, ListValue.wrap(suggestions.getList().stream().map(ValueConversions::suggestion)));
        }};
        return MapValue.wrap(map);
    }

    public static Suggestions toSuggestions(SuggestionsBuilder builder, Value value) {
        String command = builder.getInput();
        int start = builder.getStart(), length = command.length() - start;
        return switch (value) {
            case null -> Suggestions.empty().resultNow();
            case NullValue ignored -> Suggestions.empty().resultNow();
            case MapValue complex -> {
                Value list = complex.get(LIST);
                if (list.isNull()) yield Suggestions.create(command, toSuggestion(start, length, value) instanceof Suggestion suggestion ? List.of(suggestion) : List.of());
                StringRange range = toRange(complex.get(RANGE));
                if (range == null) yield toSuggestions(builder, list);
                yield new Suggestions(range, listFrom(list).stream().flatMap(v -> toSuggestion(start, length, v) instanceof Suggestion suggestion ? Stream.of(suggestion) : Stream.empty()).toList());
            }
            case AbstractListValue list -> Suggestions.create(command, list.unpack().stream().map(v -> toSuggestion(start, length, v)).toList());
            default -> Suggestions.create(command, toSuggestion(start, length, value) instanceof Suggestion suggestion ? List.of(suggestion) : List.of());
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

    public static Suggestion toSuggestion(int start, int length, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case NumericValue number -> new IntegerSuggestion(StringRange.between(start, start + length), number.getInt());
            case MapValue complex -> {
                StringRange rawRange = toRange(complex.get(RANGE)), range = rawRange != null ? StringRange.between(start + rawRange.getStart(), start + rawRange.getEnd()) : StringRange.between(start, start + length);
                Message tooltip = complex.has(TOOLTIP) ? FormattedTextValue.getTextByValue(complex.get(TOOLTIP)) : null;
                if (complex.has(VALUE))
                    yield new IntegerSuggestion(range, NumericValue.asNumber(complex.get(VALUE)).getInt(), tooltip);
                yield new Suggestion(range, complex.get(TEXT).getString(), tooltip);
            }
            default -> new Suggestion(StringRange.between(start, start + length), value.getString());
        };
    }
}
