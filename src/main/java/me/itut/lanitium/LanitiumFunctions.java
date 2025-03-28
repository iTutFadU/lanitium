package me.itut.lanitium;

import carpet.script.*;
import carpet.script.annotation.Locator;
import carpet.script.annotation.Param;
import carpet.script.annotation.ScarpetFunction;
import carpet.script.argument.BlockArgument;
import carpet.script.argument.Vector3Argument;
import carpet.script.command.CommandArgument;
import carpet.script.exception.*;
import carpet.script.language.Operators;
import carpet.script.value.*;
import carpet.utils.CommandHelper;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import me.itut.lanitium.internal.CommandSourceStackInterface;
import me.itut.lanitium.internal.carpet.ExpressionInterface;
import me.itut.lanitium.internal.carpet.FunctionValueInterface;
import me.itut.lanitium.internal.carpet.NBTSerializableValueInterface;
import me.itut.lanitium.internal.carpet.VanillaArgument;
import me.itut.lanitium.value.*;
import me.itut.lanitium.value.ValueConversions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.StyleArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static me.itut.lanitium.internal.carpet.SystemInfoInterface.options;
import static net.minecraft.Util.NIL_UUID;

public class LanitiumFunctions {
    static {
        options.put("server_tps", c -> new NumericValue(c.server().tickRateManager().tickrate()));
        options.put("server_frozen", c -> BooleanValue.of(c.server().tickRateManager().isFrozen()));
        options.put("server_sprinting", c -> BooleanValue.of(c.server().tickRateManager().isSprinting()));
        options.put("source_anchor", c -> c.source().getAnchor() == EntityAnchorArgument.Anchor.EYES ? Constants.EYES : Constants.FEET);
        options.put("source_permission", c -> NumericValue.of(((CommandSourceStackInterface)c.source()).lanitium$permissionLevel()));
        options.put("source_custom_values", c -> {
            Map<Value, Value> map = ((CommandSourceStackInterface)c.source()).lanitium$customValues();
            return map != null ? MapValue.wrap(map) : Value.NULL;
        });
        options.put("source", SourceValue::of);

        CommandArgument.builtIns.put("formatted_text", new VanillaArgument("formated_text", ComponentArgument::textComponent, (c, p) -> FormattedTextValue.of(ComponentArgument.getComponent(c, p)), param -> (ctx, builder) -> ctx.getArgument(param, ComponentArgument.class).listSuggestions(ctx, builder)));
        CommandArgument.builtIns.put("style", new VanillaArgument("style", StyleArgument::style, (c, p) -> {
            Style style = StyleArgument.getStyle(c, p);
            return SimpleFunctionValue.of(cc -> FormattedTextValue.of(((MutableComponent)FormattedTextValue.getTextByValue(cc)).withStyle(style)));
        }, param -> (ctx, builder) -> ctx.getArgument(param, StyleArgument.class).listSuggestions(ctx, builder)));
        CommandArgument.builtIns.put("item_predicate", new VanillaArgument("item_predicate", ItemPredicateArgument::new, (c, p) -> {
            ItemPredicateArgument.Result predicate = ItemPredicateArgument.getItemPredicate(c, p);
            return SimpleFunctionValue.of(i -> BooleanValue.of(predicate.test(carpet.script.value.ValueConversions.getItemStackFromValue(i, true, c.getSource().registryAccess()))));
        }, param -> (ctx, builder) -> ctx.getArgument(param, ItemPredicateArgument.class).listSuggestions(ctx, builder)));
    }

    public static Fluff.ILazyFunction findIn(Context c, Expression expr, Value functionValue) {
        if (functionValue.isNull()) throw new InternalExpressionException("function argument cannot be null");
        else if (!(functionValue instanceof FunctionValue fun)) {
            String name = functionValue.getString();
            Map<String, Fluff.ILazyFunction> functions = ((ExpressionInterface)expr).lanitium$functions();
            if (functions.containsKey(name)) return functions.get(name);
            return c.host.getAssertFunction(expr.module, name);
        } else return fun;
    }

