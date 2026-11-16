package me.itut.lanitium.internal.carpet;

import carpet.script.LazyValue;
import carpet.script.value.FunctionValue;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface FunctionValueInterface {
    void lanitium$inject(Map<String, LazyValue> state);
    void lanitium$finishClone(FunctionValue original);
    @Nullable Map<String, LazyValue> lanitium$outerState();
    int lanitium$variant();
}
