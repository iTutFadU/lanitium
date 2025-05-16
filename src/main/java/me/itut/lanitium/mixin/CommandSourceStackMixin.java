package me.itut.lanitium.mixin;

import carpet.script.utils.SnoopyCommandSource;
import carpet.script.value.Value;
import me.itut.lanitium.internal.CommandSourceStackInterface;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(CommandSourceStack.class)
public abstract class CommandSourceStackMixin implements CommandSourceStackInterface {
    @Shadow @Final
    public int permissionLevel;

    @Shadow
    public abstract CommandSourceStack withPermission(int i);

    @Unique
    private Map<Value, Value> customValues;

    @Override
    public Map<Value, Value> lanitium$customValues() {
        return customValues;
    }

    @Override
    public void lanitium$setCustomValues(Map<Value, Value> values) {
        customValues = values;
    }

    @Override
    public CommandSourceStack lanitium$withCustomValues(Map<Value, Value> values) {
        CommandSourceStack copy = permissionLevel != -1 ? withPermission(-1).withPermission(permissionLevel) : withPermission(0).withPermission(-1);
        Map<Value, Value> customValues = ((CommandSourceStackInterface)copy).lanitium$customValues();
        if (customValues == null) ((CommandSourceStackInterface)copy).lanitium$setCustomValues(values);
        else customValues.putAll(values);
        return copy;
    }

    @Inject(method = {
        "withSource",
        "withEntity",
        "withPosition",
        "withRotation",
        "withCallback(Lnet/minecraft/commands/CommandResultCallback;)Lnet/minecraft/commands/CommandSourceStack;",
        "withSuppressedOutput",
        "withPermission",
        "withMaximumPermission",
        "withAnchor",
        "withLevel",
        "withSigningContext",
    }, at = @At("TAIL"))
    private void withCustomValues(CallbackInfoReturnable<CommandSourceStack> cir) {
        ((CommandSourceStackInterface)cir.getReturnValue()).lanitium$setCustomValues(customValues);
    }

    @Mixin(value = SnoopyCommandSource.class, remap = false)
    public abstract static class Snoopy {
        @Inject(method = {
            "withEntity",
            "withPosition",
            "withRotation",
            "withCallback(Lnet/minecraft/commands/CommandResultCallback;)Lnet/minecraft/commands/CommandSourceStack;",
            "withAnchor",
            "withLevel",
            "withSigningContext",
        }, at = @At("TAIL"))
        private void withCustomValues(CallbackInfoReturnable<CommandSourceStack> cir) {
            ((CommandSourceStackInterface)cir.getReturnValue()).lanitium$setCustomValues(((CommandSourceStackInterface)this).lanitium$customValues());
        }
    }
}
