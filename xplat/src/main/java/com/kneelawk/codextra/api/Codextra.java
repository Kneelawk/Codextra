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

package com.kneelawk.codextra.api;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;

import com.kneelawk.codextra.api.codec.ErrorHandlingMapCodec;
import com.kneelawk.codextra.api.codec.KeyCheckingMapCodec;
import com.kneelawk.codextra.api.codec.MapKeyDispatchCodec;

/**
 * Codextra utility root class.
 * <p>
 * For Ops-Attachments, see {@link com.kneelawk.codextra.api.attach.AttachmentKey}.
 */
public final class Codextra {
    private Codextra() {}

    /**
     * Creates a new {@link MapCodec} that catches decoding errors and logs them.
     *
     * @param wrapped     the codec to catch decoding errors from.
     * @param errorLogger the logger to log decoding errors to.
     * @param <R>         the result type.
     * @return the created map codec.
     */
    public static <R> MapCodec<Optional<R>> errorHandlingMapCodec(MapCodec<R> wrapped, Consumer<String> errorLogger) {
        return new ErrorHandlingMapCodec<>(wrapped, errorLogger);
    }

    /**
     * Creates a new {@link MapCodec} that checks for the presence of the given keys before attempting to decode.
     *
     * @param requiredKeys the keys required before decoding can happen.
     * @param wrapped      the map codec that does the decoding.
     * @param <O>          the result type.
     * @return the created map codec.
     */
    public static <O> MapCodec<Optional<O>> keyCheckingMapCodec(Collection<String> requiredKeys, MapCodec<O> wrapped) {
        return new KeyCheckingMapCodec<>(requiredKeys, wrapped);
    }

    /**
     * Creates a new {@link MapCodec} that dispatches based on the value decoded from a different map codec.
     * <p>
     * This is similar to the {@link com.mojang.serialization.codecs.KeyDispatchCodec} but allows for a map-codec key.
     *
     * @param keyCodec  the codec of the key type.
     * @param keyGetter a function for retrieving the key when given a value.
     * @param codec     a function for retrieving the value codec when given a key.
     * @param <K>       the key type.
     * @param <V>       the value type.
     * @return the created map codec.
     */
    public static <K, V> MapCodec<V> mapKeyDispatchCodecResult(MapCodec<K> keyCodec,
                                                               Function<? super V, ? extends DataResult<? extends K>> keyGetter,
                                                               Function<? super K, ? extends DataResult<? extends MapCodec<? extends V>>> codec) {
        return new MapKeyDispatchCodec<>(keyCodec, keyGetter, codec);
    }

    /**
     * Creates a new {@link MapCodec} that dispatches based on the value decoded from a different map codec.
     * <p>
     * This is similar to the {@link com.mojang.serialization.codecs.KeyDispatchCodec} but allows for a map-codec key.
     *
     * @param keyCodec  the codec of the key type.
     * @param keyGetter a function for retrieving the key when given a value.
     * @param codec     a function for retrieving the value codec when given a key.
     * @param <K>       the key type.
     * @param <V>       the value type.
     * @return the created map codec.
     */
    public static <K, V> MapCodec<V> mapKeyDispatchCodec(MapCodec<K> keyCodec,
                                                         Function<? super V, ? extends K> keyGetter,
                                                         Function<? super K, ? extends MapCodec<? extends V>> codec) {
        return mapKeyDispatchCodecResult(keyCodec, keyGetter.andThen(DataResult::success),
            codec.andThen(DataResult::success));
    }
}
