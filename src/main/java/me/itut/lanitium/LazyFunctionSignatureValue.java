package me.itut.lanitium;

import carpet.script.value.FunctionSignatureValue;

import java.util.List;

public class LazyFunctionSignatureValue extends FunctionSignatureValue {
    public LazyFunctionSignatureValue(String name, List<String> args, String varArgs, List<String> globals) {
        super(name, args, varArgs, globals);
    }

    public static LazyFunctionSignatureValue of(FunctionSignatureValue signature) {
        if (signature instanceof LazyFunctionSignatureValue lazy)
            return lazy;
        return new LazyFunctionSignatureValue(signature.identifier(), signature.arguments(), signature.varArgs(), signature.globals());
    }

    @Override
    public String identifier() {
        return "LAZY#" + originalIdentifier();
    }

    public String originalIdentifier() {
        return super.identifier();
    }
}
