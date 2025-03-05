package me.itut.lanitium.value;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.Tokenizer;
import carpet.script.value.FunctionValue;
import carpet.script.value.ThreadValue;
import carpet.script.value.Value;

import java.util.List;
import java.util.Map;

public class LazyFunctionValue extends FunctionValue {
    public LazyFunctionValue(Expression expression, Tokenizer.Token token, String name, LazyValue body, List<String> args, String varArgs, Map<String, LazyValue> outerState) {
        super(expression, token, name, body, args, varArgs, outerState);
    }

    public static List<Value> wrapArgs(List<LazyValue> values, Context context, Context.Type type) {
        return values.stream().map(v -> (Value)new Lazy(context, type, v)).toList();
    }

    public static List<LazyValue> wrapLazyArgs(List<LazyValue> values, Context context, Context.Type type) {
        return values.stream().map(v -> {
            Lazy lazy = new Lazy(context, type, v);
            return (LazyValue)(c, t) -> lazy;
        }).toList();
    }

    @Override
    public LazyValue lazyEval(Context c, Context.Type type, Expression e, Tokenizer.Token t, List<LazyValue> lazyParams) {
        return this.execute(c, type, e, t, wrapArgs(lazyParams, c, type), null);
    }

    @Override
    public LazyValue execute(Context c, Context.Type type, Expression e, Tokenizer.Token t, List<Value> params, ThreadValue freshNewCallingThread) {
        Value output = super.execute(c, type, e, t, params, freshNewCallingThread).evalValue(c, type);
        return (cc, tt) -> output;
    }

    public LazyValue originalExecute(Context c, Context.Type type, Expression e, Tokenizer.Token t, List<Value> params, ThreadValue freshNewCallingThread) {
        return super.execute(c, type, e, t, params, freshNewCallingThread);
    }
}
