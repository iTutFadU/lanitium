package me.itut.lanitium.function;

import carpet.script.Context;
import carpet.script.annotation.ScarpetFunction;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.chars.CharList;
import me.itut.lanitium.value.parsing.*;
import net.minecraft.util.parsing.packrat.*;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.util.parsing.packrat.commands.StringReaderParserState;
import net.minecraft.util.parsing.packrat.commands.StringReaderTerms;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class Parsing {
    @ScarpetFunction
    public static Value string_reader(Context c, Value reader) {
        return StringReaderValue.of(StringReaderValue.from(reader));
    }

    @ScarpetFunction
    public static Value parsing_atom(Value atom) {
        return AtomValue.of(AtomValue.from(atom));
    }

    @ScarpetFunction(maxParams = 1)
    public static Value parsing_control(Optional<Value> control) {
        return ControlValue.of(control
            .map(ControlValue::from)
            .orElseGet(() -> new Control() {
                boolean cut = false;

                @Override
                public void cut() {
                    cut = true;
                }

                @Override
                public boolean hasCut() {
                    return cut;
                }
            })
        );
    }

    @ScarpetFunction
    public static Value parsing_control_unbound() {
        return ControlValue.UNBOUND;
    }

    @ScarpetFunction(maxParams = 1)
    public static Value parsing_dictionary(Context c, Optional<Value> dictionary) {
        return DictionaryValue.of(c, dictionary
            .map(v -> DictionaryValue.from(c, v))
            .orElseGet(Dictionary::new)
        );
    }

    @ScarpetFunction(maxParams = 1)
    public static Value parsing_error_collector(Context c, Optional<Value> collector) {
        return ErrorCollectorValue.of(c, collector
            .map(ErrorCollectorValue::from)
            .orElseGet(ErrorCollector.LongestOnly::new)
        );
    }

    @ScarpetFunction(maxParams = 2)
    public static Value parsing_grammar(Context c, Value rules, Optional<Value> top) {
        return GrammarValue.of(c, top
            .map(v -> new Grammar<>(DictionaryValue.from(c, rules), NamedRuleValue.from(c, v)))
            .orElseGet(() -> GrammarValue.from(rules))
        );
    }

    @ScarpetFunction(maxParams = 2)
    public static Value parsing_named_rule(Context c, Value name, Optional<Value> value) {
        return NamedRuleValue.of(value
            // NamedRule<StringReader, Value> cannot be converted to NamedRule<StringReader, Value>
            .map((Function<Value, NamedRule<StringReader, Value>>)v -> new NamedRule<>() {
                final Atom<Value> atom = AtomValue.from(name);
                final Rule<StringReader, Value> rule = RuleValue.from(c, v);

                @Override
                public Atom<Value> name() {
                    return atom;
                }

                @Override
                public Rule<StringReader, Value> value() {
                    return rule;
                }
            })
            .orElseGet(() -> NamedRuleValue.from(c, name))
        );
    }

    @ScarpetFunction
    public static Value parsing_rule_action(Context c, Value action) {
        return RuleActionValue.of(RuleActionValue.from(c, action));
    }

    @ScarpetFunction(maxParams = 2)
    public static Value parsing_rule(Context c, Value term, Optional<Value> action) {
        return RuleValue.of(action
            .map(v -> Rule.fromTerm(TermValue.from(c, term), RuleActionValue.from(c, v)))
            .orElseGet(() -> RuleValue.from(c, term))
        );
    }

    @ScarpetFunction
    public static Value parsing_scope(Value scope) {
        return ScopeValue.of(ScopeValue.from(scope));
    }

    @ScarpetFunction(maxParams = 2)
    public static Value parsing_state(Context c, Value collector, Optional<Value> reader) {
        return StateValue.of(c, reader
            .map((Function<Value, ParseState<StringReader>>)v -> new StringReaderParserState(ErrorCollectorValue.from(collector), StringReaderValue.from(v)))
            .orElseGet(() -> StateValue.from(collector))
        );
    }

    @ScarpetFunction
    public static Value parsing_suggestion_supplier(Context c, Value supplier) {
        return SuggestionSupplierValue.of(SuggestionSupplierValue.from(c, supplier));
    }

    @ScarpetFunction
    public static Value parsing_term(Context c, Value term) {
        return TermValue.of(TermValue.from(c, term));
    }

    @ScarpetFunction
    public static Value term_marker(Value atom, Value marker) {
        return TermValue.of(Term.marker(AtomValue.from(atom), marker));
    }

    @SuppressWarnings("unchecked")
    @ScarpetFunction(maxParams = -1)
    public static Value term_sequence(Context c, Value... terms) {
        return TermValue.of(Term.sequence(Arrays.stream(terms).map(v -> TermValue.from(c, v)).toArray(Term[]::new)));
    }

    @SuppressWarnings("unchecked")
    @ScarpetFunction(maxParams = -1)
    public static Value term_alternative(Context c, Value... terms) {
        return TermValue.of(Term.alternative(Arrays.stream(terms).map(v -> TermValue.from(c, v)).toArray(Term[]::new)));
    }

    @ScarpetFunction
    public static Value term_optional(Context c, Value term) {
        return TermValue.of(Term.optional(TermValue.from(c, term)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @ScarpetFunction(maxParams = 3)
    public static Value term_repeated(Context c, Value element, Value name, Optional<Integer> minRepetitions) {
        Atom atom = AtomValue.from(name);
        Term<StringReader> repeated = Term.repeated(NamedRuleValue.from(c, element), atom, minRepetitions.orElse(0));
        return TermValue.of((state, scope, control) -> {
            if (!repeated.parse(state, scope, control)) return false;
            scope.put(atom, ListValue.wrap((List<Value>)scope.get(atom)));
            return true;
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @ScarpetFunction(maxParams = 5)
    public static Value term_repeated_with_separator(Context c, Value element, Value name, Value separator, boolean allowTrailing, Optional<Integer> minRepetitions) {
        Atom atom = AtomValue.from(name);
        Term<StringReader> repeated = new Term.RepeatedWithSeparator<>(NamedRuleValue.from(c, element), atom, TermValue.from(c, separator), minRepetitions.orElse(0), allowTrailing);
        return TermValue.of((state, scope, control) -> {
            if (!repeated.parse(state, scope, control)) return false;
            scope.put(atom, ListValue.wrap((List<Value>)scope.get(atom)));
            return true;
        });
    }

    @ScarpetFunction
    public static Value term_look_ahead(Context c, Value term, boolean positive) {
        return TermValue.of(new Term.LookAhead<>(TermValue.from(c, term), positive));
    }

    @ScarpetFunction
    public static Value term_cut() {
        return TermValue.of(Term.cut());
    }

    @ScarpetFunction
    public static Value term_empty() {
        return TermValue.of(Term.empty());
    }

    @ScarpetFunction
    public static Value term_fail(Value message) {
        return TermValue.of(Term.fail(DelayedException.create(new SimpleCommandExceptionType(FormattedTextValue.getTextByValue(message)))));
    }

    @ScarpetFunction
    public static Value term_word(String word) {
        return TermValue.of(StringReaderTerms.word(word));
    }

    @ScarpetFunction
    public static Value term_char(String str) {
        if (str.isEmpty())
            throw new InternalExpressionException("Empty string cannot be used as a character");
        return TermValue.of(StringReaderTerms.character(str.charAt(0)));
    }

    @ScarpetFunction(maxParams = -1)
    public static Value term_chars(String... strs) {
        char[] chars = new char[strs.length];
        for (int i = 0; i < strs.length; i++) {
            if (strs[i].isEmpty())
                throw new InternalExpressionException("Empty string cannot be used as a character");
            chars[i] = strs[i].charAt(0);
        }
        CharList list = CharList.of(chars);
        return TermValue.of(new StringReaderTerms.TerminalCharacters(list) {
            @Override
            protected boolean isAccepted(char c) {
                return list.contains(c);
            }
        });
    }

    @ScarpetFunction
    public static Value term_char_selection(Context c, FunctionValue fn) {
        return TermValue.of(new StringReaderTerms.TerminalCharacters(CharList.of()) {
            @Override
            protected boolean isAccepted(char ch) {
                return fn.callInContext(c, Context.BOOLEAN, List.of(StringValue.of(String.valueOf(ch)))).evalValue(c, Context.BOOLEAN).getBoolean();
            }
        });
    }
}
