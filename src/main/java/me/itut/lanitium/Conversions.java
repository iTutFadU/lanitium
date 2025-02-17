package me.itut.lanitium;

import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;

public abstract class Conversions {
    private Conversions() {}

    @SuppressWarnings("unchecked")
    public static Value from(Object object) throws InternalExpressionException {
        return switch (object) {
            case null -> Value.NULL;
            case Value value -> value;
            case Collection<?> list -> ListValue.wrap(list.stream().map(Conversions::from));
            case Map<?, ?> map -> MapValue.wrap(Map.ofEntries(map.entrySet().stream().map(v -> new AbstractMap.SimpleEntry<>(from(v.getKey()), from(v.getValue()))).toArray(Map.Entry[]::new)));
            case String string -> StringValue.of(string);
            case Character character -> StringValue.of(character.toString());
            case Number number -> NumericValue.of(number);
            case Boolean bool -> BooleanValue.of(bool);
            default -> throw new InternalExpressionException("Cannot convert from class " + object.getClass().getSimpleName() + " (report this issue to Lanitium)");
        };
    }
}
