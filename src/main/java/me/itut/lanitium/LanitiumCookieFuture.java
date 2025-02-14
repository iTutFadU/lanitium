package me.itut.lanitium;

import carpet.script.exception.InternalExpressionException;
import carpet.script.value.BooleanValue;
import carpet.script.value.ListValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.Value;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.Tag;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class LanitiumCookieFuture extends Value {
    private final CompletableFuture<LanitiumCookie> future;

    public LanitiumCookieFuture(CompletableFuture<LanitiumCookie> future) {
        this.future = future;
    }

    @Override
    public Value in(Value args) {
        if (args instanceof ListValue lv) {
            List<Value> values = lv.getItems();
            String what = values.getFirst().getString();
            return get(what, values.subList(1, values.size()).toArray(new Value[0]));
        } else {
            return get(args.getString());
        }
    }

    public Value get(String what, Value... more) {
        return switch (what) {
            case "done" -> BooleanValue.of(future.isDone());
            case "cancelled" -> BooleanValue.of(future.isCancelled());
            default -> throw new InternalExpressionException("Unknown lanitium_cookie_future feature: " + what);
        };
    }

    @Override
    public String getString() {
        return Objects.toIdentityString(this);
    }

    @Override
    public boolean getBoolean() {
        return true;
    }

    @Override
    public Tag toTag(boolean b, RegistryAccess registryAccess) {
        throw new NBTSerializableValue.IncompatibleTypeException(this);
    }

    @Override
    public String getTypeString() {
        return "lanitium_cookie_future";
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof LanitiumCookieFuture f && future.equals(f.future);
    }

    @Override
    public int hashCode() {
        return future.hashCode();
    }
}