    private static Value internalExceptionMap(Throwable e) {
        return MapValue.wrap(new HashMap<>() {{
            put(Constants.CLASS, StringValue.of(e.getClass().getName()));
            put(Constants.MESSAGE, StringValue.of(e.getMessage()));
            put(Constants.STACK, ListValue.wrap(Arrays.stream(e.getStackTrace()).map(v -> MapValue.wrap(new HashMap<>() {{
                if (v.getClassLoaderName() instanceof String classLoaderName)
                    put(Constants.CLASS_LOADER, StringValue.of(classLoaderName));
                if (v.getModuleName() instanceof String moduleName)
                    put(Constants.MODULE, StringValue.of(moduleName));
                if (v.getModuleVersion() instanceof String moduleVersion)
                    put(Constants.MODULE_VERSION, StringValue.of(moduleVersion));
                put(Constants.CLASS, StringValue.of(v.getClassName()));
                put(Constants.METHOD, StringValue.of(v.getMethodName()));
                if (v.getFileName() instanceof String fileName)
                    put(Constants.FILE, StringValue.of(fileName));
                put(Constants.LINE, NumericValue.of(v.getLineNumber()));
                if (v.isNativeMethod())
                    put(Constants.NATIVE, Value.TRUE);
            }}))));
            if (e.getCause() instanceof Throwable cause)
                put(Constants.CAUSE, internalExceptionMap(cause));
            if (e.getSuppressed().length > 0)
                put(Constants.SUPPRESSED, ListValue.wrap(Arrays.stream(e.getSuppressed()).map(LanitiumFunctions::internalExceptionMap)));
        }});
    }

