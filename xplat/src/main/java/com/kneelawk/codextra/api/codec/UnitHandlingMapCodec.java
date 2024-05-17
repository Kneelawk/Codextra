package com.kneelawk.codextra.api.codec;

import java.util.Map;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

/**
 * A {@link MapCodec} that handles missing keys and treats them like units (empty maps).
 *
 * @param <A> the result type.
 */
public class UnitHandlingMapCodec<A> extends MapCodec<A> {
    private final String name;
    private final Codec<A> codec;

    /**
     * Creates a new {@link UnitHandlingMapCodec}.
     *
     * @param name  the field name the codec value is in.
     * @param codec the codec to wrap.
     */
    public UnitHandlingMapCodec(String name, Codec<A> codec) {
        this.name = name;
        this.codec = codec;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return Stream.of(ops.createString(name));
    }

    @Override
    public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
        T value = input.get(name);
        if (value == null) {
            return codec.parse(ops, ops.createMap(Map.of()));
        }

        return codec.parse(ops, value);
    }

    @Override
    public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        DataResult<T> valueRes = codec.encodeStart(ops, input);
        if (valueRes.isError()) {
            return prefix.withErrorsFrom(valueRes);
        }

        T value = valueRes.result().get();

        DataResult<MapLike<T>> mapRes = ops.getMap(value);
        if (mapRes.isSuccess()) {
            MapLike<T> mapValue = mapRes.result().get();
            // don't encode empty maps
            if (mapValue.entries().findAny().isEmpty()) return prefix;
        }

        return prefix.add(name, value);
    }
}
