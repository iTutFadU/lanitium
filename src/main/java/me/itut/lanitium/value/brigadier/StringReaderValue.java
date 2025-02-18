package me.itut.lanitium.value.brigadier;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.itut.lanitium.value.ObjectValue;

public class StringReaderValue extends ObjectValue<StringReader> {
    protected StringReaderValue(CarpetContext context, StringReader value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, StringReader value) {
        return value != null ? new StringReaderValue(context, value) : Value.NULL;
    }

    public static StringReader from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case StringReaderValue v -> v.value;
            default -> new StringReader(value.getString());
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "string" -> {
                checkArguments(what, more, 0);
                yield StringValue.of(value.getString());
            }
            case "set_cursor" -> {
                checkArguments(what, more, 1);
                value.setCursor(NumericValue.asNumber(more[0]).getInt());
                yield Value.NULL;
            }
            case "remaining_length" -> {
                checkArguments(what, more, 0);
                yield NumericValue.of(value.getRemainingLength());
            }
            case "total_length" -> {
                checkArguments(what, more, 0);
                yield NumericValue.of(value.getTotalLength());
            }
            case "cursor" -> {
                checkArguments(what, more, 0);
                yield NumericValue.of(value.getCursor());
            }
            case "get_read" -> {
                checkArguments(what, more, 0);
                yield StringValue.of(value.getRead());
            }
            case "get_remaining" -> {
                checkArguments(what, more, 0);
                yield StringValue.of(value.getRemaining());
            }
            case "can_read" -> {
                checkArguments(what, more, 0, 1);
                yield BooleanValue.of(value.canRead(more.length > 0 ? NumericValue.asNumber(more[0]).getInt() : 1));
            }
            case "peek" -> {
                checkArguments(what, more, 0, 1);
                yield StringValue.of(String.valueOf(value.peek(more.length > 0 ? NumericValue.asNumber(more[0]).getInt() : 0)));
            }
            case "read" -> {
                checkArguments(what, more, 0);
                yield StringValue.of(String.valueOf(value.read()));
            }
            case "skip" -> {
                checkArguments(what, more, 0);
                value.skip();
                yield Value.NULL;
            }
            case "skip_whitespace" -> {
                checkArguments(what, more, 0);
                value.skipWhitespace();
                yield Value.NULL;
            }
            case "read_int" -> {
                checkArguments(what, more, 0);
                try {
                    yield NumericValue.of(value.readInt());
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(context, e);
                }
            }
            case "read_long" -> {
                checkArguments(what, more, 0);
                try {
                    yield NumericValue.of(value.readLong());
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(context, e);
                }
            }
            case "read_double" -> {
                checkArguments(what, more, 0);
                try {
                    yield NumericValue.of(value.readDouble());
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(context, e);
                }
            }
            case "read_float" -> {
                checkArguments(what, more, 0);
                try {
                    yield NumericValue.of(value.readFloat());
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(context, e);
                }
            }
            case "read_unquoted_string" -> {
                checkArguments(what, more, 0);
                yield StringValue.of(value.readUnquotedString());
            }
            case "read_quoted_string" -> {
                checkArguments(what, more, 0);
                try {
                    yield StringValue.of(value.readQuotedString());
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(context, e);
                }
            }
            case "read_string_until" -> {
                checkArguments(what, more, 1);
                String terminator = more[0].getString();
                if (terminator.isEmpty()) throw new InternalExpressionException("Empty string cannot be used as a character");
                try {
                    yield StringValue.of(value.readStringUntil(terminator.charAt(0)));
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(context, e);
                }
            }
            case "read_string" -> {
                checkArguments(what, more, 0);
                try {
                    yield StringValue.of(value.readString());
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(context, e);
                }
            }
            case "read_boolean" -> {
                checkArguments(what, more, 0);
                try {
                    yield BooleanValue.of(value.readBoolean());
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(context, e);
                }
            }
            case "expect" -> {
                checkArguments(what, more, 1);
                String expect = more[0].getString();
                if (expect.isEmpty()) throw new InternalExpressionException("Empty string cannot be used as a character");
                try {
                    value.expect(expect.charAt(0));
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(context, e);
                }
                yield Value.NULL;
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "string_reader";
    }
}
