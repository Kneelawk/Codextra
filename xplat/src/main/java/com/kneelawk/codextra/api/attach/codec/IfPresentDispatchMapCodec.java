package com.kneelawk.codextra.api.attach.codec;

import java.util.function.Function;
import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import com.kneelawk.codextra.api.attach.AttachmentKey;

/**
 * A {@link MapCodec} that retrieves an attachment if present and uses it to determine which codec to use or uses
 * another codec if the attachment is absent.
 *
 * @param <A> the attachment type.
 * @param <R> the result type.
 */
public class IfPresentDispatchMapCodec<A, R> extends MapCodec<R> {
    private static final String COMPRESSED_KEY = "dispatched";

    private final AttachmentKey<A> key;
    private final Function<? super A, ? extends DataResult<? extends MapCodec<? extends R>>> dispatcher;
    private final MapCodec<R> ifAbsent;

    /**
     * Creates a new {@link IfPresentDispatchCodec}.
     *
     * @param key        the key of the attachment to retrieve.
     * @param dispatcher the function to get the codec based on the retrieved attachment.
     * @param ifAbsent   the codec used if the attachment is no present.
     */
    public IfPresentDispatchMapCodec(AttachmentKey<A> key,
                                     Function<? super A, ? extends DataResult<? extends MapCodec<? extends R>>> dispatcher,
                                     MapCodec<R> ifAbsent) {
        this.key = key;
        this.dispatcher = dispatcher;
        this.ifAbsent = ifAbsent;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return Stream.of(ops.createString(COMPRESSED_KEY));
    }

    @Override
    public <T> DataResult<R> decode(DynamicOps<T> ops, MapLike<T> input) {
        A attachment = key.getOrNull(ops);

        if (ops.compressMaps()) {
            if (attachment != null) {
                return dispatcher.apply(attachment).flatMap(codec -> compressedDecode(ops, input, codec));
            } else {
                return compressedDecode(ops, input, ifAbsent);
            }
        } else {
            if (attachment != null) {
                return dispatcher.apply(attachment).flatMap(codec -> codec.decode(ops, input).map(Function.identity()));
            } else {
                return ifAbsent.decode(ops, input);
            }
        }
    }

    private <T> DataResult<R> compressedDecode(DynamicOps<T> ops, MapLike<T> input, MapCodec<? extends R> codec) {
        T inputObj = input.get(COMPRESSED_KEY);
        if (inputObj == null) {
            return DataResult.error(() -> "Input does not have \"" + COMPRESSED_KEY + "\" entry: " + input);
        }
        return codec.decoder().parse(ops, inputObj).map(Function.identity());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> RecordBuilder<T> encode(R input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        A attachment = key.getOrNull(ops);

        MapCodec<R> codec;
        if (attachment != null) {
            DataResult<? extends MapCodec<? extends R>> dispatchedResult = dispatcher.apply(attachment);
            if (dispatchedResult.isError()) {
                return prefix.withErrorsFrom(dispatchedResult);
            }

            codec = (MapCodec<R>) dispatchedResult.result().get();
        } else {
            codec = ifAbsent;
        }

        if (ops.compressMaps()) {
            return prefix.add(COMPRESSED_KEY, codec.encoder().encodeStart(ops, input));
        }

        return codec.encode(input, ops, prefix);
    }
}
