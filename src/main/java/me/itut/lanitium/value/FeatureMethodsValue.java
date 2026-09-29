package me.itut.lanitium.value;

import carpet.script.Context;
import carpet.script.value.Value;

public interface FeatureMethodsValue {
    Value lanitium$feature(Context.Type type, Context ctx, String what, Value... more);
}