    public static void apply(Expression expr) {
        expr.addMathematicalUnaryFunction("atanh", d -> 0.5 * Math.log((1 + d) / (1 - d)));
        expr.addMathematicalUnaryFunction("acoth", d -> 0.5 * Math.log((d + 1) / (d - 1)));
        expr.addMathematicalUnaryFunction("asec", d -> Math.toDegrees(Math.acos(1.0 / d)));
        expr.addMathematicalUnaryFunction("acsc", d -> Math.toDegrees(Math.asin(1.0 / d)));
        expr.addMathematicalUnaryFunction("asech", d -> Math.log(1.0 / d + Math.sqrt(1.0 / Math.pow(d, 2) - 1)));
        expr.addMathematicalUnaryFunction("acsch", d -> Math.log(1.0 / d + Math.sqrt(1.0 / Math.pow(d, 2) + 1)));

        // Carpet nasty nasty
        final Fluff.ILazyFunction call;
        expr.addCustomFunction("call", call = new Fluff.AbstractLazyFunction(-1, "call") {
            @Override
            public LazyValue lazyEval(Context c, Context.Type t, Expression expr, Tokenizer.Token tok, List<LazyValue> lv) {
                if (lv.isEmpty()) {
                    throw new InternalExpressionException("'call' expects at least function name to call");
                } else if (t != Context.SIGNATURE) {
                    Fluff.ILazyFunction fun = findIn(c, expr, lv.getFirst().evalValue(c));
                    return fun.lazyEval(c, t, expr, tok, lv.subList(1, lv.size()));
                } else {
                    String name = lv.getFirst().evalValue(c, Context.NONE).getString();
                    List<String> args = new ArrayList<>();
                    List<String> globals = "fn".equals(name)
                        ? new ArrayList<>(c.variables.keySet().stream().toList())
                        : new ArrayList<>();
                    String varArgs = null;

                    for (int i = 1; i < lv.size(); ++i) {
                        Value v = lv.get(i).evalValue(c, Context.LOCALIZATION);
                        if (!v.isBound()) {
                            throw new InternalExpressionException("Only variables can be used in function signature, not  " + v.getString());
                        }

                        if (v instanceof FunctionAnnotationValue fav) {
                            if (fav.type == FunctionAnnotationValue.Type.GLOBAL) {
                                globals.add(v.boundVariable);
                            } else {
                                if (varArgs != null) {
                                    throw new InternalExpressionException("Variable argument identifier is already defined as " + varArgs + ", trying to overwrite with " + v.boundVariable);
                                }

                                varArgs = v.boundVariable;
                            }
                        } else {
                            args.add(v.boundVariable);
                        }
                    }

                    globals.remove(varArgs);
                    globals.removeAll(args);
                    Value output = new FunctionSignatureValue(name, args, varArgs, globals);
                    return (cc, tt) -> output;
                }
            }

            @Override
            public boolean pure() {
                return false;
            }

            @Override
            public boolean transitive() {
                return false;
            }

            @Override
            public Context.Type staticType(Context.Type outerType) {
                return outerType == Context.SIGNATURE ? Context.LOCALIZATION : Context.NONE;
            }
        });

        expr.addLazyBinaryOperator("::", Operators.precedence.get("attribute~:") + 20, false, true, type -> type, (c, t, l, r) -> l);
        expr.addLazyBinaryOperator("\\", Operators.precedence.get("attribute~:"), true, false, type -> type, (c, t, l, r) -> {
            Value left = l.evalValue(c, t);
            if (left instanceof WithValue with)
                return with.with(c, t, r);
            Value right = r.evalValue(c, t);

            Value[] values;
            if (right instanceof ListValue args) values = args.getItems().toArray(Value[]::new);
            else if (right instanceof StringValue || right instanceof MapValue) values = new Value[]{right};
            else throw new InternalExpressionException("'\\' must have a list, a string, or a map on the RHS");

            Value[] callArgs = new Value[values.length + 1];
            callArgs[0] = left;
            System.arraycopy(values, 0, callArgs, 1, values.length);
            return call.lazyEval(c, t, expr, Tokenizer.Token.NONE, Arrays.stream(callArgs).map(v -> (LazyValue)(cc, tt) -> v).toList());
        });
        expr.addLazyFunction("all_then", -1, (c, t, lv) -> {
            int imax = lv.size() - 1;
            Throwable error = null;

            for (int i = 0; i < imax; i++)
                try {
                    lv.get(i).evalValue(c, Context.VOID);
                } catch (Throwable e) {
                    error = e;
                }

            Value v = lv.get(imax).evalValue(c, t);
            if (error instanceof Error e)
                throw e;
            else if (error instanceof RuntimeException e)
                throw e;
            return (cc, tt) -> v;
        });
        expr.addLazyFunction("catch_all", 1, (c, t, lv) -> {
            try {
                Value output = lv.getFirst().evalValue(c, t);
                return (cc, tt) -> output;
            } catch (Throwable e) {
                return LazyValue.NULL;
            }
        });
        expr.addLazyFunction("catch", 1, (c, t, lv) -> {
            Value output;
            try {
                output = MapValue.wrap(new HashMap<>() {{
                    put(Constants.RESULT, lv.getFirst().evalValue(c, t));
                }});
            } catch (ProcessedThrowStatement e) {
                output = MapValue.wrap(Map.of(
                    Constants.ERROR, MapValue.wrap(Map.of(
                        Constants.TYPE, StringValue.of(e.thrownExceptionType.getId()),
                        Constants.VALUE, e.data,
                        Constants.TRACE, MapValue.wrap(Map.of(
                            Constants.STACK, ListValue.wrap(e.stack.stream().map(f -> ListValue.of(
                                StringValue.of(f.getModule().name()),
                                StringValue.of(f.getString()),
                                NumericValue.of(f.getToken().lineno + 1),
                                NumericValue.of(f.getToken().linepos + 1)
                            ))),
                            Constants.LOCALS, MapValue.wrap(e.context.variables.entrySet().stream().collect(Collectors.toMap(
                                s -> StringValue.of(s.getKey()),
                                s -> s.getValue().evalValue(e.context)
                            ))),
                            Constants.TOKEN, ListValue.of(
                                StringValue.of(e.token.surface),
                                NumericValue.of(e.token.lineno + 1),
                                NumericValue.of(e.token.linepos + 1)
                            )
                        ))
                    ))
                ));
            } catch (ExitStatement passthrough) {
                throw passthrough;
            } catch (Throwable e) {
                output = MapValue.wrap(Map.of(
                    Constants.ERROR, MapValue.wrap(Map.of(
                        Constants.TYPE, Constants.INTERNAL,
                        Constants.VALUE, StringValue.of(e.getMessage()),
                        Constants.TRACE, MapValue.wrap(Map.of(
                            Constants.STACK, ListValue.of(),
                            Constants.LOCALS, Value.NULL,
                            Constants.TOKEN, ListValue.of(StringValue.EMPTY, Value.ONE, Value.ONE)
                        )),
                        Constants.INTERNAL, internalExceptionMap(e),
                        Constants.THROW, SimpleFunctionValue.of(() -> {
                            throw e;
                        })
                    ))
                ));
            }
            Value finalOutput = output;
            return (cc, tt) -> finalOutput;
        });

        expr.addLazyFunction("as_entity", 2, (c, t, lv) -> {
            final CommandSourceStack source = ((CarpetContext)c).source();
            if (!(lv.getFirst().evalValue(c) instanceof EntityValue entity)) throw new InternalExpressionException("First argument to 'as_entity' must be an entity");
            if (entity.getEntity().equals(source.getEntity())) return lv.get(1);
            final Context ctx = c.recreate();
            ((CarpetContext)ctx).swapSource(source.withEntity(entity.getEntity()));
            ctx.variables = c.variables;
            final Value output = lv.get(1).evalValue(ctx);
            return (cc, tt) -> output;
        });
        expr.addLazyFunction("positioned", 2, (c, t, lv) -> {
            final CommandSourceStack source = ((CarpetContext)c).source();
            if (!(lv.getFirst().evalValue(c) instanceof ListValue b)) throw new InternalExpressionException("First argument to 'positioned' must be a list");
            final List<Value> bl = b.getItems();
            final Vec3 pos = new Vec3(NumericValue.asNumber(bl.get(0)).getDouble(), NumericValue.asNumber(bl.get(1)).getDouble(), NumericValue.asNumber(bl.get(2)).getDouble());
            if (pos.equals(source.getPosition())) return lv.get(1);
            final Context ctx = c.recreate();
            ((CarpetContext)ctx).swapSource(source.withPosition(pos));
            ctx.variables = c.variables;
            final Value output = lv.get(1).evalValue(ctx);
            return (cc, tt) -> output;
        });
        expr.addLazyFunction("rotated", 2, (c, t, lv) -> {
            final CommandSourceStack source = ((CarpetContext)c).source();
            if (!(lv.getFirst().evalValue(c) instanceof ListValue b)) throw new InternalExpressionException("First argument to 'rotated' must be a list");
            final List<Value> bl = b.getItems();
            final Vec2 rot = new Vec2(NumericValue.asNumber(bl.get(0)).getFloat(), NumericValue.asNumber(bl.get(1)).getFloat());
            if (rot.equals(source.getRotation())) return lv.get(1);
            final Context ctx = c.recreate();
            ((CarpetContext)ctx).swapSource(source.withRotation(rot));
            ctx.variables = c.variables;
            final Value output = lv.get(1).evalValue(ctx);
            return (cc, tt) -> output;
        });
        expr.addLazyFunction("anchored", 2, (c, t, lv) -> {
            final CommandSourceStack source = ((CarpetContext)c).source();
            final String name = lv.getFirst().evalValue(c).getString();
            final EntityAnchorArgument.Anchor anchor = switch (name) {
                case "eyes" -> EntityAnchorArgument.Anchor.EYES;
                case "feet" -> EntityAnchorArgument.Anchor.FEET;
                default -> throw new ThrowStatement(name, Throwables.VALUE_EXCEPTION);
            };
            if (source.getAnchor() == anchor) return lv.get(1);
            Context ctx = c.recreate();
            ((CarpetContext)ctx).swapSource(source.withAnchor(anchor));
            ctx.variables = c.variables;
            Value output = lv.get(1).evalValue(ctx);
            return (cc, tt) -> output;
        });
        expr.addLazyFunction("elevated", 2, (c, t, lv) -> {
            final CommandSourceStack source = ((CarpetContext)c).source();
            final int level = NumericValue.asNumber(lv.getFirst().evalValue(c)).getInt();
            if (source.hasPermission(level)) return lv.get(1);
            Context ctx = c.recreate();
            ((CarpetContext)ctx).swapSource(source.withPermission(level));
            ctx.variables = c.variables;
            Value output = lv.get(1).evalValue(ctx);
            return (cc, tt) -> output;
        });
        expr.addLazyFunction("with_permission", 2, (c, t, lv) -> {
            final CommandSourceStack source = ((CarpetContext)c).source();
            final int level = NumericValue.asNumber(lv.getFirst().evalValue(c)).getInt();
            final CommandSourceStack newSource = source.withPermission(level);
            if (source == newSource) return lv.get(1);
            Context ctx = c.recreate();
            ((CarpetContext)ctx).swapSource(newSource);
            ctx.variables = c.variables;
            Value output = lv.get(1).evalValue(ctx);
            return (cc, tt) -> output;
        });
        expr.addLazyFunction("with_custom_values", 2, (c, t, lv) -> {
            final CommandSourceStack source = ((CarpetContext)c).source();
            final MapValue customValues = switch (lv.getFirst().evalValue(c)) {
                case NullValue ignored -> null;
                case MapValue map -> map;
                case AbstractListValue list -> new MapValue(list.unpack());
                default -> throw new InternalExpressionException("Argument 'custom_values' must be a map");
            };
            if (customValues == null || customValues.getMap().isEmpty()) return lv.get(1);
            Context ctx = c.recreate();
            ((CarpetContext)ctx).swapSource(((CommandSourceStackInterface)source).lanitium$withCustomValues(customValues.getMap()));
            ctx.variables = c.variables;
            Value output = lv.get(1).evalValue(ctx);
            return (cc, tt) -> output;
        });
        expr.addLazyFunction("with_source", 2, (c, t, lv) -> {
            final CommandSourceStack source = SourceValue.from(lv.getFirst().evalValue(c));
            if (source == null) throw new InternalExpressionException("Source cannot be null");
            Context ctx = c.recreate();
            ((CarpetContext)ctx).swapSource(source);
            ctx.variables = c.variables;
            Value output = lv.get(1).evalValue(ctx);
            return (cc, tt) -> output;
        });
    }

