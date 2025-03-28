package me.itut.lanitium.function;

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
import com.google.gson.JsonParseException;
import me.itut.lanitium.Emoticons;
import me.itut.lanitium.internal.CommandSourceStackInterface;
import me.itut.lanitium.internal.carpet.ExpressionInterface;
import me.itut.lanitium.internal.carpet.FunctionValueInterface;
import me.itut.lanitium.internal.carpet.NBTSerializableValueInterface;
import me.itut.lanitium.internal.carpet.VanillaArgument;
import me.itut.lanitium.value.Constants;
import me.itut.lanitium.value.SimpleFunctionValue;
import me.itut.lanitium.value.SourceValue;
import me.itut.lanitium.value.WithValue;
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
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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

import java.util.*;
import java.util.stream.Collectors;

import static me.itut.lanitium.internal.carpet.SystemInfoInterface.options;

public class Apply {
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
            return SimpleFunctionValue.of(i -> BooleanValue.of(predicate.test(ValueConversions.getItemStackFromValue(i, true, c.getSource().registryAccess()))));
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
                put(Constants.SUPPRESSED, ListValue.wrap(Arrays.stream(e.getSuppressed()).map(Apply::internalExceptionMap)));
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
            if (!(lv.getFirst().evalValue(c) instanceof EntityValue entity))
                throw new InternalExpressionException("First argument to 'as_entity' must be an entity");
            if (entity.getEntity().equals(source.getEntity())) return lv.get(1);
            final Context ctx = c.recreate();
            ((CarpetContext)ctx).swapSource(source.withEntity(entity.getEntity()));
            ctx.variables = c.variables;
            final Value output = lv.get(1).evalValue(ctx);
            return (cc, tt) -> output;
        });
        expr.addLazyFunction("positioned", 2, (c, t, lv) -> {
            final CommandSourceStack source = ((CarpetContext)c).source();
            if (!(lv.getFirst().evalValue(c) instanceof ListValue b))
                throw new InternalExpressionException("First argument to 'positioned' must be a list");
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
            if (!(lv.getFirst().evalValue(c) instanceof ListValue b))
                throw new InternalExpressionException("First argument to 'rotated' must be a list");
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

    @ScarpetFunction
    public static Value emoticon() {
        return StringValue.of(Emoticons.getRandomEmoticon());
    }

    @ScarpetFunction
    public static Value emoticons_list() {
        return ListValue.wrap(Emoticons.list.stream().map(StringValue::of));
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
                    throw new ThrowStatement(e.getMessage(), DataStructures.ITERATION_END);
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

    @ScarpetFunction
    public static Value format_json(Context c, String value) {
        try {
            return FormattedTextValue.deserialize(value, ((CarpetContext)c).registryAccess());
        } catch (JsonParseException e) {
            throw new ThrowStatement(e.getMessage(), Throwables.JSON_ERROR);
        }
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
        ItemStack stack = ValueConversions.getItemStackFromValue(item, true, ((CarpetContext)c).registryAccess());
        DataComponentMap components = stack.getComponents().filter(t -> !t.isTransient());
        if (component.isPresent()) {
            ResourceLocation name = ResourceLocation.tryParse(component.get());
            if (name == null) return Value.NULL;
            Optional<Holder.Reference<DataComponentType<?>>> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(name);
            if (type.isEmpty()) return Value.NULL;
            return encodeItemComponent(components.getTyped(type.get().value()));
        }
        return ListValue.wrap(components.stream().map(v -> ValueConversions.of(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(v.type()))));
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
}
