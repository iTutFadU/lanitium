package me.itut.lanitium.mixin.carpet;

import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.value.ContainerValueInterface;
import carpet.script.value.UndefValue;
import carpet.script.value.Value;
import me.itut.lanitium.value.WithValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = UndefValue.class, remap = false)
public abstract class UndefValueMixin implements ContainerValueInterface, WithValue {
    @Shadow protected abstract RuntimeException getError();

    @Override
    public boolean put(Value where, Value value) {
        throw getError();
    }

    @Override
    public Value get(Value where) {
        throw getError();
    }

    @Override
    public boolean has(Value where) {
        throw getError();
    }

    @Override
    public boolean delete(Value where) {
        throw getError();
    }

    @Override
    public LazyValue with(Context c, Context.Type t, LazyValue arg) {
        throw getError();
    }
}
