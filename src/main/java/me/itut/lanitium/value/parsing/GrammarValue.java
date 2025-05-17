package me.itut.lanitium.value.parsing;

import carpet.script.Context;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.value.NullValue;
import carpet.script.value.Value;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.itut.lanitium.function.Apply;
import me.itut.lanitium.value.ObjectValue;
import me.itut.lanitium.value.ValueConversions;
import net.minecraft.util.parsing.packrat.commands.Grammar;

public class GrammarValue extends ObjectValue<Grammar<Value>> {
    private final Context context;
    
    protected GrammarValue(Context context, Grammar<Value> value) {
        super(value);
        this.context = context;
    }
    
    public static Value of(Context context, Grammar<Value> value) {
        return value != null ? new GrammarValue(context, value) : Value.NULL;
    }

    public static Grammar<Value> from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case GrammarValue grammar -> grammar.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to parsing_grammar");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "parse" -> {
                checkArguments(what, more, 1);
                yield value.parse(StateValue.from(more[0])).orElse(Value.NULL);
            }
            case "parse_for_commands" -> {
                checkArguments(what, more, 1);
                try {
                    yield value.parseForCommands(StringReaderValue.from(more[0]));
                } catch (CommandSyntaxException e) {
                    throw new ThrowStatement(Apply.internalExceptionMap(e), Apply.COMMAND_SYNTAX_EXCEPTION);
                }
            }
            case "parse_for_suggestions" -> {
                checkArguments(what, more, 1);
                StringReader reader = StringReaderValue.from(more[0]);
                yield ValueConversions.suggestions(value.parseForSuggestions(new SuggestionsBuilder(reader.getString(), reader.getCursor())).join());
            }
            case "rules" -> {
                checkArguments(what, more, 0);
                yield DictionaryValue.of(context, value.rules());
            }
            case "top" -> {
                checkArguments(what, more, 0);
                yield NamedRuleValue.of(value.top());
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "parsing_grammar";
    }
}
