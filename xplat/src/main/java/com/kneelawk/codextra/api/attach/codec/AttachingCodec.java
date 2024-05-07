package com.kneelawk.codextra.api.attach.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import com.kneelawk.codextra.api.attach.AttachmentKey;

/**
 * {@link Codec} for attaching a value and passing it as context to the wrapped codec.
 *
 * @param <R> the codec type of the codec this codec wraps.
 * @param <A> the attachment type this attaches.
 */
public class AttachingCodec<A, R> implements Codec<R> {
    private final AttachmentKey<A> key;
    private final A value;
    private final Codec<R> wrapped;

    /**
     * Creates a new {@link AttachingCodec}.
     *
     * @param key     the key of the attachment.
     * @param value   the value to attach.
     * @param wrapped the codec to pass the attachment to.
     */
    public AttachingCodec(AttachmentKey<A> key, A value, Codec<R> wrapped) {
        this.key = key;
        this.value = value;
        this.wrapped = wrapped;
    }

    @Override
    public <T> DataResult<Pair<R, T>> decode(DynamicOps<T> ops, T input) {
        DynamicOps<T> attached = key.push(ops, value);
        DataResult<Pair<R, T>> result = wrapped.decode(attached, input);
        key.pop(attached);
        return result;
    }

    @Override
    public <T> DataResult<T> encode(R input, DynamicOps<T> ops, T prefix) {
        DynamicOps<T> attached = key.push(ops, value);
        DataResult<T> result = wrapped.encode(input, attached, prefix);
        key.pop(attached);
        return result;
    }

    @Override
    public String toString() {
        return "AttachingCodec['" + key.getName() + "': " + value + ", " + wrapped + "]";
    }
}
