package com.kneelawk.codextra.api.attach.codec;

import java.util.function.Function;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import com.kneelawk.codextra.api.attach.AttachmentKey;

/**
 * A {@link Codec} that retrieves an attachment if present and uses it to determine which codec to use or uses another
 * codec if the attachment is absent.
 *
 * @param <A> the attachment type.
 * @param <R> the result type.
 */
public class IfPresentDispatchCodec<A, R> implements Codec<R> {
    private final AttachmentKey<A> key;
    private final Function<? super A, ? extends DataResult<? extends Codec<? extends R>>> dispatcher;
    private final Codec<R> ifAbsent;

    /**
     * Creates a new {@link IfPresentDispatchCodec}.
     *
     * @param key        the key of the attachment to retrieve.
     * @param dispatcher the function to get the codec based on the retrieved attachment.
     * @param ifAbsent   the codec used if the attachment is not present.
     */
    public IfPresentDispatchCodec(AttachmentKey<A> key,
                                  Function<? super A, ? extends DataResult<? extends Codec<? extends R>>> dispatcher,
                                  Codec<R> ifAbsent) {
        this.key = key;
        this.dispatcher = dispatcher;
        this.ifAbsent = ifAbsent;
    }

    @Override
    public <T> DataResult<Pair<R, T>> decode(DynamicOps<T> ops, T input) {
        A attachment = key.getOrNull(ops);
        if (attachment != null) {
            return dispatcher.apply(attachment)
                .flatMap(codec -> codec.decode(ops, input).map(pair -> pair.mapFirst(Function.identity())));
        } else {
            return ifAbsent.decode(ops, input);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> DataResult<T> encode(R input, DynamicOps<T> ops, T prefix) {
        A attachment = key.getOrNull(ops);
        if (attachment != null) {
            return dispatcher.apply(attachment).flatMap(codec -> ((Codec<R>) codec).encode(input, ops, prefix));
        } else {
            return ifAbsent.encode(input, ops, prefix);
        }
    }
}
