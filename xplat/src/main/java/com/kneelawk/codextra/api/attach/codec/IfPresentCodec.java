package com.kneelawk.codextra.api.attach.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import com.kneelawk.codextra.api.attach.AttachmentKey;

/**
 * A {@link Codec} that delegates to one codec if an attachment is present and another if the attachment is absent.
 *
 * @param <R> the result type.
 */
public class IfPresentCodec<R> implements Codec<R> {
    private final AttachmentKey<?> key;
    private final Codec<R> ifPresent;
    private final Codec<R> ifAbsent;

    /**
     * Creates a new {@link IfPresentCodec}.
     *
     * @param key       the key of the attachment to check the presence of.
     * @param ifPresent the codec to use if the attachment is present.
     * @param ifAbsent  the codec to use if the attachment is absent.
     */
    public IfPresentCodec(AttachmentKey<?> key, Codec<R> ifPresent, Codec<R> ifAbsent) {
        this.key = key;
        this.ifPresent = ifPresent;
        this.ifAbsent = ifAbsent;
    }

    @Override
    public <T> DataResult<Pair<R, T>> decode(DynamicOps<T> ops, T input) {
        if (key.getOrNull(ops) != null) {
            return ifPresent.decode(ops, input);
        } else {
            return ifAbsent.decode(ops, input);
        }
    }

    @Override
    public <T> DataResult<T> encode(R input, DynamicOps<T> ops, T prefix) {
        if (key.getOrNull(ops) != null) {
            return ifPresent.encode(input, ops, prefix);
        } else {
            return ifAbsent.encode(input, ops, prefix);
        }
    }
}