    // TODO: Revisit cookies
    @ScarpetFunction
    public static Value cookie(Context c, ServerPlayer player, FunctionValue callback) {
        return FutureValue.of((CarpetContext)c, player.getCookie(LanitiumCookie.class).handle((cookie, exception) -> {
            MapValue map = null;

            if (cookie != null) {
                Map<Value, Value> values = new HashMap<>();
                cookie.cookie.forEach((k, v) -> values.put(StringValue.of(k), NBTSerializableValue.of(v)));
                map = MapValue.wrap(values);
            }

            final Value mapValue = map != null ? map : Value.NULL;
            List<Value> args = new ArrayList<>() {{
                add(EntityValue.of(player));
                add(mapValue);
            }};
            boolean set = false;
            try {
                Value out = callback.callInContext(c, Context.STRING, args).evalValue(c, Context.STRING);
                if ("set".equals(out.getString())) set = true;
            } finally {
                if (set)
                    if (map == null)
                        player.setCookie(LanitiumCookie.EMPTY);
                    else {
                        Map<String, Tag> newCookie = new HashMap<>();
                        map.getMap().forEach((k, v) -> newCookie.put(k.getString(), v.toTag(true, ((CarpetContext)c).registryAccess())));
                        player.setCookie(new LanitiumCookie(newCookie));
                    }
            }

            return mapValue;
        }));
    }

