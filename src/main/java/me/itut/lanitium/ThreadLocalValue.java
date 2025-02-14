package me.itut.lanitium;

import carpet.script.Context;
import carpet.script.value.ContainerValueInterface;
import carpet.script.value.FunctionValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.Value;
import com.google.gson.JsonElement;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.Tag;

import java.util.Collections;

public class ThreadLocalValue extends Value implements ContainerValueInterface {
    private final ThreadLocal<Value> local;

    public ThreadLocalValue(Context c, FunctionValue initial) {
        local = ThreadLocal.withInitial(() -> initial.callInContext(c, Context.NONE, Collections.emptyList()).evalValue(c));
    }

    @Override
    public boolean put(Value k, Value value) {
        local.set(value);
        return true;
    }

    @Override
    public Value get(Value k) {
        return local.get();
    }

    @Override
    public boolean has(Value k) {
        return true;
    }

    @Override
    public boolean delete(Value k) {
        local.remove();
        return true;
    }

    @Override
    public String getString() {
        return "thread_local@" + Integer.toHexString(hashCode());
    }

    @Override
    public boolean getBoolean() {
        return true;
    }

    @Override
    public Tag toTag(boolean force, RegistryAccess access) {
        if (!force) throw new NBTSerializableValue.IncompatibleTypeException(this);
        return local.get().toTag(true, access);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ThreadLocalValue t && local.equals(t.local);
    }

    @Override
    public int hashCode() {
        return local.hashCode();
    }

    @Override
    public JsonElement toJson() {
        return local.get().toJson();
    }

    @Override
    public String getTypeString() {
        return "thread_local";
    }
}
