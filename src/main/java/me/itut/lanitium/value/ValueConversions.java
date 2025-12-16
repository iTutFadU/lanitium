package me.itut.lanitium.value;

import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.value.*;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.IntegerSuggestion;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.itut.lanitium.function.Apply;
import me.itut.lanitium.value.parsing.StringReaderValue;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValueConversions {
    public static ThrowStatement commandSyntaxException(CommandSyntaxException e) {
        StringReader reader = new StringReader(e.getInput());
        reader.setCursor(e.getCursor());
        return new ThrowStatement(ListValue.of(StringReaderValue.of(reader), message(e.getRawMessage())), Apply.COMMAND_SYNTAX_EXCEPTION);
    }

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

    public static Value message(Message msg) {
        return msg instanceof Component c ? FormattedTextValue.of(c) : StringValue.of(msg.getString());
    }

    public static Value aabb(AABB aabb) {
        return ListValue.of(ListValue.fromTriple(aabb.minX, aabb.minY, aabb.minZ), ListValue.fromTriple(aabb.maxX, aabb.maxY, aabb.maxZ));
    }

    public static AABB toAabb(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            default -> {
                if (value instanceof AbstractListValue list) {
                    List<Value> values = list.unpack();
                    if (values.size() == 2 && values.getFirst() instanceof AbstractListValue min && values.getLast() instanceof AbstractListValue max) {
                        List<Value> minValues = min.unpack(), maxValues = max.unpack();
                        if (minValues.size() == 3 && maxValues.size() == 3) yield new AABB(
                            NumericValue.asNumber(minValues.get(0)).getDouble(),
                            NumericValue.asNumber(minValues.get(1)).getDouble(),
                            NumericValue.asNumber(minValues.get(2)).getDouble(),
                            NumericValue.asNumber(maxValues.get(0)).getDouble(),
                            NumericValue.asNumber(maxValues.get(1)).getDouble(),
                            NumericValue.asNumber(maxValues.get(2)).getDouble()
                        );
                    }
                }
                throw new InternalExpressionException("An AABB must be a list of two triples of numbers");
            }
        };
    }
}
