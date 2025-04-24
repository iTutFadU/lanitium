package me.itut.lanitium.value;

import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.IntegerSuggestion;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValueConversions {
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
        return ListValue.wrap(suggestions.getList().stream().map(ValueConversions::suggestion));
    }

    public static Suggestions toSuggestions(SuggestionsBuilder builder, Value value) {
        String command = builder.getInput();
        int start = builder.getStart(), length = command.length() - start;
        return switch (value) {
            case null -> Suggestions.empty().resultNow();
            case NullValue ignored -> Suggestions.empty().resultNow();
            case AbstractListValue list -> Suggestions.create(command, list.unpack().stream().map(v -> toSuggestion(start, length, v)).toList());
            default -> Suggestions.create(command, toSuggestion(start, length, value) instanceof Suggestion suggestion ? List.of(suggestion) : List.of());
        };
    }

    public static Value suggestion(Suggestion suggestion) {
        Map<Value, Value> map = new HashMap<>() {{
            put(Constants.RANGE, range(suggestion.getRange()));
            put(Constants.TEXT, StringValue.of(suggestion.getText()));
            if (suggestion.getTooltip() instanceof Message tooltip)
                put(Constants.TOOLTIP, new SimpleFunctionValue(0, 0, (c, t, e, tok, lv) -> StringValue.of(tooltip.getString())));
            if (suggestion instanceof IntegerSuggestion integerSuggestion)
                put(Constants.VALUE, NumericValue.of(integerSuggestion.getValue()));
        }};
        return MapValue.wrap(map);
    }

    public static Suggestion toSuggestion(int start, int length, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case NumericValue number -> new IntegerSuggestion(StringRange.between(start, start + length), number.getInt());
            case MapValue complex -> {
                StringRange rawRange = toRange(complex.get(Constants.RANGE)), range = rawRange != null ? StringRange.between(start + rawRange.getStart(), start + rawRange.getEnd()) : StringRange.between(start, start + length);
                Message tooltip = complex.has(Constants.TOOLTIP) ? FormattedTextValue.getTextByValue(complex.get(Constants.TOOLTIP)) : null;
                if (complex.has(Constants.VALUE))
                    yield new IntegerSuggestion(range, NumericValue.asNumber(complex.get(Constants.VALUE)).getInt(), tooltip);
                yield new Suggestion(range, complex.get(Constants.TEXT).getString(), tooltip);
            }
            default -> new Suggestion(StringRange.between(start, start + length), value.getString());
        };
    }
}
