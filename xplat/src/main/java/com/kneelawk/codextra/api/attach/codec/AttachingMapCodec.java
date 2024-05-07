package com.kneelawk.codextra.api.attach.codec;

import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import com.kneelawk.codextra.api.attach.AttachmentKey;

/**
 * {@link MapCodec} for attaching a value and passing it
 *
 * @param <A> the attachment type.
 * @param <R> the type of the map codec this map codec wraps.
 */
public class AttachingMapCodec<A, R> extends MapCodec<R> {
    private final AttachmentKey<A> key;
    private final A value;
    private final MapCodec<R> wrapped;

    /**
     * Creates a new {@link AttachingMapCodec}.
     *
     * @param key     the key of the attachment.
     * @param value   the value to attach.
     * @param wrapped the codec to pass the attachment to.
     */
    public AttachingMapCodec(AttachmentKey<A> key, A value, MapCodec<R> wrapped) {
        this.key = key;
        this.value = value;
        this.wrapped = wrapped;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return wrapped.keys(ops);
    }

    @Override
    public <T> DataResult<R> decode(DynamicOps<T> ops, MapLike<T> input) {
        DynamicOps<T> attached = key.push(ops, value);
        DataResult<R> result = wrapped.decode(attached, input);
        key.pop(attached);
        return result;
    }

    @Override
    public <T> RecordBuilder<T> encode(R input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        DynamicOps<T> attached = key.push(ops, value);
        RecordBuilder<T> result = wrapped.encode(input, attached, prefix);
        key.pop(attached);
        return result;
    }

    @Override
    public String toString() {
        return "AttachingMapCodec['" + key.getName() + "': " + value + ", " + wrapped + "]";
    }
}
