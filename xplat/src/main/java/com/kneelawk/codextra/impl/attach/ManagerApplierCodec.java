package com.kneelawk.codextra.impl.attach;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import com.kneelawk.codextra.impl.CodextraImpl;

public class ManagerApplierCodec<R> implements Codec<R> {
    private final Codec<R> wrapped;

    public ManagerApplierCodec(Codec<R> wrapped) {this.wrapped = wrapped;}

    @Override
    public <T> DataResult<Pair<R, T>> decode(DynamicOps<T> ops, T input) {
        return CodextraImpl.wrapWithStreamManager(ops, newOps -> wrapped.decode(newOps, input));
    }

    @Override
    public <T> DataResult<T> encode(R input, DynamicOps<T> ops, T prefix) {
        return CodextraImpl.wrapWithStreamManager(ops, newOps -> wrapped.encode(input, newOps, prefix));
    }
}
