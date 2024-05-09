package com.kneelawk.codextra.api.attach.codec;

import java.util.Map;
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
 * @param <R> the type of the map codec this map codec wraps.
 */
public class AttachingMapCodec<R> extends MapCodec<R> {
    private final Map<AttachmentKey<?>, ?> attachmentMap;
    private final MapCodec<R> wrapped;

    /**
     * Creates a new {@link AttachingMapCodec}.
     *
     * @param key     the attachment key.
     * @param value   the value to attach.
     * @param wrapped the codec to pass the attachment to.
     * @param <A>     the attachment type.
     * @param <R>     the type of the map codec this map codec wraps.
     * @return the created codec.
     */
    public static <A, R> AttachingMapCodec<R> single(AttachmentKey<A> key, A value, MapCodec<R> wrapped) {
        return new AttachingMapCodec<>(Map.of(key, value), wrapped);
    }

    /**
     * Creates a new {@link AttachingMapCodec}.
     *
     * @param attachmentMap the map of attachments to attach.
     * @param wrapped       the codec to pass the attachments to.
     */
    public AttachingMapCodec(Map<AttachmentKey<?>, ?> attachmentMap, MapCodec<R> wrapped) {
        this.attachmentMap = attachmentMap;
        this.wrapped = wrapped;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return wrapped.keys(ops);
    }

    @Override
    public <T> DataResult<R> decode(DynamicOps<T> ops, MapLike<T> input) {
        DynamicOps<T> attached = push(ops);
        DataResult<R> result = wrapped.decode(attached, input);
        pop(attached);
        return result;
    }

    @Override
    public <T> RecordBuilder<T> encode(R input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        DynamicOps<T> attached = push(ops);
        RecordBuilder<T> result = wrapped.encode(input, attached, OpsReplacingRecordBuilder.wrap(prefix, attached));
        pop(attached);
        return OpsReplacingRecordBuilder.unwrap(result, prefix, ops);
    }

    @SuppressWarnings("unchecked")
    private <T> DynamicOps<T> push(DynamicOps<T> ops) {
        for (var entry : attachmentMap.entrySet()) {
            ops = ((AttachmentKey<Object>) entry.getKey()).push(ops, entry.getValue());
        }
        return ops;
    }

    private <T> void pop(DynamicOps<T> ops) {
        for (var key : attachmentMap.keySet()) {
            key.pop(ops);
        }
    }

    @Override
    public String toString() {
        return "AttachingMapCodec[" + attachmentMap + " " + wrapped + "]";
    }
}
