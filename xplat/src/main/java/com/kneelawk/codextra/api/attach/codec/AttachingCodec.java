package com.kneelawk.codextra.api.attach.codec;

import java.util.Map;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import com.kneelawk.codextra.api.attach.AttachmentKey;

/**
 * {@link Codec} for attaching a value and passing it as context to the wrapped codec.
 *
 * @param <R> the codec type of the codec this codec wraps.
 */
public class AttachingCodec<R> implements Codec<R> {
    private final Map<AttachmentKey<?>, ?> attachmentMap;
    private final Codec<R> wrapped;

    /**
     * Creates a new {@link AttachingCodec}.
     *
     * @param key     the key of the attachment.
     * @param value   the value to attach.
     * @param wrapped the codec to pass the attachment to.
     * @param <A>     the attachment type this attaches.
     * @param <R>     the type of codec this codec wraps.
     * @return the created codec.
     */
    public static <A, R> AttachingCodec<R> single(AttachmentKey<A> key, A value, Codec<R> wrapped) {
        return new AttachingCodec<>(Map.of(key, value), wrapped);
    }

    /**
     * Creates a new {@link AttachingCodec}.
     *
     * @param attachmentMap the map of attachments to attach.
     * @param wrapped       the codec to pass the attachments to.
     */
    public AttachingCodec(Map<AttachmentKey<?>, ?> attachmentMap, Codec<R> wrapped) {
        this.attachmentMap = attachmentMap;
        this.wrapped = wrapped;
    }

    @Override
    public <T> DataResult<Pair<R, T>> decode(DynamicOps<T> ops, T input) {
        DynamicOps<T> attached = push(ops);
        DataResult<Pair<R, T>> result = wrapped.decode(attached, input);
        pop(attached);
        return result;
    }

    @Override
    public <T> DataResult<T> encode(R input, DynamicOps<T> ops, T prefix) {
        DynamicOps<T> attached = push(ops);
        DataResult<T> result = wrapped.encode(input, attached, prefix);
        pop(attached);
        return result;
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
        return "AttachingCodec[" + attachmentMap + " " + wrapped + "]";
    }
}
