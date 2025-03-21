package me.itut.lanitium.internal.carpet;

import carpet.script.LazyValue;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface FunctionValueInterface {
    LazyValue lanitium$body();
    List<String> lanitium$args();
    @Nullable String lanitium$varArgs();
}
