package com.kneelawk.codextra.api.attach.codec;

import java.util.function.Function;
import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import com.kneelawk.codextra.api.attach.AttachmentManager;
import com.kneelawk.codextra.api.attach.CodecAttachment;

/**
 * {@link MapCodec} for retrieving an attachment.
 *
 * @param <A> the type of attachment to retrieve.
 * @param <R> the type of value to return.
 */
public class RetrievalMapCodec<A, R> extends MapCodec<R> {
    private final CodecAttachment<A> key;
    private final Function<A, R> retriever;

    /**
     * Creates a new {@link RetrievalMapCodec}.
     *
     * @param key       the key of the attachment to retrieve.
     * @param retriever the function that retrieves the desired value from the attachment value.
     */
    public RetrievalMapCodec(CodecAttachment<A> key, Function<A, R> retriever) {
        this.key = key;
        this.retriever = retriever;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return Stream.empty();
    }

    @Override
    public <T> DataResult<R> decode(DynamicOps<T> ops, MapLike<T> input) {
        AttachmentManager manager = AttachmentManager.getAttachmentManager(ops);
        if (manager == null) {
            return DataResult.error(
                () -> "DynamicOps '" + ops + "' does not support attachments. Attachment '" + key.getName() +
                    "' not present.");
        }

        A value = manager.get(key);
        if (value == null) {
            return DataResult.error(() -> "Attachment '" + key.getName() + "' not present.");
        }

        return DataResult.success(retriever.apply(value));
    }

    @Override
    public <T> RecordBuilder<T> encode(R input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        return prefix;
    }
}