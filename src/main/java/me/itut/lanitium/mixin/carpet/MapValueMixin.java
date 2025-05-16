package me.itut.lanitium.mixin.carpet;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.value.*;
import me.itut.lanitium.internal.carpet.MapValueInterface;
import me.itut.lanitium.value.Constants;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(value = MapValue.class, remap = false)
public abstract class MapValueMixin implements MapValueInterface {
    @Unique private @Nullable CarpetContext metaContext;
    @Unique private @Nullable Map<Value, Value> meta;

    @Override
    public @Nullable Map<Value, Value> lanitium$meta() {
        return meta;
    }

    @Override
    public void lanitium$setMeta(CarpetContext context, Map<Value, Value> meta) {
        metaContext = context;
        this.meta = meta;
    }

    @Override
    public void lanitium$removeMeta() {
        metaContext = null;
        meta = null;
    }

    @Inject(method = "put(Lcarpet/script/value/Value;)V", at = @At("HEAD"), cancellable = true)
    private void dontAddEmpty(Value v, CallbackInfo ci) {
        if (v instanceof ListValue pair && pair.getItems().isEmpty()) ci.cancel();
    }

    @Inject(method = "deepcopy", at = @At("RETURN"))
    private void copyMeta(CallbackInfoReturnable<Value> cir) {
        ((MapValueInterface)cir.getReturnValue()).lanitium$setMeta(metaContext, meta);
    }

    @Inject(method = "getTypeString", at = @At("HEAD"), cancellable = true)
    private void customType(CallbackInfoReturnable<String> cir) {
        if (meta != null && meta.get(Constants.__TYPE) instanceof Value type)
            cir.setReturnValue(type.getString());
    }

    @Inject(method = "getString", at = @At("HEAD"), cancellable = true)
    private void customStr(CallbackInfoReturnable<String> cir) {
        if (meta != null && meta.get(Constants.__STR) instanceof FunctionValue fn)
            cir.setReturnValue(fn.callInContext(metaContext, Context.NONE, List.of((MapValue)(Object)this)).evalValue(metaContext, Context.NONE).getString());
    }

    @Inject(method = "hashCode", at = @At("HEAD"), cancellable = true)
    private void customHash(CallbackInfoReturnable<Integer> cir) {
        if (meta != null && meta.get(Constants.__HASH) instanceof FunctionValue fn)
            cir.setReturnValue(NumericValue.asNumber(fn.callInContext(metaContext, Context.NONE, List.of((MapValue)(Object)this)).evalValue(metaContext, Context.NONE)).getInt());
    }

    @Inject(method = "equals", at = @At("HEAD"), cancellable = true)
    private void customEq(Object o, CallbackInfoReturnable<Boolean> cir) {
        if (!(o instanceof Value other)) return;
        if (meta != null && meta.get(Constants.__EQ) instanceof FunctionValue fn)
            cir.setReturnValue(fn.callInContext(metaContext, Context.BOOLEAN, List.of((MapValue)(Object)this, other)).evalValue(metaContext, Context.BOOLEAN).getBoolean());
    }
}
