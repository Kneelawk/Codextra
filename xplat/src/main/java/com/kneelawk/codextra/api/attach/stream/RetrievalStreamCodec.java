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

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;

import com.kneelawk.codextra.api.attach.AttachmentKey;

/**
 * A {@link StreamCodec} that retrieves an attachment from the decoding context.
 *
 * @param <A> the attachment type.
 * @param <B> the buffer type.
 * @param <V> the result type.
 */
public class RetrievalStreamCodec<A, B extends ByteBuf, V> implements StreamCodec<B, V> {
    private final AttachmentKey<A> key;
    private final Function<? super A, ? extends V> retriever;

    /**
     * Creates a new {@link RetrievalStreamCodec}.
     *
     * @param key       the attachment key to look up.
     * @param retriever the function that gets the desired value from the attachment.
     */
    public RetrievalStreamCodec(AttachmentKey<A> key, Function<? super A, ? extends V> retriever) {
        this.key = key;
        this.retriever = retriever;
    }

    @Override
    public V decode(B object) {
        return retriever.apply(key.getOrThrow(object));
    }

    @Override
    public void encode(B object, V object2) {
    }
}
