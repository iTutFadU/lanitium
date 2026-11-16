package me.itut.lanitium.internal.carpet;

import carpet.script.*;
import carpet.script.value.*;
import carpet.script.exception.InternalExpressionException;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.Hash;
import me.itut.lanitium.value.Constants;
import me.itut.lanitium.value.ObjectFunctionValue;
import me.itut.lanitium.value.ObjectValue;

import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

public enum ValueSemantics {;
    public static boolean getBoolean(Value v) {
        return switch (v) {
            case NumericValue num -> num.getDouble() != 0.0;
            default -> v.getBoolean();
        };
    }

    private static InternalExpressionException cannotBeDoneBin(Value a, Value b, String what) {
        return new InternalExpressionException(a.getString() + " and " + b.getString() + " cannot be " + what);
    }

    private static InternalExpressionException cannotUnevenSizes(String what) {
        return new InternalExpressionException("cannot " + what + " lists of uneven sizes");
    }

    public static Value opposite(Value v) {
        return switch (v) {
            case NumericValue n -> new NumericValue(-n.getDouble());
            case StringValue n -> StringValue.of(new StringBuilder(n.getString()).reverse().toString());
            case ListValue n -> ListValue.wrap(n.getItems().stream()
                .map(ValueSemantics::opposite)
                .collect(Collectors.<Value, ArrayList<Value>>toCollection(ArrayList::new))
            );
            default -> throw new InternalExpressionException("cannot take the opposite of " + v.getString());
        };
    }

    public static Value add(Value a, Value b) {
        if (a instanceof NumericValue n && b instanceof NumericValue m) return new NumericValue(n.getDouble() + m.getDouble());
        if (a instanceof FormattedTextValue || b instanceof FormattedTextValue) return FormattedTextValue.combine(a, b);
        if (a instanceof StringValue || b instanceof StringValue) return StringValue.of(a.getString() + b.getString());
        if (a instanceof AbstractListValue && !(a instanceof ListValue)) throw cannotBeDoneBin(a, b, "added");
        if (b instanceof AbstractListValue && !(b instanceof ListValue)) throw cannotBeDoneBin(a, b, "added");

        List<Value> cs;
        if (a instanceof ListValue as) {
            int size = as.length();
            cs = new ArrayList<>(size);

            if (b instanceof ListValue bs) {
                if (size != bs.length()) throw cannotUnevenSizes("add");
                // Error out on concurrent modification
                for (Iterator<Value> ai = as.iterator(), bi = bs.iterator(); ai.hasNext() || bi.hasNext();)
                    cs.add(add(ai.next(), bi.next()));
            } else for (Value ae : as) cs.add(add(ae, b));
        } else if (b instanceof ListValue bs) {
            cs = new ArrayList<>(bs.length());
            for (Value be : bs) cs.add(add(a, be));
        } else return a.add(b);
        return ListValue.wrap(cs);
    }

    public static Value subtract(Value a, Value b) {
        if (a instanceof NumericValue n && b instanceof NumericValue m) return new NumericValue(n.getDouble() - m.getDouble());
        if (a instanceof StringValue || b instanceof StringValue) return StringValue.of(a.getString().replace(b.getString(), ""));
        if (a instanceof AbstractListValue && !(a instanceof ListValue)) throw cannotBeDoneBin(a, b, "subtracted");
        if (b instanceof AbstractListValue && !(b instanceof ListValue)) throw cannotBeDoneBin(a, b, "subtracted");

        List<Value> cs;
        if (a instanceof ListValue as) {
            int size = as.length();
            cs = new ArrayList<>(size);

            if (b instanceof ListValue bs) {
                if (size != bs.length()) throw cannotUnevenSizes("subtract");
                // Error out on concurrent modification
                for (Iterator<Value> ai = as.iterator(), bi = bs.iterator(); ai.hasNext() || bi.hasNext();)
                    cs.add(subtract(ai.next(), bi.next()));
            } else for (Value ae : as) cs.add(subtract(ae, b));
        } else if (b instanceof ListValue bs) {
            cs = new ArrayList<>(bs.length());
            for (Value be : bs) cs.add(subtract(a, be));
        } else return a.subtract(b);
        return ListValue.wrap(cs);
    }

