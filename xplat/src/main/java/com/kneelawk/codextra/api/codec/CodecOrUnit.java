package com.kneelawk.codextra.api.codec;

import java.util.Optional;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

/**
 * Type that maybe holds a codec or maybe holds a supplier.
 *
 * @param <A> the type this decodes/encodes.
 */
public sealed interface CodecOrUnit<A> {
    /**
     * Creates a unit.
     *
     * @param supplier the unit supplier.
     * @param <A>      the supplied type.
     * @return the created unit.
     */
    static <A> CodecOrUnit<A> unit(Supplier<A> supplier) {
        return new Unit<>(supplier);
    }

    /**
     * Creates a unit.
     *
     * @param value the value to supply.
     * @param <A>   the supplied type.
     * @return the created unit.
     */
    static <A> CodecOrUnit<A> unit(A value) {
        return unit(() -> value);
    }

    /**
     * Creates a codec wrapper.
     *
     * @param codec the codec to wrap.
     * @param <A>   the codec type.
     * @return the created codec wrapper.
     */
    static <A> CodecOrUnit<A> codec(Codec<A> codec) {
        return new Wrapper<>(codec);
    }

    /**
     * Creates a map codec that reads the given field.
     *
     * @param name the field to read.
     * @return the map codec.
     */
    MapCodec<A> fieldOf(String name);

    /**
     * Creates a map codec that reads the given field or returns {@link Optional#empty()}.
     *
     * @param name the field to read.
     * @return the map codec.
     */
    MapCodec<Optional<A>> optionalFieldOf(String name);

    /**
     * Creates a map codec that reads the given field or returns the default value.
     *
     * @param name         the field to read.
     * @param defaultValue the default value.
     * @return the map codec.
     */
    MapCodec<A> optionalFieldOf(String name, A defaultValue);

    /**
     * Creates a map codec that reads the given field or returns {@link Optional#empty()} if an error occurs.
     *
     * @param name the field to read.
     * @return the map codec.
     */
    MapCodec<Optional<A>> lenientOptionalFieldOf(String name);

    /**
     * Creates a map codec that reads the given field or returns the default value if an error occurs.
     *
     * @param name         the field to read.
     * @param defaultValue the default value.
     * @return the map codec.
     */
    MapCodec<A> lenientOptionalFieldOf(String name, A defaultValue);

    /**
     * Unit type, encodes nothing, returns supplied value.
     *
     * @param supplier the value to return.
     * @param <A>      the type to return.
     */
    record Unit<A>(Supplier<A> supplier) implements CodecOrUnit<A> {

        @Override
        public MapCodec<A> fieldOf(String name) {
            return MapCodec.unit(supplier);
        }

        @Override
        public MapCodec<Optional<A>> optionalFieldOf(String name) {
            return MapCodec.unit(() -> Optional.of(supplier.get()));
        }

        @Override
        public MapCodec<A> optionalFieldOf(String name, A defaultValue) {
            return MapCodec.unit(supplier);
        }

        @Override
        public MapCodec<Optional<A>> lenientOptionalFieldOf(String name) {
            return MapCodec.unit(() -> Optional.of(supplier.get()));
        }

        @Override
        public MapCodec<A> lenientOptionalFieldOf(String name, A defaultValue) {
            return MapCodec.unit(supplier);
        }
    }

    /**
     * Wrapper type, contains a codec.
     *
     * @param codec the contained codec.
     * @param <A>   the type to decode/encode.
     */
    record Wrapper<A>(Codec<A> codec) implements CodecOrUnit<A> {

        @Override
        public MapCodec<A> fieldOf(String name) {
            return codec.fieldOf(name);
        }

        @Override
        public MapCodec<Optional<A>> optionalFieldOf(String name) {
            return codec.optionalFieldOf(name);
        }

        @Override
        public MapCodec<A> optionalFieldOf(String name, A defaultValue) {
            return codec.optionalFieldOf(name, defaultValue);
        }

        @Override
        public MapCodec<Optional<A>> lenientOptionalFieldOf(String name) {
            return codec.lenientOptionalFieldOf(name);
        }

        @Override
        public MapCodec<A> lenientOptionalFieldOf(String name, A defaultValue) {
            return codec.lenientOptionalFieldOf(name, defaultValue);
        }
    }
}
