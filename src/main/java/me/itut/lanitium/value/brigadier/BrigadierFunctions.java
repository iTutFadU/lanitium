package me.itut.lanitium.value.brigadier;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.annotation.Param;
import carpet.script.annotation.ScarpetFunction;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.ListValue;
import carpet.script.value.NumericValue;
import carpet.script.value.Value;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import me.itut.lanitium.value.ContextValue;
import me.itut.lanitium.value.StringReaderValue;
import me.itut.lanitium.value.Util;
import me.itut.lanitium.value.brigadier.argument.ArgumentTypeValue;
import me.itut.lanitium.value.brigadier.argument.EntitySelectorValue;
import me.itut.lanitium.value.brigadier.builder.LiteralArgumentBuilderValue;
import me.itut.lanitium.value.brigadier.builder.RequiredArgumentBuilderValue;
import me.itut.lanitium.value.brigadier.context.*;
import me.itut.lanitium.value.brigadier.function.*;
import me.itut.lanitium.value.brigadier.suggestion.SuggestionValue;
import me.itut.lanitium.value.brigadier.suggestion.SuggestionsBuilderValue;
import me.itut.lanitium.value.brigadier.suggestion.SuggestionsFuture;
import me.itut.lanitium.value.brigadier.suggestion.SuggestionsValue;
import me.itut.lanitium.value.brigadier.tree.ArgumentCommandNodeValue;
import me.itut.lanitium.value.brigadier.tree.CommandNodeValue;
import me.itut.lanitium.value.brigadier.tree.LiteralCommandNodeValue;
import me.itut.lanitium.value.brigadier.tree.RootCommandNodeValue;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static me.itut.lanitium.internal.carpet.EntityValueSelectorCache.selectorCache;

public class BrigadierFunctions {
    @ScarpetFunction
    public void perform_command(Context c, Value parseResults, String command) {
        ((CarpetContext)c).server().getCommands().performCommand(ParseResultsValue.from(parseResults), command);
    }

    @ScarpetFunction
    public Value literal(Context c, String name) {
        return LiteralArgumentBuilderValue.of((CarpetContext)c, Commands.literal(name));
    }

    @ScarpetFunction
    public Value argument(Context c, String name, Value type) {
        return RequiredArgumentBuilderValue.of((CarpetContext)c, Commands.argument(name, ArgumentTypeValue.from(type)));
    }

    @ScarpetFunction
    public Value commands_dispatcher(Context c) {
        return CommandDispatcherValue.of((CarpetContext)c, ((CarpetContext)c).server().getCommands().getDispatcher());
    }

    @ScarpetFunction
    public Value commands_parse_exception(Context c, Value parseResults) {
        CommandSyntaxException error = Commands.getParseException(ParseResultsValue.from(parseResults));
        return error != null ? new CommandSyntaxError((CarpetContext)c, error) : Value.NULL;
    }

    @ScarpetFunction(maxParams = 1)
    public Value command_dispatcher(Context c, Optional<Value> root) {
        return CommandDispatcherValue.of((CarpetContext)c, root.map(value -> new CommandDispatcher<>(RootCommandNodeValue.from(value))).orElseGet(CommandDispatcher::new));
    }

    @ScarpetFunction(maxParams = 2)
    public Value command_syntax_error(Context c, String message, Optional<Value> reader) {
        return new CommandSyntaxError((CarpetContext)c, reader.map(v -> new SimpleCommandExceptionType(() -> message).createWithContext(StringReaderValue.from(v))).orElseGet(() -> new SimpleCommandExceptionType(() -> message).create()));
    }

