package me.itut.lanitium;

import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import net.minecraft.network.chat.Component;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class Conversions {
    private Conversions() {}

    public static Value from(Object object) throws InternalExpressionException {
        return switch (object) {
            case null -> Value.NULL;
            case Value value -> value;
            case Collection<?> list -> ListValue.wrap(list.stream().map(Conversions::from));
            case Map<?, ?> map -> {
                Map<Value, Value> converted = new HashMap<>();
                map.forEach((k, v) -> converted.put(from(k), from(v)));
                yield MapValue.wrap(converted);
            }
            case String string -> StringValue.of(string);
            case Character character -> StringValue.of(character.toString());
            case Number number -> NumericValue.of(number);
            case Boolean bool -> BooleanValue.of(bool);
            case Component component -> FormattedTextValue.of(component);
            default -> throw new InternalExpressionException("Cannot convert from class " + object.getClass().getSimpleName() + " (report this issue to Lanitium)");
        };
    }
}
