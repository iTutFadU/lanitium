package me.itut.lanitium.internal.carpet;

import carpet.script.Fluff;

import java.util.Map;

public interface ExpressionInterface {
    Map<String, Fluff.ILazyOperator> lanitium$operators();
    Map<String, Fluff.ILazyFunction> lanitium$functions();
}
