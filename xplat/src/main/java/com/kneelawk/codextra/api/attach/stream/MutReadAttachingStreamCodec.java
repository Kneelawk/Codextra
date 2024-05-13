package com.kneelawk.codextra.api.attach.stream;

import java.util.function.Function;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import com.kneelawk.codextra.api.attach.AttachmentKey;
import com.kneelawk.codextra.api.attach.AttachmentManager;

/**
 * A {@link StreamCodec} that decodes one value and then attaches it to the context when decoding the result value,
 * but that also allows mutation of the attachment while encoding, making sure those changes show up in the decoded
 * attachment.
 * <p>
 * This is good for things like palettes.
 *
 * @param <A>  the attachment type.
 * @param <B1> the buffer type of this stream codec.
 * @param <B2> the buffer type of the wrapped codec.
 * @param <V>  the result type.
 */
public class MutReadAttachingStreamCodec<A, B1 extends FriendlyByteBuf, B2 extends FriendlyByteBuf, V>
    implements StreamCodec<B1, V> {
    private final AttachmentKey<A> key;
    private final StreamCodec<? super B1, A> keyCodec;
    private final ChildBufferFactory<? super B1, B2> wrappedBufferCtor;
    private final StreamCodec<? super B2, V> wrappedCodec;
    private final Function<? super V, ? extends A> attachmentGetter;

    /**
     * Creates a new {@link MutReadAttachingStreamCodec}.
     *
     * @param key               the attachment key.
     * @param keyCodec          the key's codec.
     * @param wrappedBufferCtor for creating the buffer used by the wrapped codec, as writing wrapped codec values to
     *                          the main buffer must be delayed.
     * @param wrappedCodec      the codec that will be invoked with the attachment attached.
     * @param attachmentGetter  a function for getting the attachment when given the result type. This may simply create
     *                          a new attachment if the attachment is intended to get all its value from being mutated
     *                          while encoding.
     */
    public MutReadAttachingStreamCodec(AttachmentKey<A> key, StreamCodec<? super B1, A> keyCodec,
                                       ChildBufferFactory<? super B1, B2> wrappedBufferCtor,
                                       StreamCodec<? super B2, V> wrappedCodec,
                                       Function<? super V, ? extends A> attachmentGetter) {
        this.key = key;
        this.keyCodec = keyCodec;
        this.wrappedCodec = wrappedCodec;
        this.wrappedBufferCtor = wrappedBufferCtor;
        this.attachmentGetter = attachmentGetter;
    }

    @Override
    public V decode(B1 buf) {
        A attachment = keyCodec.decode(buf);

        key.push(buf, attachment);
        try {
            int wrappedBufLen = buf.readVarInt();
            B2 wrappedBuf = wrappedBufferCtor.create(wrappedBufLen, buf);
            AttachmentManager.sync(buf, wrappedBuf);
            buf.readBytes(wrappedBuf, wrappedBufLen);

            return wrappedCodec.decode(wrappedBuf);
        } finally {
            key.pop(buf);
        }
    }

    @Override
    public void encode(B1 buf, V input) {
        A attachment = attachmentGetter.apply(input);

        B2 wrappedBuf;
        key.push(buf, attachment);
        try {
            wrappedBuf = wrappedBufferCtor.create(0, buf);
            AttachmentManager.sync(buf, wrappedBuf);

            wrappedCodec.encode(wrappedBuf, input);
        } finally {
            key.pop(buf);
        }

        // this stuff should not happen if wrappedCodec.encode fails
        keyCodec.encode(buf, attachment);

        // write the wrapped buffer
        buf.writeVarInt(wrappedBuf.readableBytes());
        buf.writeBytes(wrappedBuf, wrappedBuf.readerIndex(), wrappedBuf.readableBytes());
    }
}
