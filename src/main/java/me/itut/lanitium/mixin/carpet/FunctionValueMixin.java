package me.itut.lanitium.mixin.carpet;

import carpet.script.LazyValue;
import carpet.script.value.FunctionValue;
import me.itut.lanitium.internal.carpet.FunctionValueInterface;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = FunctionValue.class, remap = false)
public abstract class FunctionValueMixin implements FunctionValueInterface {
    @Shadow @Nullable private Map<String, LazyValue> outerState;

    @Override
    public void lanitium$inject(Map<String, LazyValue> state) {
        if (outerState != null)
            outerState.putAll(state);
        else
            outerState = new HashMap<>(state);
    }
}
