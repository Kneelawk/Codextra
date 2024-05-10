package com.kneelawk.codextra.api.codec;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

/**
 * Like {@link com.mojang.serialization.codecs.OptionalFieldCodec} but with an extra error-handling consumer.
 * <p>
 * This is intended for situations where an invalid decode should be logged to the user, but can be recovered from.
 *
 * @param <R> the type this codec encodes/decodes.
 */
public class ErrorHandlingMapCodec<R> extends MapCodec<Optional<R>> {
    private final MapCodec<R> wrapped;
    private final Consumer<String> errorLogger;

    /**
     * Creates a new {@link ErrorHandlingMapCodec}.
     *
     * @param wrapped     the map codec that does the encoding/decoding.
     * @param errorLogger the consumer that is called if an error occurs.
     */
    public ErrorHandlingMapCodec(MapCodec<R> wrapped, Consumer<String> errorLogger) {
        this.wrapped = wrapped;
        this.errorLogger = errorLogger;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return wrapped.keys(ops);
    }

    @Override
    public <T> DataResult<Optional<R>> decode(DynamicOps<T> ops, MapLike<T> input) {
        return DataResult.success(wrapped.decode(ops, input).resultOrPartial(errorLogger));
    }

    @Override
    public <T> RecordBuilder<T> encode(Optional<R> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        if (input.isEmpty()) {
            return prefix;
        }

        return wrapped.encode(input.get(), ops, prefix);
    }
}
