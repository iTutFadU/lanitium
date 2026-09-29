package me.itut.lanitium.value;

import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.value.FrameworkValue;
import carpet.script.value.Value;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class WithMetaValue extends FrameworkValue {
    public static final WithMetaValue RESET = new WithMetaValue(null, null);
    public static final LazyValue LAZY_RESET = LazyValue.ofConstant(RESET);

    public final @Nullable Context context;
    public final @Nullable Map<Value, Value> meta;

    public WithMetaValue(@Nullable Context context, @Nullable Map<Value, Value> meta) {
        this.context = context;
        this.meta = meta;
    }
}
