package me.itut.lanitium.mixin.carpet;

import carpet.script.value.ListValue;
import carpet.script.value.Value;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = ListValue.class, remap = false)
public abstract class ListValueMixin {
    @Shadow @Final protected List<Value> items;

    @Inject(method = "toTag", at = @At("HEAD"), cancellable = true)
    private void fixedToTag(boolean force, RegistryAccess regs, CallbackInfoReturnable<Tag> cir) {
        ListTag tag = new ListTag();
        tag.addAll(items.stream().map(e -> e.toTag(force, regs)).toList());
        cir.setReturnValue(tag);
    }
}
