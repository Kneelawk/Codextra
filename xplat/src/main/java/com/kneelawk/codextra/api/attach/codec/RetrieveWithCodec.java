package com.kneelawk.codextra.api.attach.codec;

import java.util.function.BiFunction;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import com.kneelawk.codextra.api.attach.AttachmentKey;

/**
 * A {@link Codec} that decodes a value and combines it with an attachment to create the result.
 *
 * @param <A> the attachment type.
 * @param <O> the type the attachment is combined with.
 * @param <R> the result type.
 */
public class RetrieveWithCodec<A, O, R> implements Codec<R> {
    private final AttachmentKey<A> key;
    private final Codec<O> withCodec;
    private final BiFunction<? super A, ? super O, ? extends DataResult<? extends R>> retriever;
    private final BiFunction<? super A, ? super R, ? extends DataResult<? extends O>> reverse;

    /**
     * Creates a new {@link RetrieveWithCodec}.
     *
     * @param key       the attachment key to find the attachment.
     * @param withCodec the codec for the value to be combined with the attachment.
     * @param retriever the function that combines the attachment with the decoded value.
     * @param reverse   the function that gets the value to be encoded when given the attachment and the combined value.
     */
    public RetrieveWithCodec(AttachmentKey<A> key, Codec<O> withCodec,
                             BiFunction<? super A, ? super O, ? extends DataResult<? extends R>> retriever,
                             BiFunction<? super A, ? super R, ? extends DataResult<? extends O>> reverse) {
        this.key = key;
        this.withCodec = withCodec;
        this.retriever = retriever;
        this.reverse = reverse;
    }

    @Override
    public <T> DataResult<Pair<R, T>> decode(DynamicOps<T> ops, T input) {
        return key.getResult(ops).flatMap(attachment -> withCodec.decode(ops, input).flatMap(
            with -> retriever.apply(attachment, with.getFirst()).map(result -> Pair.of(result, with.getSecond()))));
    }

    @Override
    public <T> DataResult<T> encode(R input, DynamicOps<T> ops, T prefix) {
        return key.getResult(ops).flatMap(
            attachment -> reverse.apply(attachment, input).flatMap(input2 -> withCodec.encode(input2, ops, prefix)));
    }

    @Override
    public String toString() {
        return "RetrieveWithCodec[" + key + " " + withCodec + " " + retriever + " " + reverse + "]";
    }
}
