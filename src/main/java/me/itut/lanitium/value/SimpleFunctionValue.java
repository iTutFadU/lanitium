package me.itut.lanitium.value;

import carpet.script.Expression;
import carpet.script.Fluff;
import carpet.script.LazyValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.ListValue;
import carpet.script.value.Value;

import java.util.List;
import java.util.Map;
import java.util.function.*;

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

    public SimpleFunctionValue(LazyValue body, List<String> args, String varArgs) {
        super(Expression.none, null, "_", body, args, varArgs, null);
    }

    public static SimpleFunctionValue of(Supplier<Value> fn) {
        return new SimpleFunctionValue((c, t) -> fn.get(), List.of(), null);
    }

    public static SimpleFunctionValue of(UnaryOperator<Value> fn) {
        return new SimpleFunctionValue((c, t) -> fn.apply(c.getVariable("_1").evalValue(c, t)), List.of("_1"), null);
    }

    public static SimpleFunctionValue of(BinaryOperator<Value> fn) {
        return new SimpleFunctionValue((c, t) -> fn.apply(c.getVariable("_1").evalValue(c, t), c.getVariable("_2").evalValue(c, t)), List.of("_1", "_2"), null);
    }

    public static SimpleFunctionValue of(Fluff.TriFunction<Value, Value, Value, Value> fn) {
        return new SimpleFunctionValue((c, t) -> fn.apply(c.getVariable("_1").evalValue(c, t), c.getVariable("_2").evalValue(c, t), c.getVariable("_3").evalValue(c, t)), List.of("_1", "_2", "_3"), null);
    }

    public static SimpleFunctionValue of(Fluff.QuadFunction<Value, Value, Value, Value, Value> fn) {
        return new SimpleFunctionValue((c, t) -> fn.apply(c.getVariable("_1").evalValue(c, t), c.getVariable("_2").evalValue(c, t), c.getVariable("_3").evalValue(c, t), c.getVariable("_4").evalValue(c, t)), List.of("_1", "_2", "_3", "_4"), null);
    }

    public static SimpleFunctionValue of(Fluff.QuinnFunction<Value, Value, Value, Value, Value, Value> fn) {
        return new SimpleFunctionValue((c, t) -> fn.apply(c.getVariable("_1").evalValue(c, t), c.getVariable("_2").evalValue(c, t), c.getVariable("_3").evalValue(c, t), c.getVariable("_4").evalValue(c, t), c.getVariable("_5").evalValue(c, t)), List.of("_1", "_2", "_3", "_4", "_5"), null);
    }

    public static SimpleFunctionValue of(Fluff.SexFunction<Value, Value, Value, Value, Value, Value, Value> fn) {
        return new SimpleFunctionValue((c, t) -> fn.apply(c.getVariable("_1").evalValue(c, t), c.getVariable("_2").evalValue(c, t), c.getVariable("_3").evalValue(c, t), c.getVariable("_4").evalValue(c, t), c.getVariable("_5").evalValue(c, t), c.getVariable("_6").evalValue(c, t)), List.of("_1", "_2", "_3", "_4", "_5", "_6"), null);
    }

    public static SimpleFunctionValue ofVarargs(Function<List<Value>, Value> fn) {
        return new SimpleFunctionValue((c, t) -> fn.apply(((ListValue)c.getVariable("_v").evalValue(c, t)).getItems()), List.of(), "_v");
    }

    public static SimpleFunctionValue ofVarargs(BiFunction<Value, List<Value>, Value> fn) {
        return new SimpleFunctionValue((c, t) -> fn.apply(c.getVariable("_1").evalValue(c, t), ((ListValue)c.getVariable("_v").evalValue(c, t)).getItems()), List.of("_1"), "_v");
    }

    public static SimpleFunctionValue ofVarargs(Fluff.TriFunction<Value, Value, List<Value>, Value> fn) {
        return new SimpleFunctionValue((c, t) -> fn.apply(c.getVariable("_1").evalValue(c, t), c.getVariable("_2").evalValue(c, t), ((ListValue)c.getVariable("_v").evalValue(c, t)).getItems()), List.of("_1", "_2"), "_v");
    }

    public static SimpleFunctionValue ofVarargs(Fluff.QuadFunction<Value, Value, Value, List<Value>, Value> fn) {
        return new SimpleFunctionValue((c, t) -> fn.apply(c.getVariable("_1").evalValue(c, t), c.getVariable("_2").evalValue(c, t), c.getVariable("_3").evalValue(c, t), ((ListValue)c.getVariable("_v").evalValue(c, t)).getItems()), List.of("_1", "_2", "_3"), "_v");
    }

    public static SimpleFunctionValue ofVarargs(Fluff.QuinnFunction<Value, Value, Value, Value, List<Value>, Value> fn) {
        return new SimpleFunctionValue((c, t) -> fn.apply(c.getVariable("_1").evalValue(c, t), c.getVariable("_2").evalValue(c, t), c.getVariable("_3").evalValue(c, t), c.getVariable("_4").evalValue(c, t), ((ListValue)c.getVariable("_v").evalValue(c, t)).getItems()), List.of("_1", "_2", "_3", "_4"), "_v");
    }

    public static SimpleFunctionValue ofVarargs(Fluff.SexFunction<Value, Value, Value, Value, Value, List<Value>, Value> fn) {
        return new SimpleFunctionValue((c, t) -> fn.apply(c.getVariable("_1").evalValue(c, t), c.getVariable("_2").evalValue(c, t), c.getVariable("_3").evalValue(c, t), c.getVariable("_4").evalValue(c, t), c.getVariable("_5").evalValue(c, t), ((ListValue)c.getVariable("_v").evalValue(c, t)).getItems()), List.of("_1", "_2", "_3", "_4", "_5"), "_v");
    }

    public static SimpleFunctionValue run(Runnable fn) {
        return of(() -> {
            fn.run();
            return Value.NULL;
        });
    }

    public static SimpleFunctionValue run(Consumer<Value> fn) {
        return of(a -> {
            fn.accept(a);
            return Value.NULL;
        });
    }

    public static SimpleFunctionValue run(BiConsumer<Value, Value> fn) {
        return of((a, b) -> {
            fn.accept(a, b);
            return Value.NULL;
        });
    }

    public static SimpleFunctionValue run(TriConsumer<Value, Value, Value> fn) {
        return of((a, b, c) -> {
            fn.accept(a, b, c);
            return Value.NULL;
        });
    }

    public static SimpleFunctionValue run(QuadConsumer<Value, Value, Value, Value> fn) {
        return of((a, b, c, d) -> {
            fn.accept(a, b, c, d);
            return Value.NULL;
        });
    }

    public static SimpleFunctionValue run(QuinnConsumer<Value, Value, Value, Value, Value> fn) {
        return of((a, b, c, d, e) -> {
            fn.accept(a, b, c, d, e);
            return Value.NULL;
        });
    }

    public static SimpleFunctionValue run(SexConsumer<Value, Value, Value, Value, Value, Value> fn) {
        return of((a, b, c, d, e, f) -> {
            fn.accept(a, b, c, d, e, f);
            return Value.NULL;
        });
    }

    public static SimpleFunctionValue runVarargs(Consumer<List<Value>> fn) {
        return ofVarargs(a -> {
            fn.accept(a);
            return Value.NULL;
        });
    }

    public static SimpleFunctionValue runVarargs(BiConsumer<Value, List<Value>> fn) {
        return ofVarargs((a, b) -> {
            fn.accept(a, b);
            return Value.NULL;
        });
    }

    public static SimpleFunctionValue runVarargs(TriConsumer<Value, Value, List<Value>> fn) {
        return ofVarargs((a, b, c) -> {
            fn.accept(a, b, c);
            return Value.NULL;
        });
    }

    public static SimpleFunctionValue runVarargs(QuadConsumer<Value, Value, Value, List<Value>> fn) {
        return ofVarargs((a, b, c, d) -> {
            fn.accept(a, b, c, d);
            return Value.NULL;
        });
    }

    public static SimpleFunctionValue runVarargs(QuinnConsumer<Value, Value, Value, Value, List<Value>> fn) {
        return ofVarargs((a, b, c, d, e) -> {
            fn.accept(a, b, c, d, e);
            return Value.NULL;
        });
    }

    public static SimpleFunctionValue runVarargs(SexConsumer<Value, Value, Value, Value, Value, List<Value>> fn) {
        return ofVarargs((a, b, c, d, e, f) -> {
            fn.accept(a, b, c, d, e, f);
            return Value.NULL;
        });
    }
}
