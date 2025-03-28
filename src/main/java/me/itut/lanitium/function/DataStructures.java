package me.itut.lanitium.function;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.annotation.ScarpetFunction;
import carpet.script.exception.ProcessedThrowStatement;
import carpet.script.exception.Throwables;
import carpet.script.value.*;
import me.itut.lanitium.value.ByteBufferValue;
import me.itut.lanitium.value.FutureValue;
import me.itut.lanitium.value.StringReaderValue;
import me.itut.lanitium.value.ThreadLocalValue;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

public class DataStructures {
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
    public static Value thread_local(Context c, FunctionValue initial) {
        return new ThreadLocalValue(c, initial);
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
}
