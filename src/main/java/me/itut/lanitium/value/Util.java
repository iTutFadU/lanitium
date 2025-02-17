package me.itut.lanitium.value;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import com.mojang.brigadier.context.StringRange;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

public abstract class Util {
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
                if (values.size() == 2)
                    yield new StringRange(NumericValue.asNumber(values.getFirst()).getInt(), NumericValue.asNumber(values.get(1)).getInt());
                throw new InternalExpressionException("A string range must be a list with two elements");
            }
            default -> throw new InternalExpressionException("A string range must be a list with two elements");
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
