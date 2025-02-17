package me.itut.lanitium.value;

import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.value.FunctionValue;

import java.util.List;
import java.util.Map;

public class SimpleFunctionValue extends FunctionValue {
    public SimpleFunctionValue(LazyValue body, List<String> args, String varArgs) {
        super(Expression.none, null, "_", body, args, varArgs, Map.of());
    }
}
