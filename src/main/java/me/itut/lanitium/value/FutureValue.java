package me.itut.lanitium.value;

import carpet.script.Context;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ProcessedThrowStatement;
import carpet.script.value.*;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class FutureValue extends ObjectValue<CompletableFuture<Value>> {
    private final Context context;

    protected FutureValue(Context context, CompletableFuture<Value> value) {
        super(value);
        this.context = context;
    }

    public static Value of(Context context, CompletableFuture<Value> value) {
        return value != null ? new FutureValue(context, value) : null;
    }

    public static CompletableFuture<Value> from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case FutureValue v -> v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to future");
        };
    }

    public Value get(String what, Value... more) {
        return switch (what) {
            case "done" -> {
                checkArguments(what, more, 0);
                yield BooleanValue.of(value.isDone());
            }
            case "join" -> {
                checkArguments(what, more, 0);
                yield value.join();
            }
            case "get_now" -> {
                checkArguments(what, more, 0);
                yield value.getNow(more[0]);
            }
            case "result_now" -> {
                checkArguments(what, more, 0);
                yield value.resultNow();
            }
            case "error_now" -> {
                checkArguments(what, more, 0);
                yield switch (value.exceptionNow()) {
                    case ProcessedThrowStatement threw -> threw.data;
                    case Throwable t -> StringValue.of(t.getMessage());
                };
            }
            case "complete" -> {
                checkArguments(what, more, 1);
                yield BooleanValue.of(value.complete(more[0]));
            }
            case "complete_throw" -> {
                checkArguments(what, more, 1);
                yield BooleanValue.of(value.completeExceptionally(new RuntimeException(more[0].getString())));
            }
            case "when_complete" -> {
                checkArguments(what, more, 1);
                if (!(Objects.requireNonNull(more[0]) instanceof FunctionValue callback))
                    throw new InternalExpressionException("future~'" + what + "' expects a functions as an argument");
                value.whenComplete((value, exception) -> callback.callInContext(context, Context.VOID, exception == null ? List.of(value) : List.of()).evalValue(context, Context.VOID));
                yield this;
            }
            case "handle" -> {
                checkArguments(what, more, 1);
                if (!(Objects.requireNonNull(more[0]) instanceof FunctionValue callback))
                    throw new InternalExpressionException("future~'" + what + "' expects a functions as an argument");
                yield of(context, value.handle((value, exception) -> callback.callInContext(context, Context.NONE, exception == null ? List.of(value) : List.of()).evalValue(context)));
            }
            case "cancel" -> {
                checkArguments(what, more, 1);
                yield BooleanValue.of(value.cancel(more[0].getBoolean()));
            }
            case "cancelled" -> {
                checkArguments(what, more, 0);
                yield BooleanValue.of(value.isCancelled());
            }
            case "state" -> {
                checkArguments(what, more, 0);
                yield StringValue.of(value.state().name().toLowerCase());
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "future";
    }

    @Override
    public Value deepcopy() {
        return new FutureValue(context, value.copy());
    }
}
