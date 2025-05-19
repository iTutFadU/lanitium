package me.itut.lanitium.internal.carpet;

import carpet.script.CarpetScriptHost;
import carpet.script.argument.FunctionArgument;
import carpet.script.command.CommandArgument;
import carpet.script.external.Carpet;
import carpet.script.external.Vanilla;
import carpet.script.value.*;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.itut.lanitium.value.SourceValue;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.parsing.packrat.*;
import net.minecraft.util.parsing.packrat.Dictionary;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.util.parsing.packrat.commands.GreedyPredicateParseRule;
import net.minecraft.util.parsing.packrat.commands.StringReaderTerms;
import net.minecraft.util.parsing.packrat.commands.UnquotedStringParseRule;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandParser {
    public static CommandNode<CommandSourceStack> parseCommand(CarpetScriptHost host, Map<String, Value> branches, Map<String, Value> requirements) throws CommandSyntaxException {
        return Node.construct(host, branches, requirements).createCommand(host);
    }

    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_ARGUMENT = DelayedException.create(new SimpleCommandExceptionType(Component.literal("Expected an argument")));
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_ARGUMENT_SEPARATOR = DelayedException.create(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherExpectedArgumentSeparator());

    public static final Grammar<List<Argument>> BRANCH_GRAMMAR;
    public static final Grammar<List<String>> PATH_GRAMMAR;

    static {
        Dictionary<StringReader> dictionary = new Dictionary<>();

        Atom<String> string = Atom.of("string");
        dictionary.put(string, state -> {
            state.input().skipWhitespace();

            String str;
            try {
                str = state.input().readString();
            } catch (CommandSyntaxException e) {
                state.errorCollector().store(state.mark(), e);
                return null;
            }

            if (str.isEmpty()) {
                state.errorCollector().store(state.mark(), CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedStartOfQuote().createWithContext(state.input()));
                return null;
            }

            return str;
        });

        Atom<Argument> literalArgument = Atom.of("literal_argument");
        dictionary.put(literalArgument, dictionary.named(string), scope -> new LiteralArgument(scope.getOrThrow(string)));

        Atom<String> argumentName = Atom.of("argument_name");
        Atom<String> argumentType = Atom.of("argument_type");
        Atom<Argument> requiredArgument = Atom.of("required_argument");
        dictionary.put(requiredArgument, Term.sequence(
            StringReaderTerms.character('<'),
            dictionary.namedWithAlias(string, argumentName),
            Term.optional(Term.sequence(
                StringReaderTerms.character(':'),
                dictionary.namedWithAlias(string, argumentType)
            )),
            StringReaderTerms.character('>')
        ), scope -> {
            String name = scope.getOrThrow(argumentName);
            return new RequiredArgument(name, scope.getOrDefault(argumentType, name));
        });

        Atom<Argument> argument = Atom.of("argument");
        Atom<List<Argument>> arguments = Atom.of("arguments");
        NamedRule<StringReader, List<Argument>> argumentsRule = dictionary.put(arguments, Term.repeatedWithoutTrailingSeparator(dictionary.forward(argument), arguments, (state, scope, control) -> {
            StringReader reader = state.input();
            if (!reader.canRead() || !Character.isWhitespace(reader.peek())) {
                state.errorCollector().store(reader.getCursor(), ERROR_EXPECTED_ARGUMENT_SEPARATOR);
                return false;
            }
            reader.skip();
            return true;
        }), scope -> scope.getOrThrow(arguments));

        Atom<List<String>> path = Atom.of("path");
        NamedRule<StringReader, List<String>> pathRule = dictionary.put(path, dictionary.named(arguments), scope -> argumentListToPath(scope.getOrThrow(arguments)));

        Atom<Boolean> fork = Atom.of("fork");
        Atom<Argument> forwardArgument = Atom.of("forward_argument");
        dictionary.put(forwardArgument, Term.sequence(
            (state, scope, control) -> {
                StringReader reader = state.input();
                reader.skipWhitespace();
                if (!reader.canRead(2) || reader.peek() != '-' || reader.peek(1) != '<' && reader.peek(1) != '>') {
                    state.errorCollector().store(state.mark(), CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().create("->"));
                    return false;
                }
                scope.put(fork, reader.peek(1) == '<');
                state.restore(state.mark() + 2);
                return true;
            },
            Term.cut(),
            dictionary.named(path)
        ), scope -> new ForwardArgument(scope.getOrThrow(fork), scope.getOrThrow(path)));

        dictionary.put(argument, Term.alternative(
            dictionary.namedWithAlias(requiredArgument, argument),
            dictionary.namedWithAlias(forwardArgument, argument),
            dictionary.namedWithAlias(literalArgument, argument)
        ), scope -> scope.getOrThrow(argument));

        BRANCH_GRAMMAR = new Grammar<>(dictionary, argumentsRule);
        PATH_GRAMMAR = new Grammar<>(dictionary, pathRule);
    }

    public static List<String> argumentListToPath(List<Argument> arguments) {
        List<String> path = new ArrayList<>();
        arguments.forEach(arg -> {
            switch (arg) {
                case LiteralArgument literal -> path.add(literal.surface);
                case RequiredArgument required -> path.add(required.surface);
                case ForwardArgument forward -> path.addAll(forward.path);
            }
        });
        return path;
    }

    public static List<Argument> parseBranch(String input) throws CommandSyntaxException {
        StringReader reader = new StringReader(input);
        List<Argument> arguments = BRANCH_GRAMMAR.parseForCommands(reader);
        reader.skipWhitespace();
        if (reader.canRead()) throw ERROR_EXPECTED_ARGUMENT.create(input, reader.getCursor());
        return arguments;
    }

    public static List<String> parsePath(String input) throws CommandSyntaxException {
        StringReader reader = new StringReader(input);
        List<String> path = PATH_GRAMMAR.parseForCommands(reader);
        reader.skipWhitespace();
        if (reader.canRead()) throw ERROR_EXPECTED_ARGUMENT.create(input, reader.getCursor());
        return path;
    }

    public static Predicate<CommandSourceStack> parseRequirement(CarpetScriptHost host, Value value) throws CommandSyntaxException {
        return switch (value) {
            case null -> s -> true;
            case NullValue ignored -> s -> true;
            case NumericValue number -> {
                int level = number.getInt();
                yield s -> s.hasPermission(level);
            }
            case FunctionValue fun -> { // Copied
                if (fun.getNumParams() != 1) {
                    throw CommandArgument.error("Custom command permission function should expect 1 argument");
                }
                String hostName = host.getName();
                yield s -> {
                    try {
                        Runnable token = Carpet.startProfilerSection("Scarpet command");
                        try {
                            return host.scriptServer().modules.get(hostName).retrieveOwnForExecution(s).handleCommand(s, fun, List.of(s.getEntity() instanceof ServerPlayer player ? new EntityValue(player) : Value.NULL)).getBoolean();
                        } finally {
                            token.run();
                        }
                    } catch (CommandSyntaxException e) {
                        Carpet.Messenger_message(s, "rb Unable to run app command: " + e.getMessage());
                        return false;
                    }
                };
            }
            default -> {
                String string = value.getString().toLowerCase(Locale.ROOT);
                yield switch (string) {
                    case "ops" -> s -> s.hasPermission(2);
                    case "server" -> s -> !(s.getEntity() instanceof ServerPlayer);
                    case "players" -> s -> s.getEntity() instanceof ServerPlayer;
                    case "all" -> s -> true;
                    case "console" -> s -> s.getEntity() == null;
                    case "entity" -> s -> s.getEntity() != null;
                    default -> throw CommandArgument.error("Unknown command permission: " + string);
                };
            }
        };
    }

    public static class Node {
        public final String name;
        public List<RequiredArgument> argumentList = List.of();
        public Argument argument;
        public Predicate<CommandSourceStack> requirement;
        public FunctionArgument execute;
        public Node forward;
        public final Map<String, Node> children = new HashMap<>();

        public Node(String name) {
            this.name = name;
        }

        public static Node construct(CarpetScriptHost host, Map<String, Value> commands, Map<String, Value> requirements) throws CommandSyntaxException {
            Node root = new Node(host.getName());
            root.argument = new LiteralArgument(root.name);
            for (Map.Entry<String, Value> entry : commands.entrySet()) {
                List<Argument> branch = parseBranch(entry.getKey());
                Node parent = root, node;
                List<RequiredArgument> arguments = new ArrayList<>();
                for (Argument arg : branch) {
                    if (arg instanceof RequiredArgument required)
                        arguments.add(required);
                    node = new Node(arg.surface);
                    node.argument = arg;
                    node.argumentList = new ArrayList<>(arguments);
                    parent = parent.addChild(node);
                }
                parent.execute = FunctionArgument.fromCommandSpec(host, entry.getValue());
            }
            r: for (Map.Entry<String, Value> entry : requirements.entrySet()) {
                List<String> path = parsePath(entry.getKey());
                Predicate<CommandSourceStack> requirement = parseRequirement(host, entry.getValue());
                Node node = root;
                for (String argument : path) {
                    if (!node.children.containsKey(argument)) continue r;
                    node = node.children.get(argument);
                }
                node.requirement = requirement;
            }
            return root;
        }

        public Node addChild(Node node) {
            if (forward != null) return node;
            if (node.argument instanceof ForwardArgument) {
                forward(node);
                return node;
            }
            if (children.containsKey(node.name)) {
                Node child = children.get(node.name);
                if (node.execute != null) child.execute = node.execute;
                if (child.forward == null) for (Node grandchild : node.children.values())
                    child.addChild(grandchild);
                return child;
            }
            children.put(node.name, node);
            return node;
        }

        public void forward(Node forward) {
            this.forward = forward;
            children.clear();
        }

        public CommandNode<CommandSourceStack> createCommand(CarpetScriptHost host) throws CommandSyntaxException {
            CommandNode<CommandSourceStack> node;
            String commandName = host.getName();
            Command<CommandSourceStack> command = null;
            RedirectModifier<CommandSourceStack> modifier;
            boolean forks;
            Predicate<CommandSourceStack> requirement = this.requirement;
            if (requirement == null) requirement = s -> true;
            if (execute != null)
                command = (ctx) -> {
                    Runnable token = Carpet.startProfilerSection("Scarpet command");
                    try {
                        CarpetScriptHost cHost = Vanilla.MinecraftServer_getScriptServer(ctx.getSource().getServer()).modules.get(commandName).retrieveOwnForExecution(ctx.getSource());
                        List<Value> args = new ArrayList<>(execute.function.getArguments().size());
                        for (RequiredArgument arg : argumentList)
                            args.add(((CommandArgumentInterface)CommandArgument.getTypeForArgument(arg.type, cHost)).lanitium$getValueFromContext(ctx, arg.surface));
                        args.addAll(execute.args);
                        return (int)cHost.handleCommand(ctx.getSource(), execute.function, args).readInteger();
                    } finally {
                        token.run();
                    }
                };
            if (forward != null) {
                FunctionArgument modify = forward.execute;
                modifier = (ctx) -> {
                    Runnable token = Carpet.startProfilerSection("Scarpet command");
                    try {
                        CarpetScriptHost cHost = Vanilla.MinecraftServer_getScriptServer(ctx.getSource().getServer()).modules.get(commandName).retrieveOwnForExecution(ctx.getSource());
                        List<Value> args = new ArrayList<>(modify.function.getArguments().size());
                        for (RequiredArgument arg : argumentList)
                            args.add(((CommandArgumentInterface)CommandArgument.getTypeForArgument(arg.type, cHost)).lanitium$getValueFromContext(ctx, arg.surface));
                        args.addAll(modify.args);
                        return switch (cHost.handleCommand(ctx.getSource(), modify.function, args)) {
                            case SourceValue s -> List.of(s.value);
                            case AbstractListValue list -> list.unpack().stream().flatMap(v -> v instanceof SourceValue s ? Stream.of(s.value) : Stream.empty()).toList();
                            default -> List.of(ctx.getSource());
                        };
                    } finally {
                        token.run();
                    }
                };
                forks = ((ForwardArgument)forward.argument).forks;

                Commands commands = host.scriptServer().server.getCommands();
                if (argument instanceof RequiredArgument required) {
                    RequiredArgumentBuilder<CommandSourceStack, ?> builder = CommandArgument.argumentNode(required.type, host);
                    node = new CustomArgumentCommandNode<>(commands, ((ForwardArgument)forward.argument).path, name, builder.getType(), command, requirement, modifier, forks, builder.getSuggestionsProvider());
                } else
                    node = new CustomLiteralCommandNode(commands, ((ForwardArgument)forward.argument).path, name, command, requirement, modifier, forks);
            } else if (argument instanceof RequiredArgument required) {
                RequiredArgumentBuilder<CommandSourceStack, ?> builder = CommandArgument.argumentNode(required.type, host);
                node = new ArgumentCommandNode<>(name, builder.getType(), command, requirement, null, null, false, builder.getSuggestionsProvider());
            } else
                node = new LiteralCommandNode<>(name, command, requirement, null, null, false);

            for (Node child : children.values()) node.addChild(child.createCommand(host));
            return node;
        }

        private static class CustomArgumentCommandNode<T> extends ArgumentCommandNode<CommandSourceStack, T> {
            private final Commands commands;
            private final List<String> path;

            public CustomArgumentCommandNode(Commands commands, List<String> path, String name, ArgumentType<T> type, Command<CommandSourceStack> command, Predicate<CommandSourceStack> requirement, RedirectModifier<CommandSourceStack> modifier, boolean forks, SuggestionProvider<CommandSourceStack> suggestions) {
                super(name, type, command, requirement, null, modifier, forks, suggestions);
                this.commands = commands;
                this.path = path;
            }

            @Override
            public CommandNode<CommandSourceStack> getRedirect() {
                return commands.getDispatcher().findNode(path);
            }
        }

        private static class CustomLiteralCommandNode extends LiteralCommandNode<CommandSourceStack> {
            private final Commands commands;
            private final List<String> path;

            public CustomLiteralCommandNode(Commands commands, List<String> path, String name, Command<CommandSourceStack> command, Predicate<CommandSourceStack> requirement, RedirectModifier<CommandSourceStack> modifier, boolean forks) {
                super(name, command, requirement, null, modifier, forks);
                this.commands = commands;
                this.path = path;
            }

            @Override
            public CommandNode<CommandSourceStack> getRedirect() {
                return commands.getDispatcher().findNode(path);
            }
        }
    }

    public static abstract sealed class Argument {
        public final String surface;

        public Argument(String surface) {
            this.surface = surface;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Argument argument)) return false;
            return surface.equals(argument.surface);
        }

        @Override
        public int hashCode() {
            return Objects.hash(getClass(), surface);
        }
    }

    public static final class LiteralArgument extends Argument {
        public LiteralArgument(String literal) {
            super(literal);
        }

        @Override
        public String toString() {
            return StringArgumentType.escapeIfRequired(surface);
        }
    }

    public static final class RequiredArgument extends Argument {
        public final String type;

        public RequiredArgument(String surface, String type) {
            super(surface);
            this.type = type;
        }

        @Override
        public String toString() {
            return "<" + StringArgumentType.escapeIfRequired(surface) + (surface.equals(type) ? ">" : ":" + StringArgumentType.escapeIfRequired(type) + ">");
        }
    }

    public static final class ForwardArgument extends Argument {
        public final boolean forks;
        public final List<String> path;

        public ForwardArgument(boolean forks, List<String> path) {
            super("");
            this.forks = forks;
            this.path = path;
        }

        @Override
        public String toString() {
            return (forks ? "-<" : "->") + path.stream().map(v -> " " + StringArgumentType.escapeIfRequired(v)).collect(Collectors.joining());
        }
    }
}