    @ScarpetFunction(maxParams = -1)
    public Value parse_results(Context c, Value context, Optional<Value> reader, @Param.KeyValuePairs Optional<Map<Value, Value>> exceptions) {
        if (reader.isPresent()) {
            Map<CommandNode<CommandSourceStack>, CommandSyntaxException> map = new HashMap<>();
            exceptions.ifPresent(m -> m.forEach((k, v) -> map.put(CommandNodeValue.from(k), ((CommandSyntaxError)v).value)));
            return ParseResultsValue.of((CarpetContext)c, new ParseResults<>(CommandContextBuilderValue.from(context), StringReaderValue.from(reader.get()), map));
        }
        return ParseResultsValue.of((CarpetContext)c, new ParseResults<>(CommandContextBuilderValue.from(context)));
    }

    @ScarpetFunction
    public Value command_context_builder(Context c, Value dispatcher, ContextValue source, Value root, int start) {
        return CommandContextBuilderValue.of((CarpetContext)c, new CommandContextBuilder<>(CommandDispatcherValue.from(dispatcher), source.value.source(), RootCommandNodeValue.from(root), start));
    }

    @ScarpetFunction(maxParams = 10)
    public Value command_context(Context c, ContextValue source, String input, @Param.KeyValuePairs(allowMultiparam = false) Map<String, Value> arguments, Value command, Value root, @Param.AllowSingleton List<Value> nodes, Value range, Value child, Value modifier, boolean forks) {
        Map<String, ParsedArgument<CommandSourceStack, ?>> map = new HashMap<>();
        arguments.forEach((k, v) -> map.put(k, ParsedArgumentValue.from(v)));
        return CommandContextValue.of((CarpetContext)c, new CommandContext<>(source.value.source(), input, map, CommandValue.from((CarpetContext)c, command), CommandNodeValue.from(root), nodes.stream().map(ParsedCommandNodeValue::from).toList(), Util.toRange(range), CommandContextValue.from(child), RedirectModifierValue.from((CarpetContext)c, modifier), forks));
    }

    @ScarpetFunction(maxParams = -1)
    public Value context_chain(Context c, Value... chain) {
        return switch (chain.length) {
            case 0 -> Value.NULL;
            case 1 -> ContextChainValue.of((CarpetContext)c, new ContextChain<>(List.of(), CommandContextValue.from(chain[0])));
            default -> ContextChainValue.of((CarpetContext)c, new ContextChain<>(Arrays.stream(chain, 0, chain.length - 1).map(CommandContextValue::from).toList(), CommandContextValue.from(chain[chain.length - 1])));
        };
    }

    @ScarpetFunction
    public Value context_chain_try_flatten(Context c, Value context) {
        return ContextChainValue.of((CarpetContext)c, ContextChain.tryFlatten(CommandContextValue.from(context)).orElse(null));
    }

    @ScarpetFunction
    public Value context_chain_run_modifier(Context c, Value modifier, ContextValue source, Value consumer, boolean forked) {
        try {
            return ListValue.wrap(ContextChain.runModifier(CommandContextValue.from(modifier), source.value.source(), ResultConsumerValue.from((CarpetContext)c, consumer), forked).stream().map(v -> Util.source((CarpetContext)c, v)));
        } catch (CommandSyntaxException e) {
            throw CommandSyntaxError.create((CarpetContext)c, e);
        }
    }

    @ScarpetFunction
    public Value context_chain_run_executable(Context c, Value modifier, ContextValue source, Value consumer, boolean forked) {
        try {
            return NumericValue.of(ContextChain.runExecutable(CommandContextValue.from(modifier), source.value.source(), ResultConsumerValue.from((CarpetContext)c, consumer), forked));
        } catch (CommandSyntaxException e) {
            throw CommandSyntaxError.create((CarpetContext)c, e);
        }
    }

    @ScarpetFunction
    public Value custom_parsed_argument(Context c, int start, int end, Value value) {
        return ParsedArgumentValue.of((CarpetContext)c, new ParsedArgument<>(start, end, value));
    }

    @ScarpetFunction
    public Value parsed_command_node(Context c, Value node, Value range) {
        return ParsedCommandNodeValue.of((CarpetContext)c, new ParsedCommandNode<>(CommandNodeValue.from(node), Util.toRange(range)));
    }

