package me.itut.lanitium.mixin.carpet;

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

    @Redirect(method = "RPNToParseTree", at = @At(value = "NEW", target = "(Lcarpet/script/LazyValue;Ljava/util/List;Lcarpet/script/Tokenizer$Token;)Lcarpet/script/Expression$ExpressionNode;"))
    private Expression.ExpressionNode checkMethodCall(LazyValue op, List<Expression.ExpressionNode> args, Tokenizer.Token token) {
        if (((TokenInterface)token).lanitium$type() == TokenTypeInterface.OPERATOR && ".".equals(token.surface)) {
            List<LazyValue> params = new ArrayList<>(2 + args.getLast().args.size());
            params.add(args.getFirst().op);
            params.add(LazyValue.ofConstant(StringValue.of(args.getLast().token.surface)));
            params.addAll(args.getLast().args.stream().map(v -> v.op).toList());
            return new Expression.ExpressionNode((c, t) -> functions.get("call_method").lazyEval(c, t, (Expression)(Object)this, token, params).evalValue(c, t), args, token);
        }
        return new Expression.ExpressionNode(op, args, token);
    }
}
