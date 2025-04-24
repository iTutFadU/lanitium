package me.itut.lanitium.value;

import carpet.script.*;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.ThreadValue;
import carpet.script.value.Value;
import me.itut.lanitium.internal.Magic;

import java.util.List;

public class SimpleFunctionValue extends FunctionValue {
    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }

    @FunctionalInterface
    public interface QuadConsumer<A, B, C, D> {
        void accept(A a, B b, C c, D d);
    }

    @FunctionalInterface
    public interface QuinnConsumer<A, B, C, D, E> {
        void accept(A a, B b, C c, D d, E e);
    }

    @FunctionalInterface
    public interface SexConsumer<A, B, C, D, E, F> {
        void accept(A a, B b, C c, D d, E e, F f);
    }

    public final int minParams, maxParams;
    public final Fluff.QuinnFunction<Context, Context.Type, Expression, Tokenizer.Token, List<Value>, Value> body;

    public SimpleFunctionValue(int minParams, int maxParams, Fluff.QuinnFunction<Context, Context.Type, Expression, Tokenizer.Token, List<Value>, Value> body) {
        super(Expression.none, null, "_", null, List.of(), null, null);
        this.minParams = minParams;
        this.maxParams = maxParams;
        this.body = body;
    }

    public static SimpleFunctionValue runnable(int minParams, int maxParams, QuinnConsumer<Context, Context.Type, Expression, Tokenizer.Token, List<Value>> body) {
        return new SimpleFunctionValue(minParams, maxParams, (c, t, e, tok, lv) -> {
            body.accept(c, t, e, tok, lv);
            return Value.NULL;
        });
    }

    @Override
    public LazyValue execute(Context c, Context.Type type, Expression e, Tokenizer.Token t, List<Value> args, ThreadValue freshNewCallingThread) {
        if (args.size() < minParams || maxParams >= 0 && args.size() > maxParams)
            throw new ExpressionException(c, e, t, "Incorrect number of arguments for function. Should be %s, not %d".formatted(maxParams < 0 ? "at least " + minParams : minParams < maxParams ? "from " + minParams + " to " + maxParams : minParams, args.size()));

        Value ret = body.apply(c, type, e, t, args);
        return (cc, tt) -> ret;
    }

    @Override
    protected Value clone() {
        try {
            return (SimpleFunctionValue)Magic.cloneObject(this);
        } catch (CloneNotSupportedException e) {
            // should never happen
            throw new InternalExpressionException("Cannot make a copy of value: " + this);
        }
    }
}
