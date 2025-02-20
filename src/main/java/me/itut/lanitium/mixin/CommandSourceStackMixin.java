package me.itut.lanitium.mixin;

import carpet.script.value.Value;
import me.itut.lanitium.internal.CommandSourceStackCustomValues;
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
public abstract class CommandSourceStackMixin implements CommandSourceStackCustomValues {
    @Shadow @Final
    private int permissionLevel;

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
        Map<Value, Value> customValues = ((CommandSourceStackCustomValues)copy).lanitium$customValues();
        if (customValues == null) ((CommandSourceStackCustomValues)copy).lanitium$setCustomValues(values);
        else customValues.putAll(values);
        return copy;
    }

    @Inject(method = {
        "withSource(Lnet/minecraft/commands/CommandSource;)Lnet/minecraft/commands/CommandSourceStack;",
        "withEntity(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/commands/CommandSourceStack;",
        "withPosition(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/commands/CommandSourceStack;",
        "withRotation(Lnet/minecraft/world/phys/Vec2;)Lnet/minecraft/commands/CommandSourceStack;",
        "withCallback(Lnet/minecraft/commands/CommandResultCallback;)Lnet/minecraft/commands/CommandSourceStack;",
        "withSuppressedOutput()Lnet/minecraft/commands/CommandSourceStack;",
        "withPermission(I)Lnet/minecraft/commands/CommandSourceStack;",
        "withMaximumPermission(I)Lnet/minecraft/commands/CommandSourceStack;",
        "withAnchor(Lnet/minecraft/commands/arguments/EntityAnchorArgument$Anchor;)Lnet/minecraft/commands/CommandSourceStack;",
        "withLevel(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/commands/CommandSourceStack;",
        "withSigningContext(Lnet/minecraft/commands/CommandSigningContext;Lnet/minecraft/util/TaskChainer;)Lnet/minecraft/commands/CommandSourceStack;",
    }, at = @At("TAIL"))
    private void withCustomValues(CallbackInfoReturnable<CommandSourceStack> cir) {
        ((CommandSourceStackCustomValues)cir.getReturnValue()).lanitium$setCustomValues(customValues);
    }
}