    public static Value multiply(Value a, Value b) {
        if (a instanceof NumericValue n && b instanceof NumericValue m) return new NumericValue(n.getDouble() * m.getDouble());
        if (a instanceof StringValue && b instanceof NumericValue n) return StringValue.of(a.getString().repeat(n.getInt()));
        if (a instanceof NumericValue n && b instanceof StringValue) return StringValue.of(b.getString().repeat(n.getInt()));
        if (a instanceof StringValue && b instanceof StringValue) throw cannotBeDoneBin(a, b, "multiplied");
        if (a instanceof AbstractListValue && !(a instanceof ListValue)) throw cannotBeDoneBin(a, b, "multiplied");
        if (b instanceof AbstractListValue && !(b instanceof ListValue)) throw cannotBeDoneBin(a, b, "multiplied");

        // TODO: composition

        List<Value> cs;
        if (a instanceof ListValue as) {
            int size = as.length();
            cs = new ArrayList<>(size);

            if (b instanceof ListValue bs) {
                if (size != bs.length()) throw cannotUnevenSizes("multiply");
                // Error out on concurrent modification
                for (Iterator<Value> ai = as.iterator(), bi = bs.iterator(); ai.hasNext() || bi.hasNext();)
                    cs.add(multiply(ai.next(), bi.next()));
            } else for (Value ae : as) cs.add(multiply(ae, b));
        } else if (b instanceof ListValue bs) {
            cs = new ArrayList<>(bs.length());
            for (Value be : bs) cs.add(multiply(a, be));
        } else return a.multiply(b);
        return ListValue.wrap(cs);
    }

    public static Value divide(Value a, Value b) {
        if (a instanceof NumericValue n && b instanceof NumericValue m) return new NumericValue(n.getDouble() / m.getDouble());
        if (a instanceof StringValue && b instanceof NumericValue n) {
            String str = a.getString();
            return StringValue.of(str.substring(0, str.length() / n.getInt()));
        }
        if (b instanceof StringValue) throw cannotBeDoneBin(a, b, "divided");
        if (a instanceof AbstractListValue && !(a instanceof ListValue)) throw cannotBeDoneBin(a, b, "divided");
        if (b instanceof AbstractListValue && !(b instanceof ListValue)) throw cannotBeDoneBin(a, b, "divided");

        List<Value> cs;
        if (a instanceof ListValue as) {
            int size = as.length();
            cs = new ArrayList<>(size);

            if (b instanceof ListValue bs) {
                if (size != bs.length()) throw cannotUnevenSizes("divide");
                // Error out on concurrent modification
                for (Iterator<Value> ai = as.iterator(), bi = bs.iterator(); ai.hasNext() || bi.hasNext();)
                    cs.add(divide(ai.next(), bi.next()));
            } else for (Value ae : as) cs.add(divide(ae, b));
        } else if (b instanceof ListValue bs) {
            cs = new ArrayList<>(bs.length());
            for (Value be : bs) cs.add(divide(a, be));
        } else return a.divide(b);
        return ListValue.wrap(cs);
    }

