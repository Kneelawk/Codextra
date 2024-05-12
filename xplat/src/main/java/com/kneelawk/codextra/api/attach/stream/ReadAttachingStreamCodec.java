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

import java.util.function.Function;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import com.kneelawk.codextra.api.attach.AttachmentKey;

/**
 * A {@link StreamCodec} that decodes one value and then attaches it to the context when decoding the result value.
 *
 * @param <A> the attachment type.
 * @param <B> the buffer type.
 * @param <V> the result type.
 */
public class ReadAttachingStreamCodec<A, B extends FriendlyByteBuf, V> implements StreamCodec<B, V> {
    private final AttachmentKey<A> key;
    private final StreamCodec<? super B, A> keyCodec;
    private final StreamCodec<? super B, V> wrappedCodec;
    private final Function<? super V, ? extends A> attachmentGetter;

    /**
     * Creates a new {@link ReadAttachingStreamCodec}.
     *
     * @param key              the attachment key.
     * @param keyCodec         the codec for decoding the attachment.
     * @param wrappedCodec     the codec that is invoked with the attachment attached.
     * @param attachmentGetter a function for getting the attachment when given the result type.
     */
    public ReadAttachingStreamCodec(AttachmentKey<A> key, StreamCodec<? super B, A> keyCodec,
                                    StreamCodec<? super B, V> wrappedCodec,
                                    Function<? super V, ? extends A> attachmentGetter) {
        this.key = key;
        this.keyCodec = keyCodec;
        this.wrappedCodec = wrappedCodec;
        this.attachmentGetter = attachmentGetter;
    }

    @Override
    public V decode(B object) {
        A attachment = keyCodec.decode(object);

        key.push(object, attachment);
        try {
            return wrappedCodec.decode(object);
        } finally {
            key.pop(object);
        }
    }

    @Override
    public void encode(B object, V object2) {
        A attachment = attachmentGetter.apply(object2);

        keyCodec.encode(object, attachment);

        key.push(object, attachment);
        try {
            wrappedCodec.encode(object, object2);
        } finally {
            key.pop(object);
        }
    }

    @Override
    public String toString() {
        return "ReadAttachingStreamCodec[" + key + " " + keyCodec + " " + wrappedCodec + " " + attachmentGetter + "]";
    }
}
