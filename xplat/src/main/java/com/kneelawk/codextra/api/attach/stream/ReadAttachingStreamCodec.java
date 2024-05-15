/*
 * MIT License
 *
 * Copyright (c) 2024 Cyan Kneelawk.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.kneelawk.codextra.api.attach.stream;

import java.util.Map;
import java.util.function.Function;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import com.kneelawk.codextra.api.attach.AttachmentKey;

/**
 * A {@link StreamCodec} that decodes one value and uses it to create attachments to attach to the context when
 * decoding the result value.
 *
 * @param <B> the buffer type.
 * @param <K> the key type.
 * @param <V> the result type.
 */
public class ReadAttachingStreamCodec<B extends FriendlyByteBuf, K, V> implements StreamCodec<B, V> {
    private final StreamCodec<? super B, K> keyCodec;
    private final Function<? super K, ? extends Map<AttachmentKey<?>, ?>> attachmentsGetter;
    private final StreamCodec<? super B, V> wrappedCodec;
    private final Function<? super V, ? extends K> keyGetter;

    /**
     * Creates a new {@link ReadAttachingStreamCodec} that uses its key as its single attachment.
     *
     * @param key          the attachment key.
     * @param keyCodec     the codec for the attachment.
     * @param wrappedCodec the codec to pass the attachment to.
     * @param keyGetter    the function to get the key from the result.
     * @param <A>          the attachment type.
     * @param <B>          the buffer type.
     * @param <V>          the result type.
     * @return the new stream codec.
     */
    public static <A, B extends FriendlyByteBuf, V> ReadAttachingStreamCodec<B, A, V> single(AttachmentKey<A> key,
                                                                                             StreamCodec<? super B, A> keyCodec,
                                                                                             StreamCodec<? super B, V> wrappedCodec,
                                                                                             Function<? super V, ? extends A> keyGetter) {
        return new ReadAttachingStreamCodec<>(keyCodec, a -> Map.of(key, a), wrappedCodec, keyGetter);
    }

    /**
     * Creates a new {@link ReadAttachingStreamCodec}.
     *
     * @param keyCodec          the codec for the key to be turned into attachments.
     * @param attachmentsGetter the function to turn the key into attachments.
     * @param wrappedCodec      the codec to pass the attachments to.
     * @param keyGetter         the function to get the key back from the result type.
     */
    public ReadAttachingStreamCodec(StreamCodec<? super B, K> keyCodec,
                                    Function<? super K, ? extends Map<AttachmentKey<?>, ?>> attachmentsGetter,
                                    StreamCodec<? super B, V> wrappedCodec,
                                    Function<? super V, ? extends K> keyGetter) {
        this.keyCodec = keyCodec;
        this.attachmentsGetter = attachmentsGetter;
        this.wrappedCodec = wrappedCodec;
        this.keyGetter = keyGetter;
    }

    @Override
    public V decode(B object) {
        K key = keyCodec.decode(object);

        Map<AttachmentKey<?>, ?> attachmentMap = attachmentsGetter.apply(key);

        push(object, attachmentMap);
        try {
            return wrappedCodec.decode(object);
        } finally {
            pop(object, attachmentMap);
        }
    }

    @Override
    public void encode(B object, V object2) {
        K key = keyGetter.apply(object2);

        Map<AttachmentKey<?>, ?> attachmentMap = attachmentsGetter.apply(key);

        keyCodec.encode(object, key);

        push(object, attachmentMap);
        try {
            wrappedCodec.encode(object, object2);
        } finally {
            pop(object, attachmentMap);
        }
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
        return "ReadAttachingStreamCodec[" + keyCodec + " " + attachmentsGetter + " " + wrappedCodec + " " + keyGetter +
            "]";
    }
}
