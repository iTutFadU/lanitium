package me.itut.lanitium.internal.carpet;

import carpet.script.Context;
import carpet.script.value.*;
import me.itut.lanitium.value.Constants;
import me.itut.lanitium.value.SimpleFunctionValue;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface MapValueInterface {
    class LMetaValue extends FrameworkValue implements ContainerValueInterface {
        public final MapValue self;

        public LMetaValue(MapValue self) {
            this.self = self;
        }

        @Override
        public boolean put(Value where, Value value) {
            if (value.isNull()) {
                ((MapValueInterface)self).lanitium$setMeta(null);
                return true;
            }
            if (!(value instanceof MapValue meta))
                return false;
            ((MapValueInterface)self).lanitium$setMeta(meta.getMap());
            return true;
        }

        @Override
        public Value get(Value where) {
            Map<Value, Value> meta = ((MapValueInterface)self).lanitium$meta();
            return meta != null ? MapValue.wrap(meta) : Value.NULL;
        }

        @Override
        public boolean has(Value where) {
            return ((MapValueInterface)self).lanitium$meta() != null;
        }

        @Override
        public boolean delete(Value where) {
            if (!has(null)) return false;
            ((MapValueInterface)self).lanitium$setMeta(null);
            return true;
        }
    }

    Map<Value, Value> defaultMetaMethods = Collections.unmodifiableMap(new HashMap<>() {{
        put(Constants.__META, new SimpleFunctionValue(1, 1, (c, t, e, tok, lv) -> {
            Value none = t == Context.LVALUE ? new LContainerValue(null, null) : Value.NULL;
            if (!(lv.getFirst() instanceof MapValue self)) return none;
            Map<Value, Value> meta = ((MapValueInterface)self).lanitium$meta();
            if (meta == null) return none;
            if (t == Context.LVALUE) return new LContainerValue(new LMetaValue(self), null);
            return MapValue.wrap(meta);
        }));
    }});

    @Nullable Map<Value, Value> lanitium$meta();
    void lanitium$setMeta(@Nullable Map<Value, Value> meta);
}
