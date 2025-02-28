package me.itut.lanitium.value;

import carpet.script.Context;
import carpet.script.LazyValue;

public interface WithValue {
    LazyValue with(Context c, Context.Type t, LazyValue arg);
}
