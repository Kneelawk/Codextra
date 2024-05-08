package com.kneelawk.codextra.api.attach.codec;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import com.kneelawk.codextra.api.attach.AttachmentKey;

/**
 * A {@link MapCodec} that decodes a value and combines it with an attachment to create a result.
 *
 * @param <A> the attachment type.
 * @param <O> the type the attachment is combined with.
 * @param <R> the result type.
 */
public class RetrieveWithMapCodec<A, O, R> extends MapCodec<R> {
    private final AttachmentKey<A> key;
    private final MapCodec<O> withCodec;
    private final BiFunction<? super A, ? super O, ? extends DataResult<? extends R>> retriever;
    private final BiFunction<? super A, ? super R, ? extends DataResult<? extends O>> reverse;

    /**
     * Creates a new {@link RetrieveWithMapCodec}.
     *
     * @param key       the attachment key to find the attachment.
     * @param withCodec the codec for the value to be combined with the attachment.
     * @param retriever the function that combines the attachment with the decoded value.
     * @param reverse   the function that gets the value to be encoded when given the attachment and the combined value.
     */
    public RetrieveWithMapCodec(AttachmentKey<A> key, MapCodec<O> withCodec,
                                BiFunction<? super A, ? super O, ? extends DataResult<? extends R>> retriever,
                                BiFunction<? super A, ? super R, ? extends DataResult<? extends O>> reverse) {
        this.key = key;
        this.withCodec = withCodec;
        this.retriever = retriever;
        this.reverse = reverse;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return withCodec.keys(ops);
    }

    @Override
    public <T> DataResult<R> decode(DynamicOps<T> ops, MapLike<T> input) {
        return key.getResult(ops)
            .flatMap(attachment -> withCodec.decode(ops, input).flatMap(with -> retriever.apply(attachment, with))
                .map(Function.identity()));
    }

    @Override
    public <T> RecordBuilder<T> encode(R input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        DataResult<O> result =
            key.getResult(ops).flatMap(attachment -> reverse.apply(attachment, input).map(Function.identity()));
        if (result.isError()) {
            return prefix.withErrorsFrom(result);
        }

        return withCodec.encode(result.result().get(), ops, prefix);
    }

    @Override
    public String toString() {
        return "RetrieveWithMapCodec[" + key + " " + withCodec + " " + retriever + " " + reverse + "]";
    }
}
