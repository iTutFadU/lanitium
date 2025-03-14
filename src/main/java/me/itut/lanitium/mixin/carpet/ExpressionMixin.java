package me.itut.lanitium.mixin.carpet;

import carpet.script.Module;
import carpet.script.*;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FunctionValue;
import me.itut.lanitium.internal.carpet.ExpressionInterface;
import me.itut.lanitium.value.LazyFunctionValue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(value = Expression.class, remap = false)
public abstract class ExpressionMixin implements ExpressionInterface {
    @Shadow(remap = false)
    public Module module;
    @Shadow(remap = false) @Final
    private Map<String, Fluff.ILazyFunction> functions;

    @Override
    public Map<String, Fluff.ILazyFunction> lanitium$functions() {
        return functions;
    }

    @Unique // A copy of Expression#createUserDefinedFunction
    private FunctionValue createUserDefinedLazyFunction(Context context, String name, Expression expr, Tokenizer.Token token, List<String> arguments, String varArgs, List<String> outers, LazyValue code) {
        if (functions.containsKey(name)) {
            throw new ExpressionException(context, expr, token, "Function " + name + " would mask a built-in function");
        }
        Map<String, LazyValue> contextValues = new HashMap<>();
        for (String outer : outers) {
            LazyValue lv = context.getVariable(outer);
            if (lv == null) {
                throw new InternalExpressionException("Variable " + outer + " needs to be defined in outer scope to be used as outer parameter, and cannot be global");
            } else {
                contextValues.put(outer, lv);
            }
        }
        if (contextValues.isEmpty()) {
            contextValues = null;
        }

        FunctionValue result = new LazyFunctionValue(expr, token, name, code, arguments, varArgs, contextValues);
        if (!name.equals("_")) {
            context.host.addUserDefinedFunction(context, module, name, result);
        }
        return result;
    }

    @Inject(method = "createUserDefinedFunction", at = @At("HEAD"), cancellable = true)
    public void createUserDefinedFunctionLazyCheck(Context context, String name, Expression expr, Tokenizer.Token token, List<String> arguments, String varArgs, List<String> outers, LazyValue code, CallbackInfoReturnable<FunctionValue> cir) {
        if (name.startsWith("LAZY#")) // Forgive me
            cir.setReturnValue(createUserDefinedLazyFunction(context, name.substring(5), expr, token, arguments, varArgs, outers, code));
    }
}
