package com.kneelawk.codextra.api.attach.codec;

import java.util.function.Function;
import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import com.kneelawk.codextra.api.attach.AttachmentKey;
import com.kneelawk.codextra.api.util.FunctionUtils;

/**
 * A {@link MapCodec} that retrieves an attachment and determines which codec to use based on that attachment.
 *
 * @param <A> the type of attachment this retrieves.
 * @param <R> the type this is a codec for.
 */
public class AttachmentDispatchMapCodec<A, R> extends MapCodec<R> {
    private static final String COMPRESSED_KEY = "dispatched";

    private final AttachmentKey<A> key;
    private final Function<? super A, ? extends DataResult<? extends MapCodec<? extends R>>> dispatcher;

    /**
     * Creates a new {@link AttachmentDispatchMapCodec}.
     *
     * @param key        the key of the attachment to retrieve.
     * @param dispatcher the function to get the codec based on the retrieved attachment.
     */
    public AttachmentDispatchMapCodec(AttachmentKey<A> key,
                                      Function<? super A, ? extends DataResult<? extends MapCodec<? extends R>>> dispatcher) {
        this.key = key;
        this.dispatcher = dispatcher;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return Stream.of(ops.createString(COMPRESSED_KEY));
    }

    @Override
    public <T> DataResult<R> decode(DynamicOps<T> ops, MapLike<T> input) {
        if (ops.compressMaps()) {
            return key.getResult(ops).flatMap(dispatcher.andThen(FunctionUtils.dataIdentity())).flatMap(codec -> {
                T inputObj = input.get(COMPRESSED_KEY);
                if (inputObj == null) {
                    return DataResult.error(() -> "Input does not have \"" + COMPRESSED_KEY + "\" entry: " + input);
                }
                return codec.decoder().parse(ops, inputObj).map(Function.identity());
            });
        }

        return key.getResult(ops).flatMap(dispatcher.andThen(FunctionUtils.dataIdentity()))
            .flatMap(codec -> codec.decode(ops, input)).map(Function.identity());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> RecordBuilder<T> encode(R input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        DataResult<? extends MapCodec<? extends R>> dispatchedResult =
            key.getResult(ops).flatMap(dispatcher.andThen(FunctionUtils.dataIdentity()));
        if (dispatchedResult.isError()) {
            return prefix.withErrorsFrom(dispatchedResult);
        }

        // intentional cast, as dispatching makes sure the same codec is used for encoding as decoding
        MapCodec<R> dispatched = (MapCodec<R>) dispatchedResult.result().get();
        if (ops.compressMaps()) {
            return prefix.add(COMPRESSED_KEY, dispatched.codec().encodeStart(ops, input));
        }

        return dispatched.encode(input, ops, prefix);
    }

    @Override
    public String toString() {
        return "AttachmentDispatchMapCodec[" + key + " " + dispatcher + "]";
    }
}
