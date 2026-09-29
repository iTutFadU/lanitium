package me.itut.lanitium.mixin.compat;

import carpet.script.Context;
import carpet.script.value.Value;
import me.itut.lanitium.value.FeatureMethodsValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.replaceitem.discarpet.script.values.common.DiscordValue", remap = false)
public abstract class DiscordValueMixin implements FeatureMethodsValue {
    @Shadow
    public abstract Value getProperty(String property);

    @Override
    public Value lanitium$feature(Context.Type type, Context ctx, String what, Value... more) {
        return getProperty(what);
    }
}
