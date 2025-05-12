package me.itut.lanitium.value;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.TagParser;

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
                    yield Value.NULL;
                }
            }
            case "read_long" -> {
                checkArguments(what, more, 0);
                try {
                    yield NumericValue.of(value.readLong());
                } catch (CommandSyntaxException e) {
                    yield Value.NULL;
                }
            }
            case "read_double" -> {
                checkArguments(what, more, 0);
                try {
                    yield NumericValue.of(value.readDouble());
                } catch (CommandSyntaxException e) {
                    yield Value.NULL;
                }
            }
            case "read_float" -> {
                checkArguments(what, more, 0);
                try {
                    yield NumericValue.of(value.readFloat());
                } catch (CommandSyntaxException e) {
                    yield Value.NULL;
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
                    yield Value.NULL;
                }
            }
            case "read_string_until" -> {
                checkArguments(what, more, 1);
                try {
                    yield StringValue.of(value.readStringUntil(ValueConversions.toChar(more[0])));
                } catch (CommandSyntaxException e) {
                    yield Value.NULL;
                }
            }
            case "read_string" -> {
                checkArguments(what, more, 0);
                try {
                    yield StringValue.of(value.readString());
                } catch (CommandSyntaxException e) {
                    yield Value.NULL;
                }
            }
            case "read_boolean" -> {
                checkArguments(what, more, 0);
                try {
                    yield BooleanValue.of(value.readBoolean());
                } catch (CommandSyntaxException e) {
                    yield Value.NULL;
                }
            }
            case "read_nbt" -> {
                checkArguments(what, more, 0);
                try {
                    yield NBTSerializableValue.of(TagParser.NBT_OPS_PARSER.parseAsArgument(value));
                } catch (CommandSyntaxException e) {
                    yield Value.NULL;
                }
            }
            case "read_compound" -> {
                checkArguments(what, more, 0);
                try {
                    yield NBTSerializableValue.of(TagParser.parseCompoundAsArgument(value));
                } catch (CommandSyntaxException e) {
                    yield Value.NULL;
                }
            }
            case "read_int_or_panic" -> {
                checkArguments(what, more, 0);
                try {
                    yield NumericValue.of(value.readInt());
                } catch (CommandSyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            case "read_long_or_panic" -> {
                checkArguments(what, more, 0);
                try {
                    yield NumericValue.of(value.readLong());
                } catch (CommandSyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            case "read_double_or_panic" -> {
                checkArguments(what, more, 0);
                try {
                    yield NumericValue.of(value.readDouble());
                } catch (CommandSyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            case "read_float_or_panic" -> {
                checkArguments(what, more, 0);
                try {
                    yield NumericValue.of(value.readFloat());
                } catch (CommandSyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            case "read_quoted_string_or_panic" -> {
                checkArguments(what, more, 0);
                try {
                    yield StringValue.of(value.readQuotedString());
                } catch (CommandSyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            case "read_string_until_or_panic" -> {
                checkArguments(what, more, 1);
                try {
                    yield StringValue.of(value.readStringUntil(ValueConversions.toChar(more[0])));
                } catch (CommandSyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            case "read_string_or_panic" -> {
                checkArguments(what, more, 0);
                try {
                    yield StringValue.of(value.readString());
                } catch (CommandSyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            case "read_boolean_or_panic" -> {
                checkArguments(what, more, 0);
                try {
                    yield BooleanValue.of(value.readBoolean());
                } catch (CommandSyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            case "read_nbt_or_panic" -> {
                checkArguments(what, more, 0);
                try {
                    yield NBTSerializableValue.of(TagParser.NBT_OPS_PARSER.parseAsArgument(value));
                } catch (CommandSyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            case "read_compound_or_panic" -> {
                checkArguments(what, more, 0);
                try {
                    yield NBTSerializableValue.of(TagParser.parseCompoundAsArgument(value));
                } catch (CommandSyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            case "index_of", "find" -> {
                checkArguments(what, more, 1, 2);
                String find = more[0].getString();
                int length = more.length > 1 ? NumericValue.asNumber(more[1]).getInt() : value.getRemainingLength();
                if (find.length() > length) yield Value.NULL;
                int cursor = value.getCursor(), index = value.getString().indexOf(find, cursor, cursor + length);
                if (index < 0) yield Value.NULL;
                if ("find".equals(what)) value.setCursor(index);
                yield NumericValue.of(index);
            }
            case "index_of_last", "find_last" -> {
                checkArguments(what, more, 1, 2);
                String find = more[0].getString();
                int length = more.length > 1 ? NumericValue.asNumber(more[1]).getInt() : value.getRemainingLength();
                if (find.length() > length) yield Value.NULL;
                int cursor = value.getCursor(), index = cursor + value.getRemaining().lastIndexOf(find, length);
                if (index < cursor) yield Value.NULL;
                if ("find_last".equals(what)) value.setCursor(index);
                yield NumericValue.of(index);
            }
            case "next_span", "expect_span" -> {
                checkArguments(what, more, 1);
                String span = more[0].getString();
                if (!value.canRead(span.length())) yield Value.NULL;
                String read = value.getString().substring(value.getCursor(), value.getCursor() + span.length());
                if (!read.equals(span)) yield Value.NULL;
                if ("expect_span".equals(what)) value.setCursor(value.getCursor() + span.length());
                yield StringValue.of(read);
            }
            case "next_not_span", "expect_not_span" -> {
                checkArguments(what, more, 1);
                String span = more[0].getString();
                if (!value.canRead(span.length())) yield Value.NULL;
                String read = value.getString().substring(value.getCursor(), value.getCursor() + span.length());
                if (read.equals(span)) yield Value.NULL;
                if ("expect_not_span".equals(what)) value.setCursor(value.getCursor() + span.length());
                yield StringValue.of(read);
            }
            case "next", "expect" -> {
                checkArguments(what, more, 1);
                if (!value.canRead()) yield Value.NULL;
                char c = value.peek();
                if (c != ValueConversions.toChar(more[0])) yield Value.NULL;
                if ("expect".equals(what)) value.skip();
                yield StringValue.of(String.valueOf(c));
            }
            case "next_not", "expect_not" -> {
                checkArguments(what, more, 1);
                if (!value.canRead()) yield Value.NULL;
                char c = value.peek();
                if (c == ValueConversions.toChar(more[0])) yield Value.NULL;
                if ("expect_not".equals(what)) value.skip();
                yield StringValue.of(String.valueOf(c));
            }
            case "next_range", "expect_range" -> {
                checkArguments(what, more, 2);
                if (!value.canRead()) yield Value.NULL;
                char c = value.peek();
                if (c < ValueConversions.toChar(more[0]) || c > ValueConversions.toChar(more[1])) yield Value.NULL;
                if ("expect_range".equals(what)) value.skip();
                yield StringValue.of(String.valueOf(c));
            }
            case "next_not_range", "expect_not_range" -> {
                checkArguments(what, more, 2);
                if (!value.canRead()) yield Value.NULL;
                char c = value.peek();
                if (c >= ValueConversions.toChar(more[0]) || c <= ValueConversions.toChar(more[1])) yield Value.NULL;
                if ("expect_not_range".equals(what)) value.skip();
                yield StringValue.of(String.valueOf(c));
            }
            case "next_list", "expect_list" -> {
                checkArguments(what, more, 1, -1);
                if (!value.canRead()) yield Value.NULL;
                char c = value.peek();
                for (Value v : more) if (c == ValueConversions.toChar(v)) {
                    if ("expect_list".equals(what)) value.skip();
                    yield StringValue.of(String.valueOf(c));
                }
                yield Value.NULL;
            }
            case "next_not_list", "expect_not_list" -> {
                checkArguments(what, more, 1, -1);
                if (!value.canRead()) yield Value.NULL;
                char c = value.peek();
                for (Value v : more) if (c == ValueConversions.toChar(v)) {
                    if ("expect_not_list".equals(what)) value.skip();
                    yield Value.NULL;
                }
                yield StringValue.of(String.valueOf(c));
            }
            case "next_range_list", "expect_range_list" -> {
                checkArguments(what, more, 2, -1);
                if ((more.length & 1) != 0) throw new InternalExpressionException("Range list must have an even size");
                if (!value.canRead()) yield Value.NULL;
                char c = value.peek();
                for (int i = 0; i < more.length; i += 2) if (c >= ValueConversions.toChar(more[i]) && c <= ValueConversions.toChar(more[i + 1])) {
                    if ("expect_range_list".equals(what)) value.skip();
                    yield StringValue.of(String.valueOf(c));
                }
                yield Value.NULL;
            }
            case "next_not_range_list", "expect_not_range_list" -> {
                checkArguments(what, more, 2, -1);
                if ((more.length & 1) != 0) throw new InternalExpressionException("Range list must have an even size");
                if (!value.canRead()) yield Value.NULL;
                char c = value.peek();
                for (int i = 0; i < more.length; i += 2) if (c >= ValueConversions.toChar(more[i]) && c <= ValueConversions.toChar(more[i + 1])) {
                    if ("expect_not_range_list".equals(what)) value.skip();
                    yield Value.NULL;
                }
                yield StringValue.of(String.valueOf(c));
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getString() {
        return value.getString();
    }

    @Override
    public String getTypeString() {
        return "string_reader";
    }

    @Override
    public Value deepcopy() {
        return new StringReaderValue(context, new StringReader(value));
    }
}
