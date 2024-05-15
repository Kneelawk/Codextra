package com.kneelawk.codextra.api.attach.stream;

import java.util.Map;
import java.util.function.Function;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import com.kneelawk.codextra.api.attach.AttachmentKey;
import com.kneelawk.codextra.api.attach.AttachmentManager;

/**
 * A {@link StreamCodec} that decodes one value and uses it create attachments to attach to the context when decoding
 * the result value, but that also allows mutation of the attachment while encoding, making sure those changes show up
 * in the decoded key.
 * <p>
 * This is good for things like palettes.
 *
 * @param <B1> the buffer type of this stream codec.
 * @param <B2> the buffer type of the wrapped codec.
 * @param <K>  the key type.
 * @param <V>  the result type.
 */
public class MutReadAttachingStreamCodec<B1 extends FriendlyByteBuf, B2 extends FriendlyByteBuf, K, V>
    implements StreamCodec<B1, V> {
    private final StreamCodec<? super B1, K> keyCodec;
    private final Function<? super K, ? extends Map<AttachmentKey<?>, ?>> attachmentsGetter;
    private final ChildBufferFactory<? super B1, B2> wrappedBufferCtor;
    private final StreamCodec<? super B2, V> wrappedCodec;
    private final Function<? super V, ? extends K> keyGetter;

    /**
     * Creates a new {@link MutReadAttachingStreamCodec} that uses its key as its single attachment.
     *
     * @param key               the attachment key.
     * @param keyCodec          the codec for the attachment.
     * @param wrappedBufferCtor for creating the buffer used by the wrapped codec, as writing wrapped codec values to
     *                          the main buffer must be delayed.
     * @param wrappedCodec      the codec to pass the attachment to.
     * @param keyGetter         a function for getting the attachment when given the result type. This may simply create
     *                          a new attachment if the attachment is intended to get all its value from being mutated
     *                          while encoding.
     * @param <A>               the attachment type.
     * @param <B1>              the buffer type of this stream codec.
     * @param <B2>              the buffer type of the wrapped codec.
     * @param <V>               the result type.
     * @return the created stream codec.
     */
    public static <A, B1 extends FriendlyByteBuf, B2 extends FriendlyByteBuf, V> MutReadAttachingStreamCodec<B1, B2, A, V> single(
        AttachmentKey<A> key, StreamCodec<? super B1, A> keyCodec, ChildBufferFactory<? super B1, B2> wrappedBufferCtor,
        StreamCodec<? super B2, V> wrappedCodec, Function<? super V, ? extends A> keyGetter) {
        return new MutReadAttachingStreamCodec<>(keyCodec, a -> Map.of(key, a), wrappedBufferCtor, wrappedCodec,
            keyGetter);
    }

    /**
     * Creates a new {@link MutReadAttachingStreamCodec}.
     *
     * @param keyCodec          the codec for the key to be turned into attachments.
     * @param attachmentsGetter the function to turn the key into attachments.
     * @param wrappedBufferCtor for creating the buffer used by the wrapped codec, as writing wrapped codec values to
     *                          the main buffer must be delayed.
     * @param wrappedCodec      the codec to pass the attachments to.
     * @param keyGetter         the function to get the key back from the result type. This may simply create
     *                          a new key if the key is intended to get all its value from being mutated
     *                          while encoding.
     */
    public MutReadAttachingStreamCodec(StreamCodec<? super B1, K> keyCodec,
                                       Function<? super K, ? extends Map<AttachmentKey<?>, ?>> attachmentsGetter,
                                       ChildBufferFactory<? super B1, B2> wrappedBufferCtor,
                                       StreamCodec<? super B2, V> wrappedCodec,
                                       Function<? super V, ? extends K> keyGetter) {
        this.keyCodec = keyCodec;
        this.attachmentsGetter = attachmentsGetter;
        this.wrappedBufferCtor = wrappedBufferCtor;
        this.wrappedCodec = wrappedCodec;
        this.keyGetter = keyGetter;
    }

    @Override
    public V decode(B1 buf) {
        K key = keyCodec.decode(buf);

        Map<AttachmentKey<?>, ?> attachmentMap = attachmentsGetter.apply(key);

        push(buf, attachmentMap);
        try {
            int wrappedBufLen = buf.readVarInt();
            B2 wrappedBuf = wrappedBufferCtor.create(wrappedBufLen, buf);
            AttachmentManager.sync(buf, wrappedBuf);
            buf.readBytes(wrappedBuf, wrappedBufLen);

            return wrappedCodec.decode(wrappedBuf);
        } finally {
            pop(buf, attachmentMap);
        }
    }

    @Override
    public void encode(B1 buf, V input) {
        K key = keyGetter.apply(input);

        Map<AttachmentKey<?>, ?> attachmentMap = attachmentsGetter.apply(key);

        B2 wrappedBuf;
        push(buf, attachmentMap);
        try {
            wrappedBuf = wrappedBufferCtor.create(0, buf);
            AttachmentManager.sync(buf, wrappedBuf);

            wrappedCodec.encode(wrappedBuf, input);
        } finally {
            pop(buf, attachmentMap);
        }

        // this stuff should not happen if wrappedCodec.encode fails
        keyCodec.encode(buf, key);

        // write the wrapped buffer
        buf.writeVarInt(wrappedBuf.readableBytes());
        buf.writeBytes(wrappedBuf, wrappedBuf.readerIndex(), wrappedBuf.readableBytes());
    }

    @SuppressWarnings("unchecked")
    private void push(FriendlyByteBuf buf, Map<AttachmentKey<?>, ?> attachmentMap) {
        for (var entry : attachmentMap.entrySet()) {
            ((AttachmentKey<Object>) entry.getKey()).push(buf, entry.getValue());
        }
    }

    private void pop(FriendlyByteBuf buf, Map<AttachmentKey<?>, ?> attachmentMap) {
        for (var key : attachmentMap.keySet()) {
            key.pop(buf);
        }
    }

    @Override
    public String toString() {
        return "MutReadAttachingStreamCodec[" + keyCodec + " " + attachmentsGetter + " " + wrappedBufferCtor + " " +
            wrappedCodec + " " + keyGetter + "]";
    }
}
