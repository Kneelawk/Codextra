package com.kneelawk.codextra.api;

import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.network.codec.StreamCodec;

/**
 * Codextra Stream codec utility root class.
 * <p>
 * For attachments, see {@link com.kneelawk.codextra.api.attach.AttachmentKey}.
 */
public final class CodextraStreams {
    private CodextraStreams() {}

    /**
     * Creates a {@link StreamCodec} that returns the supplied value when decoding and does nothing when encoding.
     *
     * @param supplier the value supplier.
     * @param <B>      the buffer type.
     * @param <V>      the value type.
     * @return the unit stream codec.
     */
    public static <B, V> StreamCodec<B, V> unit(Supplier<V> supplier) {
        return new StreamCodec<>() {
            @Override
            public V decode(B object) {
                return supplier.get();
            }

            @Override
            public void encode(B object, V object2) {
                // do nothing here
            }
        };
    }

    /**
     * Version of {@link StreamCodec#dispatch(Function, Function)} that accepts more specific buffer types.
     *
     * @param keyCodec    the key codec.
     * @param keyGetter   the key getter.
     * @param codecGetter gets the value codec for a given key.
     * @param <B>         the buffer type.
     * @param <K>         the key type.
     * @param <V>         the value type.
     * @return the created stream codec.
     */
    @SuppressWarnings("unchecked")
    public static <B, K, V> StreamCodec<B, V> dispatch(StreamCodec<? super B, K> keyCodec,
                                                       Function<? super V, ? extends K> keyGetter,
                                                       Function<? super K, ? extends StreamCodec<? super B, ? extends V>> codecGetter) {
        return new StreamCodec<B, V>() {
            @Override
            public V decode(B buf) {
                K key = keyCodec.decode(buf);
                StreamCodec<? super B, ? extends V> valueCodec = codecGetter.apply(key);
                return valueCodec.decode(buf);
            }

            @Override
            public void encode(B buf, V input) {
                K key = keyGetter.apply(input);
                StreamCodec<? super B, ? extends V> valueCodec = codecGetter.apply(key);
                keyCodec.encode(buf, key);
                ((StreamCodec<? super B, V>) valueCodec).encode(buf, input);
            }
        };
    }
}
