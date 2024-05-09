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

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import com.kneelawk.codextra.api.attach.AttachmentKey;

/**
 * A {@link StreamCodec} for attaching a value and passing it as context to the wrapped codec.
 *
 * @param <B> the buffer type.
 * @param <V> the result type.
 */
public class AttachingStreamCodec<B extends FriendlyByteBuf, V> implements StreamCodec<B, V> {
    private final Map<AttachmentKey<?>, ?> attachmentMap;
    private final StreamCodec<? super B, V> wrapped;

    /**
     * Creates a new {@link AttachingStreamCodec}.
     *
     * @param key     the attachment key.
     * @param value   the value to attach.
     * @param wrapped the stream codec to pass the attachment to.
     * @param <A>     the attachment type.
     * @param <B>     the buffer type.
     * @param <V>     the result type.
     * @return the created stream codec.
     */
    public static <A, B extends FriendlyByteBuf, V> AttachingStreamCodec<B, V> single(AttachmentKey<A> key, A value,
                                                                                      StreamCodec<? super B, V> wrapped) {
        return new AttachingStreamCodec<>(Map.of(key, value), wrapped);
    }

    /**
     * Creates a new {@link AttachingStreamCodec}.
     *
     * @param attachmentMap the map of attachments to attach.
     * @param wrapped       the stream codec to pass the attachments to.
     */
    public AttachingStreamCodec(Map<AttachmentKey<?>, ?> attachmentMap, StreamCodec<? super B, V> wrapped) {
        this.attachmentMap = attachmentMap;
        this.wrapped = wrapped;
    }

    @Override
    public V decode(B object) {
        push(object);
        try {
            return wrapped.decode(object);
        } finally {
            pop(object);
        }
    }

    @Override
    public void encode(B object, V object2) {
        push(object);
        try {
            wrapped.encode(object, object2);
        } finally {
            pop(object);
        }
    }

    @SuppressWarnings("unchecked")
    private void push(FriendlyByteBuf buf) {
        for (var entry : attachmentMap.entrySet()) {
            ((AttachmentKey<Object>) entry.getKey()).push(buf, entry.getValue());
        }
    }

    private void pop(FriendlyByteBuf buf) {
        for (var key : attachmentMap.keySet()) {
            key.pop(buf);
        }
    }
}
