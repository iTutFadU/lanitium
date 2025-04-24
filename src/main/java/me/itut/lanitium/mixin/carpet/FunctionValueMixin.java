package me.itut.lanitium.mixin.carpet;

import carpet.script.LazyValue;
import carpet.script.value.FunctionValue;
import me.itut.lanitium.internal.carpet.FunctionValueInterface;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(value = FunctionValue.class, remap = false)
public abstract class FunctionValueMixin implements FunctionValueInterface {
    @Shadow @Final private LazyValue body;
    @Shadow @Final private List<String> args;
    @Shadow @Final private String varArgs;
    @Shadow @Nullable private Map<String, LazyValue> outerState;
    @Shadow private long variant;

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

    @Override
    public void lanitium$inject(Map<String, LazyValue> state) {
        if (outerState != null)
            outerState.putAll(state);
        else
            outerState = new HashMap<>(state);
    }

    @Override
    public void lanitium$setVariant(long variant) {
        this.variant = variant;
    }
}
