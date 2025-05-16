package me.itut.lanitium.internal.carpet;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.value.*;
import me.itut.lanitium.value.Constants;
import me.itut.lanitium.value.SimpleFunctionValue;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface MapValueInterface {
    class LMetaValue extends FrameworkValue implements ContainerValueInterface {
        private final CarpetContext context;
        public final MapValue self;

        public LMetaValue(CarpetContext context, MapValue self) {
            this.context = context;
            this.self = self;
        }

        @Override
        public boolean put(Value where, Value value) {
            if (value.isNull()) {
                ((MapValueInterface)self).lanitium$setMeta(null, null);
                return true;
            }
            if (!(value instanceof MapValue meta))
                return false;
            ((MapValueInterface)self).lanitium$setMeta(context, meta.getMap());
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
            ((MapValueInterface)self).lanitium$removeMeta();
            return true;
        }
    }

    Map<Value, Value> defaultMetaMethods = Map.of(
        Constants.__META, new SimpleFunctionValue(1, 1, (c, t, e, tok, lv) -> {
            MapValue self = (MapValue)lv.getFirst();
            return t == Context.LVALUE
                ? new LContainerValue(new LMetaValue((CarpetContext)c, self), null)
                : MapValue.wrap(((MapValueInterface)self).lanitium$meta());
        }),
        Constants.__STR, new SimpleFunctionValue(1, 1, (c, t, e, tok, lv) -> StringValue.of(lv.getFirst().getString())),
        Constants.__HASH, new SimpleFunctionValue(1, 1, (c, t, e, tok, lv) -> NumericValue.of(lv.getFirst().hashCode())),
        Constants.__EQ, new SimpleFunctionValue(2, 2, (c, t, e, tok, lv) -> BooleanValue.of(lv.getFirst().equals(lv.getLast())))
    );

    @Nullable Map<Value, Value> lanitium$meta();
    void lanitium$setMeta(CarpetContext context, Map<Value, Value> meta);
    void lanitium$removeMeta();
}
