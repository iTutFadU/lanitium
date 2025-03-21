package me.itut.lanitium.mixin.carpet;

import carpet.script.LazyValue;
import carpet.script.value.FunctionValue;
import me.itut.lanitium.internal.carpet.FunctionValueInterface;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = FunctionValue.class, remap = false)
public abstract class FunctionValueMixin implements FunctionValueInterface {
    @Shadow @Final private LazyValue body;
    @Shadow @Final private List<String> args;
    @Shadow @Final private String varArgs;

    @Override
    public LazyValue lanitium$body() {
        return body;
    }

    @Override
    public List<String> lanitium$args() {
        return args;
    }

    @Override
    public @Nullable String lanitium$varArgs() {
        return varArgs;
    }
}
