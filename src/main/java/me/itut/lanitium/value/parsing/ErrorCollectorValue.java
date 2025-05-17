package me.itut.lanitium.value.parsing;

import carpet.script.Context;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.NullValue;
import carpet.script.value.NumericValue;
import carpet.script.value.Value;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.itut.lanitium.value.ObjectValue;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.ErrorCollector;

public class ErrorCollectorValue extends ObjectValue<ErrorCollector<StringReader>> {
    private final Context context;

    protected ErrorCollectorValue(Context context, ErrorCollector<StringReader> value) {
        super(value);
        this.context = context;
    }

    public static Value of(Context context, ErrorCollector<StringReader> value) {
        return value != null ? new ErrorCollectorValue(context, value) : Value.NULL;
    }

    public static ErrorCollector<StringReader> from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case ErrorCollectorValue collector -> collector.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to parsing_error_collector");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "store" -> {
                checkArguments(what, more, 2, 3);
                int i = NumericValue.asNumber(more[0]).getInt();
                DelayedException<?> error = DelayedException.create(new SimpleCommandExceptionType(FormattedTextValue.getTextByValue(more[more.length - 1])));
                if (more.length == 3) value.store(i, SuggestionSupplierValue.from(context, more[1]), error);
                else value.store(i, error);
                yield Value.NULL;
            }
            case "finish" -> {
                checkArguments(what, more, 1);
                value.finish(NumericValue.asNumber(more[0]).getInt());
                yield Value.NULL;
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "parsing_error_collector";
    }
}
