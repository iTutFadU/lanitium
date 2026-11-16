package me.itut.lanitium.internal.carpet;

import carpet.script.*;
import carpet.script.value.*;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.language.Arithmetic;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.itut.lanitium.internal.carpet.Parser.Node;
import me.itut.lanitium.value.WithMetaValue;
import org.jetbrains.annotations.Nullable;

import java.util.function.*;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Codegen {
    public final Expression expression;
    public final Context context;
    public final LanitiumConfig.Structured config;

    public Codegen(Parser parser) {
        expression = parser.expression;
        context = parser.context;
        config = parser.config;
    }

    public class FnValue extends FunctionValue {
        private static final LazyValue whatʔ = LazyValue.ofConstant(StringValue.of("🤨"));

        protected final Instruction[] code;
        protected final Var[] captureSlots;
        protected final Value[] captured;

        public FnValue(
            Expression expression, Token token, List<String> args, String varArgs,
            Instruction[] code, Var[] captureSlots, Value[] captured
        ) {
            this.code = code;
            this.captureSlots = captureSlots;
            this.captured = captured;
            super(expression, token, "_", whatʔ, args, varArgs, null);
        }

        public FnValue(FnValue template, Value[] captured) {
            this(
                template.getExpression(),
                template.getToken(),
                template.getArguments(),
                template.getVarArgs(),
                template.code,
                template.captureSlots,
                captured
            );
        }

        @Override
        protected Value clone() {
            FnValue ret = new FnValue(
                getExpression(),
                getToken(),
                getArguments(),
                getVarArgs(),
                code,
                captureSlots,
                captured
            );
            ((FunctionValueInterface)ret).lanitium$finishClone(this);
            return ret;
        }

        @Override
        public LazyValue execute(Context c, Context.Type type, Expression e, Token t, List<Value> params, @Nullable ThreadValue freshNewCallingThread) {
            assertArgsOk(params, fixedArgs -> { // Copied, almost
                List<String> args = getArguments();
                @Nullable String varArgs = getVarArgs();

                if (fixedArgs) // wrong number of args for fixed args
                    throw new ExpressionException(c, e, t,
                        "Incorrect number of arguments for function. Should be "
                        + args.size() + ", not " + params.size() + " like " + args
                    );
                // too few args for varargs

                List<String> argList = new ArrayList<>(args);
                argList.add("... " + varArgs);
                throw new ExpressionException(c, e, t,
                    "Incorrect number of arguments for function. Should be at least "
                    + args.size() + ", not " + params.size() + " like " + argList
                );
            });

            Context newFrame = c.recreate();
            if (freshNewCallingThread != null) newFrame.setThreadContext(freshNewCallingThread);

            throw new UnsupportedOperationException("TODO!");
            // final Value retVal = TODO;
            // return (cc, tt) -> retVal;
        }
    }

    public static Set<String> KEYWORDS = Set.of(new String[] {
        "_",
        "anchored",
        "as_entity",
        "break",
        "c_for",
        "catch",
        "continue",
        "do_all",
        "elevated",
        "euler",
        "false",
        "fn",
        "for",
        "if",
        "import",
        "in_dimension",
        "loop",
        "match",
        "matches",
        "null",
        "on",
        "pi",
        "positioned",
        "return",
        "rotated",
        "true",
        "try",
        "while",
        "with_custom_values",
        "with_permission",
        "with_source",
    });

    public record Var(int index) {
        private static ObjectList<Var> INDEXED = new ObjectArrayList<>();

        public static Var at(int i) {
            if (i >= INDEXED.size()) INDEXED.size(i + 1);
            Var v = INDEXED.get(i);
            if (v == null) INDEXED.set(i, v = new Var(i));
            return v;
        }

        public static @Nullable Var next(GenContext ctx, int[] indices) {
            return ctx != GenContext.DISCARD ? at(nextAssignment(indices)) : null;
        }
    }

    public record MaybeUnpack(Var from, boolean unpack) {}

    public record Global(int index) {
        private static ObjectList<Global> INDEXED = new ObjectArrayList<>();

        public static Global at(int i) {
            if (i >= INDEXED.size()) INDEXED.size(i + 1);
            Global v = INDEXED.get(i);
            if (v == null) INDEXED.set(i, v = new Global(i));
            return v;
        }
    }

    public record Pos(int index) {
        private static ObjectList<Pos> INDEXED = new ObjectArrayList<>();

        public static Pos at(int i) {
            if (i >= INDEXED.size()) INDEXED.size(i + 1);
            Pos v = INDEXED.get(i);
            if (v == null) INDEXED.set(i, v = new Pos(i));
            return v;
        }
    }

    public sealed interface InvokeTarget {
        record Builtin(BiFunction<
            @Nullable ? super Context,
            ? super Value[],
            @Nullable ? extends Value
        > fn) implements InvokeTarget {}
        record Func(Var from) implements InvokeTarget {}
        record Index(Var from) implements InvokeTarget {}
        record Method(Var from, StringValue name) implements InvokeTarget {}
    }

    public sealed interface InvokeArgs {
        record Empty() implements InvokeArgs {}
        record Single(Var from) implements InvokeArgs {}
        record Fixed(Var[] from) implements InvokeArgs {}
        record VarArgs(Var[] before, Var varArgs, Var[] after) implements InvokeArgs {}
        record Mixed(MaybeUnpack[] varArgs) implements InvokeArgs {}
    }

    public sealed interface Instruction {
        record Move(Var from, Var to) implements Instruction {}
        record Constant(Value value, Var to) implements Instruction {}
        record Lambda(Var[] capture, FnValue template, Var to) implements Instruction {}
        record Forget(Var slot) implements Instruction {}

        record GlobalRead(Global global, Var to) implements Instruction {}
        record InvokeRead(Token token, InvokeTarget target, InvokeArgs args, Optional<Var> to) implements Instruction {}
        record MemberRead(Var on, StringValue name, Var to) implements Instruction {}

        record GlobalStore(Var from, Global global) implements Instruction {}
        record InvokeStore(Var from, Token token, InvokeTarget target, InvokeArgs args) implements Instruction {}
        record MemberStore(Var from, Var on, StringValue name) implements Instruction {}

        record Jump(Pos pos) implements Instruction {}
        record Branch(Pos onTrue, Pos onFalse, Var from, Predicate<? super Value> op) implements Instruction {}
        record Return(Var from) implements Instruction {}

        record ContextSave(Var to) implements Instruction {}
        record ContextLoad(Var from) implements Instruction {}
        record Catch(Pos handler, Var to) implements Instruction {}
        record Throw(Var from) implements Instruction {}

        // TODO: ContainerValueInterface functions fn()[]
        // TODO: Do-notation
    }

    public static int[] newIndices() {
        return new int[1];
    }

    public static int nextAssignment(int[] indices) {
        return indices[0]++;
    }

    public record BasicBlock(Int2ObjectMap<Int2IntMap> phis, List<Instruction> code) {}

    public record DeferPoint(Pos pos, List<Instruction> defer) {}

    public sealed interface LValue {
        record Invoke(Token token, InvokeTarget target, InvokeArgs args) implements LValue {}
        record Member(Var on, StringValue name) implements LValue {}
    }

    public sealed interface Binding extends LValue {
        boolean isConstant();
    }

    public record LocalBinding(Var slot, boolean mutable) implements Binding {
        @Override
        public boolean isConstant() {
            return !mutable;
        }
    }

    public record GlobalBinding(Global slot, Mutability mutability) implements Binding {
        public enum Mutability { CONSTANT, MUTABLE, IMPORTED }

        @Override
        public boolean isConstant() {
            return mutability == Mutability.CONSTANT;
        }
    }

    public enum GenContext { DISCARD, USE }

    public record AstToCfgContext(
        Set<LocalBinding> bindings,
        Map<String, LocalBinding> scope,
        Map<String, @Nullable LocalBinding> captured,
        Predicate<? super String> outer,
        Map<String, GlobalBinding> globals,
        Int2ObjectMap<Token> linkMutable,
        List<BasicBlock> blocks,
        @Nullable DeferPoint breakDefer,
        @Nullable DeferPoint continueDefer,
        Pos[] currentBlock,
        int[] indices
    ) {
        public AstToCfgContext newScope() {
            return new AstToCfgContext(
                bindings,
                new Object2ObjectOpenHashMap<>(scope),
                captured,
                outer,
                globals,
                linkMutable,
                blocks,
                breakDefer,
                continueDefer,
                currentBlock,
                indices
            );
        }

        public AstToCfgContext newBranch(
            @Nullable DeferPoint breakDefer,
            @Nullable DeferPoint continueDefer,
            Pos[] currentBlock
        ) {
            return new AstToCfgContext(
                bindings,
                scope,
                captured,
                outer,
                globals,
                linkMutable,
                blocks,
                breakDefer,
                continueDefer,
                currentBlock,
                indices
            );
        }

        public BasicBlock current() {
            return blocks.get(currentBlock[0].index);
        }

        public Pos createBlock() {
            BasicBlock block = new BasicBlock(new Int2ObjectOpenHashMap<>(), new ArrayList<>());
            blocks.add(block);
            return Pos.at(blocks.size() - 1);
        }
    }

    public Function<Node, Var> astToArgsConverter(AstToCfgContext context) {
        return node -> astToCfg(GenContext.USE, context, node);
    }

    public InvokeArgs astToArgsSimple(AstToCfgContext context, List<? extends Node> args) {
        return astToArgs(context, List.of(), args, List.of(), astToArgsConverter(context), astToArgsConverter(context));
    }

    public InvokeArgs astToArgsPlus(
        AstToCfgContext context,
        List<? extends Var> prefix, List<? extends Node> args, List<? extends Var> suffix
    ) {
        return astToArgs(context, prefix, args, suffix, astToArgsConverter(context), astToArgsConverter(context));
    }

    public InvokeArgs astToArgs(
        AstToCfgContext context,
        List<? extends Var> prefix, List<? extends Node> args, List<? extends Var> suffix,
        Function<? super Node, ? extends Var> argsConv,
        Function<? super Node, ? extends Var> varargsConv
    ) {
        int total = prefix.size() + args.size() + suffix.size();

        if (total == 0) return new InvokeArgs.Empty();
        if (total == 1) {
            if (args.isEmpty()) return new InvokeArgs.Single((prefix.isEmpty() ? suffix : prefix).getFirst());
            Node arg = args.getFirst();
            Token token = arg.token();
            if (TokenInterface.type(token) == TokenTypeInterface.UNARY_OPERATOR && "...".equals(token.surface))
                return new InvokeArgs.VarArgs(
                    new Var[0],
                    varargsConv.apply(arg.children().getFirst()),
                    new Var[0]
                );
            return new InvokeArgs.Single(argsConv.apply(arg));
        }

        List<Var> before = new ArrayList<>(total);
        before.addAll(prefix);
        Var varArgs = null;
        List<Var> after = null;
        Iterator<? extends Node> it = args.iterator();
        Node arg;

        varArgs: {
            fixed: {
                while (it.hasNext()) {
                    arg = it.next();
                    Token token = arg.token();
                    if (TokenInterface.type(token) == TokenTypeInterface.UNARY_OPERATOR && "...".equals(token.surface)) {
                        varArgs = varargsConv.apply(arg.children().getFirst());
                        after = new ArrayList<>(total - before.size() - 1);
                        break fixed;
                    }

                    before.add(argsConv.apply(arg));
                }

                before.addAll(suffix);
                return new InvokeArgs.Fixed(before.toArray(new Var[0]));
            }

            while (it.hasNext()) {
                arg = it.next();
                Token token = arg.token();
                if (TokenInterface.type(token) == TokenTypeInterface.UNARY_OPERATOR && "...".equals(token.surface))
                    break varArgs;
                after.add(argsConv.apply(arg));
            }

            after.addAll(suffix);
            return new InvokeArgs.VarArgs(before.toArray(new Var[0]), varArgs, after.toArray(new Var[0]));
        }

        int i = 0;
        MaybeUnpack[] unpack = new MaybeUnpack[total];
        for (Var v : before) unpack[i++] = new MaybeUnpack(v, false);
        unpack[i++] = new MaybeUnpack(varArgs, true);
        for (Var v : after) unpack[i++] = new MaybeUnpack(v, false);
        unpack[i++] = new MaybeUnpack(varargsConv.apply(arg.children().getFirst()), true);

        while (it.hasNext()) {
            arg = it.next();
            Token token = arg.token();
            unpack[i++] = TokenInterface.type(token) == TokenTypeInterface.UNARY_OPERATOR && "...".equals(token.surface)
                ? new MaybeUnpack(varargsConv.apply(arg.children().getFirst()), true)
                : new MaybeUnpack(argsConv.apply(arg), false);
        }

        for (Var v : suffix) unpack[i++] = new MaybeUnpack(v, false);
        return new InvokeArgs.Mixed(unpack);
    }

    public Binding findVariable(AstToCfgContext context, Token token, boolean mutating) {
        final String name = token.surface;
        final Binding binding;

        search: {
            if (context.scope.get(name) instanceof LocalBinding local) {
                binding = local;
                break search;
            }

            if (!context.captured.containsKey(name)) {
                if (!context.outer.test(name)) {
                    LocalBinding local = new LocalBinding(Var.at(nextAssignment(context.indices)), false);
                    context.captured.put(name, local);
                    binding = local;
                    break search;
                }
                context.captured.put(name, null);
            } else if (context.captured.get(name) instanceof LocalBinding local) {
                binding = local;
                break search;
            }

            if (!(context.globals.get(name) instanceof GlobalBinding global)) {
                binding = null;
                break search;
            }

            if (mutating && global.mutability == GlobalBinding.Mutability.IMPORTED)
                context.linkMutable.putIfAbsent(global.slot.index, token);
            binding = global;
        }

        if (binding == null)
            throw new ExpressionException(this.context, expression, token, "Unknown variable");
        if (mutating && binding.isConstant())
            throw new ExpressionException(this.context, expression, token, "Cannot reassign a constant");
        return binding;
    }

    public @Nullable Var binOpToCfg(
        GenContext ctx,
        AstToCfgContext context,
        Node node,
        Supplier<InvokeTarget> two,
        Supplier<InvokeTarget> many
    ) {
        if (node.children().size() == 3) {
            Var a = astToCfg(GenContext.USE, context, node.children().getFirst());
            Var b = astToCfg(GenContext.USE, context, node.children().getLast());
            Var out = Var.next(ctx, context.indices);
            context.current().code.add(new Instruction.InvokeRead(node.token(),
                two.get(),
                new InvokeArgs.Fixed(new Var[] { a, b }),
                Optional.ofNullable(out)
            ));
            return out;
        }

        Var[] in = new Var[node.children().size() + 1 >> 1];
        for (int i = 0; i < in.length; i++)
            in[i] = astToCfg(GenContext.USE, context, node.children().get(i << 1));
        Var out = Var.next(ctx, context.indices);
        context.current().code.add(new Instruction.InvokeRead(node.children().get(1).token(),
            many.get(),
            new InvokeArgs.Fixed(in),
            Optional.ofNullable(out)
        ));
        return out;
    }

    public record BinderNode(Node node, boolean mutable, Token op) {}

    public sealed interface Condition {
        record Truthy(Node value) implements Condition {}
        record Match(BinderNode[] binders, Node value) implements Condition {}
        record NoMatch(BinderNode[] binders, Node value) implements Condition {}
        record Every(Node[] conditions) implements Condition {}
        record NotAll(Node[] conditions) implements Condition {}
        record Any(Node[] conditions) implements Condition {}
        record None(Node[] conditions) implements Condition {}

        static Condition of(final Node original) {
            boolean negate = false;
            Node node = original;
            while (TokenInterface.type(node.token()) == TokenTypeInterface.UNARY_OPERATOR && "!".equals(node.token().surface)) {
                negate = !negate;
                node = node.children().getFirst();
            }

            if (TokenInterface.type(node.token()) != TokenTypeInterface.OPERATOR) return new Truthy(original);
            if ("&&".equals(node.token().surface) || "||".equals(node.token().surface)) {
                Node[] conditions = new Node[node.children().size() + 1 >> 1];
                for (int i = 0; i < conditions.length; i++) conditions[i] = node.children().get(i << 1);
                return "&&".equals(node.token().surface)
                    ? negate ? new NotAll(conditions) : new Every(conditions)
                    : negate ? new None(conditions) : new Any(conditions);
            }

            if (!":=".equals(node.token().surface) && !"::".equals(node.token().surface)) return new Truthy(original);

            BinderNode[] binders = new BinderNode[node.children().size() >> 1];
            for (int i = 0; i < binders.length; i++) {
                Token token = node.children().get(i << 1 | 1).token();
                binders[i] = new BinderNode(node.children().get(i << 1), ":=".equals(token.surface), token);
            }

            return negate
                ? new NoMatch(binders, node.children().getLast())
                : new   Match(binders, node.children().getLast());
        }
    }

    public sealed interface Pattern {
        sealed interface Invoke extends Pattern {}

        Node node();

        record Wildcard(Node node) implements Pattern {}
        record Binding(Node node) implements Pattern {}
        record Index(Node node, Node on, List<? extends Node> with) implements Invoke {}
        record Call(Node node, Node on, List<? extends Node> with) implements Invoke {}
        record Method(Node node, Node on, Token name, List<? extends Node> with) implements Invoke {}
        record Member(Node node, Node on, Token name) implements Pattern {}
        record ListOf(Node node, List<? extends Pattern> elements) implements Pattern {}
    }

    public Pattern astToPattern(Node node, @Nullable Set<String> names, @Nullable String duplicationMessage) {
        Token token = node.token();
        List<Node> children = node.children();
        return switch (TokenInterface.type(token).name()) {
            case TokenTypeName.VARIABLE -> switch (token.surface) {
                case "_" -> new Pattern.Wildcard(node);
                default -> {
                    if (KEYWORDS.contains(token.surface)) throw new ExpressionException(this.context, expression, token,
                        "A keyword cannot be used as a variable name"
                    );
                    if (names != null) {
                        if (names.contains(token.surface))
                            throw new ExpressionException(this.context, expression, token, duplicationMessage);
                        names.add(token.surface);
                    }
                    yield new Pattern.Binding(node);
                }
            };
            case TokenTypeName.OPEN_PAREN -> {
                Node head = children.getFirst();
                List<Node> tail = children.subList(1, children.size());
                yield switch (token.surface) {
                    case "[" -> new Pattern.Index(node, head, tail);
                    case "(" -> head.children() != null
                        || TokenInterface.type(head.token()) != TokenTypeInterface.OPEN_PAREN
                        ? new Pattern.Call(node, head, tail)
                        : switch (head.token().surface) {
                            case "[" -> new Pattern.ListOf(node, tail.stream()
                                .map(n -> switch (astToPattern(n, names, duplicationMessage)) {
                                    case Pattern.Wildcard p -> p;
                                    case Pattern.Binding p -> p;
                                    case Pattern.Invoke p -> throw new ExpressionException(this.context, expression, p.node().token(),
                                        "Invoke patterns are only allowed at the top level"
                                    );
                                    case Pattern.Member p -> throw new ExpressionException(this.context, expression, p.node.token(),
                                        "Member pattern is only allowed at the top level"
                                    );
                                    case Pattern.ListOf p -> throw new ExpressionException(this.context, expression, p.node.token(),
                                        "Nested patterns are not yet supported"
                                    );
                                })
                                .toList()
                            );
                            case "{" -> throw new ExpressionException(this.context, expression, token,
                                "Map patterns are not yet supported"
                            );
                            default -> throw new AssertionError();
                        };
                    default -> throw new AssertionError();
                };
            }
            default -> {
                if (TokenInterface.type(token) == TokenTypeInterface.OPERATOR && ".".equals(token.surface)) {
                    Node on = node.opLhs();
                    Node last = children.getLast();
                    Token lastToken = last.token();
                    if (TokenInterface.type(lastToken) == TokenTypeInterface.VARIABLE)
                        yield new Pattern.Member(node, on, lastToken);
                    List<Node> lastChildren = last.children();
                    yield new Pattern.Method(
                        children.get(children.size() - 2),
                        on,
                        lastChildren.getFirst().token(),
                        lastChildren.subList(0, lastChildren.size() - 1)
                    );
                }
                throw new ExpressionException(this.context, expression, token, "Invalid pattern");
            }
        };
    }

    public record BinderPattern(Pattern pat, boolean mutable, Token op) {}

    public void astToCfgBindAssert(AstToCfgContext context, BinderNode[] binders, Var from) {
        for (BinderPattern pat : Arrays.stream(binders)
            .map(b -> new BinderPattern(astToPattern(b.node, new HashSet<>(), "Duplicate binding"), b.mutable, b.op))
            .toList()
            .reversed()
        ) switch (pat.pat) {
            case Pattern.Wildcard _ -> {}
            case Pattern.Binding(Node node) -> {
                LocalBinding bind = new LocalBinding(Var.at(nextAssignment(context.indices)), pat.mutable);
                context.bindings.add(bind);
                context.scope.put(node.token().surface, bind);
                context.current().code.add(new Instruction.Move(from, bind.slot));
            }
            case Pattern.Invoke p -> throw new ExpressionException(this.context, expression, p.node().token(),
                "Invoke patterns are not allowed here, use '=' instead"
            );
            case Pattern.Member p -> throw new ExpressionException(this.context, expression, p.node.token(),
                "Member pattern is not allowed here, use '=' instead"
            );
            case Pattern.ListOf(Node node, List<? extends Pattern> elements) -> {
                Token tok = node.token();
                int size = elements.size();
                context.current().code.add(new Instruction.InvokeRead(tok,
                    new InvokeTarget.Builtin((_, args) -> {
                        if (!(args[0] instanceof ListValue list))
                            throw new ExpressionException(this.context, expression, tok,
                                "Expected a list, got: " + args[0].getString()
                            );

                        if (list.length() != size)
                            throw new ExpressionException(this.context, expression, tok,
                                "Expected " + size + " elements, got " + list.length()
                            );

                        return Value.NULL;
                    }),
                    new InvokeArgs.Single(from),
                    Optional.empty()
                ));

                int i = 0;
                for (Pattern elem : elements) switch (elem) {
                    case Pattern.Wildcard _ -> i++;
                    case Pattern.Binding(Node n) -> {
                        LocalBinding bind = new LocalBinding(Var.at(nextAssignment(context.indices)), pat.mutable);
                        context.bindings.add(bind);
                        context.scope.put(n.token().surface, bind);
                        Var index = Var.at(nextAssignment(context.indices));
                        context.current().code.add(new Instruction.Constant(new NumericValue((long)i), index));
                        context.current().code.add(new Instruction.InvokeRead(n.token(),
                            new InvokeTarget.Builtin((_, args) ->
                                ((ListValue)args[0]).getItems().get(NumericValue.asNumber(args[1]).getInt())
                            ),
                            new InvokeArgs.Fixed(new Var[] { from, index }),
                            Optional.of(bind.slot)
                        ));
                        i++;
                    }
                    default -> throw new AssertionError();
                }
            }
        }
    }

    public Var astToCfgBindCheck(AstToCfgContext context, BinderNode[] binders, Var from) {
        List<BinderPattern> patterns = Arrays.stream(binders)
            .map(b -> new BinderPattern(astToPattern(b.node, new HashSet<>(), "Duplicate binding"), b.mutable, b.op))
            .toList()
            .reversed();
        Pos done = null;
        Int2IntMap phi = null;
        Var result = null, check = null;

        for (BinderPattern pat : patterns) switch (pat.pat) {
            case Pattern.Wildcard _ -> {}
            case Pattern.Binding(Node node) -> {
                LocalBinding bind = new LocalBinding(Var.at(nextAssignment(context.indices)), pat.mutable);
                context.bindings.add(bind);
                context.scope.put(node.token().surface, bind);
                context.current().code.add(new Instruction.Move(from, bind.slot));
            }
            case Pattern.Invoke p -> throw new ExpressionException(this.context, expression, p.node().token(),
                "Invoke patterns are not allowed here, use '=' instead"
            );
            case Pattern.Member p -> throw new ExpressionException(this.context, expression, p.node.token(),
                "Member pattern is not allowed here, use '=' instead"
            );
            case Pattern.ListOf(Node node, List<? extends Pattern> elements) -> {
                if (done == null) {
                    done = context.createBlock();
                    phi = new Int2IntOpenHashMap();
                    result = Var.at(nextAssignment(context.indices));
                    context.blocks.get(done.index).phis.put(result.index, phi);
                }

                int size = elements.size();
                check = Var.at(nextAssignment(context.indices));
                context.current().code.add(new Instruction.InvokeRead(node.token(),
                    new InvokeTarget.Builtin((_, args) -> BooleanValue.of(
                        args[0] instanceof ListValue list && list.length() == size
                    )),
                    new InvokeArgs.Single(from),
                    Optional.of(check)
                ));

                Pos matched = context.createBlock();
                phi.put(context.currentBlock[0].index, check.index);
                context.current().code.add(new Instruction.Branch(matched, done, check, ValueSemantics::getBoolean));
                context.currentBlock[0] = matched;

                int i = 0;
                for (Pattern elem : elements) switch (elem) {
                    case Pattern.Wildcard _ -> i++;
                    case Pattern.Binding(Node n) -> {
                        LocalBinding bind = new LocalBinding(Var.at(nextAssignment(context.indices)), pat.mutable);
                        context.bindings.add(bind);
                        context.scope.put(n.token().surface, bind);
                        Var index = Var.at(nextAssignment(context.indices));
                        context.current().code.add(new Instruction.Constant(new NumericValue((long)i), index));
                        context.current().code.add(new Instruction.InvokeRead(n.token(),
                            new InvokeTarget.Builtin((_, args) ->
                                ((ListValue)args[0]).getItems().get(NumericValue.asNumber(args[1]).getInt())
                            ),
                            new InvokeArgs.Fixed(new Var[] { from, index }),
                            Optional.of(bind.slot)
                        ));
                        i++;
                    }
                    default -> throw new AssertionError();
                }
            }
        }

        if (done == null) {
            Var to = Var.at(nextAssignment(context.indices));
            context.current().code.add(new Instruction.Constant(Value.TRUE, to));
            return to;
        }

        phi.put(context.currentBlock[0].index, check.index);
        context.current().code.add(new Instruction.Jump(done));
        context.currentBlock[0] = done;
        return result;
    }

    public Pos andToCfgBindChecks(
        AstToCfgContext context, Node[] conditions,
        Pos onTrue, Pos onFalse, @Nullable Int2IntMap phi
    ) {
        for (Node node : conditions) switch (Condition.of(node)) {
            case Condition.Truthy _, Condition.NotAll _, Condition.Any _ -> {
                Var from = astToCfg(GenContext.USE, context, node);
                context.current().code.add(new Instruction.Branch(onTrue, onFalse, from, ValueSemantics::getBoolean));
                if (phi != null) phi.put(context.currentBlock[0].index, from.index);
                context.currentBlock[0] = onTrue;
                onTrue = context.createBlock();
            }
            case Condition.Match(BinderNode[] binders, Node value) -> {
                Var from = astToCfgBindCheck(context, binders, astToCfg(GenContext.USE, context, value));
                context.current().code.add(new Instruction.Branch(onTrue, onFalse, from, ValueSemantics::getBoolean));
                if (phi != null) phi.put(context.currentBlock[0].index, from.index);
                context.currentBlock[0] = onTrue;
                onTrue = context.createBlock();
            }
            case Condition.NoMatch _ -> throw new ExpressionException(
                this.context, expression, node.token(), "Only positive matches are allowed with '&&'"
            );
            case Condition.Every(Node[] every) -> onTrue = andToCfgBindChecks(context, every, onTrue, onFalse, phi);
            case Condition.None(Node[] none) -> onTrue = orToCfgBindChecks(context, none, onFalse, onTrue, phi);
        }
        return onTrue;
    }

    public Pos orToCfgBindChecks(
        AstToCfgContext context, Node[] conditions,
        Pos onTrue, Pos onFalse, @Nullable Int2IntMap phi
    ) {
        for (Node node : conditions) switch (Condition.of(node)) {
            case Condition.Truthy _, Condition.Every _, Condition.None _ -> {
                Var from = astToCfg(GenContext.USE, context, node);
                context.current().code.add(new Instruction.Branch(onTrue, onFalse, from, ValueSemantics::getBoolean));
                if (phi != null) phi.put(context.currentBlock[0].index, from.index);
                context.currentBlock[0] = onFalse;
                onFalse = context.createBlock();
            }
            case Condition.Match _ -> throw new ExpressionException(
                this.context, expression, node.token(), "Only negative matches are allowed with '||'"
            );
            case Condition.NoMatch(BinderNode[] binders, Node value) -> {
                Var from = astToCfgBindCheck(context, binders, astToCfg(GenContext.USE, context, value));
                context.current().code.add(new Instruction.Branch(onFalse, onTrue, from, ValueSemantics::getBoolean));
                if (phi != null) phi.put(context.currentBlock[0].index, from.index);
                context.currentBlock[0] = onFalse;
                onFalse = context.createBlock();
            }
            case Condition.NotAll(Node[] notAll) -> onFalse = andToCfgBindChecks(context, notAll, onFalse, onTrue, phi);
            case Condition.Any(Node[] any) -> onFalse = orToCfgBindChecks(context, any, onTrue, onFalse, phi);
        }
        return onFalse;
    }

    public Var assignmentInstruction(AstToCfgContext context, LValue to, Token op, Var from) {
        if ("=".equals(op.surface)) {
            context.current().code.add(switch (to) {
                case LocalBinding(Var slot, _) -> new Instruction.Move(from, slot);
                case GlobalBinding(Global global, _) -> new Instruction.GlobalStore(from, global);
                case LValue.Invoke(Token token, InvokeTarget target, InvokeArgs args) ->
                    new Instruction.InvokeStore(from, token, target, args);
                case LValue.Member(Var on, StringValue name) -> new Instruction.MemberStore(from, on, name);
            });
            return from;
        }

        Var temp = to instanceof LocalBinding(Var slot, _) ? slot : Var.at(nextAssignment(context.indices));
        switch (to) {
            case LocalBinding(Var slot, _) -> {}
            case GlobalBinding(Global global, _) ->
                context.current().code.add(new Instruction.GlobalRead(global, temp));
            case LValue.Invoke(Token token, InvokeTarget target, InvokeArgs args) ->
                context.current().code.add(new Instruction.InvokeRead(token, target, args, Optional.of(temp)));
            case LValue.Member(Var on, StringValue name) ->
                context.current().code.add(new Instruction.MemberRead(on, name, temp));
        }

        Var result = to instanceof LocalBinding(Var slot, _) ? slot : Var.at(nextAssignment(context.indices));
        BiFunction<Context, Value[], Value> fun = switch (op.surface) {
            case "+=" -> (_, args) -> ValueSemantics.add(args[0], args[1]);
            case "-=" -> (_, args) -> ValueSemantics.subtract(args[0], args[1]);
            case "*=" -> (_, args) -> ValueSemantics.multiply(args[0], args[1]);
            case "/=" -> (_, args) -> ValueSemantics.divide(args[0], args[1]);
            case "%=" -> (_, args) -> ValueSemantics.modulo(args[0], args[1]);
            case "^=" -> (_, args) -> ValueSemantics.exponent(args[0], args[1]);
            case "&=" -> (_, args) -> ValueSemantics.getBoolean(args[0]) ? args[1] : args[0];
            case "|=" -> (_, args) -> ValueSemantics.getBoolean(args[0]) ? args[0] : args[1];
            case "?=" -> (_, args) -> args[0].isNull() ? args[1] : args[0];
            default -> throw new AssertionError();
        };

        context.current().code.add(new Instruction.InvokeRead(op,
            new InvokeTarget.Builtin(fun),
            new InvokeArgs.Fixed(new Var[] { temp, from }),
            Optional.of(result)
        ));
        switch (to) {
            case LocalBinding _ -> {}
            case GlobalBinding(Global global, _) ->
                context.current().code.add(new Instruction.GlobalStore(result, global));
            case LValue.Invoke(Token token, InvokeTarget target, InvokeArgs args) ->
                context.current().code.add(new Instruction.InvokeStore(result, token, target, args));
            case LValue.Member(Var on, StringValue name) ->
                context.current().code.add(new Instruction.MemberStore(result, on, name));
        }

        return result;
    }

    public @Nullable Var constantInstruction(GenContext ctx, AstToCfgContext context, Value value) {
        if (ctx == GenContext.DISCARD) return null;
        Var to = Var.at(nextAssignment(context.indices));
        context.current().code.add(new Instruction.Constant(Value.NULL, to));
        return to;
    }

    public @Nullable Var astToCfg(GenContext ctx, AstToCfgContext context, Node node) {
        Token token = node.token();
        return switch (TokenInterface.type(token).name()) {
            case TokenTypeName.CONSTANT -> switch (token.surface) {
                case "null" -> constantInstruction(ctx, context, Value.NULL);
                default -> throw new IllegalArgumentException("Unknown constant token: " + token.surface);
            };
            case TokenTypeName.LITERAL -> {
                NumericValue value;
                try {
                    value = (token.surface.length() >= 2 && token.surface.charAt(0) == '0'
                        ? switch (token.surface.charAt(1)) {
                            case 'b' -> Optional.of(new NumericValue(Long.parseLong(
                                token.surface, 2, token.surface.length(), 2
                            )));
                            case 'o' -> Optional.of(new NumericValue(Long.parseLong(
                                token.surface, 2, token.surface.length(), 8
                            )));
                            case 'x' -> Optional.of(new NumericValue(Long.parseLong(
                                token.surface, 2, token.surface.length(), 16
                            )));
                            default -> Optional.<NumericValue>empty();
                        } : Optional.<NumericValue>empty()
                    ).orElseGet(() -> token.surface.indexOf('.') >= 0 || token.surface.indexOf('E') >= 0
                        ? new NumericValue(Double.parseDouble(token.surface))
                        : new NumericValue(Long.parseLong(token.surface.substring(2)))
                    );
                } catch (NumberFormatException e) {
                    throw new ExpressionException(this.context, expression, token, "Invalid number: " + e.getMessage());
                }

                if (ctx == GenContext.DISCARD) yield null;
                Var to = Var.at(nextAssignment(context.indices));
                context.current().code.add(new Instruction.Constant(value, to));
                yield to;
            }
            case TokenTypeName.STRINGPARAM -> {
                Var v = Var.at(nextAssignment(context.indices));
                context.current().code.add(new Instruction.Constant(StringValue.of(token.surface), v));
                if (node.children() == null || node.children().isEmpty()) yield v;

                InvokeTarget invokeTarget = new InvokeTarget.Builtin(
                    (_, args) -> StringValue.of(Arrays.stream(args)
                        .map(Value::getString)
                        .collect(Collectors.joining())
                    )
                );

                Var[] args = new Var[1 + node.children().size()];
                InvokeArgs invokeArgs = new InvokeArgs.Fixed(args);
                int i = 0;
                args[i++] = v;
                for (Node child : node.children())
                    args[i++] = astToCfg(GenContext.USE, context, child);

                v = Var.next(ctx, context.indices);
                context.current().code.add(new Instruction.InvokeRead(token, invokeTarget, invokeArgs, Optional.ofNullable(v)));
                yield v;
            }
            case TokenTypeName.VARIABLE -> switch (token.surface) {
                case "null" -> constantInstruction(ctx, context, Value.NULL);
                case "false" -> constantInstruction(ctx, context, Value.FALSE);
                case "true" -> constantInstruction(ctx, context, Value.TRUE);
                case "pi" -> constantInstruction(ctx, context, Arithmetic.PI);
                case "euler" -> constantInstruction(ctx, context, Arithmetic.euler);
                default -> {
                    if (KEYWORDS.contains(token.surface))
                        throw new ExpressionException(this.context, expression, token, "This keyword cannot be used here");

                    Binding binding = findVariable(context, token, false);
                    if (ctx == GenContext.DISCARD) yield null;

                    yield switch (binding) {
                        case LocalBinding(Var from, boolean _) -> from;
                        case GlobalBinding(Global global, GlobalBinding.Mutability _) -> {
                            Var to = Var.at(nextAssignment(context.indices));
                            context.current().code.add(new Instruction.GlobalRead(global, to));
                            yield to;
                        }
                    };
                }
            };
            case TokenTypeName.UNARY_OPERATOR -> switch (token.surface) {
                case "..." -> throw new ExpressionException(this.context, expression, token, "'...' is not allowed here");
                case "." -> throw new ExpressionException(this.context, expression, token, "Unary dot is not allowed here");
                case "+" -> {
                    Var in = astToCfg(GenContext.USE, context, node.children().getFirst());
                    Var out = Var.next(ctx, context.indices);
                    context.current().code.add(new Instruction.InvokeRead(token,
                        new InvokeTarget.Builtin((_, args) -> NumericValue.asNumber(args[0])),
                        new InvokeArgs.Single(in),
                        Optional.ofNullable(out)
                    ));
                    yield out;
                }
                case "-" -> {
                    Var in = astToCfg(GenContext.USE, context, node.children().getFirst());
                    Var out = Var.next(ctx, context.indices);
                    context.current().code.add(new Instruction.InvokeRead(token,
                        new InvokeTarget.Builtin((_, args) -> ValueSemantics.opposite(args[0])),
                        new InvokeArgs.Single(in),
                        Optional.ofNullable(out)
                    ));
                    yield out;
                }
                case "!" -> {
                    Var in = astToCfg(GenContext.USE, context, node.children().getFirst());
                    if (in == null) yield null;
                    Var out = Var.next(ctx, context.indices);
                    context.current().code.add(new Instruction.InvokeRead(token,
                        new InvokeTarget.Builtin((_, args) -> BooleanValue.of(!ValueSemantics.getBoolean(args[0]))),
                        new InvokeArgs.Single(in),
                        Optional.ofNullable(out)
                    ));
                    yield out;
                }
                case "?" -> {
                    Var in = astToCfg(GenContext.USE, context, node.children().getFirst());
                    if (in == null) yield null;
                    Var out = Var.next(ctx, context.indices);
                    context.current().code.add(new Instruction.InvokeRead(token,
                        new InvokeTarget.Builtin((_, args) -> BooleanValue.of(!args[0].isNull())),
                        new InvokeArgs.Single(in),
                        Optional.ofNullable(out)
                    ));
                    yield out;
                }
                default -> throw new IllegalArgumentException("Unknown unary operator: " + token.surface);
            };
            case TokenTypeName.OPERATOR -> switch (token.surface) {
                case "+" -> binOpToCfg(ctx, context, node,
                    () -> new InvokeTarget.Builtin((_, args) -> ValueSemantics.add(args[0], args[1])),
                    () -> new InvokeTarget.Builtin((_, args) -> {
                        Value acc = args[0];
                        for (int i = 1; i < args.length; i++) acc = ValueSemantics.add(acc, args[i]);
                        return acc;
                    })
                );
                case "-" -> binOpToCfg(ctx, context, node,
                    () -> new InvokeTarget.Builtin((_, args) -> ValueSemantics.subtract(args[0], args[1])),
                    () -> new InvokeTarget.Builtin((_, args) -> {
                        Value acc = args[0];
                        for (int i = 1; i < args.length; i++) acc = ValueSemantics.subtract(acc, args[i]);
                        return acc;
                    })
                );
                case "*" -> binOpToCfg(ctx, context, node,
                    () -> new InvokeTarget.Builtin((_, args) -> ValueSemantics.multiply(args[0], args[1])),
                    () -> new InvokeTarget.Builtin((_, args) -> {
                        Value acc = args[0];
                        for (int i = 1; i < args.length; i++) acc = ValueSemantics.multiply(acc, args[i]);
                        return acc;
                    })
                );
                case "/" -> binOpToCfg(ctx, context, node,
                    () -> new InvokeTarget.Builtin((_, args) -> ValueSemantics.divide(args[0], args[1])),
                    () -> new InvokeTarget.Builtin((_, args) -> {
                        Value acc = args[0];
                        for (int i = 1; i < args.length; i++) acc = ValueSemantics.divide(acc, args[i]);
                        return acc;
                    })
                );
                case "%" -> binOpToCfg(ctx, context, node,
                    () -> new InvokeTarget.Builtin((_, args) -> ValueSemantics.modulo(args[0], args[1])),
                    () -> new InvokeTarget.Builtin((_, args) -> {
                        Value acc = args[0];
                        for (int i = 1; i < args.length; i++) acc = ValueSemantics.modulo(acc, args[i]);
                        return acc;
                    })
                );
                case "^" -> binOpToCfg(ctx, context, node,
                    () -> new InvokeTarget.Builtin((_, args) -> ValueSemantics.exponent(args[0], args[1])),
                    () -> new InvokeTarget.Builtin((_, args) -> {
                        Value acc = args[args.length - 1];
                        for (int i = args.length - 2; i >= 0; i--) acc = ValueSemantics.exponent(args[i], acc);
                        return acc;
                    })
                );
                case "&" -> binOpToCfg(ctx, context, node,
                    () -> new InvokeTarget.Builtin((_, args) -> ValueSemantics.getBoolean(args[0]) ? args[1] : args[0]),
                    () -> new InvokeTarget.Builtin((_, args) -> {
                        for (int i = 0, max = args.length - 1; i < max; i++)
                            if (!ValueSemantics.getBoolean(args[i])) return args[i];
                        return args[args.length - 1];
                    })
                );
                case "|" -> binOpToCfg(ctx, context, node,
                    () -> new InvokeTarget.Builtin((_, args) -> ValueSemantics.getBoolean(args[0]) ? args[0] : args[1]),
                    () -> new InvokeTarget.Builtin((_, args) -> {
                        for (int i = 0, max = args.length - 1; i < max; i++)
                            if (ValueSemantics.getBoolean(args[i])) return args[i];
                        return args[args.length - 1];
                    })
                );
                case "?" -> binOpToCfg(ctx, context, node,
                    () -> new InvokeTarget.Builtin((_, args) -> args[0].isNull() ? args[1] : args[0]),
                    () -> new InvokeTarget.Builtin((_, args) -> {
                        for (int i = 0, max = args.length - 1; i < max; i++)
                            if (!args[i].isNull()) return args[i];
                        return args[args.length - 1];
                    })
                );
                case "&&" -> {
                    Node[] conditions = new Node[node.children().size() >> 1];
                    for (int i = 0; i < conditions.length; i++) conditions[i] = node.children().get(i << 1);
                    Var to = Var.next(ctx, context.indices);
                    AstToCfgContext scoped = context.newScope();
                    Int2IntMap phi = to != null ? new Int2IntOpenHashMap(conditions.length + 1) : null;
                    Pos mergePos = context.createBlock();
                    Pos passPos = andToCfgBindChecks(context, conditions, context.createBlock(), mergePos, phi);
                    if (to != null) context.blocks.get(mergePos.index).phis.put(to.index, phi);

                    context.currentBlock[0] = passPos;
                    if (to != null) phi.put(passPos.index, astToCfg(ctx, scoped, node.children().getLast()).index);
                    context.current().code.add(new Instruction.Jump(mergePos));
                    context.currentBlock[0] = mergePos;
                    yield to;
                }
                case "||" -> {
                    Node[] conditions = new Node[node.children().size() >> 1];
                    for (int i = 0; i < conditions.length; i++) conditions[i] = node.children().get(i << 1);
                    Var to = Var.next(ctx, context.indices);
                    AstToCfgContext scoped = context.newScope();
                    Int2IntMap phi = to != null ? new Int2IntOpenHashMap(conditions.length + 1) : null;
                    Pos mergePos = context.createBlock();
                    Pos passPos = orToCfgBindChecks(context, conditions, mergePos, context.createBlock(), phi);
                    if (to != null) context.blocks.get(mergePos.index).phis.put(to.index, phi);

                    context.currentBlock[0] = passPos;
                    if (to != null) phi.put(passPos.index, astToCfg(ctx, scoped, node.children().getLast()).index);
                    context.current().code.add(new Instruction.Jump(mergePos));
                    context.currentBlock[0] = mergePos;
                    yield to;
                }
                case "??" -> {
                    Pos mergePos = context.createBlock();
                    Var to = Var.next(ctx, context.indices);
                    Int2IntMap phi = to != null ? new Int2IntOpenHashMap(node.children().size() + 1 >> 1) : null;
                    if (to != null) context.blocks.get(mergePos.index).phis.put(to.index, phi);

                    for (int i = 0, max = node.children().size() - 1; i < max; i += 2) {
                        Var branch = astToCfg(GenContext.USE, context, node.children().get(i));
                        if (to != null) phi.put(context.currentBlock[0].index, branch.index);
                        Pos branchPos = context.createBlock();
                        context.current().code.add(new Instruction.Branch(branchPos, mergePos, branch, Value::isNull));
                        context.currentBlock[0] = branchPos;
                    }

                    if (to != null)
                        phi.put(context.currentBlock[0].index, astToCfg(ctx, context, node.children().getLast()).index);
                    context.current().code.add(new Instruction.Jump(mergePos));
                    context.currentBlock[0] = mergePos;
                    yield to;
                }
                case "==", "<", "<=", ">", ">=" -> binOpToCfg(ctx, context, node,
                    () -> new InvokeTarget.Builtin(switch (token.surface) {
                        case "==" -> (_, args) -> BooleanValue.of(ValueSemantics.equal(args[0], args[1]));
                        case "<"  -> (_, args) -> BooleanValue.of(ValueSemantics.compare(args[0], args[1]) < 0);
                        case "<=" -> (_, args) -> BooleanValue.of(ValueSemantics.compare(args[0], args[1]) <= 0);
                        case ">"  -> (_, args) -> BooleanValue.of(ValueSemantics.compare(args[0], args[1]) > 0);
                        case ">=" -> (_, args) -> BooleanValue.of(ValueSemantics.compare(args[0], args[1]) >= 0);
                        default -> throw new AssertionError();
                    }),
                    () -> {
                        enum Relation { EQ, LT, LE, GT, GE }
                        Relation[] relations = new Relation[node.children().size() >> 1];
                        for (int i = 0; i < relations.length; i++)
                            relations[i] = switch (node.children().get(i << 1 | 1).token().surface) {
                                case "==" -> Relation.EQ;
                                case "<"  -> Relation.LT;
                                case "<=" -> Relation.LE;
                                case ">"  -> Relation.GT;
                                case ">=" -> Relation.GE;
                                default -> throw new AssertionError();
                            };

                        return new InvokeTarget.Builtin((c, args) -> {
                            for (int i = 0; i < relations.length; i++) if (!switch (relations[i]) {
                                case EQ -> ValueSemantics.equal(args[i], args[i + 1]);
                                case LT -> ValueSemantics.compare(args[i], args[i + 1]) < 0;
                                case LE -> ValueSemantics.compare(args[i], args[i + 1]) <= 0;
                                case GT -> ValueSemantics.compare(args[i], args[i + 1]) > 0;
                                case GE -> ValueSemantics.compare(args[i], args[i + 1]) >= 0;
                            }) return Value.FALSE;
                            return Value.TRUE;
                        });
                    }
                );
                case "!=" -> binOpToCfg(ctx, context, node,
                    () -> new InvokeTarget.Builtin((_, args) -> BooleanValue.of(!ValueSemantics.equal(args[0], args[1]))),
                    () -> new InvokeTarget.Builtin((_, args) -> {
                        ObjectSet<Value> set = new ObjectOpenCustomHashSet<>(args.length, ValueSemantics.HASH);
                        for (Value v : args) if (!set.add(v)) return Value.FALSE;
                        return Value.TRUE;
                    })
                );
                case "=", "+=", "-=", "*=", "/=", "%=", "^=", "&=", "|=", "?=" /* , "&&=", "||=", "??=" */ -> {
                    Var to = astToCfg(GenContext.USE, context, node.children().getLast());
                    for (int i = node.children().size() - 2; i >= 0; i -= 2) {
                        Token opToken = node.children().get(i + 1).token();
                        String op = opToken.surface;
                        Pattern pat = astToPattern(node.children().get(i), new HashSet<>(), "Duplicate assignment");
                        boolean compound = !"=".equals(op);
                        switch (pat) {
                            case Pattern.Wildcard(Node n) -> {
                                if (compound) throw new ExpressionException(this.context, expression, n.token(),
                                    "'_' can only be assigned to"
                                );
                            }
                            case Pattern.Binding(Node variable) -> to = assignmentInstruction(
                                context, findVariable(context, variable.token(), true), opToken, to
                            );
                            case Pattern.Index(Node n, Node on, List<? extends Node> with) -> to = assignmentInstruction(
                                context,
                                new LValue.Invoke(n.token(),
                                    new InvokeTarget.Index(astToCfg(GenContext.USE, context, on)),
                                    astToArgsSimple(context, with)
                                ),
                                opToken, to
                            );
                            case Pattern.Call(Node n, Node on, List<? extends Node> with) -> to = assignmentInstruction(
                                context,
                                new LValue.Invoke(n.token(),
                                    new InvokeTarget.Func(astToCfg(GenContext.USE, context, on)),
                                    astToArgsSimple(context, with)
                                ),
                                opToken, to
                            );
                            case Pattern.Method(Node n, Node on, Token name, List<? extends Node> with) -> to = assignmentInstruction(
                                context,
                                new LValue.Invoke(n.token(),
                                    new InvokeTarget.Method(
                                        astToCfg(GenContext.USE, context, on),
                                        new StringValue(name.surface)
                                    ),
                                    astToArgsSimple(context, with)
                                ),
                                opToken, to
                            );
                            case Pattern.Member(Node _, Node on, Token name) -> to = assignmentInstruction(
                                context,
                                new LValue.Member(
                                    astToCfg(GenContext.USE, context, on),
                                    new StringValue(name.surface)
                                ),
                                opToken, to
                            );
                            case Pattern.ListOf(Node n, List<? extends Pattern> elements) -> {
                                Token tok = n.token();
                                int size = elements.size();
                                context.current().code.add(new Instruction.InvokeRead(tok,
                                    new InvokeTarget.Builtin((_, args) -> {
                                        if (!(args[0] instanceof ListValue list))
                                            throw new ExpressionException(this.context, expression, tok,
                                                "Expected a list, got: " + args[0].getString()
                                            );

                                        if (list.length() != size)
                                            throw new ExpressionException(this.context, expression, tok,
                                                "Expected " + size + " elements, got " + list.length()
                                            );

                                        return Value.NULL;
                                    }),
                                    new InvokeArgs.Single(to),
                                    Optional.empty()
                                ));

                                int j = 0;
                                Var[] vars = compound && (i > 0 || ctx != GenContext.DISCARD) ? new Var[size] : null;
                                for (Pattern patpat : elements) switch (patpat) {
                                    case Pattern.Wildcard _ -> {
                                        if (compound) throw new ExpressionException(this.context, expression, n.token(),
                                            "'_' can only be assigned to"
                                        );
                                    }
                                    case Pattern.Binding(Node variable) -> {
                                        Var v = assignmentInstruction(
                                            context, findVariable(context, variable.token(), true), opToken, to
                                        );
                                        Var index = Var.at(nextAssignment(context.indices));
                                        context.current().code.add(new Instruction.Constant(new NumericValue((long)j), index));
                                        context.current().code.add(new Instruction.InvokeRead(variable.token(),
                                            new InvokeTarget.Builtin((_, args) ->
                                                ((ListValue)args[0]).getItems().get(NumericValue.asNumber(args[1]).getInt())
                                            ),
                                            new InvokeArgs.Fixed(new Var[] { to, index }),
                                            Optional.of(v)
                                        ));
                                        if (vars != null) vars[j] = v;
                                        j++;
                                    }
                                    default -> throw new AssertionError();
                                }

                                if (vars != null) {
                                    to = Var.at(nextAssignment(context.indices));
                                    context.current().code.add(new Instruction.InvokeRead(tok,
                                        new InvokeTarget.Builtin((_, args) -> ListValue.of(args)),
                                        switch (vars.length) {
                                            case 0 -> new InvokeArgs.Empty();
                                            case 1 -> new InvokeArgs.Single(vars[0]);
                                            default -> new InvokeArgs.Fixed(vars);
                                        },
                                        Optional.of(to)
                                    ));
                                } else if (compound) to = null;
                            }
                        }
                    }
                    yield to;
                }
                case "<><" -> {
                    List<Node> children = node.children();
                    Var container = astToCfg(GenContext.USE, context, children.getFirst());
                    List<Node> elements = new ArrayList<>(children.size() >> 1);
                    for (int i = 2, max = children.size(); i < max; i += 2) elements.add(children.get(i));

                    context.current().code.add(new Instruction.InvokeRead(token,
                        new InvokeTarget.Builtin((_, args) -> {
                            ValueSemantics.extend(args[0], Arrays.asList(args).subList(1, args.length));
                            return Value.NULL;
                        }),
                        astToArgsPlus(context, List.of(container), elements, List.of()),
                        Optional.empty()
                    ));
                    yield container;
                }
                case ":=" -> throw new ExpressionException(this.context, expression, token, "':=' is not allowed here");
                case "::" -> throw new ExpressionException(this.context, expression, token, "'::' is not allowed here");
                case "->" -> {
                    Node signature = node.children().getFirst();
                    List<Node> argNodes = signature.children();
                    if (TokenInterface.type(signature.token()) != TokenTypeInterface.OPEN_PAREN
                     || TokenInterface.type(argNodes.getFirst().token()) != TokenTypeInterface.VARIABLE
                     || !"fn".equals(argNodes.getFirst().token().surface))
                        throw new ExpressionException(this.context, expression, signature.token(),
                            "Expected fn() on the left side of '->'"
                        );

                    List<String> args = new ArrayList<>(argNodes.size() - 1);
                    String varArgs = null;
                    for (int i = 1; i < argNodes.size(); i++) {
                        Node arg = argNodes.get(i);
                        Token tok = arg.token();
                        boolean setVarArgs = TokenInterface.type(tok) == TokenTypeInterface.UNARY_OPERATOR
                            && "...".equals(tok.surface);
                        if (setVarArgs) {
                            arg = arg.children().getFirst();
                            tok = arg.token();
                        }

                        if (TokenInterface.type(tok) != TokenTypeInterface.VARIABLE)
                            throw new ExpressionException(this.context, expression, tok, "Invalid argument name");
                        if ("_".equals(tok.surface)) {}
                        else if (KEYWORDS.contains(tok.surface))
                            throw new ExpressionException(this.context, expression, tok,
                                "A keyword cannot be used as an argument name"
                            );
                        else if (tok.surface.equals(varArgs) || args.contains(tok.surface))
                            throw new ExpressionException(this.context, expression, tok, "Duplicate argument name");

                        if (setVarArgs) {
                            if (i + 1 < argNodes.size()) throw new ExpressionException(this.context, expression, tok,
                                "Varargs must be the last argument"
                            );
                            varArgs = tok.surface;
                        } else args.add(tok.surface);
                    }

                    int[] indices = newIndices();
                    Set<LocalBinding> bindings = new HashSet<>();
                    Map<String, LocalBinding> scope = new HashMap<>();
                    for (String arg : args) {
                        LocalBinding binding = new LocalBinding(Var.at(nextAssignment(indices)), false);
                        bindings.add(binding);
                        if (!"_".equals(arg)) scope.put(arg, binding);
                    }
                    if (varArgs != null) {
                        LocalBinding binding = new LocalBinding(Var.at(nextAssignment(indices)), false);
                        bindings.add(binding);
                        if (!"_".equals(varArgs)) scope.put(varArgs, binding);
                    }

                    BasicBlock start = new BasicBlock(new Int2ObjectOpenHashMap<>(), new ArrayList<>());
                    List<Var> capture = new ArrayList<>();
                    AstToCfgContext funContext = new AstToCfgContext(
                        bindings,
                        scope,
                        new HashMap<>(),
                        outer -> {
                            if (context.scope.get(outer) instanceof LocalBinding local) {
                                capture.add(local.slot);
                                return true;
                            }

                            if (context.captured.containsKey(outer)) {
                                if (context.captured.get(outer) instanceof LocalBinding local) {
                                    capture.add(local.slot);
                                    return true;
                                }
                                return false;
                            }

                            if (!context.outer.test(outer)) {
                                context.captured.put(outer, null);
                                return false;
                            }

                            Var v = Var.at(nextAssignment(context.indices));
                            context.captured.put(outer, new LocalBinding(v, false));
                            capture.add(v);
                            return true;
                        },
                        context.globals,
                        context.linkMutable,
                        new ArrayList<>(List.of(start)),
                        null,
                        null,
                        new Pos[] { Pos.at(0) },
                        indices
                    );

                    Var result = astToCfg(GenContext.USE, funContext, node.opRhs());
                    throw new UnsupportedOperationException("TODO: Has to wait for flattening...");
                }
                case "." -> {
                    List<Node> children = node.children();
                    Var on = astToCfg(GenContext.USE, context, children.getFirst());
                    member: {
                        for (int j = 2, max = children.size(); j < max; j += 2) {
                            if (TokenInterface.type(children.get(j).token()) != TokenTypeInterface.VARIABLE) break member;
                            context.current().code.add(new Instruction.MemberRead(
                                on,
                                new StringValue(children.get(j).token().surface),
                                on = Var.at(nextAssignment(context.indices))
                            ));
                        }
                        yield on;
                    }

                    Node last = children.getLast();
                    List<Node> lastChildren = last.children();
                    Var to = Var.next(ctx, context.indices);
                    context.current().code.add(new Instruction.InvokeRead(
                        children.get(children.size() - 2).token(),
                        new InvokeTarget.Method(on, new StringValue(lastChildren.getFirst().token().surface)),
                        astToArgsSimple(context, lastChildren.subList(1, lastChildren.size())),
                        Optional.ofNullable(to)
                    ));
                    yield to;
                }
                case ";" -> {
                    for (int i = 0, max = node.children().size() - 1; i < max; i += 2) {
                        Node stmt = node.children().get(i);
                        if (TokenInterface.type(stmt.token()) == TokenTypeInterface.OPERATOR
                         && (":=".equals(stmt.token().surface) || "::".equals(stmt.token().surface))) {
                            BinderNode[] binders = new BinderNode[stmt.children().size() >> 1];
                            for (int j = 0; j < binders.length; j++) {
                                Token tok = stmt.children().get(j << 1 | 1).token();
                                binders[j] = new BinderNode(stmt.children().get(j << 1), ":=".equals(tok.surface), tok);
                            }
                            astToCfgBindAssert(context, binders, astToCfg(GenContext.USE, context, stmt.children().getLast()));
                        } else astToCfg(GenContext.DISCARD, context, node.children().get(i));
                    }
                    yield astToCfg(ctx, context, node.children().getLast());
                }
                default -> throw new IllegalArgumentException("Unknown binary operator: " + token.surface);
            };
            case TokenTypeName.OPEN_PAREN -> {
                Var to = Var.next(ctx, context.indices);
                Node fun = node.children().getFirst();
                List<Node> arguments = node.children().subList(1, node.children().size());

                if ("[".equals(token.surface)) {
                    context.current().code.add(new Instruction.InvokeRead(token,
                        new InvokeTarget.Index(astToCfg(GenContext.USE, context, fun)),
                        astToArgsSimple(context, arguments),
                        Optional.ofNullable(to)
                    ));
                    yield to;
                }

                if (!"(".equals(token.surface)) throw new AssertionError();
                switch (TokenInterface.type(fun.token()).name()) {
                case TokenTypeName.VARIABLE -> { switch (fun.token().surface) {
                    // TODO
                } }
                case TokenTypeName.OPEN_PAREN -> { switch (fun.token().surface) {
                    case "[" -> {
                        context.current().code.add(new Instruction.InvokeRead(token,
                            new InvokeTarget.Builtin((c, args) -> c == null ? null : ListValue.of(args)),
                            astToArgsSimple(context, arguments),
                            Optional.ofNullable(to)
                        ));
                        yield to;
                    }
                    case "{" -> {
                        class KVPairValue extends FrameworkValue {
                            public final Value key, value;

                            public KVPairValue(Value key, Value value) {
                                this.key = key;
                                this.value = value;
                            }
                        }

                        context.current().code.add(new Instruction.InvokeRead(token,
                            new InvokeTarget.Builtin((c, args) -> {
                                if (c == null) return null;

                                Map<Value, Value> map = ValueSemantics.newMap();
                                MapValue ret = MapValue.wrap(map);
                                for (Value v : args) switch (v) {
                                    case KVPairValue kv -> map.put(kv.key, kv.value);
                                    case WithMetaValue meta ->
                                        ((MapValueInterface)ret).lanitium$setMeta(meta.context, meta.meta);
                                    case ListValue list when list.length() == 2 ->
                                        map.put(list.getItems().getFirst(), list.getItems().getLast());
                                    default -> throw new InternalExpressionException(
                                        "Only pairs can be put in a map: " + v.getString()
                                    );
                                }

                                return ret;
                            }),
                            astToArgs(context, List.of(), arguments, List.of(),
                            entry -> {
                                Token tok = entry.token();
                                if (TokenInterface.type(tok) == TokenTypeInterface.OPERATOR && "=".equals(tok.surface)) {
                                    Node key = entry.children().getFirst();
                                    if (TokenInterface.type(key.token()) == TokenTypeInterface.UNARY_OPERATOR
                                     && ".".equals(key.token().surface)
                                     && key.children().getFirst() instanceof Node keyName) {
                                        if (TokenInterface.type(keyName.token()) == TokenTypeInterface.VARIABLE) {
                                            Var keyVar = Var.at(nextAssignment(context.indices));
                                            context.current().code.add(new Instruction.Constant(
                                                new StringValue(keyName.token().surface),
                                                keyVar
                                            ));

                                            Var out = Var.at(nextAssignment(context.indices));
                                            context.current().code.add(new Instruction.InvokeRead(tok,
                                                new InvokeTarget.Builtin((_, args) -> new KVPairValue(args[0], args[1])),
                                                new InvokeArgs.Fixed(new Var[] {
                                                    keyVar,
                                                    astToCfg(GenContext.USE, context, entry.opRhs())
                                                }),
                                                Optional.of(out)
                                            ));
                                            return out;
                                        }

                                        if (TokenInterface.type(keyName.token()) == TokenTypeInterface.OPEN_PAREN
                                         && keyName.children().getFirst() instanceof Node method
                                         && TokenInterface.type(method.token()) == TokenTypeInterface.VARIABLE) {
                                            if (!"__meta".equals(method.token().surface))
                                                throw new ExpressionException(this.context, expression, method.token(),
                                                    "Only '__meta' is allowed here"
                                                );

                                            if (keyName.children().size() > 1)
                                                throw new ExpressionException(this.context, expression, keyName.token(),
                                                    "Unexpected arguments for .__meta()"
                                                );

                                            Var out = Var.at(nextAssignment(context.indices));
                                            context.current().code.add(new Instruction.InvokeRead(tok,
                                                new InvokeTarget.Builtin((c, args) -> {
                                                    if (c == null) return null;

                                                    if (args[0].isNull()) return WithMetaValue.RESET;
                                                    if (!(args[0] instanceof MapValue map))
                                                        throw new InternalExpressionException(
                                                            ".__meta() expected a map or null: " + args[0].getString()
                                                        );
                                                    return new WithMetaValue(c, map.getMap());
                                                }),
                                                new InvokeArgs.Single(astToCfg(GenContext.USE, context, entry.opRhs())),
                                                Optional.of(out)
                                            ));
                                            return out;
                                        }
                                    } else if (TokenInterface.type(key.token()) == TokenTypeInterface.OPEN_PAREN
                                            && "[".equals(key.token().surface)) {
                                        if (key.children().size() != 1)
                                            throw new ExpressionException(this.context, expression, key.token(),
                                                "An entry index must have one argument"
                                            );

                                        Var out = Var.at(nextAssignment(context.indices));
                                        context.current().code.add(new Instruction.InvokeRead(tok,
                                            new InvokeTarget.Builtin((_, args) -> new KVPairValue(args[0], args[1])),
                                            new InvokeArgs.Fixed(new Var[] {
                                                astToCfg(GenContext.USE, context, key.children().getFirst()),
                                                astToCfg(GenContext.USE, context, entry.opRhs())
                                            }),
                                            Optional.of(out)
                                        ));
                                        return out;
                                    }
                                }

                                throw new ExpressionException(this.context, expression, tok,
                                    "An entry must be '.name = value', '[key] = value', '...value', or '.__meta() = value'"
                                );
                            },
                            astToArgsConverter(context)),
                            Optional.ofNullable(to)
                        ));
                        yield to;
                    }
                } }
                }

                context.current().code.add(new Instruction.InvokeRead(token,
                    new InvokeTarget.Func(astToCfg(GenContext.USE, context, fun)),
                    astToArgsSimple(context, arguments),
                    Optional.ofNullable(to)
                ));
                yield to;
            }
            case String type -> throw new IllegalArgumentException("Unexpected token type: " + type);
        };
    }
}
