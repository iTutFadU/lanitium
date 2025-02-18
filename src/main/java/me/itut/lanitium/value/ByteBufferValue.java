package me.itut.lanitium.value;

import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.context.StringRange;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.Tag;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

public class ByteBufferValue extends AbstractListValue implements ContainerValueInterface {
    public final ByteBuffer buffer;

    protected ByteBufferValue(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public static Value of(ByteBuffer buffer) {
        return buffer != null ? new ByteBufferValue(buffer) : Value.NULL;
    }

    public static ByteBuffer from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case ByteBufferValue v -> v.buffer;
            case AbstractListValue v -> ByteBuffer.wrap(v.unpack().stream().mapToInt(n -> NumericValue.asNumber(n).getInt()).collect(ByteArrayOutputStream::new, (baos, i) -> baos.write((byte)i), (baos1, baos2) -> baos1.write(baos2.toByteArray(), 0, baos2.size())).toByteArray());
            case StringValue v -> ByteBuffer.wrap(v.getString().getBytes(StandardCharsets.UTF_8));
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to byte_buffer");
        };
    }

    @Override
    public Value in(Value args) {
        List<Value> values = Util.listFrom(args);
        if (values.isEmpty()) return get("null");
        return get(values.getFirst().getString(), values.subList(1, values.size()).toArray(Value[]::new));
    }

    public Value get(String what, Value... more) {
        return switch (what) {
            case "rewind" -> {
                ObjectValue.checkArguments("byte_buffer~'" + what + "'", more.length, 0);
                buffer.rewind();
                yield this;
            }
            case "remaining" -> {
                ObjectValue.checkArguments("byte_buffer~'" + what + "'", more.length, 0);
                yield NumericValue.of(buffer.remaining());
            }
            case "has_remaining" -> {
                ObjectValue.checkArguments("byte_buffer~'" + what + "'", more.length, 0);
                yield BooleanValue.of(buffer.hasRemaining());
            }
            case "position" -> {
                ObjectValue.checkArguments("byte_buffer~'" + what + "'", more.length, 0);
                yield NumericValue.of(buffer.position());
            }
            case "set_position" -> {
                ObjectValue.checkArguments("byte_buffer~'" + what + "'", more.length, 1);
                buffer.position(NumericValue.asNumber(more[0]).getInt());
                yield this;
            }
            default -> throw new InternalExpressionException("Unknown byte_buffer feature: " + what);
        };
    }

    @Override
    public int length() {
        return buffer.array().length;
    }

    @Override
    public String getTypeString() {
        return "byte_buffer";
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ByteBufferValue v && buffer.equals(v.buffer);
    }

    @Override
    public int hashCode() {
        return buffer.hashCode();
    }

    @Override
    public JsonElement toJson() {
        byte[] src = buffer.array();
        JsonArray array = new JsonArray(src.length);
        for (int i = 0; i < src.length; i++) {
            array.set(i, new JsonPrimitive(src[i]));
        }
        return array;
    }

    @Override
    public ByteBufferValue clone() {
        return new ByteBufferValue(ByteBuffer.wrap(buffer.array()));
    }

    @Override
    public boolean put(Value where, Value value) {
        if (!where.isNull()) {
            int index = NumericValue.asNumber(where).getInt();
            if (value instanceof AbstractListValue list) {
                ByteBuffer src = from(list);
                assert src != null;
                buffer.put(index, src.array());
            } else buffer.put(index, (byte)NumericValue.asNumber(value).getInt());
        } else if (value instanceof AbstractListValue list) buffer.put(from(list));
        else buffer.put((byte)NumericValue.asNumber(value).getInt());
        return true;
    }

    @Override
    public Value get(Value where) {
        if (where.isNull()) return NumericValue.of(buffer.get());
        if (where instanceof AbstractListValue list) {
            StringRange range = Util.toRange(list);
            assert range != null;
            if (list.length() == 1) {
                byte[] arr = new byte[range.getEnd()];
                buffer.get(arr);
                return ListValue.wrap(IntStream.range(0, arr.length).mapToObj(i -> NumericValue.of(arr[i])));
            }
            byte[] arr = new byte[range.getLength()];
            buffer.get(range.getStart(), arr);
            return ListValue.wrap(IntStream.range(0, arr.length).mapToObj(i -> NumericValue.of(arr[i])));
        }
        return NumericValue.of(buffer.get(NumericValue.asNumber(where).getInt()));
    }

    @Override
    public boolean has(Value where) {
        return false;
    }

    @Override
    public boolean delete(Value where) {
        return false;
    }

    @Override
    public String getString() {
        return "[B;...]";
    }

    @Override
    public boolean getBoolean() {
        return buffer.array().length != 0;
    }

    @Override
    public Tag toTag(boolean force, RegistryAccess regs) {
        return new ByteArrayTag(buffer.array());
    }

    @Override
    public Iterator<Value> iterator() {
        final ByteBuffer iter = buffer.asReadOnlyBuffer().rewind();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iter.hasRemaining();
            }

            @Override
            public Value next() {
                if (!hasNext()) throw new NoSuchElementException();
                return NumericValue.of(iter.get());
            }
        };
    }
}