    public static Value modulo(Value a, Value b) {
        if (a instanceof NumericValue n && b instanceof NumericValue m)
            return new NumericValue(n.getDouble() - Math.floor(n.getDouble() / m.getDouble()) * m.getDouble());
        if (a instanceof StringValue && b instanceof NumericValue) throw cannotBeDoneBin(a, b, "taken modulo of");

        if (b instanceof StringValue) {
            char[] chars = a.getString().toCharArray();
            char[] delim = b.getString().toCharArray();
            ArrayList<Value> split = new ArrayList<>();
            if (delim.length == 0) {
                split.ensureCapacity(chars.length);
                for (char c : chars) split.add(new StringValue("" + c));
            } else {
                int start = 0;
                split: for (int i = 0, max = chars.length - delim.length; i <= max;) {
                    for (int j = 0; j < delim.length; j++) if (chars[i + j] != delim[j]) {
                        i++;
                        continue split;
                    }
                    split.add(new StringValue(new String(chars, start, i - start)));
                    start = i += delim.length;
                }
                split.add(new StringValue(new String(chars, start, chars.length - start)));
            }
            return ListValue.wrap(split);
        }

        if (a instanceof AbstractListValue && !(a instanceof ListValue)) throw cannotBeDoneBin(a, b, "taken modulo of");
        if (b instanceof AbstractListValue && !(b instanceof ListValue)) throw cannotBeDoneBin(a, b, "taken modulo of");

        List<Value> cs;
        if (a instanceof ListValue as) {
            int size = as.length();
            cs = new ArrayList<>(size);

            if (b instanceof ListValue bs) {
                if (size != bs.length()) throw cannotUnevenSizes("modulo");
                // Error out on concurrent modification
                for (Iterator<Value> ai = as.iterator(), bi = bs.iterator(); ai.hasNext() || bi.hasNext();)
                    cs.add(modulo(ai.next(), bi.next()));
            } else for (Value ae : as) cs.add(modulo(ae, b));
        } else if (b instanceof ListValue bs) {
            cs = new ArrayList<>(bs.length());
            for (Value be : bs) cs.add(modulo(a, be));
        } else throw cannotBeDoneBin(a, b, "taken modulo of");
        return ListValue.wrap(cs);
    }

    public static Value exponent(Value a, Value b) {
        if (a instanceof NumericValue n && b instanceof NumericValue m)
            return new NumericValue(Math.pow(n.getDouble(), m.getDouble()));
        if (a instanceof StringValue || b instanceof StringValue) throw cannotBeDoneBin(a, b, "exponentiated");
        if (a instanceof AbstractListValue && !(a instanceof ListValue)) throw cannotBeDoneBin(a, b, "exponentiated");
        if (b instanceof AbstractListValue && !(b instanceof ListValue)) throw cannotBeDoneBin(a, b, "exponentiated");

        List<Value> cs;
        if (a instanceof ListValue as) {
            int size = as.length();
            cs = new ArrayList<>(size);

            if (b instanceof ListValue bs) {
                if (size != bs.length()) throw cannotUnevenSizes("exponentiate");
                // Error out on concurrent modification
                for (Iterator<Value> ai = as.iterator(), bi = bs.iterator(); ai.hasNext() || bi.hasNext();)
                    cs.add(exponent(ai.next(), bi.next()));
            } else for (Value ae : as) cs.add(exponent(ae, b));
        } else if (b instanceof ListValue bs) {
            cs = new ArrayList<>(bs.length());
            for (Value be : bs) cs.add(exponent(a, be));
        } else throw cannotBeDoneBin(a, b, "exponentiated");
        return ListValue.wrap(cs);
    }

    public static final Hash.Strategy<Value> HASH = new Hash.Strategy<>() {
        @Override
        public boolean equals(Value a, Value b) {
            return equal(a, b);
        }

        @Override
        public int hashCode(Value v) {
            return hash(v);
        }
    };

    public static Object2ObjectLinkedOpenCustomHashMap<Value, Value> newMap() {
        return new Object2ObjectLinkedOpenCustomHashMap<>(HASH);
    }

    public static boolean equal(Value a, Value b) {
        return switch (a) {
            case NullValue _ -> b.isNull();
            case NumericValue n -> b instanceof NumericValue m && n.getDouble() == m.getDouble();
            case StringValue n -> b instanceof StringValue m && n.getString().equals(m.getString());
            case ListValue n -> {
                if (!(b instanceof ListValue m && n.length() == m.length())) yield false;
                for (Iterator<Value> ai = n.iterator(), bi = m.iterator(); ai.hasNext() || bi.hasNext();) {
                    Value ae = ai.next(), be = bi.next();
                    if (!equal(ae, be)) yield false;
                }
                yield true;
            }
            case MapValue n -> {
                if (((MapValueInterface)n).lanitium$meta() instanceof Map<Value, Value> meta
                 && meta.get(Constants.__EQ) instanceof FunctionValue) yield a.equals(b);

                if (!(b instanceof MapValue m && n.length() == m.length())) yield false;

                Map<Value, Value> ma = n.getMap(), mb = m.getMap();
                if (!(ma instanceof Object2ObjectLinkedOpenCustomHashMap<Value, Value>)
                 && mb instanceof Object2ObjectLinkedOpenCustomHashMap<Value, Value>) {
                    ma = mb;
                    mb = n.getMap();
                }

                final Map<Value, Value> maybe = mb;
                yield ma.entrySet().stream()
                    .allMatch(e -> {
                        Value v = maybe.get(e.getKey());
                        return v != null && equal(e.getValue(), v);
                    });
            }
            default -> a.equals(b);
        };
    }

