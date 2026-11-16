package me.itut.lanitium.mixin.carpet;

import carpet.script.LazyValue;
import carpet.script.value.FunctionValue;
import me.itut.lanitium.internal.carpet.FunctionValueInterface;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = FunctionValue.class, remap = false)
public abstract class FunctionValueMixin implements FunctionValueInterface {
    @Shadow private @Nullable Map<String, LazyValue> outerState;
    @Shadow private int variant;

    @Override
    @Accessor("outerState")
    public abstract @Nullable Map<String, LazyValue> lanitium$outerState();

    @Override
    @Accessor("variant")
    public abstract int lanitium$variant();

    @Override
    public void lanitium$inject(Map<String, LazyValue> state) {
        if (outerState != null)
            outerState.putAll(state);
        else
            outerState = new HashMap<>(state);
    }

    @Override
    public void lanitium$finishClone(FunctionValue original) {
        FunctionValueInterface cast = (FunctionValueInterface)original;
        outerState = cast.lanitium$outerState();
        variant = cast.lanitium$variant();
    }
}
