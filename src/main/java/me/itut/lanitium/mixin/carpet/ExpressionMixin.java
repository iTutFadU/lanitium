package me.itut.lanitium.mixin.carpet;

import carpet.script.Module;
import carpet.script.*;
import me.itut.lanitium.internal.carpet.ExpressionInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = Expression.class, remap = false)
public abstract class ExpressionMixin implements ExpressionInterface {
    @Shadow @Final private Map<String, Fluff.ILazyOperator> operators;
    @Shadow @Final private Map<String, Fluff.ILazyFunction> functions;

    @Override
    public Map<String, Fluff.ILazyOperator> lanitium$operators() {
        return operators;
    }

    @Override
    public Map<String, Fluff.ILazyFunction> lanitium$functions() {
        return functions;
    }

    @Redirect(method = "createUserDefinedFunction", at = @At(value = "INVOKE", target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z"))
    private boolean fnCheck(String name, Object o) {
        return name.equals("fn") || name.equals("_");
    }
}
