package me.itut.lanitium.internal.carpet;

import carpet.script.CarpetContext;
import carpet.script.utils.SystemInfo;
import carpet.script.value.Value;

import java.util.Map;
import java.util.function.Function;

public interface SystemInfoInterface {
    @SuppressWarnings("InstantiationOfUtilityClass")
    Map<String, Function<CarpetContext, Value>> options = ((SystemInfoInterface)new SystemInfo()).lanitium$getOptions();

    Map<String, Function<CarpetContext, Value>> lanitium$getOptions();
}
