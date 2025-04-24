package me.itut.lanitium.internal;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Magic {
    private static final Unsafe UNSAFE;
    private static final MethodHandles.Lookup LOOKUP;
    private static final SpecialCall OBJECT_CLONE;

    static {
        UNSAFE = Unsafe.getUnsafe();
        LOOKUP = MethodHandles.lookup();
        OBJECT_CLONE = new SpecialCall(Object.class, "clone", MethodType.methodType(Object.class));
    }

    /** @see Object#clone() */
    @SuppressWarnings("RedundantThrows")
    public static Object cloneObject(Object o) throws CloneNotSupportedException {
        return OBJECT_CLONE.call(o);
    }

    private static final class SpecialCall {
        private final Class<?> clazz;
        private final String name;
        private final MethodType type;
        private final Map<Class<?>, MethodHandle> handles = new ConcurrentHashMap<>();

        private SpecialCall(Class<?> clazz, String name, MethodType type) {
            this.clazz = clazz;
            this.name = name;
            this.type = type;
        }

        /** Effectively throws anything (whatever the method throws) */
        private Object call(Object... args) {
            try {
                return handles.computeIfAbsent(args[0].getClass(), k -> {
                    try {
                        return LOOKUP.findSpecial(clazz, name, type, k);
                    } catch (NoSuchMethodException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }).invoke(args);
            } catch (Throwable e) {
                UNSAFE.throwException(e);
                return null;
            }
        }
    }
}
