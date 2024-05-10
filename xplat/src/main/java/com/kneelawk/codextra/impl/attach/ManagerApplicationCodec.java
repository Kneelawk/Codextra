package com.kneelawk.codextra.impl.attach;

import java.util.function.Function;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import com.kneelawk.codextra.impl.CodextraImpl;
import com.kneelawk.codextra.impl.mixin.api.CodextraAttachmentManagerHolder;

public class ManagerApplicationCodec<R> implements Codec<R> {
    private final Codec<R> wrapped;

    public ManagerApplicationCodec(Codec<R> wrapped) {this.wrapped = wrapped;}

    @Override
    public <T> DataResult<Pair<R, T>> decode(DynamicOps<T> ops, T input) {
        return wrap(ops, newOps -> wrapped.decode(newOps, input));
    }

    @Override
    public <T> DataResult<T> encode(R input, DynamicOps<T> ops, T prefix) {
        return wrap(ops, newOps -> wrapped.encode(input, newOps, prefix));
    }

    private static <T, R> R wrap(DynamicOps<T> ops, Function<DynamicOps<T>, R> func) {
        AttachmentManagerImpl manager = CodextraImpl.streamManager();

        AttachmentManagerImpl oldManager = null;
        CodextraAttachmentManagerHolder holder = CodextraImpl.getHolder(ops);
        if (!manager.isEmpty()) {
            if (holder != null) {
                oldManager = holder.codextra_getAttachmentManager();
                holder.codextra_setAttachmentManager(manager);
            } else {
                AttachmentOps<T> newOps = new AttachmentOps<>(ops);
                newOps.codextra_setAttachmentManager(manager);
                ops = newOps;
            }
        }

        R result = func.apply(ops);

        if (holder != null && oldManager != null) {
            holder.codextra_setAttachmentManager(oldManager);
        }

        return result;
    }
}
