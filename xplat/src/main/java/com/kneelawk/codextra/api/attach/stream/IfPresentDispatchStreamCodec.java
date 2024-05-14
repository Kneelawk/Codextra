package com.kneelawk.codextra.api.attach.stream;

import java.util.function.Function;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;

import com.kneelawk.codextra.api.attach.AttachmentKey;

/**
 * A {@link StreamCodec} that retrieves an attachment if present and uses it to determine which codec to use or uses
 * another codec if the attachment is absent.
 *
 * @param <A> the attachment type.
 * @param <B> the buffer type.
 * @param <V> the result type.
 */
public class IfPresentDispatchStreamCodec<A, B extends ByteBuf, V> implements StreamCodec<B, V> {
    private final AttachmentKey<A> key;
    private final Function<? super A, ? extends StreamCodec<? super B, ? extends V>> dispatcher;
    private final StreamCodec<? super B, V> ifAbsent;

    /**
     * Creates a new {@link IfPresentDispatchStreamCodec}.
     *
     * @param key        the key of the attachment to retrieve.
     * @param dispatcher the function to get the codec based on the retrieved attachment.
     * @param ifAbsent   the codec used if the attachment is not present.
     */
    public IfPresentDispatchStreamCodec(AttachmentKey<A> key,
                                        Function<? super A, ? extends StreamCodec<? super B, ? extends V>> dispatcher,
                                        StreamCodec<? super B, V> ifAbsent) {
        this.key = key;
        this.dispatcher = dispatcher;
        this.ifAbsent = ifAbsent;
    }

    @Override
    public V decode(B buf) {
        A attachment = key.getOrNull(buf);

        if (attachment != null) {
            return dispatcher.apply(attachment).decode(buf);
        } else {
            return ifAbsent.decode(buf);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void encode(B buf, V input) {
        A attachment = key.getOrNull(buf);

        if (attachment != null) {
            ((StreamCodec<? super B, V>) dispatcher.apply(attachment)).encode(buf, input);
        } else {
            ifAbsent.encode(buf, input);
        }
    }
}
