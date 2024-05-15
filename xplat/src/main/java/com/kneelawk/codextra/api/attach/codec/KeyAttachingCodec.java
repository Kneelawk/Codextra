package com.kneelawk.codextra.api.attach.codec;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import com.kneelawk.codextra.api.attach.AttachmentKey;
import com.kneelawk.codextra.api.util.FunctionUtils;

/**
 * A {@link MapCodec} that decodes one value and uses it to create attachments to attach to the context when decoding
 * the other value.
 *
 * @param <K> the key type.
 * @param <R> the result type.
 */
public class KeyAttachingCodec<K, R> extends MapCodec<R> {
    private final MapCodec<K> keyCodec;
    private final Function<? super K, ? extends DataResult<? extends Map<AttachmentKey<?>, ?>>> attachmentsGetter;
    private final MapCodec<R> wrappedCodec;
    private final Function<? super R, ? extends DataResult<? extends K>> keyGetter;

    /**
     * Creates a new {@link KeyAttachingCodec} that uses its key as its single attachment.
     *
     * @param key          the attachment key.
     * @param keyCodec     the codec for the attachment.
     * @param wrappedCodec the codec to pass the attachment to.
     * @param keyGetter    the function to get the key from the result.
     * @param <A>          the attachment type.
     * @param <R>          the result type.
     * @return the new codec.
     */
    public static <A, R> KeyAttachingCodec<A, R> single(AttachmentKey<A> key, MapCodec<A> keyCodec,
                                                        MapCodec<R> wrappedCodec,
                                                        Function<? super R, ? extends DataResult<? extends A>> keyGetter) {
        return new KeyAttachingCodec<>(keyCodec, a -> DataResult.success(Map.of(key, a)), wrappedCodec, keyGetter);
    }

    /**
     * Creates a new {@link KeyAttachingCodec}.
     *
     * @param keyCodec          the codec for the key to be turned into attachments.
     * @param attachmentsGetter the function to turn the key into attachments.
     * @param wrappedCodec      the codec to pass the attachments to.
     * @param keyGetter         the function to get the key back from the result type.
     */
    public KeyAttachingCodec(MapCodec<K> keyCodec,
                             Function<? super K, ? extends DataResult<? extends Map<AttachmentKey<?>, ?>>> attachmentsGetter,
                             MapCodec<R> wrappedCodec,
                             Function<? super R, ? extends DataResult<? extends K>> keyGetter) {
        this.keyCodec = keyCodec;
        this.attachmentsGetter = attachmentsGetter;
        this.wrappedCodec = wrappedCodec;
        this.keyGetter = keyGetter;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return Stream.concat(keyCodec.keys(ops), wrappedCodec.keys(ops));
    }

    @Override
    public <T> DataResult<R> decode(DynamicOps<T> ops, MapLike<T> input) {
        return keyCodec.decode(ops, input).flatMap(attachmentsGetter.andThen(FunctionUtils.dataIdentity()))
            .flatMap(attachmentMap -> {
                DynamicOps<T> attached = push(ops, attachmentMap);
                DataResult<R> result = wrappedCodec.decode(attached, input);
                pop(attached, attachmentMap);
                return result;
            });
    }

    @Override
    public <T> RecordBuilder<T> encode(R input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        DataResult<? extends K> keyResult = keyGetter.apply(input);
        if (keyResult.isError()) {
            return prefix.withErrorsFrom(keyResult);
        }

        K key = keyResult.result().get();
        DataResult<? extends Map<AttachmentKey<?>, ?>> attachmentMapResult = attachmentsGetter.apply(key);
        if (attachmentMapResult.isError()) {
            return prefix.withErrorsFrom(attachmentMapResult);
        }

        prefix = keyCodec.encode(key, ops, prefix);

        Map<AttachmentKey<?>, ?> attachmentMap = attachmentMapResult.result().get();
        DynamicOps<T> attached = push(ops, attachmentMap);
        RecordBuilder<T> wrapped =
            wrappedCodec.encode(input, attached, OpsReplacingRecordBuilder.wrap(prefix, attached));
        pop(attached, attachmentMap);

        return OpsReplacingRecordBuilder.unwrap(wrapped, prefix, ops);
    }

    @SuppressWarnings("unchecked")
    private <T> DynamicOps<T> push(DynamicOps<T> ops, Map<AttachmentKey<?>, ?> attachmentMap) {
        for (var entry : attachmentMap.entrySet()) {
            ops = ((AttachmentKey<Object>) entry.getKey()).push(ops, entry.getValue());
        }
        return ops;
    }

    private <T> void pop(DynamicOps<T> ops, Map<AttachmentKey<?>, ?> attachmentMap) {
        for (var key : attachmentMap.keySet()) {
            key.pop(ops);
        }
    }

    @Override
    public String toString() {
        return "KeyAttachingCodec[" + keyCodec + " " + attachmentsGetter + " " + wrappedCodec + " " + keyGetter + "]";
    }
}
