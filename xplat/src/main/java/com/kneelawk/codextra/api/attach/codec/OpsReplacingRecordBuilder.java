package com.kneelawk.codextra.api.attach.codec;

import java.util.function.UnaryOperator;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.RecordBuilder;

/**
 * {@link RecordBuilder} that delegates to another record builder but that replaces the ops.
 *
 * @param <T> the type this record builder builds.
 */
public class OpsReplacingRecordBuilder<T> implements RecordBuilder<T> {
    private DynamicOps<T> ops;
    private final RecordBuilder<T> delegate;

    /**
     * Wraps the given builder, so that it uses the given ops.
     *
     * @param delegate the registry builder to wrap.
     * @param newOps   the ops to use instead of the delegates ops.
     * @param <T>      the type this record builder builds.
     * @return the wrapper record builder, with the given ops.
     */
    public static <T> RecordBuilder<T> wrap(RecordBuilder<T> delegate, DynamicOps<T> newOps) {
        if (delegate instanceof OpsReplacingRecordBuilder<T> replacing) {
            replacing.setOps(newOps);
            return replacing;
        } else {
            return new OpsReplacingRecordBuilder<>(delegate, newOps);
        }
    }

    /**
     * Unwraps the given builder if it is an ops-replacing builder.
     *
     * @param wrapped     the builder returned by {@link com.mojang.serialization.MapCodec#encode(Object, DynamicOps, RecordBuilder)}.
     * @param original    the original builder before it got wrapped.
     * @param originalOps the original ops before it was changed.
     * @param <T>         the type this record builder builds.
     * @return the unwrapped builder.
     */
    public static <T> RecordBuilder<T> unwrap(RecordBuilder<T> wrapped, RecordBuilder<T> original,
                                              DynamicOps<T> originalOps) {
        if (wrapped instanceof OpsReplacingRecordBuilder<T> replacing) {
            if (wrapped == original) {
                replacing.setOps(originalOps);
                return replacing;
            } else {
                return replacing.unwrap();
            }
        }
        return wrapped;
    }

    /**
     * Creates a new {@link OpsReplacingRecordBuilder}.
     *
     * @param delegate the delegate.
     * @param ops      the ops to use instead of the delegate's ops.
     */
    private OpsReplacingRecordBuilder(RecordBuilder<T> delegate, DynamicOps<T> ops) {
        this.ops = ops;
        this.delegate = delegate;
    }

    /**
     * Gets this record builder's delegate.
     *
     * @return this record builder's delegate.
     */
    private RecordBuilder<T> unwrap() {
        return delegate;
    }

    /**
     * Sets this record builder's ops.
     *
     * @param ops the new ops.
     */
    private void setOps(DynamicOps<T> ops) {
        this.ops = ops;
    }

    private RecordBuilder<T> res(RecordBuilder<T> res) {
        if (res == delegate) return this;
        if (res instanceof OpsReplacingRecordBuilder<T> replacing) {
            replacing.setOps(ops);
            return replacing;
        }
        return new OpsReplacingRecordBuilder<>(res, ops);
    }

    @Override
    public DynamicOps<T> ops() {
        return ops;
    }

    @Override
    public RecordBuilder<T> add(T key, T value) {
        return res(delegate.add(key, value));
    }

    @Override
    public RecordBuilder<T> add(T key, DataResult<T> value) {
        return res(delegate.add(key, value));
    }

    @Override
    public RecordBuilder<T> add(DataResult<T> key, DataResult<T> value) {
        return res(delegate.add(key, value));
    }

    @Override
    public RecordBuilder<T> withErrorsFrom(DataResult<?> result) {
        return res(delegate.withErrorsFrom(result));
    }

    @Override
    public RecordBuilder<T> setLifecycle(Lifecycle lifecycle) {
        return res(delegate.setLifecycle(lifecycle));
    }

    @Override
    public RecordBuilder<T> mapError(UnaryOperator<String> onError) {
        return res(delegate.mapError(onError));
    }

    @Override
    public DataResult<T> build(T prefix) {
        return delegate.build(prefix);
    }

    @Override
    public DataResult<T> build(DataResult<T> prefix) {
        return delegate.build(prefix);
    }
}
