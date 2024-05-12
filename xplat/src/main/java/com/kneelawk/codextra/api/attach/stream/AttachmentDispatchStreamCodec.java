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
 * A {@link StreamCodec} that retrieves an attachment and determines which codec to use based on that attachment.
 *
 * @param <A> the type of attachment this retrieves.
 * @param <B> the buffer type.
 * @param <R> th type this is a codec for.
 */
public class AttachmentDispatchStreamCodec<A, B extends ByteBuf, R> implements StreamCodec<B, R> {
    private final AttachmentKey<A> key;
    private final Function<? super A, ? extends StreamCodec<? super B, ? extends R>> dispatcher;

    /**
     * Creates a new {@link AttachmentDispatchStreamCodec}.
     *
     * @param key        the key of the attachment to retrieve.
     * @param dispatcher the function to get the codec based on the retrieved attachment.
     */
    public AttachmentDispatchStreamCodec(AttachmentKey<A> key,
                                         Function<? super A, ? extends StreamCodec<? super B, ? extends R>> dispatcher) {
        this.key = key;
        this.dispatcher = dispatcher;
    }

    @Override
    public R decode(B object) {
        A attachment = key.getOrThrow(object);
        StreamCodec<? super B, ? extends R> codec = dispatcher.apply(attachment);
        return codec.decode(object);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void encode(B object, R object2) {
        A attachment = key.getOrThrow(object);
        // intentional cast, as dispatching makes sure the same codec is used for encoding as decoding
        StreamCodec<? super B, R> codec = (StreamCodec<? super B, R>) dispatcher.apply(attachment);
        codec.encode(object, object2);
    }

    @Override
    public String toString() {
        return "AttachmentDispatchStreamCodec[" + key + " " + dispatcher + "]";
    }
}
