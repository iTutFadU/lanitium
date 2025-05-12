package me.itut.lanitium.function;

import carpet.script.Context;
import carpet.script.annotation.ScarpetFunction;
import carpet.script.value.*;
import me.itut.lanitium.value.Symbol;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class Symbols {
    @ScarpetFunction
    public static Value symbol() {
        return new Symbol(new Object());
    }

    @ScarpetFunction
    public static Value identity_hash(Value v) {
        return NumericValue.of(System.identityHashCode(v));
    }

    @ScarpetFunction
    public static Value is_self_referential(Value container) {
        return BooleanValue.of(isSelfReferential(container, Collections.newSetFromMap(new IdentityHashMap<>())));
    }

    public static boolean isSelfReferential(Value container, Set<Object> found) {
        return switch (container) {
            case ListValue l -> {
                if (found.contains(l.getItems())) yield true;
                found.add(l.getItems());
                if (l.getItems().stream().anyMatch(found::contains)) yield true;
                found.remove(l.getItems());
                yield false;
            }
            case MapValue m -> {
                if (found.contains(m.getMap())) yield true;
                found.add(m.getMap());
                if (m.getMap().entrySet().stream().anyMatch(e -> found.contains(e.getValue()) || found.contains(e.getKey())))
                    yield true;
                found.remove(m.getMap());
                yield false;
            }
            default -> false;
        };
    }

    @ScarpetFunction
    public static Value safe_str(Value container) {
        return StringValue.of(safeString(container, Collections.newSetFromMap(new IdentityHashMap<>())));
    }

    public static String safeString(Value container, Set<Object> traversed) {
        return switch (container) {
            case ListValue l -> {
                if (traversed.contains(l.getItems())) yield "[...]";
                traversed.add(l.getItems());
                try {
                    yield "[" + l.getItems().stream().map(e -> safeString(e, traversed)).collect(Collectors.joining(", ")) + "]";
                } finally {
                    traversed.remove(l.getItems());
                }
            }
            case MapValue m -> {
                if (traversed.contains(m.getMap())) yield "{...}";
                traversed.add(m.getMap());
                try {
                    yield "{" + m.getMap().entrySet().stream().map(e -> safeString(e.getKey(), traversed) + ": " + safeString(e.getValue(), traversed)).collect(Collectors.joining(", ")) + "}";
                } finally {
                    traversed.remove(m.getMap());
                }
            }
            default -> container.getString();
        };
    }

    /*
        b = [];     ┌>( a )─┐
        a = [b];    │       │
        b += a;     └─( b )<┘

        e = [];     ┌────>( c )──────┐
        d = [e];    │                │
        c = [d];    └─( e )<──( d )<─┘
        e += c;

        g = [];     ┌>( f )─┐
        f = [g];    │       │
        g += f;     └─( g )<┘

        safe_equal(a, b) => true
        safe_equal(a, c) => false
        safe_equal(a, f) => true
        safe_equal(c, e) => true
    */
    @ScarpetFunction
    public static Value safe_equal(Value a, Value b) {
        return BooleanValue.of(safeEqual(a, b, new IdentityHashMap<>(), new IdentityHashMap<>(), new int[1]));
    }

    public static boolean safeEqual(Value a, Value b, IdentityHashMap<Object, Integer> ta, IdentityHashMap<Object, Integer> tb, int[] counter) {
        if (a == b) return true;
        if (a instanceof ListValue != b instanceof ListValue
            || a instanceof MapValue != b instanceof MapValue)
            return false;
        return switch (a) {
            case ListValue v -> {
                List<Value> la = v.getItems(), lb = ((ListValue)b).getItems();
                if (la == lb) yield true;
                if (ta.containsKey(la)) yield ta.get(la).equals(tb.get(lb));
                if (tb.containsKey(lb)) yield false;
                if (la.size() != lb.size()) yield false;
                if (la.isEmpty()) yield true;
                ta.put(la, counter[0]);
                tb.put(lb, counter[0]++);
                for (Iterator<Value> ita = la.iterator(), itb = lb.iterator(); ita.hasNext(); ) {
                    Value ea = ita.next(), eb = itb.next();
                    if (!safeEqual(ea, eb, ta, tb, counter)) yield false;
                }
                ta.remove(la);
                tb.remove(lb);
                yield true;
            }
            case MapValue v -> {
                Map<Value, Value> ma = v.getMap(), mb = ((MapValue)b).getMap();
                if (ma == mb) yield true;
                if (ta.containsKey(ma)) yield ta.get(ma).equals(tb.get(mb));
                if (tb.containsKey(mb)) yield false;
                if (ma.size() != mb.size()) yield false;
                if (ma.isEmpty()) yield true;
                ta.put(ma, counter[0]);
                tb.put(mb, counter[0]++);
                LinkedList<Map.Entry<Value, Value>> entries = new LinkedList<>(mb.entrySet().stream().toList());
                for (Map.Entry<Value, Value> ea : ma.entrySet()) { // O((n^2+n)/2) worst case
                    Value ka = ea.getKey(), va = ea.getValue();
                    @SuppressWarnings("unchecked")
                    Map.Entry<Value, Value>[] entry = new Map.Entry[]{null};
                    try {
                        entries.removeIf(eb -> {
                            if (entry[0] != null || !safeEqual(ka, eb.getKey(), ta, tb, counter)) return false;
                            if (!safeEqual(va, eb.getValue(), ta, tb, counter)) throw new SafeNotEqualException();
                            entry[0] = eb;
                            return true;
                        });
                    } catch (SafeNotEqualException ignored) {
                        yield false;
                    }
                    if (entry[0] == null) yield false;
                }
                assert entries.isEmpty();
                ta.remove(ma);
                tb.remove(mb);
                yield true;
            }
            default -> a.equals(b);
        };
    }

    private static class SafeNotEqualException extends RuntimeException {}

    @ScarpetFunction
    public static Value safe_copy(Value container) {
        return safeCopy(container, new IdentityHashMap<>());
    }

    public static Value safeCopy(Value container, IdentityHashMap<Object, Value> copied) {
        return switch (container) {
            case ListValue l -> {
                if (copied.containsKey(l.getItems())) yield copied.get(l.getItems());
                List<Value> list = new ArrayList<>(l.length());
                ListValue copy = ListValue.wrap(list);
                if (l.length() == 0) yield copy;
                copied.put(l.getItems(), copy);
                list.addAll(l.getItems().stream().map(e -> safeCopy(e, copied)).toList());
                copied.remove(l.getItems());
                yield copy;
            }
            case MapValue m -> {
                if (copied.containsKey(m.getMap())) yield copied.get(m.getMap());
                Map<Value, Value> map = HashMap.newHashMap(m.length());
                MapValue copy = MapValue.wrap(map);
                if (m.length() == 0) yield copy;
                copied.put(m.getMap(), copy);
                map.putAll(m.getMap().entrySet().stream().collect(Collectors.toMap(
                    e -> safeCopy(e.getKey(), copied),
                    e -> safeCopy(e.getValue(), copied)
                )));
                copied.remove(m.getMap());
                yield copy;
            }
            default -> container.deepcopy();
        };
    }

    /*
        map_elem = (fn(e) -> e + 1);
        map_key = (fn(k, v) -> k + 2);
        map_value = (fn(k, v) -> v + 3);
        safe_deep_map('str', map_elem, map_key, map_value) => 'str'
        safe_deep_map(['str'], map_elem, map_key, map_value) => ['str1']
        safe_deep_map({'a' -> 'b'}, map_elem, map_key, map_value) => {'a2' -> 'b3'}

        loop_inner = [];
        loop_outer = [loop_inner];
        loop_inner += loop_outer;
        print(loop_outer) => 'Your thoughts are too deep'
        print(safe_str(loop_outer)) => '[[[...]]]'

        loop_inner = {};
        loop_list = [loop_inner];
        loop_outer = {'a' -> loop_inner, 'b' -> loop_list};
        loop_inner.c = loop_outer;
        loop_inner.d = loop_list;
        print(safe_str(loop_outer)) => '{a: {c: {...}, d: [{...}]}, b: [{c: {...}, d: [...]}]}'

        { ─────────────────────────────────────────────────────────────┬─( outer )
            a: { ────────────────────────────────────┬─( inner )       │
                c: {...}, <-( outer )                │                 │
                                                     │                 │
                d: [ ───────────────────┬─( list )   │                 │
                    {...} <-( inner )   │            │                 │
                ] ──────────────────────┘            │                 │
            }, ──────────────────────────────────────┘                 │
                                                                       │
            b: [ ─────────────────────────────────────────┬─( list )   │
                { ──────────────────────────┬─( inner )   │            │
                    c: {...}, <-( outer )   │             │            │
                                            │             │            │
                    d: [...] <-( list )     │             │            │
                } ──────────────────────────┘             │            │
            ] ────────────────────────────────────────────┘            │
        } ─────────────────────────────────────────────────────────────┘
    */
    @ScarpetFunction(maxParams = 5)
    public static Value safe_deep_map(Context c, Context.Type t, Value container, FunctionValue lf, FunctionValue kf, FunctionValue vf, Optional<FunctionValue> ref) {
        return safeDeepMap(container,
            v -> lf.callInContext(c, t, List.of(v)).evalValue(c, t),
            (k, v) -> kf.callInContext(c, t, List.of(k, v)).evalValue(c, t),
            (k, v) -> vf.callInContext(c, t, List.of(k, v)).evalValue(c, t),
            ref.map(f -> (BinaryOperator<Value>)(a, b) -> f.callInContext(c, t, List.of(a, b)).evalValue(c, t)).orElse((a, b) -> b),
            new IdentityHashMap<>());
    }

    public static Value safeDeepMap(Value container, UnaryOperator<Value> lf, BinaryOperator<Value> kf, BinaryOperator<Value> vf, BinaryOperator<Value> ref, IdentityHashMap<Object, Value> mapped) {
        return switch (container) {
            case ListValue l -> {
                if (mapped.containsKey(l.getItems())) yield ref.apply(l, mapped.get(l.getItems()));
                List<Value> list = new ArrayList<>(l.length());
                ListValue copy = ListValue.wrap(list);
                if (l.length() == 0) yield copy;
                mapped.put(l.getItems(), copy);
                list.addAll(l.getItems().stream().map(v -> lf.apply(safeDeepMap(v, lf, kf, vf, ref, mapped))).toList());
                mapped.remove(l.getItems());
                yield copy;
            }
            case MapValue m -> {
                if (mapped.containsKey(m.getMap())) yield ref.apply(m, mapped.get(m.getMap()));
                Map<Value, Value> map = HashMap.newHashMap(m.length());
                MapValue copy = MapValue.wrap(map);
                if (m.length() == 0) yield copy;
                mapped.put(m.getMap(), copy);
                map.putAll(m.getMap().entrySet().stream().map(e -> new AbstractMap.SimpleEntry<>(
                    safeDeepMap(e.getKey(), lf, kf, vf, ref, mapped),
                    safeDeepMap(e.getValue(), lf, kf, vf, ref, mapped)
                )).collect(Collectors.toMap(
                    e -> kf.apply(e.getKey(), e.getValue()),
                    e -> vf.apply(e.getKey(), e.getValue())
                )));
                mapped.remove(m.getMap());
                yield copy;
            }
            default -> container;
        };
    }
}
