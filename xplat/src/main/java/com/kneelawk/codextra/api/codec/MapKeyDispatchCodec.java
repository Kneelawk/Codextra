package com.kneelawk.codextra.api.codec;

import java.util.function.Function;
import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

/**
 * Version of {@link com.mojang.serialization.codecs.KeyDispatchCodec} that takes a {@link MapCodec} as its key.
 *
 * @param <K> the key type.
 * @param <V> the value type.
 */
public class MapKeyDispatchCodec<K, V> extends MapCodec<V> {
    private static final String COMPRESSED_VALUE_KEY = "codextra_dispatch_value";
    private final MapCodec<K> keyCodec;
    private final Function<? super V, ? extends DataResult<? extends K>> keyGetter;
    private final Function<? super K, ? extends DataResult<? extends MapDecoder<? extends V>>> decoder;
    private final Function<? super V, ? extends DataResult<? extends MapEncoder<V>>> encoder;

    /**
     * Creates a new {@link MapKeyDispatchCodec}.
     *
     * @param keyCodec  the codec of the key type.
     * @param keyGetter a function for retrieving the key when given a value.
     * @param decoder   a function for retrieving the value decoder when given a key.
     * @param encoder   a function for retrieving the value encoder when given a value.
     */
    public MapKeyDispatchCodec(MapCodec<K> keyCodec,
                               Function<? super V, ? extends DataResult<? extends K>> keyGetter,
                               Function<? super K, ? extends DataResult<? extends MapDecoder<? extends V>>> decoder,
                               Function<? super V, ? extends DataResult<? extends MapEncoder<V>>> encoder) {
        this.keyCodec = keyCodec;
        this.keyGetter = keyGetter;
        this.decoder = decoder;
        this.encoder = encoder;
    }

    /**
     * Creates a new {@link MapKeyDispatchCodec}.
     *
     * @param keyCodec  the codec of the key type.
     * @param keyGetter a function for retrieving the key when given a value.
     * @param codec     a function for retrieving the value codec when given a key.
     */
    public MapKeyDispatchCodec(MapCodec<K> keyCodec,
                               Function<? super V, ? extends DataResult<? extends K>> keyGetter,
                               Function<? super K, ? extends DataResult<? extends MapCodec<? extends V>>> codec) {
        this(keyCodec, keyGetter, codec, v -> getCodec(keyGetter, codec, v));
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return Stream.concat(keyCodec.keys(ops), Stream.of(ops.createString(COMPRESSED_VALUE_KEY)));
    }

    @Override
    public <T> DataResult<V> decode(DynamicOps<T> ops, MapLike<T> input) {
        return keyCodec.decode(ops, input).flatMap(keyValue -> decoder.apply(keyValue).flatMap(valueDecoder -> {
            if (ops.compressMaps()) {
                T valueHolder = input.get(COMPRESSED_VALUE_KEY);
                if (valueHolder == null) {
                    return DataResult.error(
                        () -> "MapKeyDispatchCodec missing value entry '" + COMPRESSED_VALUE_KEY + "'. Input: " +
                            input);
                }
                return valueDecoder.decoder().parse(ops, valueHolder).map(Function.identity());
            }
            return valueDecoder.decode(ops, input).map(Function.identity());
        }));
    }

    @Override
    public <T> RecordBuilder<T> encode(V input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        final DataResult<? extends MapEncoder<V>> encoderResult = encoder.apply(input);
        final DataResult<? extends K> keyResult = keyGetter.apply(input);
        if (encoderResult.isError() || keyResult.isError()) {
            return prefix.withErrorsFrom(encoderResult).withErrorsFrom(keyResult);
        }

        final MapEncoder<V> elementEncoder = encoderResult.result().get();
        final K keyValue = keyResult.result().get();
        if (ops.compressMaps()) {
            return keyCodec.encode(keyValue, ops, prefix)
                .add(COMPRESSED_VALUE_KEY, elementEncoder.encoder().encodeStart(ops, input));
        }

        return elementEncoder.encode(input, ops, keyCodec.encode(keyValue, ops, prefix));
    }

    @SuppressWarnings("unchecked")
    private static <K, V> DataResult<? extends MapEncoder<V>> getCodec(
        Function<? super V, ? extends DataResult<? extends K>> type,
        Function<? super K, ? extends DataResult<? extends MapEncoder<? extends V>>> encoder, V input) {
        return type.apply(input).flatMap(key -> encoder.apply(key).map(Function.identity()))
            .map(c -> ((MapEncoder<V>) c));
    }

    @Override
    public String toString() {
        return "CustomKeyDispatchCodec[" + keyCodec + " " + keyGetter + " " + decoder + "]";
    }
}
