package me.itut.lanitium.mixin.carpet;

import carpet.script.*;
import carpet.script.Module;
import carpet.script.exception.*;
import carpet.script.value.FunctionValue;
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
public abstract class ExpressionMixin {
    @Shadow(remap = false)
    public Module module;
    @Shadow(remap = false) @Final
    private Map<String, Fluff.ILazyFunction> functions;

    @Unique
    public FunctionValue createUserDefinedLazyFunction(Context context, String name, Expression expr, Tokenizer.Token token, List<String> arguments, String varArgs, List<String> outers, LazyValue code) {
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

    @Inject(method = "createUserDefinedFunction(Lcarpet/script/Context;Ljava/lang/String;Lcarpet/script/Expression;Lcarpet/script/Tokenizer$Token;Ljava/util/List;Ljava/lang/String;Ljava/util/List;Lcarpet/script/LazyValue;)Lcarpet/script/value/FunctionValue;", at = @At("HEAD"), cancellable = true, remap = false)
    public void createUserDefinedFunctionLazyCheck(Context context, String name, Expression expr, Tokenizer.Token token, List<String> arguments, String varArgs, List<String> outers, LazyValue code, CallbackInfoReturnable<FunctionValue> cir) {
        if (name.startsWith("LAZY#")) // Forgive me
            cir.setReturnValue(createUserDefinedLazyFunction(context, name.substring(5), expr, token, arguments, varArgs, outers, code));
    }
}