    public static int hash(Value v) {
        return switch (v) {
            case NumericValue n -> Double.hashCode(n.getDouble());
            case StringValue n -> n.getString().hashCode();
            case ListValue n -> {
                int hash = 1;
                for (Value e : n.getItems()) hash = hash * 31 + hash(e);
                yield hash;
            }
            case MapValue n -> {
                if (((MapValueInterface)n).lanitium$meta() instanceof Map<Value, Value> meta
                 && meta.get(Constants.__HASH) instanceof FunctionValue) yield v.hashCode();

                int hash = 0;
                for (Map.Entry<Value, Value> e : n.getMap().entrySet())
                    hash += hash(e.getKey()) ^ hash(e.getValue());
                yield hash;
            }
            default -> v.hashCode();
        };
    }

    private static <V extends Value> V compareCheck(Value a, Value b, Class<? extends V> clazz) {
        if (!clazz.isAssignableFrom(b.getClass())) throw cannotBeDoneBin(a, b, "compared");
        return clazz.cast(b);
    }

    public static int compare(Value a, Value b) {
        if (b.isNull()) return a.isNull() ? 0 : 1;
        return switch (a) {
            case NullValue _ -> -1;
            case NumericValue n -> Double.compare(n.getDouble(), compareCheck(a, b, NumericValue.class).getDouble());
            case StringValue n -> n.getString().compareTo(compareCheck(a, b, StringValue.class).getString());
            case ListValue n -> {
                ListValue m = compareCheck(a, b, ListValue.class);
                List<Value> as = n.getItems(), bs = m.getItems();
                if (as.size() != bs.size()) yield Integer.compare(as.size(), bs.size());
                for (Iterator<Value> ai = as.iterator(), bi = bs.iterator(); ai.hasNext() || bi.hasNext();) {
                    int cmp = compare(ai.next(), bi.next());
                    if (cmp != 0) yield cmp;
                }
                yield 0;
            }
            case MapValue n -> throw cannotBeDoneBin(a, b, "compared");
            case ObjectValue n -> {
                if (!(n.value instanceof Comparable) || !(b instanceof ObjectValue m))
                    throw cannotBeDoneBin(a, b, "compared");
                yield n.compareTo(m);
            }
            case ObjectFunctionValue n -> {
                if (!(n.value instanceof Comparable) || !(b instanceof ObjectFunctionValue m))
                    throw cannotBeDoneBin(a, b, "compared");
                yield n.compareTo(m);
            }
            default -> a.compareTo(b);
        };
    }

    // TODO: better fish?
    public static void extend(Value container, List<Value> elements) {
        switch (container) {
            case NullValue _ -> {}
            case ListValue list -> list.extend(elements);
            case MapValue map -> {
                Map<Value, Value> entries = map.getMap();
                for (Value v : elements) if (!(v instanceof ListValue entry) || entry.length() != 2)
                    throw new InternalExpressionException("Only pairs can be appended to a map: " + v.getString());
                for (Value v : elements) {
                    List<Value> pair = ((ListValue)v).getItems();
                    entries.put(pair.getFirst(), pair.getLast());
                }
            }
            default -> throw new InternalExpressionException(
                "Cannot extend " + container.getString() + " with [" + elements.stream()
                    .map(Value::getString)
                    .collect(Collectors.joining(", ")) + "]"
            );
        }
    }
}
