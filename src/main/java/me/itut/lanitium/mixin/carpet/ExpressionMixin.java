package me.itut.lanitium.mixin.carpet;

import carpet.script.Module;
import carpet.script.*;
import carpet.script.value.StringValue;
import me.itut.lanitium.internal.carpet.ExpressionInterface;
import me.itut.lanitium.internal.carpet.TokenInterface;
import me.itut.lanitium.internal.carpet.TokenTypeInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
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

    @Redirect(method = "getOrSetAnyVariable", at = @At(value = "INVOKE", target = "Lcarpet/script/ScriptHost;getGlobalVariable(Lcarpet/script/Module;Ljava/lang/String;)Lcarpet/script/LazyValue;"))
    private LazyValue checkLocalVariable(ScriptHost instance, Module module, String name) {
        if (!name.startsWith("global_")) return null;
        return instance.getGlobalVariable(module, name);
    }

    @Redirect(method = "RPNToParseTree", at = @At(value = "NEW", target = "(Lcarpet/script/LazyValue;Ljava/util/List;Lcarpet/script/Token;)Lcarpet/script/Expression$ExpressionNode;"))
    private Expression.ExpressionNode checkMethodCall(LazyValue op, List<Expression.ExpressionNode> args, Token token) {
        if (((TokenInterface)token).lanitium$type() == TokenTypeInterface.OPERATOR && ".".equals(token.surface)) {
            Expression.ExpressionNode self = args.getFirst(), call = args.getLast();
            args = new ArrayList<>(1 + call.args.size());
            args.add(self);
            args.addAll(call.args);
            Expression.ExpressionNode method = call.args.getFirst();
            String name = method.token.surface.substring(1);
            method.op = LazyValue.ofConstant(new StringValue(name));
            method.token.surface = name;
            List<LazyValue> params = args.stream().map(v -> v.op).toList();
            return new Expression.ExpressionNode((c, t) -> functions.get("call_method").lazyEval(c, t, (Expression)(Object)this, token, params).evalValue(c, t), args, ((TokenInterface)token).lanitium$morphedInto(TokenTypeInterface.FUNCTION, "call_method"));
        }
        return new Expression.ExpressionNode(op, args, token);
    }
}
