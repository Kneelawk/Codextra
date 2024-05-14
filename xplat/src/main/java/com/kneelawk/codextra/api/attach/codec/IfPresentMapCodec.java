package com.kneelawk.codextra.api.attach.codec;

import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import com.kneelawk.codextra.api.attach.AttachmentKey;

/**
 * A {@link MapCodec} that delegates to one codec if an attachment is present and another if the attachment is absent.
 *
 * @param <R> the result type.
 */
public class IfPresentMapCodec<R> extends MapCodec<R> {
    private final AttachmentKey<?> key;
    private final MapCodec<R> ifPresent;
    private final MapCodec<R> ifAbsent;

    /**
     * Creates a new {@link IfPresentMapCodec}.
     *
     * @param key       the key of the attachment to check the presence of.
     * @param ifPresent the codec to use if the attachment is present.
     * @param ifAbsent  the codec to use if the attachment is absent.
     */
    public IfPresentMapCodec(AttachmentKey<?> key, MapCodec<R> ifPresent, MapCodec<R> ifAbsent) {
        this.key = key;
        this.ifPresent = ifPresent;
        this.ifAbsent = ifAbsent;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return Stream.concat(ifPresent.keys(ops), ifAbsent.keys(ops));
    }

    @Override
    public <T> DataResult<R> decode(DynamicOps<T> ops, MapLike<T> input) {
        if (key.getOrNull(ops) != null) {
            return ifPresent.decode(ops, input);
        } else {
            return ifAbsent.decode(ops, input);
        }
    }

    @Override
    public <T> RecordBuilder<T> encode(R input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        if (key.getOrNull(ops) != null) {
            return ifPresent.encode(input, ops, prefix);
        } else {
            return ifAbsent.encode(input, ops, prefix);
        }
    }
}
