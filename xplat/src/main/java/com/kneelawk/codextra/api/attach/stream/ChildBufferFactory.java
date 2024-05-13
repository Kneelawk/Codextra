package com.kneelawk.codextra.api.attach.stream;

/**
 * Creates a buffer that inherits metadata (like {@link net.minecraft.core.RegistryAccess}) from a parent buffer.
 * <p>
 * Note: Codextra buffer attachments are usually automatically synced separately, unless stated otherwise.
 *
 * @param <B1> the parent buffer type.
 * @param <B2> the child buffer type.
 */
@FunctionalInterface
public interface ChildBufferFactory<B1, B2> {
    /**
     * Create a new, blank buffer with the given initial capacity
     * and with metadata (like {@link net.minecraft.core.RegistryAccess}) from the parent buffer.
     * <p>
     * Buffer attachments will usually automatically be synced separately, unless stated otherwise.
     *
     * @param initialCapacity the initial capacity of the buffer to create, may be 0 if encoding.
     * @param parent          the parent buffer to get metadata from.
     * @return a new, blank buffer.
     */
    B2 create(int initialCapacity, B1 parent);
}