    @ScarpetFunction
    public static void cookie_reset(ServerPlayer player) {
        player.setCookie(LanitiumCookie.EMPTY);
    }

    @ScarpetFunction
    public static void cookie_secret(String secret) {
        Lanitium.COOKIE.setSecret(secret);
    }

    public static final Throwables ITERATION_END = Throwables.register("iteration_end", Throwables.THROWN_EXCEPTION_TYPE);

    public static class CustomIterator extends LazyListValue {
        private final Context context;
        private final FunctionValue hasNext, next, reset;
        public final Value state;

        public CustomIterator(Context context, FunctionValue hasNext, FunctionValue next, FunctionValue reset, Value state) {
            this.context = context;
            this.hasNext = hasNext;
            this.next = next;
            this.reset = reset;
            this.state = state;
        }

        @Override
        public boolean hasNext() {
            return hasNext.callInContext(context, Context.BOOLEAN, List.of(state)).evalValue(context).getBoolean();
        }

        @Override
        public Value next() {
            try {
                return next.callInContext(context, Context.NONE, List.of(state)).evalValue(context);
            } catch (ProcessedThrowStatement e) {
                if (e.thrownExceptionType == ITERATION_END)
                    throw new NoSuchElementException(e.getMessage());
                else throw e;
            }
        }

        @Override
        public void reset() {
            reset.callInContext(context, Context.VOID, List.of(state));
        }

        @Override
        public Value deepcopy() {
            return new CustomIterator(context, hasNext, next, reset, state.deepcopy());
        }
    }

    @ScarpetFunction
    public static Value iterator(Context c, FunctionValue hasNext, FunctionValue next, FunctionValue reset, Value state) {
        return new CustomIterator(c, hasNext, next, reset, state);
    }

