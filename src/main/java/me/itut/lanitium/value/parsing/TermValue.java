package me.itut.lanitium.value.parsing;

import carpet.script.Context;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.BooleanValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.NullValue;
import carpet.script.value.Value;
import com.mojang.brigadier.StringReader;
import me.itut.lanitium.value.ObjectFunctionValue;
import me.itut.lanitium.value.SimpleFunctionValue;
import net.minecraft.util.parsing.packrat.Control;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Scope;
import net.minecraft.util.parsing.packrat.Term;

import java.util.List;

public class TermValue extends ObjectFunctionValue<Term<StringReader>> {
    protected TermValue(Term<StringReader> value) {
        super(value, value instanceof Fn fn ? fn.fn : new SimpleFunctionValue(3, 3, (c, t, e, tok, lv) -> BooleanValue.of(value.parse(StateValue.from(lv.getFirst()), ScopeValue.from(lv.get(1)), ControlValue.from(lv.getLast())))));
    }

    protected TermValue(TermValue self) {
        super(self.value, self);
    }

    public static Value of(Term<StringReader> value) {
        return value != null ? new TermValue(value) : Value.NULL;
    }

    public static Term<StringReader> from(Context c, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case TermValue term -> term.value;
            case ObjectFunctionValue<?> ignored -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to parsing_convert");
            case FunctionValue fn -> new Fn(c, new SimpleFunctionValue(3, 3, (cc, t, e, tok, lv) -> fn.execute(cc, t, e, tok, lv, null).evalValue(cc, t)));
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to parsing_term");
        };
    }

    @Override
    public String getTypeString() {
        return "parsing_term";
    }

    @Override
    public String getString() {
        return value.toString();
    }

    @Override
    protected Value clone() {
        return new TermValue(this);
    }

    private record Fn(Context c, SimpleFunctionValue fn) implements Term<StringReader> {
        @Override
        public boolean parse(ParseState<StringReader> state, Scope scope, Control control) {
            return fn.callInContext(c, Context.BOOLEAN, List.of(StateValue.of(c, state), ScopeValue.of(scope), ControlValue.of(control))).evalValue(c, Context.BOOLEAN).getBoolean();
        }
    }
}
