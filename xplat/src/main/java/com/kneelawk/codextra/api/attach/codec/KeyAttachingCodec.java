package com.kneelawk.codextra.api.attach.codec;

import java.util.function.Function;
import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import com.kneelawk.codextra.api.attach.AttachmentKey;
import com.kneelawk.codextra.api.codec.OpsReplacingRecordBuilder;

/**
 * A {@link MapCodec} that decodes one value and attaches it to the context when decoding the other value.
 *
 * @param <A> the attachment type.
 * @param <R> the result type.
 */
public class KeyAttachingCodec<A, R> extends MapCodec<R> {
    private final AttachmentKey<A> key;
    private final MapCodec<A> keyCodec;
    private final MapCodec<R> wrappedCodec;
    private final Function<R, DataResult<A>> attachmentGetter;

    /**
     * Creates a new {@link KeyAttachingCodec}.
     *
     * @param key              the key of the attachment to attach.
     * @param keyCodec         the codec that decodes the attachment.
     * @param wrappedCodec     the codec that is invoked with the attachment attached.
     * @param attachmentGetter a function for getting the attachment when given the result type.
     */
    public KeyAttachingCodec(AttachmentKey<A> key, MapCodec<A> keyCodec, MapCodec<R> wrappedCodec,
                             Function<R, DataResult<A>> attachmentGetter) {
        this.key = key;
        this.keyCodec = keyCodec;
        this.wrappedCodec = wrappedCodec;
        this.attachmentGetter = attachmentGetter;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return Stream.concat(keyCodec.keys(ops), wrappedCodec.keys(ops));
    }

    @Override
    public <T> DataResult<R> decode(DynamicOps<T> ops, MapLike<T> input) {
        return keyCodec.decode(ops, input).flatMap(keyValue -> {
            DynamicOps<T> attached = key.push(ops, keyValue);
            DataResult<R> result = wrappedCodec.decode(attached, input);
            key.pop(attached);
            return result;
        });
    }

    @Override
    public <T> RecordBuilder<T> encode(R input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        DataResult<A> attachmentResult = attachmentGetter.apply(input);
        if (attachmentResult.isError()) {
            return prefix.withErrorsFrom(attachmentResult);
        }

        A attachment = attachmentResult.result().get();
        prefix = keyCodec.encode(attachment, ops, prefix);

        DynamicOps<T> attached = key.push(ops, attachment);
        prefix = wrappedCodec.encode(input, attached, new OpsReplacingRecordBuilder<>(attached, prefix));
        if (prefix instanceof OpsReplacingRecordBuilder<T> replacing) prefix = replacing.unwrap();
        key.pop(attached);

        return prefix;
    }
}