    @ScarpetFunction
    public static Value iterate(AbstractListValue list) {
        @SuppressWarnings("unchecked")
        Iterator<Value>[] it = new Iterator[]{list.iterator()};
        return ListValue.of(
            SimpleFunctionValue.of(() -> BooleanValue.of(it[0].hasNext())),
            SimpleFunctionValue.of(() -> {
                try {
                    return it[0].next();
                } catch (NoSuchElementException e) {
                    throw new ThrowStatement(e.getMessage(), ITERATION_END);
                }
            }),
            SimpleFunctionValue.run(() -> {
                synchronized (list) {
                    list.fatality();
                    it[0] = list.iterator();
                }
            })
        );
    }

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
                if (m.getMap().entrySet().stream().anyMatch(e -> found.contains(e.getValue()) || found.contains(e.getKey()))) yield true;
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
                for (Iterator<Value> ita = la.iterator(), itb = lb.iterator(); ita.hasNext();) {
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
                    Map.Entry<Value, Value>[] entry = new Map.Entry[] {null};
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

    private static Value safeCopy(Value container, IdentityHashMap<Object, Value> copied) {
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
    loop_inner:'c' = loop_outer;
    loop_inner:'d' = loop_list;
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

    private static Value safeDeepMap(Value container, UnaryOperator<Value> lf, BinaryOperator<Value> kf, BinaryOperator<Value> vf, BinaryOperator<Value> ref, IdentityHashMap<Object, Value> mapped) {
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

    @ScarpetFunction
    public static Value thread_local(Context c, FunctionValue initial) {
        return new ThreadLocalValue(c, initial);
    }

    @ScarpetFunction(maxParams = -1)
    public static Value inject(FunctionValue fn, @Param.KeyValuePairs Map<String, Value> vars) {
        Value clone = fn.deepcopy();
        ((FunctionValueInterface)clone).lanitium$inject(vars.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> {
                Value bound = e.getValue().reboundedTo(e.getKey());
                return (c, t) -> bound;
            }
        )));
        return clone;
    }

    @ScarpetFunction(maxParams = 1)
    public static void display_server_motd(Optional<Value> motd) {
        Lanitium.CONFIG.displayMotd = motd.map(FormattedTextValue::getTextByValue).orElse(null);
    }

    @ScarpetFunction(maxParams = 1)
    public static void display_server_players_online(Optional<Integer> current) {
        Lanitium.CONFIG.displayPlayersOnline = current.orElse(null);
    }

    @ScarpetFunction(maxParams = 1)
    public static void display_server_players_max(Optional<Integer> max) {
        Lanitium.CONFIG.displayPlayersMax = max.orElse(null);
    }

    @ScarpetFunction(maxParams = -1)
    public static void display_server_players_sample(String... players) {
        Lanitium.CONFIG.displayPlayersSampleProfiles = Stream.of(players).map(v -> new GameProfile(NIL_UUID, v)).toList();
    }

    @ScarpetFunction
    public static void display_server_players_sample_default() {
        Lanitium.CONFIG.displayPlayersSampleProfiles = null;
    }

    @ScarpetFunction
    public static void set_server_tps(Context c, double tps) {
        ((CarpetContext)c).server().tickRateManager().setTickRate((float)tps);
    }

    @ScarpetFunction
    public static void set_server_frozen(Context c, boolean frozen) {
        ((CarpetContext)c).server().tickRateManager().setFrozen(frozen);
    }

    @ScarpetFunction
    public static void server_sprint(Context c, int ticks) {
        ((CarpetContext)c).server().tickRateManager().requestGameToSprint(ticks);
    }

    @ScarpetFunction
    public static Value emoticon() {
        return StringValue.of(Emoticons.getRandomEmoticon());
    }

    @ScarpetFunction
    public static Value emoticons_list() {
        return ListValue.wrap(Emoticons.list.stream().map(StringValue::of));
    }

    @ScarpetFunction
    public static Value format_json(Context c, String value) {
        try {
            return FormattedTextValue.deserialize(value, ((CarpetContext)c).registryAccess());
        } catch (JsonParseException e) {
            throw new ThrowStatement(e.getMessage(), Throwables.JSON_ERROR);
        }
    }

    @ScarpetFunction(maxParams = 2)
    public static void send_success(Context c, Value message, Optional<Boolean> broadcast) {
        ((CarpetContext)c).source().sendSuccess(() -> FormattedTextValue.getTextByValue(message), broadcast.orElse(false));
    }

    @ScarpetFunction
    public static void send_failure(Context c, Value message) {
        ((CarpetContext)c).source().sendFailure(FormattedTextValue.getTextByValue(message));
    }

    @ScarpetFunction
    public static void send_system_message(Context c, Value message) {
        ((CarpetContext)c).source().sendSystemMessage(FormattedTextValue.getTextByValue(message));
    }

    @ScarpetFunction(maxParams = -1)
    public static void send_commands_update(Context c, ServerPlayer... players) {
        MinecraftServer server = ((CarpetContext)c).server();
        if (players.length == 0) CommandHelper.notifyPlayersCommandsChanged(server);
        else server.schedule(new TickTask(server.getTickCount(), () -> {
            for (ServerPlayer player : players) server.getCommands().sendCommands(player);
        }));
    }

    @ScarpetFunction // system_info('source_permission') >= level
    public static Value has_permission(Context c, int level) {
        return BooleanValue.of(((CarpetContext)c).source().hasPermission(level));
    }

    @ScarpetFunction
    public static Value char_to_int(String str) {
        if (str.isEmpty()) return Value.NULL;
        return NumericValue.of((int)str.charAt(0));
    }

    @ScarpetFunction
    public static Value int_to_char(int i) {
        return StringValue.of(String.valueOf((char)i));
    }

    @ScarpetFunction
    public static Value name_of_code_point(String str) {
        if (str.isEmpty()) return Value.NULL;
        return StringValue.of(Character.getName(str.charAt(0)));
    }

    @ScarpetFunction
    public static Value code_point_by_name(String name) {
        try {
            return StringValue.of(String.valueOf(Character.codePointOf(name)));
        } catch (IllegalArgumentException e) {
            return Value.NULL;
        }
    }

    @ScarpetFunction(maxParams = 2)
    public static Value parse_signed_int(String str, Optional<Integer> radix) {
        try {
            return NumericValue.of(Long.parseLong(str, radix.orElse(10)));
        } catch (NumberFormatException e) {
            return Value.NULL;
        }
    }

    @ScarpetFunction(maxParams = 2)
    public static Value parse_unsigned_int(String str, Optional<Integer> radix) {
        try {
            return NumericValue.of(Long.parseUnsignedLong(str, radix.orElse(10)));
        } catch (NumberFormatException e) {
            return Value.NULL;
        }
    }

    @ScarpetFunction
    public static Value to_nbt(Context c, Value v) {
        return NBTSerializableValue.of(v.toTag(true, ((CarpetContext)c).registryAccess()));
    }

    @ScarpetFunction
    public static Value encode_bytes(String data) {
        return ByteBufferValue.of(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8)));
    }

    @ScarpetFunction
    public static Value decode_bytes(ByteBufferValue data) {
        return StringValue.of(new String(data.buffer.array(), StandardCharsets.UTF_8));
    }

    @ScarpetFunction
    public static Value encode_nbt_bytes(Context c, NBTSerializableValue data) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            NbtIo.writeAnyTag(data.getTag(), new DataOutputStream(stream));
        } catch (IOException e) {
            throw new ThrowStatement(e.getMessage(), Throwables.IO_EXCEPTION);
        }
        return ByteBufferValue.of(ByteBuffer.wrap(stream.toByteArray()));
    }

    @ScarpetFunction
    public static Value decode_nbt_bytes(ByteBufferValue data) {
        ByteArrayInputStream stream = new ByteArrayInputStream(data.buffer.array());
        try {
            return NBTSerializableValue.of(NbtIo.readAnyTag(new DataInputStream(stream), NbtAccounter.unlimitedHeap()));
        } catch (IOException e) {
            throw new ThrowStatement(e.getMessage(), Throwables.IO_EXCEPTION);
        }
    }

    @ScarpetFunction
    public static Value encode_compressed_compound(Context c, NBTSerializableValue data) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            NbtIo.writeCompressed(data.getCompoundTag(), new DataOutputStream(stream));
        } catch (IOException e) {
            throw new ThrowStatement(e.getMessage(), Throwables.IO_EXCEPTION);
        }
        return ByteBufferValue.of(ByteBuffer.wrap(stream.toByteArray()));
    }

    @ScarpetFunction
    public static Value decode_compressed_compound(ByteBufferValue data) {
        ByteArrayInputStream stream = new ByteArrayInputStream(data.buffer.array());
        try {
            return NBTSerializableValue.of(NbtIo.readCompressed(new DataInputStream(stream), NbtAccounter.unlimitedHeap()));
        } catch (IOException e) {
            throw new ThrowStatement(e.getMessage(), Throwables.IO_EXCEPTION);
        }
    }

    @ScarpetFunction(maxParams = 2)
    public static Value pretty_nbt(Context c, NBTSerializableValue nbt, Optional<String> indent) {
        return FormattedTextValue.of(new TextComponentTagVisitor(indent.orElse("")).visit(nbt.getTag()));
    }

    @ScarpetFunction
    public static Value nbt_type(NBTSerializableValue nbt) {
        return StringValue.of(nbt.getTag().getType().getName().toLowerCase());
    }

    @ScarpetFunction
    public static Value nbt_list_type(NBTSerializableValue nbt) {
        if (!(nbt.getTag() instanceof ListTag list)) return Value.NULL;
        return StringValue.of(TagTypes.getType(list.getElementType()).getName().toLowerCase());
    }

    @ScarpetFunction(maxParams = -1)
    public static Value byte_buffer(int... values) {
        byte[] arr = new byte[values.length];
        IntStream.range(0, arr.length).forEach(i -> arr[i] = (byte)values[i]);
        return ByteBufferValue.of(ByteBuffer.wrap(arr));
    }

    @ScarpetFunction
    public static Value string_reader(Context c, Value reader) {
        return StringReaderValue.of((CarpetContext)c, StringReaderValue.from(reader));
    }

    @ScarpetFunction
    public static Value command_suggestions(Context c, String command) {
        CommandDispatcher<CommandSourceStack> dispatcher = ((CarpetContext)c).server().getCommands().getDispatcher();
        return FutureValue.of((CarpetContext)c, dispatcher.getCompletionSuggestions(dispatcher.parse(command, ((CarpetContext)c).source())).thenApply(ValueConversions::suggestions));
    }

    @ScarpetFunction(maxParams = 1)
    public static Value future(Context c, Optional<Value> completed) {
        return FutureValue.of((CarpetContext)c, completed.map(CompletableFuture::completedFuture).orElseGet(CompletableFuture::new));
    }

    @ScarpetFunction
    public static Value allocate_list(int capacity) {
        return ListValue.wrap(new ArrayList<>(capacity));
    }

    @ScarpetFunction
    public static Value allocate_map(int capacity) {
        return MapValue.wrap(new HashMap<>(capacity));
    }

    @ScarpetFunction
    public static Value allocate_byte_buffer(int capacity) {
        return ByteBufferValue.of(ByteBuffer.allocate(capacity));
    }

    @ScarpetFunction
    public static Value possible_block_states(Context c, Value value) {
        ServerLevel level = ((CarpetContext)c).level();
        return ListValue.wrap(BlockArgument.findIn((CarpetContext)c, List.of(value), 0, true).block.getBlockState().getBlock().getStateDefinition().getPossibleStates().stream().map(s -> new BlockValue(s, level, (BlockPos)null)));
    }

    @ScarpetFunction
    public static Value block_state_map(Context c, Value value) {
        Collection<Property<?>> properties = BlockArgument.findIn((CarpetContext)c, List.of(value), 0, true).block.getBlockState().getBlock().getStateDefinition().getProperties();
        Map<Value, Value> map = new HashMap<>(properties.size());
        properties.forEach(p -> map.put(StringValue.of(p.getName()), ListValue.wrap(p.getPossibleValues().stream().map(v -> StringValue.of(v instanceof StringRepresentable str ? str.getSerializedName() : v.toString())))));
        return MapValue.wrap(map);
    }

    @ScarpetFunction(maxParams = 2) // https://github.com/gnembon/fabric-carpet/pull/1996
    public static Value item_components(Context c, Value item, Optional<String> component) {
        ItemStack stack = carpet.script.value.ValueConversions.getItemStackFromValue(item, true, ((CarpetContext)c).registryAccess());
        DataComponentMap components = stack.getComponents().filter(t -> !t.isTransient());
        if (component.isPresent()) {
            ResourceLocation name = ResourceLocation.tryParse(component.get());
            if (name == null) return Value.NULL;
            Optional<Holder.Reference<DataComponentType<?>>> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(name);
            if (type.isEmpty()) return Value.NULL;
            return encodeItemComponent(components.getTyped(type.get().value()));
        }
        return ListValue.wrap(components.stream().map(v -> carpet.script.value.ValueConversions.of(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(v.type()))));
    }

    private static Value encodeItemComponent(TypedDataComponent<?> v) {
        if (v == null) return Value.NULL;
        return switch (v.value()) {
            case Boolean bool -> BooleanValue.of(bool);
            case Number number -> NumericValue.of(number);
            case String str -> StringValue.of(str);
            case Component component -> FormattedTextValue.of(component);
            default -> NBTSerializableValueInterface.decodeTag(v.encodeValue(NbtOps.INSTANCE).result().orElse(null));
        };
    }

    @ScarpetFunction(maxParams = -1)
    public static void explode(Context c, @Locator.Vec3d Vec3 center, double radius, Map<String, Value> settings) {
        Entity source = null;
        if (settings.get("source") instanceof EntityValue e) source = e.getEntity();

        new ServerExplosion(((CarpetContext)c).level(), source, switch (settings.getOrDefault("damage_type", Value.NULL)) {
            case NullValue ignored -> null;
            case Value d -> {
                Optional<Holder.Reference<DamageType>> optionalType = ((CarpetContext)c).registry(Registries.DAMAGE_TYPE).get(ResourceLocation.tryParse(d.getString()));
                if (optionalType.isEmpty()) yield null;
                Holder.Reference<DamageType> type = optionalType.get();

                yield switch (settings.getOrDefault("damage_source", Value.NULL)) {
                    case NullValue ignored -> source == null
                        ? new DamageSource(type, center)
                        : new DamageSource(type, source);
                    case EntityValue e -> new DamageSource(type, e.getEntity());
                    case Value v -> new DamageSource(type, Vector3Argument.findIn(List.of(v), 0).vec);
                };
            }
        }, new SimpleExplosionDamageCalculator(settings.getOrDefault("explode_blocks", Value.TRUE).getBoolean(), settings.getOrDefault("damage_entities", Value.TRUE).getBoolean(), switch (settings.getOrDefault("knockback_multiplier", Value.NULL)) {
            case NumericValue n -> Optional.of(n.getFloat());
            default -> Optional.empty();
        }, Optional.ofNullable(switch (settings.getOrDefault("immune_blocks", Value.NULL)) {
            case NullValue ignored -> null;
            case AbstractListValue list -> HolderSet.direct(list.unpack().stream().map(v -> Holder.direct(BlockArgument.findIn(((CarpetContext)c), List.of(v), 0, true).block.getBlockState().getBlock())).toList());
            case Value b -> HolderSet.direct(Holder.direct(BlockArgument.findIn(((CarpetContext)c), List.of(b), 0, true).block.getBlockState().getBlock()));
        })), center, (float)radius, settings.getOrDefault("fire", Value.FALSE).getBoolean(), switch (settings.getOrDefault("block_interaction", Value.NULL).getString()) {
            case "keep" -> Explosion.BlockInteraction.KEEP;
            case "decay" -> Explosion.BlockInteraction.DESTROY_WITH_DECAY;
            case "trigger" -> Explosion.BlockInteraction.TRIGGER_BLOCK;
            default -> Explosion.BlockInteraction.DESTROY;
        }).explode();
    }

    //    @ScarpetFunction
//    public static void send_game_packet(Context c, EntityValue p, String type, Value... values) {
//        CarpetContext context = (CarpetContext)c;
//        ServerPlayer player = EntityValue.getPlayerByValue(context.server(), p);
//        ServerPlayerConnection connection = player.connection;
//        switch (type) {
//            default -> throw new InternalExpressionException("Unknown packet type: " + type);
//        }
//    }
}
