package me.itut.lanitium.value;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.value.ContainerValueInterface;
import carpet.script.value.FunctionValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.Value;
import com.google.gson.JsonElement;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.Tag;

import java.util.Collections;

public class ThreadLocalValue extends ObjectValue<ThreadLocal<Value>> implements ContainerValueInterface {
    public ThreadLocalValue(Context c, FunctionValue initial) {
        super((CarpetContext)c, ThreadLocal.withInitial(() -> initial.callInContext(c, Context.NONE, Collections.emptyList()).evalValue(c)));
    }

    @Override
    public boolean put(Value k, Value value) {
        this.value.set(value);
        return true;
    }

    @Override
    public Value get(Value k) {
        return value.get();
    }

    @Override
    public boolean has(Value k) {
        return true;
    }

    @Override
    public boolean delete(Value k) {
        value.remove();
        return true;
    }

    @Override
    public Tag toTag(boolean force, RegistryAccess access) {
        if (!force) throw new NBTSerializableValue.IncompatibleTypeException(this);
        return value.get().toTag(true, access);
    }

    @Override
    public JsonElement toJson() {
        return value.get().toJson();
    }

    @Override
    public Value get(String what, Value... more) {
        return unknownFeature(what);
    }

    @Override
    public String getTypeString() {
        return "thread_local";
    }
}