    @ScarpetFunction
    public Value suggestion_context(Context c, Value parent, int start) {
        return SuggestionContextValue.of((CarpetContext)c, new SuggestionContext<>(CommandNodeValue.from(parent), start));
    }

    @ScarpetFunction
    public Value suggestions_builder(Context c, String input, int start) {
        return SuggestionsBuilderValue.of((CarpetContext)c, new SuggestionsBuilder(input, start));
    }

    @ScarpetFunction(maxParams = 1)
    public Value suggestions_future(Context c, Optional<Value> value) {
        if (value.isEmpty()) return SuggestionsFuture.empty((CarpetContext)c);
        if (value.get() instanceof FunctionValue fn) return SuggestionsFuture.of((CarpetContext)c, CompletableFuture.supplyAsync(() -> SuggestionsValue.from(fn.callInContext(c, Context.NONE, List.of()).evalValue(c))));
        return SuggestionsFuture.of((CarpetContext)c, CompletableFuture.completedFuture(SuggestionsValue.from(value.get())));
    }

    @ScarpetFunction(maxParams = -1)
    public Value suggestions(Context c, Value range, Value... suggestions) {
        return SuggestionsValue.of((CarpetContext)c, new Suggestions(Util.toRange(range), Arrays.stream(suggestions).map(SuggestionValue::from).toList()));
    }

    @ScarpetFunction(maxParams = -1)
    public Value suggestions_merge(Context c, String command, Value... suggestions) {
        return SuggestionsValue.of((CarpetContext)c, Suggestions.merge(command, Arrays.stream(suggestions).map(SuggestionsValue::from).toList()));
    }

    @ScarpetFunction(maxParams = -1)
    public Value suggestions_create(Context c, String command, Value... suggestions) {
        return SuggestionsValue.of((CarpetContext)c, Suggestions.create(command, Arrays.stream(suggestions).map(SuggestionValue::from).toList()));
    }

    @ScarpetFunction(maxParams = 3)
    public Value suggestion(Context c, Value range, String text, Optional<String> tooltip) {
        return SuggestionValue.of((CarpetContext)c, new Suggestion(Util.toRange(range), text, tooltip.map(v -> (Message)() -> v).orElse(null)));
    }

    @ScarpetFunction
    public Value argument_command_node(Context c, String name, Value type, Value command, Value requirement, Value redirect, Value modifier, boolean forks, Value suggestions) {
        return ArgumentCommandNodeValue.of((CarpetContext)c, new ArgumentCommandNode<>(name, ArgumentTypeValue.from(type), CommandValue.from((CarpetContext)c, command), RequirementValue.from((CarpetContext)c, requirement), CommandNodeValue.from(redirect), RedirectModifierValue.from((CarpetContext)c, modifier), forks, SuggestionsProviderValue.from((CarpetContext)c, suggestions)));
    }

    @ScarpetFunction
    public Value literal_command_node(Context c, String literal, Value command, Value requirement, Value redirect, Value modifier, boolean forks) {
        return LiteralCommandNodeValue.of((CarpetContext)c, new LiteralCommandNode<>(literal, CommandValue.from((CarpetContext)c, command), RequirementValue.from((CarpetContext)c, requirement), CommandNodeValue.from(redirect), RedirectModifierValue.from((CarpetContext)c, modifier), forks));
    }

    @ScarpetFunction
    public Value root_command_node(Context c) {
        return RootCommandNodeValue.of((CarpetContext)c, new RootCommandNode<>());
    }

    @ScarpetFunction
    public Value create_entity_selector(Context c, String selector) {
        if (selectorCache.get(selector) instanceof EntitySelector s) {
            return EntitySelectorValue.of((CarpetContext)c, s);
        }
        try {
            EntitySelector s = new EntitySelectorParser(new StringReader(selector), true).parse();
            selectorCache.put(selector, s);
            return EntitySelectorValue.of((CarpetContext)c, s);
        } catch (CommandSyntaxException e) {
            throw new InternalExpressionException("Cannot select entities from " + selector);
        }
    }
}
