package me.itut.lanitium.function;

import carpet.script.*;
import carpet.script.exception.BreakStatement;
import carpet.script.exception.ContinueStatement;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.language.Operators;
import carpet.script.value.*;
import me.itut.lanitium.internal.carpet.ExpressionInterface;
import me.itut.lanitium.value.pattern.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Patterns {
    public static void apply(Expression expr) {
        final int andPrecedence = Operators.precedence.get("and&&");
        Fluff.ILazyOperator and = new Fluff.ILazyOperator() {
            @Override
            public int getPrecedence() {
                return andPrecedence;
            }

            @Override
            public boolean isLeftAssoc() {
                return true;
            }

            @Override
            public LazyValue lazyEval(Context c, Context.Type t, Expression e, Token tok, LazyValue l, LazyValue r) {
                if (t == Context.LVALUE) {
                    Value p = l.evalValue(c, t);
                    ConditionPatternValue.checkPattern(e, tok, c, p);
                    Value ret = new ConditionPatternValue(e, tok, p, r);
                    return (cc, tt) -> ret;
                }
                Value v = l.evalValue(c, Context.BOOLEAN);
                return v.getBoolean() ? r : (cc, tt) -> v;
            }

            @Override
            public boolean pure() {
                return true;
            }

            @Override
            public boolean transitive() {
                return false;
            }
        };
        ((ExpressionInterface)expr).lanitium$operators().put("&&", and);
        ((ExpressionInterface)expr).lanitium$functions().put("and", new Fluff.ILazyFunction() {
            @Override
            public int getNumParams() {
                return -1;
            }

            @Override
            public boolean numParamsVaries() {
                return true;
            }

            @Override
            public LazyValue lazyEval(Context c, Context.Type t, Expression e, Token tok, List<LazyValue> lv) {
                if (t == Context.LVALUE) return switch (lv.size()) {
                    case 0 -> LazyValue.TRUE;
                    case 1 -> lv.getFirst();
                    case 2 -> and.lazyEval(c, t, e, tok, lv.getFirst(), lv.getLast());
                    default -> throw new ExpressionException(c, e, tok, "Multiple condition patterns are not allowed, use pattern && (condition1 && condition2) instead of pattern && condition1 && condition2");
                };

                Value v = Value.TRUE;
                for (LazyValue l : lv) {
                    Value val = l.evalValue(c, Context.BOOLEAN);
                    if (val instanceof final FunctionUnpackedArgumentsValue fuav)
                        for (Value it : fuav) {
                            if (!it.getBoolean())
                                return (cc, tt) -> it;
                            v = it;
                        }
                    else if (!val.getBoolean())
                        return (cc, tt) -> val;
                    else v = val;
                }
                Value ret = v;
                return (cc, tt) -> ret;
            }

            @Override
            public boolean pure() {
                return true;
            }

            @Override
            public boolean transitive() {
                return false;
            }
        });

        expr.addLazyBinaryOperatorWithDelegation("->", "define", Operators.precedence.get("def->"), false, false, (c, t, e, tok, l, r) -> {
            if (t == Context.LVALUE) {
                Value p = r.evalValue(c, t);
                EntryPatternValue.checkPattern(c, p);
                Value ret = new EntryPatternValue(e, tok, l.evalValue(c), p);
                return (cc, tt) -> ret;
            } if (t == Context.MAPDEF) {
                Value result = ListValue.of(l.evalValue(c), r.evalValue(c));
                return (cc, tt) -> result;
            }
            Value v = l.evalValue(c, Context.SIGNATURE);
            if (!(v instanceof final FunctionSignatureValue sign)) {
                throw new InternalExpressionException("'->' operator requires a function signature on the LHS");
            }
            Value result = expr.createUserDefinedFunction(c, sign.identifier(), e, tok, sign.arguments(), sign.varArgs(), sign.globals(), r);
            return (cc, tt) -> result;
        });

        final int unaryPrecedence = Operators.precedence.get("unary+-!...");
        Fluff.ILazyOperator unpack = new Fluff.ILazyOperator() {
            @Override
            public int getPrecedence() {
                return unaryPrecedence;
            }

            @Override
            public boolean isLeftAssoc() {
                return false;
            }

            @Override
            public boolean pure() {
                return true;
            }

            @Override
            public boolean transitive() {
                return false;
            }

            @Override
            public LazyValue lazyEval(Context c, Context.Type t, Expression e, Token tok, LazyValue v, LazyValue v2) {
                if (t == Context.LVALUE) {
                    Value p = v.evalValue(c, t);
                    RestPatternValue.checkPattern(e, tok, c, p);
                    Value ret = new RestPatternValue(e, tok, p);
                    return (cc, tt) -> ret;
                } if (t == Context.LOCALIZATION)
                    return (cc, tt) -> new FunctionAnnotationValue(v.evalValue(c), FunctionAnnotationValue.Type.VARARG);
                if (!(v.evalValue(c, t) instanceof final AbstractListValue alv))
                    throw new InternalExpressionException("Unable to unpack a non-list");
                FunctionUnpackedArgumentsValue fuaval = new FunctionUnpackedArgumentsValue(alv.unpack());
                return (cc, tt) -> fuaval;
            }
        };
        ((ExpressionInterface)expr).lanitium$operators().put("...u", unpack);
        ((ExpressionInterface)expr).lanitium$functions().put("unpack", new Fluff.ILazyFunction() {
            @Override
            public int getNumParams() {
                return 1;
            }

            @Override
            public boolean numParamsVaries() {
                return false;
            }

            @Override
            public LazyValue lazyEval(Context c, Context.Type t, Expression e, Token tok, List<LazyValue> lv) {
                return unpack.lazyEval(c, t, e, tok, lv.getFirst(), null);
            }

            @Override
            public boolean pure() {
                return true;
            }

            @Override
            public boolean transitive() {
                return false;
            }
        });


        expr.addLazyFunctionWithDelegation("l", -1, true, false, (c, t, e, tok, lv) -> {
            if (t == Context.LVALUE) {
                int[] vars = {-1, 0};
                List<Value> values = lv.stream().map(l -> {
                    Value v = l.evalValue(c, t);
                    ListPatternValue.checkPattern(c, v, vars, vars[1]++);
                    return v;
                }).toList();
                Value ret = new ListPatternValue(e, tok, values, vars[0]);
                return (cc, tt) -> ret;
            }
            List<Value> items = Fluff.AbstractLazyFunction.unpackLazy(lv, c, Context.NONE);
            Value ret = items.size() == 1 && lv.getFirst() instanceof final LazyListValue llv
                ? ListValue.wrap(llv.unroll())
                : new ListValue.ListConstructorValue(items);
            return (cc, tt) -> ret;
        });

        expr.addLazyFunctionWithDelegation("m", -1, true, false, (c, t, e, tok, lv) -> {
            if (t == Context.LVALUE) {
                int[] vars = {-1, 0, 0};
                List<Value> values = lv.stream().map(l -> {
                    Value v = l.evalValue(c, t);
                    MapPatternValue.checkPattern(e, tok, c, v, vars, vars[2]++, false, false);
                    return v;
                }).toList();
                Value ret = new MapPatternValue(e, tok, values, vars[0], vars[1]);
                return (cc, tt) -> ret;
            }
            List<Value> pairs = Fluff.AbstractLazyFunction.unpackLazy(lv, c, Context.MAPDEF);
            Value ret = pairs.size() == 1 && lv.getFirst() instanceof final LazyListValue llv
                ? new MapValue(llv.unroll())
                : new MapValue(pairs);
            return (cc, tt) -> ret;
        });

        expr.addLazyBinaryOperatorWithDelegation("=", "assign", Operators.precedence.get("assign=<>"), false, false, (c, t, e, tok, l, r) -> {
            Value pattern = l.evalValue(c, Context.LVALUE);
            if (t == Context.LVALUE) {
                DefaultPatternValue.checkPattern(e, tok, c, pattern);
                Value ret = new DefaultPatternValue(e, tok, pattern, r);
                return (cc, tt) -> ret;
            }

            if (pattern.isBound()) {
                Value rebounded = r.evalValue(c).reboundedTo(pattern.boundVariable);
                e.setAnyVariable(c, rebounded.boundVariable, (cc, tt) -> rebounded);
                return (cc, tt) -> rebounded;
            }

            Patterns.checkAssignmentPattern(e, tok, c, pattern);
            Value assign = r.evalValue(c);
            Patterns.AssignmentOperation operation = Patterns.assignPattern(e, tok, c, pattern, assign);
            operation.traverse(mutation -> {
                if (mutation.lvalue instanceof LContainerValue container) {
                    if (container.container() != null)
                        container.container().put(container.address(), mutation.rvalue);
                } else {
                    Value rebounded = mutation.rvalue.reboundedTo(mutation.lvalue.boundVariable);
                    e.setAnyVariable(c, rebounded.boundVariable, (cc, tt) -> rebounded);
                }
            });
            return (cc, tt) -> assign;
        });

        expr.addLazyBinaryOperatorWithDelegation("+=", "append", Operators.precedence.get("assign=<>"), false, false, (c, t, e, tok, l, r) -> {
            Value pattern = l.evalValue(c, Context.LVALUE);
            if (t == Context.LVALUE) {
                DefaultPatternValue.checkPattern(e, tok, c, pattern);
                Value ret = new DefaultPatternValue(e, tok, pattern, r);
                return (cc, tt) -> ret;
            }

            if (pattern.isBound()) {
                Value ret;
                if (pattern instanceof ListValue || pattern instanceof MapValue) {
                    ((AbstractListValue)pattern).append(r.evalValue(c));
                    ret = pattern;
                } else {
                    ret = pattern.add(r.evalValue(c)).bindTo(pattern.boundVariable);
                    e.setAnyVariable(c, ret.boundVariable, (cc, tt) -> ret);
                }
                return (cc, tt) -> ret;
            }

            Patterns.checkAssignmentPattern(e, tok, c, pattern);
            Patterns.AssignmentOperation operation = Patterns.assignPattern(e, tok, c, pattern, r.evalValue(c));
            if (t == Context.VOID) {
                operation.traverse(mutation -> {
                    if (mutation.lvalue instanceof LContainerValue container) {
                        if (container.container() == null) return;
                        Value element = container.container().get(container.address());
                        if (element instanceof ListValue || element instanceof MapValue)
                            ((AbstractListValue)element).append(mutation.rvalue);
                        else
                            container.container().put(container.address(), element.add(mutation.rvalue));
                    } else if (mutation.lvalue instanceof ListValue || mutation.lvalue instanceof MapValue)
                        ((AbstractListValue)mutation.lvalue).append(mutation.rvalue);
                    else {
                        Value bound = mutation.lvalue.add(mutation.rvalue).bindTo(mutation.lvalue.boundVariable);
                        e.setAnyVariable(c, bound.boundVariable, (cc, tt) -> bound);
                    }
                });
                return LazyValue.NULL;
            }

            Value ret = operation.traverse(mutation -> {
                if (mutation.lvalue instanceof LContainerValue container) {
                    if (container.container() == null) return Value.NULL;
                    Value element = container.container().get(container.address());
                    if (element instanceof ListValue || element instanceof MapValue) {
                        ((AbstractListValue)element).append(mutation.rvalue);
                        return element;
                    }
                    Value sum = element.add(mutation.rvalue);
                    container.container().put(container.address(), sum);
                    return sum;
                }

                if (mutation.lvalue instanceof ListValue || mutation.lvalue instanceof MapValue) {
                    ((AbstractListValue)mutation.lvalue).append(mutation.rvalue);
                    return mutation.lvalue;
                }
                Value bound = mutation.lvalue.add(mutation.rvalue).bindTo(mutation.lvalue.boundVariable);
                e.setAnyVariable(c, bound.boundVariable, (cc, tt) -> bound);
                return bound;
            });
            return (cc, tt) -> ret;
        });

        expr.addLazyBinaryOperatorWithDelegation("<>", "swap", Operators.precedence.get("assign=<>"), false, false, (c, t, e, tok, l, r) -> {
            Value ll = l.evalValue(c, Context.LVALUE);
            checkAssignmentPattern(e, tok, c, ll);
            Value rl = r.evalValue(c, Context.LVALUE);
            checkAssignmentPattern(e, tok, c, rl);

            if (ll.isBound() && rl.isBound()) {
                Value rv = ll.reboundedTo(rl.boundVariable);
                Value lv = rl.reboundedTo(ll.boundVariable);
                e.setAnyVariable(c, lv.boundVariable, (cc, tt) -> lv);
                e.setAnyVariable(c, rv.boundVariable, (cc, tt) -> rv);
                return (cc, tt) -> rv;
            }

            Value rv = r.evalValue(c); // ehhh
            AssignmentOperation assignLeft = assignPattern(e, tok, c, ll, rv);
            Value lv = l.evalValue(c); // ehhh
            AssignmentOperation assignRight = assignPattern(e, tok, c, rl, lv);

            assignLeft.traverse(mutation -> {
                if (mutation.lvalue instanceof LContainerValue container) {
                    if (container.container() != null)
                        container.container().put(container.address(), mutation.rvalue);
                } else {
                    Value rebounded = mutation.rvalue.reboundedTo(mutation.lvalue.boundVariable);
                    e.setAnyVariable(c, rebounded.boundVariable, (cc, tt) -> rebounded);
                }
            });

            assignRight.traverse(mutation -> {
                if (mutation.lvalue instanceof LContainerValue container) {
                    if (container.container() != null)
                        container.container().put(container.address(), mutation.rvalue);
                } else {
                    Value rebounded = mutation.rvalue.reboundedTo(mutation.lvalue.boundVariable);
                    e.setAnyVariable(c, rebounded.boundVariable, (cc, tt) -> rebounded);
                }
            });

            return (cc, tt) -> rv;
        });

        expr.addLazyFunctionWithDelegation("switch", -1, false, false, (c, t, e, tok, lv) -> {
            if (lv.size() % 2 == 0) throw new InternalExpressionException("'switch' requires a value to test and pairs of pattern and result");
            Value test = lv.getFirst().evalValue(c);
            Map<String, @Nullable LazyValue> scope = new HashMap<>();
            try {
                for (int i = 1, size = lv.size(); i < size; i += 2) try {
                    Value pattern = lv.get(i).evalValue(c, Context.LVALUE);
                    checkSwitchPattern(e, tok, c, pattern);
                    if (!switchPattern(c, pattern, test, scope)) continue;
                    Value ret = lv.get(i + 1).evalValue(c, t);
                    return (cc, tt) -> ret;
                } catch (ContinueStatement ignored) {} finally {
                    scope.forEach((k, v) -> {
                        if (v == null)
                            c.delVariable(k);
                        else
                            c.setVariable(k, v);
                    });
                    scope.clear();
                }
            } catch (BreakStatement exc) {
                Value ret = exc.retval != null ? exc.retval : Value.NULL;
                return (cc, tt) -> ret;
            }
            throw new InternalExpressionException("No branches matched in a switch: " + test.getString());
        });
    }

    private static void checkAssignmentPattern(Expression expression, Token token, Context context, Value pattern) throws ExpressionException {
        switch (pattern) {
            case LContainerValue ignored -> {}
            case ConditionPatternValue condition -> throw new ExpressionException(context, condition.expression, condition.token, "Condition pattern is not allowed in assignment");
            case DefaultPatternValue defaultPattern -> checkAssignmentPattern(defaultPattern.expression, defaultPattern.token, context, defaultPattern.pattern);
            case EntryPatternValue entry -> checkAssignmentPattern(entry.expression, entry.token, context, entry.pattern);
            case ListPatternValue list -> list.values.forEach(v -> checkAssignmentPattern(list.expression, list.token, context, v));
            case MapPatternValue map -> map.values.forEach(v -> checkAssignmentPattern(map.expression, map.token, context, v));
            case RestPatternValue rest -> checkAssignmentPattern(rest.expression, rest.token, context, rest.pattern);
            default -> {
                if (!pattern.isBound())
                    throw new ExpressionException(context, expression, token, "Literal pattern is not allowed in assignment");
            }
        }
    }

    private static AssignmentOperation assignPattern(Expression expression, Token token, Context context, Value pattern, Value assign) throws ExpressionException {
        return switch (pattern) {
            case ListPatternValue list -> {
                if (!(assign instanceof AbstractListValue l) || l instanceof MapValue)
                    throw new ExpressionException(context, list.expression, list.token, "List pattern expects a list, not a " + assign.getTypeString());
                List<Value> items = l.unpack();

                int size = list.rest >= 0 ? list.values.size() - 1 : list.values.size();

                if (items.size() < size)
                    throw new ExpressionException(context, list.expression, list.token, "Too few elements: expected %s, got %d".formatted(list.rest < 0 ? size : "at least " + size, items.size()));
                if (list.rest < 0 && items.size() > size)
                    throw new ExpressionException(context, list.expression, list.token, "Too many elements: expected %d, got %d".formatted(size, items.size()));

                List<AssignmentOperation> operations = new ArrayList<>(list.values.size());
                if (list.rest < 0) {
                    for (Iterator<Value> itl = list.values.iterator(), itr = items.iterator(); itl.hasNext(); )
                        operations.add(assignPattern(list.expression, list.token, context, itl.next(), itr.next()));
                } else {
                    for (int i = 0; i < list.rest; i++)
                        operations.add(assignPattern(list.expression, list.token, context, list.values.get(i), items.get(i)));
                    int len = list.values.size(), off = items.size() - len;
                    for (int i = list.rest + 1; i < len; i++)
                        operations.add(assignPattern(list.expression, list.token, context, list.values.get(i), items.get(i + off)));

                    RestPatternValue rest = (RestPatternValue)list.values.get(list.rest);
                    operations.add(assignPattern(rest.expression, rest.token, context, rest.pattern, ListValue.wrap(items.subList(list.rest, list.rest + off + 1))));
                }
                yield new AssignmentList(operations, list.rest);
            }
            case MapPatternValue map -> {
                if (!(assign instanceof MapValue m))
                    throw new ExpressionException(context, map.expression, map.token, "Map pattern expects a map, not a " + assign.getTypeString());
                Map<Value, Value> pairs = new HashMap<>(m.getMap());

                if (pairs.size() < map.minSize)
                    throw new ExpressionException(context, map.expression, map.token, "Too few entries: expected %s, got %d".formatted(map.rest >= 0 ? "at least " + map.minSize : map.values.size() > map.minSize ? "from " + map.minSize + " to " + map.values.size() : map.minSize, pairs.size()));
                else if (map.rest < 0 && pairs.size() > map.values.size())
                    throw new ExpressionException(context, map.expression, map.token, "Too many entries: expected %s, got %d".formatted(map.values.size() > map.minSize ? "from " + map.minSize + " to " + map.values.size() : map.minSize, pairs.size()));

                List<AssignmentMap.Entry> entries = new ArrayList<>(map.values.size());

                for (Value p : map.values) {
                    Expression expr = expression;
                    Token tok = token;
                    LazyValue defaultValue = null;
                    Value key = null;

                    if (p instanceof EntryPatternValue e) {
                        expr = e.expression;
                        tok = e.token;
                        key = e.key;
                        p = e.pattern;
                    }
                    if (p instanceof DefaultPatternValue d) {
                        expr = d.expression;
                        tok = d.token;
                        defaultValue = d.defaultValue;
                        p = d.pattern;
                    }

                    if (p instanceof RestPatternValue) continue;
                    if (key == null) key = StringValue.of(p.boundVariable);

                    Value value = pairs.remove(key);
                    if (value == null && defaultValue == null)
                        throw new ExpressionException(context, expr, tok, "Entry not found for map pattern: " + key.getString());
                    if (value == null) value = defaultValue.evalValue(context);
                    entries.add(new AssignmentMap.Entry(key, assignPattern(expr, tok, context, p, value)));
                }

                if (map.rest >= 0) {
                    RestPatternValue rest = (RestPatternValue)map.values.get(map.rest);
                    entries.add(new AssignmentMap.Entry(null, assignPattern(rest.expression, rest.token, context, rest.pattern, MapValue.wrap(pairs))));
                }

                yield new AssignmentMap(entries, map.rest);
            }
            case LContainerValue container -> new AssignmentMutation(container, assign);
            case DefaultPatternValue defaultPattern -> throw new ExpressionException(context, defaultPattern.expression, defaultPattern.token, "Default pattern is not assignable, use var1 = var2 = value instead of (var1 = var2) = value for chained assignment");
            case EntryPatternValue entry -> throw new ExpressionException(context, entry.expression, entry.token, "Entry pattern is not assignable, use key -> var = value instead of (key -> var) = value for definition with assignment");
            case RestPatternValue rest -> throw new ExpressionException(context, rest.expression, rest.token, "Rest pattern is not assignable, use ...(var = value) instead of ...var = value to spread an assignment");
            default -> {
                assert pattern.isBound();
                yield new AssignmentMutation(pattern, assign);
            }
        };
    }

    private sealed interface AssignmentOperation {
        Value traverse(Function<AssignmentMutation, Value> function);
        void traverse(Consumer<AssignmentMutation> consumer);
    }

    private record AssignmentMutation(Value lvalue, Value rvalue) implements AssignmentOperation {
        @Override
        public Value traverse(Function<AssignmentMutation, Value> function) {
            return function.apply(this);
        }

        @Override
        public void traverse(Consumer<AssignmentMutation> consumer) {
            consumer.accept(this);
        }
    }

    private record AssignmentList(List<AssignmentOperation> operations, int rest) implements AssignmentOperation {
        @Override
        public Value traverse(Function<AssignmentMutation, Value> function) {
            List<Value> values = operations.stream().map(v -> v.traverse(function)).collect(Collectors.toCollection(ArrayList::new));
            if (rest < 0) return ListValue.wrap(values);
            List<Value> r = ((AbstractListValue)values.getLast()).unpack();
            List<Value> list = new ArrayList<>(values.size() - 1 + r.size());
            list.addAll(values.subList(0, rest));
            list.addAll(r);
            list.addAll(values.subList(rest, values.size() - 1));
            return ListValue.wrap(list);
        }

        @Override
        public void traverse(Consumer<AssignmentMutation> consumer) {
            for (AssignmentOperation operation : operations)
                operation.traverse(consumer);
        }
    }

    private record AssignmentMap(List<Entry> entries, int rest) implements AssignmentOperation {
        private record Entry(Value key, AssignmentOperation value) {
        }

        @Override
        public Value traverse(Function<AssignmentMutation, Value> function) {
            if (rest < 0)
                return MapValue.wrap(entries.stream().collect(Collectors.toMap(e -> e.key, e -> e.value.traverse(function))));
            List<?> values = entries.stream().map(e -> e.key != null ? new Value[]{e.key, e.value.traverse(function)} : e.value.traverse(function)).toList();
            Map<Value, Value> map = new HashMap<>(((MapValue)values.removeLast()).getMap());
            return MapValue.wrap(values.stream().collect(Collectors.toMap(p -> ((Value[])p)[0], p -> ((Value[])p)[1], (a, b) -> b, () -> map)));
        }

        @Override
        public void traverse(Consumer<AssignmentMutation> consumer) {
            for (Entry entry : entries)
                entry.value.traverse(consumer);
        }
    }

    private static void checkSwitchPattern(Expression expression, Token token, Context context, Value pattern) throws ExpressionException {
        switch (pattern) {
            case LContainerValue ignored -> throw new ExpressionException(context, expression, token, "Container mutation is not allowed in switch, use get(container[index]) instead of container[index] to test against the value");
            case ConditionPatternValue condition -> checkSwitchPattern(condition.expression, condition.token, context, condition.pattern);
            case DefaultPatternValue defaultPattern -> checkSwitchPattern(defaultPattern.expression, defaultPattern.token, context, defaultPattern.pattern);
            case EntryPatternValue entry -> checkSwitchPattern(entry.expression, entry.token, context, entry.pattern);
            case ListPatternValue list -> list.values.forEach(v -> checkSwitchPattern(list.expression, list.token, context, v));
            case MapPatternValue map -> map.values.forEach(v -> checkSwitchPattern(map.expression, map.token, context, v));
            case RestPatternValue rest -> checkSwitchPattern(rest.expression, rest.token, context, rest.pattern);
            default -> {
                if (pattern.isBound() && pattern.boundVariable.startsWith("global_"))
                    throw new ExpressionException(context, expression, token, "Global variable assignment is not allowed in switch");
            }
        }
    }

    private static boolean switchPattern(Context context, Value pattern, Value test, Map<String, @Nullable LazyValue> scope) throws ExpressionException {
        return switch (pattern) {
            case ConditionPatternValue condition -> switchPattern(context, condition.pattern, test, scope) && condition.condition.evalValue(context, Context.BOOLEAN).getBoolean();
            case ListPatternValue list -> {
                if (!(test instanceof AbstractListValue l) || l instanceof MapValue) yield false;
                List<Value> items = l.unpack();

                int size = list.rest >= 0 ? list.values.size() - 1 : list.values.size();
                if (items.size() < size || list.rest < 0 && items.size() > size) yield false;

                if (list.rest < 0) {
                    for (Iterator<Value> itl = list.values.iterator(), itr = items.iterator(); itl.hasNext();)
                        if (!switchPattern(context, itl.next(), itr.next(), scope)) yield false;
                    yield true;
                }

                for (int i = 0; i < list.rest; i++)
                    if (!switchPattern(context, list.values.get(i), items.get(i), scope)) yield false;
                int len = list.values.size(), off = items.size() - len;
                for (int i = list.rest + 1; i < len; i++)
                    if (!switchPattern(context, list.values.get(i), items.get(i + off), scope)) yield false;

                Value rest = list.values.get(list.rest);
                LazyValue condition = LazyValue.TRUE;
                if (rest instanceof ConditionPatternValue c) {
                    condition = c.condition;
                    rest = c.pattern;
                }
                RestPatternValue r = (RestPatternValue)rest;
                ListValue slice = ListValue.wrap(items.subList(list.rest, list.rest + off + 1));
                yield switchPattern(context, r.pattern, slice, scope) && condition.evalValue(context, Context.BOOLEAN).getBoolean();
            }
            case MapPatternValue map -> {
                if (!(test instanceof MapValue m)) yield false;
                Map<Value, Value> pairs = new HashMap<>(m.getMap());

                if (pairs.size() < map.minSize || map.rest < 0 && pairs.size() > map.values.size())
                    yield false;

                for (Value p : map.values) {
                    LazyValue condition = LazyValue.TRUE, defaultValue = null;
                    Value key = null;

                    if (p instanceof EntryPatternValue entry) {
                        key = entry.key;
                        p = entry.pattern;
                    }
                    if (p instanceof DefaultPatternValue defaultPattern) {
                        defaultValue = defaultPattern.defaultValue;
                        p = defaultPattern.pattern;
                    }
                    if (p instanceof ConditionPatternValue conditionPattern) {
                        condition = conditionPattern.condition;
                        p = conditionPattern.pattern;
                    }

                    if (p instanceof RestPatternValue) continue;
                    if (key == null) key = StringValue.of(p.boundVariable);

                    Value value = pairs.remove(key);
                    if (value == null && (defaultValue == null || !switchPattern(context, p, defaultValue.evalValue(context), scope))) yield false;
                    if (!switchPattern(context, p, value, scope) || !condition.evalValue(context, Context.BOOLEAN).getBoolean()) yield false;
                }

                if (map.rest >= 0) {
                    Value p = map.values.get(map.rest);
                    LazyValue condition = LazyValue.TRUE;

                    if (p instanceof ConditionPatternValue conditionPattern) {
                        condition = conditionPattern.condition;
                        p = conditionPattern.pattern;
                    }

                    yield switchPattern(context, ((RestPatternValue)p).pattern, MapValue.wrap(pairs), scope) && condition.evalValue(context, Context.BOOLEAN).getBoolean();
                }

                yield true;
            }
            case DefaultPatternValue defaultPattern -> throw new ExpressionException(context, defaultPattern.expression, defaultPattern.token, "Default pattern is not allowed in a switch branch");
            case EntryPatternValue entry -> throw new ExpressionException(context, entry.expression, entry.token, "Entry pattern is not allowed in a switch branch");
            case RestPatternValue rest -> throw new ExpressionException(context, rest.expression, rest.token, "Rest pattern is not allowed in a switch branch");
            default -> {
                if (pattern.isBound()) {
                    if (!scope.containsKey(pattern.boundVariable))
                        scope.put(pattern.boundVariable, context.getVariable(pattern.boundVariable));
                    Value rebounded = test.reboundedTo(pattern.boundVariable);
                    context.setVariable(pattern.boundVariable, (c, t) -> rebounded);
                    yield true;
                }
                yield test.equals(pattern);
            }
        };
    }
}
