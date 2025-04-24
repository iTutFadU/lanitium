package me.itut.lanitium.mixin.carpet;

import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.Value;
import me.itut.lanitium.internal.carpet.MapValueInterface;
import me.itut.lanitium.value.Constants;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = MapValue.class, remap = false)
public abstract class MapValueMixin implements MapValueInterface {
    @Unique
    private @Nullable Map<Value, Value> meta;

    @Override
    public @Nullable Map<Value, Value> lanitium$meta() {
        return meta;
    }

    @Override
    public void lanitium$setMeta(@Nullable Map<Value, Value> meta) {
        this.meta = meta;
    }

    @Inject(method = "put(Lcarpet/script/value/Value;)V", at = @At("HEAD"), cancellable = true)
    private void dontAddEmpty(Value v, CallbackInfo ci) {
        if (v instanceof ListValue pair && pair.getItems().isEmpty()) ci.cancel();
    }

    @Inject(method = "deepcopy", at = @At("RETURN"))
    private void copyMeta(CallbackInfoReturnable<Value> cir) {
        ((MapValueInterface)cir.getReturnValue()).lanitium$setMeta(meta);
    }

    @Inject(method = "getTypeString", at = @At("HEAD"), cancellable = true)
    private void customType(CallbackInfoReturnable<String> cir) {
        if (meta != null && meta.get(Constants.__TYPE) instanceof Value type)
            cir.setReturnValue(type.getString());
    }
}
