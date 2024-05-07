package com.kneelawk.codextra.api.attach.codec;

import java.util.function.Function;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import com.kneelawk.codextra.api.attach.AttachmentKey;

/**
 * A {@link Codec} that retrieves an attachment and determines which codec to use based on that attachment.
 *
 * @param <A> the type of attachment this retrieves.
 * @param <R> the type this is a codec for.
 */
public class AttachmentDispatchCodec<A, R> implements Codec<R> {
    private final AttachmentKey<A> key;
    private final Function<A, DataResult<Codec<R>>> dispatcher;

    /**
     * Creates a new {@link AttachmentDispatchCodec}.
     *
     * @param key        the key of the attachment to retrieve.
     * @param dispatcher the function to get the codec based on the retrieved attachment.
     */
    public AttachmentDispatchCodec(AttachmentKey<A> key, Function<A, DataResult<Codec<R>>> dispatcher) {
        this.key = key;
        this.dispatcher = dispatcher;
    }

    @Override
    public <T> DataResult<Pair<R, T>> decode(DynamicOps<T> ops, T input) {
        return key.getResult(ops).flatMap(dispatcher).flatMap(codec -> codec.decode(ops, input));
    }

    @Override
    public <T> DataResult<T> encode(R input, DynamicOps<T> ops, T prefix) {
        return key.getResult(ops).flatMap(dispatcher).flatMap(codec -> codec.encode(input, ops, prefix));
    }
}
