package me.itut.lanitium.function;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.annotation.ScarpetFunction;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import carpet.script.value.*;
import me.itut.lanitium.value.ByteBufferValue;
import net.minecraft.nbt.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class Encoding {
    @ScarpetFunction
    public static Value char_to_int(String str) {
        if (str.isEmpty()) return Value.NULL;
        return NumericValue.of((int)str.charAt(0));
    }

    @ScarpetFunction
    public static Value int_to_char(int i) {
        return StringValue.of(String.valueOf((char)i));
    }

    @ScarpetFunction
    public static Value name_of_code_point(String str) {
        if (str.isEmpty()) return Value.NULL;
        return StringValue.of(Character.getName(str.charAt(0)));
    }

    @ScarpetFunction
    public static Value code_point_by_name(String name) {
        try {
            return StringValue.of(String.valueOf(Character.codePointOf(name)));
        } catch (IllegalArgumentException e) {
            return Value.NULL;
        }
    }

    @ScarpetFunction(maxParams = 2)
    public static Value parse_signed_int(String str, Optional<Integer> radix) {
        try {
            return NumericValue.of(Long.parseLong(str, radix.orElse(10)));
        } catch (NumberFormatException e) {
            return Value.NULL;
        }
    }

    @ScarpetFunction(maxParams = 2)
    public static Value parse_unsigned_int(String str, Optional<Integer> radix) {
        try {
            return NumericValue.of(Long.parseUnsignedLong(str, radix.orElse(10)));
        } catch (NumberFormatException e) {
            return Value.NULL;
        }
    }

    @ScarpetFunction
    public static Value to_nbt(Context c, Value v) {
        return NBTSerializableValue.of(v.toTag(true, ((CarpetContext)c).registryAccess()));
    }

    @ScarpetFunction
    public static Value encode_bytes(String data) {
        return ByteBufferValue.of(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8)));
    }

    @ScarpetFunction
    public static Value decode_bytes(ByteBuffer data) {
        return StringValue.of(new String(data.array(), StandardCharsets.UTF_8));
    }

    @ScarpetFunction
    public static Value encode_nbt_bytes(Context c, NBTSerializableValue data) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            NbtIo.writeAnyTag(data.getTag(), new DataOutputStream(stream));
        } catch (IOException e) {
            throw new ThrowStatement(e.getMessage(), Throwables.IO_EXCEPTION);
        }
        return ByteBufferValue.of(ByteBuffer.wrap(stream.toByteArray()));
    }

    @ScarpetFunction
    public static Value decode_nbt_bytes(ByteBuffer data) {
        ByteArrayInputStream stream = new ByteArrayInputStream(data.array());
        try {
            return NBTSerializableValue.of(NbtIo.readAnyTag(new DataInputStream(stream), NbtAccounter.unlimitedHeap()));
        } catch (IOException e) {
            throw new ThrowStatement(e.getMessage(), Throwables.IO_EXCEPTION);
        }
    }

    @ScarpetFunction
    public static Value encode_compressed_compound(Context c, NBTSerializableValue data) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            NbtIo.writeCompressed(data.getCompoundTag(), new DataOutputStream(stream));
        } catch (IOException e) {
            throw new ThrowStatement(e.getMessage(), Throwables.IO_EXCEPTION);
        }
        return ByteBufferValue.of(ByteBuffer.wrap(stream.toByteArray()));
    }

    @ScarpetFunction
    public static Value decode_compressed_compound(ByteBuffer data) {
        ByteArrayInputStream stream = new ByteArrayInputStream(data.array());
        try {
            return NBTSerializableValue.of(NbtIo.readCompressed(new DataInputStream(stream), NbtAccounter.unlimitedHeap()));
        } catch (IOException e) {
            throw new ThrowStatement(e.getMessage(), Throwables.IO_EXCEPTION);
        }
    }

    @ScarpetFunction(maxParams = 2)
    public static Value pretty_nbt(Context c, NBTSerializableValue nbt, Optional<String> indent) {
        return FormattedTextValue.of(new TextComponentTagVisitor(indent.orElse("")).visit(nbt.getTag()));
    }

    @ScarpetFunction
    public static Value nbt_type(NBTSerializableValue nbt) {
        return StringValue.of(nbt.getTag().getType().getName().toLowerCase());
    }

    @ScarpetFunction
    public static Value nbt_list_type(NBTSerializableValue nbt) {
        if (!(nbt.getTag() instanceof ListTag list)) return Value.NULL;
        return StringValue.of(TagTypes.getType(list.getElementType()).getName().toLowerCase());
    }
}
