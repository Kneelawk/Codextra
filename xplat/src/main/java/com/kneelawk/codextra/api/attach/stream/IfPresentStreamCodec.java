package com.kneelawk.codextra.api.attach.stream;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;

import com.kneelawk.codextra.api.attach.AttachmentKey;

/**
 * A {@link StreamCodec} that delegates to one codec if an attachment is present and another if the attachment is absent.
 *
 * @param <B> the buffer type.
 * @param <V> the result type.
 */
public class IfPresentStreamCodec<B extends ByteBuf, V> implements StreamCodec<B, V> {
    private final AttachmentKey<?> key;
    private final StreamCodec<? super B, V> ifPresent;
    private final StreamCodec<? super B, V> ifAbsent;

    /**
     * Creates a new {@link IfPresentStreamCodec}.
     *
     * @param key       the key of the attachment to check the presence of.
     * @param ifPresent the codec to use if the attachment is present.
     * @param ifAbsent  the codec to use if the attachment is absent.
     */
    public IfPresentStreamCodec(AttachmentKey<?> key, StreamCodec<? super B, V> ifPresent,
                                StreamCodec<? super B, V> ifAbsent) {
        this.key = key;
        this.ifPresent = ifPresent;
        this.ifAbsent = ifAbsent;
    }

    @Override
    public V decode(B buf) {
        if (key.getOrNull(buf) != null) {
            return ifPresent.decode(buf);
        } else {
            return ifAbsent.decode(buf);
        }
    }

    @Override
    public void encode(B buf, V input) {
        if (key.getOrNull(buf) != null) {
            ifPresent.encode(buf, input);
        } else {
            ifAbsent.encode(buf, input);
        }
    }
}
