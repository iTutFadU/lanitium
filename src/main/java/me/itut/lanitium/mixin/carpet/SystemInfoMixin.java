package me.itut.lanitium.mixin.carpet;

import carpet.script.CarpetContext;
import carpet.script.utils.SystemInfo;
import carpet.script.value.Value;
import me.itut.lanitium.internal.carpet.SystemInfoInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.function.Function;

@Mixin(value = SystemInfo.class, remap = false)
public abstract class SystemInfoMixin implements SystemInfoInterface {
    @Shadow(remap = false) @Final
    private static Map<String, Function<CarpetContext, Value>> options;

    @Override
    public Map<String, Function<CarpetContext, Value>> lanitium$getOptions() {
        return options;
    }
}
