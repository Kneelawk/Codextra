package com.kneelawk.codextra.api.codec;

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
     * Creates a new {@link OpsReplacingRecordBuilder}.
     *
     * @param ops      the ops to use instead of the delegate's ops.
     * @param delegate the delegate.
     */
    public OpsReplacingRecordBuilder(DynamicOps<T> ops, RecordBuilder<T> delegate) {
        this.ops = ops;
        this.delegate = delegate;
    }

    /**
     * Gets this record builder's delegate.
     *
     * @return this record builder's delegate.
     */
    public RecordBuilder<T> unwrap() {
        return delegate;
    }

    /**
     * Sets this record builder's ops.
     *
     * @param ops the new ops.
     */
    public void setOps(DynamicOps<T> ops) {
        this.ops = ops;
    }

    private RecordBuilder<T> res(RecordBuilder<T> res) {
        if (res == delegate) return this;
        if (res instanceof OpsReplacingRecordBuilder<T> replacing) {

        }
        return new OpsReplacingRecordBuilder<>(ops, res);
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
