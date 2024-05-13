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
 * A {@link MapCodec} that decodes one value and attaches it to the context when decoding the other value, but that
 * also allows mutation of the attachment while encoding, making sure those changes show up in the decoded attachment.
 *
 * @param <A> the attachment type.
 * @param <R> the result type.
 */
public class MutKeyAttachingCodec<A, R> extends MapCodec<R> {
    private final AttachmentKey<A> key;
    private final MapCodec<A> keyCodec;
    private final MapCodec<R> wrappedCodec;
    private final Function<? super R, ? extends DataResult<? extends A>> attachmentGetter;

    /**
     * Creates a new {@link MutKeyAttachingCodec}.
     *
     * @param key              the key of the attachment to attach.
     * @param keyCodec         the codec that decodes the attachment.
     * @param wrappedCodec     the codec that is invoked with the attachment attached.
     * @param attachmentGetter a function for getting the attachment when given the result type. This may simply create
     *                         a new attachment if the attachment is intended to get all its value from being mutated
     *                         while encoding.
     */
    public MutKeyAttachingCodec(AttachmentKey<A> key, MapCodec<A> keyCodec, MapCodec<R> wrappedCodec,
                                Function<? super R, ? extends DataResult<? extends A>> attachmentGetter) {
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
        DataResult<? extends A> attachmentResult = attachmentGetter.apply(input);
        if (attachmentResult.isError()) {
            return prefix.withErrorsFrom(attachmentResult);
        }

        A attachment = attachmentResult.result().get();

        DynamicOps<T> attached = key.push(ops, attachment);
        RecordBuilder<T> wrapped =
            wrappedCodec.encode(input, attached, OpsReplacingRecordBuilder.wrap(prefix, attached));
        key.pop(attached);
        RecordBuilder<T> unwrapped = OpsReplacingRecordBuilder.unwrap(wrapped, prefix, ops);

        // we can encode the key last but read it first because this is not a stream codec
        return keyCodec.encode(attachment, ops, unwrapped);
    }
}
