package me.itut.lanitium.value;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import com.mojang.brigadier.context.StringRange;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

public final class Util {
    private Util() {}

    public static Value range(StringRange range) {
        return range != null ? ListValue.of(NumericValue.of(range.getStart()), NumericValue.of(range.getEnd())) : Value.NULL;
    }

    public static StringRange toRange(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case AbstractListValue list -> {
                List<Value> values = list.unpack();
                yield switch (values.size()) {
                    case 1 -> StringRange.at(NumericValue.asNumber(values.getFirst()).getInt());
                    case 2 -> StringRange.between(NumericValue.asNumber(values.getFirst()).getInt(), NumericValue.asNumber(values.get(1)).getInt());
                    default -> throw new InternalExpressionException("A range must be either a list with one or two elements, or a number");
                };
            }
            case NumericValue number -> StringRange.at(number.getInt());
            default -> throw new InternalExpressionException("A range must be either a list with one or two elements, or a number");
        };
    }

    public static Value source(CarpetContext context, CommandSourceStack source) {
        if (source == null) return Value.NULL;
        CarpetContext copy = context.duplicate();
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
}
