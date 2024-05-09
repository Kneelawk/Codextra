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

import java.util.function.BiFunction;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;

import com.kneelawk.codextra.api.attach.AttachmentKey;

/**
 * A {@link StreamCodec} that decodes a value and combines it with an attachment to create a result.
 *
 * @param <A> the attachment type.
 * @param <B> the buffer type.
 * @param <O> the type the attachment is combined with.
 * @param <R> the result type.
 */
public class RetrieveWithStreamCodec<A, B extends ByteBuf, O, R> implements StreamCodec<B, R> {
    private final AttachmentKey<A> key;
    private final StreamCodec<? super B, O> withCodec;
    private final BiFunction<? super A, ? super O, ? extends R> retriever;
    private final BiFunction<? super A, ? super R, ? extends O> reverse;

    /**
     * Creates a new {@link RetrieveWithStreamCodec}.
     *
     * @param key       the attachment key.
     * @param withCodec the codec for the value to be combined with the attachment.
     * @param retriever the function that combines the attachment with the decoded value.
     * @param reverse   the function that gets the value to be encoded when given the attachment and the combined value.
     */
    public RetrieveWithStreamCodec(AttachmentKey<A> key, StreamCodec<? super B, O> withCodec,
                                   BiFunction<? super A, ? super O, ? extends R> retriever,
                                   BiFunction<? super A, ? super R, ? extends O> reverse) {
        this.key = key;
        this.withCodec = withCodec;
        this.retriever = retriever;
        this.reverse = reverse;
    }

    @Override
    public R decode(B object) {
        A attachment = key.getOrThrow(object);
        O with = withCodec.decode(object);
        return retriever.apply(attachment, with);
    }

    @Override
    public void encode(B object, R object2) {
        A attachment = key.getOrThrow(object);
        O with = reverse.apply(attachment, object2);
        withCodec.encode(object, with);
    }
}
