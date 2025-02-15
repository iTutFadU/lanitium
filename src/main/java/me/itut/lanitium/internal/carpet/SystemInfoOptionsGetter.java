package me.itut.lanitium.internal.carpet;

import carpet.script.CarpetContext;
import carpet.script.value.Value;

import java.util.Map;
import java.util.function.Function;

public interface SystemInfoOptionsGetter {
    Map<String, Function<CarpetContext, Value>> lanitium$getOptions();
}
