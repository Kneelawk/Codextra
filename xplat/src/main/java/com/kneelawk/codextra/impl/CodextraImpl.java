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

package com.kneelawk.codextra.impl;

import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.ByteBuf;

import com.mojang.serialization.DynamicOps;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.DelegatingOps;

import com.kneelawk.codextra.api.attach.AttachmentKey;
import com.kneelawk.codextra.impl.attach.AttachmentManagerImpl;
import com.kneelawk.codextra.impl.attach.AttachmentOps;
import com.kneelawk.codextra.impl.mixin.api.CodextraAttachmentManagerHolder;
import com.kneelawk.codextra.impl.mixin.impl.DelegatingOpsAccessor;

public class CodextraImpl {
    private static final ThreadLocal<AttachmentManagerImpl> STREAM_MANAGER = new ThreadLocal<>();

    public static @Nullable AttachmentManagerImpl streamManager() {
        return STREAM_MANAGER.get();
    }

    public static void putStreamManager(ByteBuf buf) {
        STREAM_MANAGER.set(getAttachmentManager(buf));
    }

    public static void removeStreamManager() {
        STREAM_MANAGER.remove();
    }

    public static <T, R> R wrapWithStreamManager(DynamicOps<T> ops, Function<DynamicOps<T>, R> wrapped) {
        AttachmentManagerImpl manager = STREAM_MANAGER.get();
        AttachmentManagerImpl oldManager = null;
        CodextraAttachmentManagerHolder holder = CodextraImpl.getHolder(ops);
        if (manager != null) {
            if (holder != null) {
                oldManager = holder.codextra_getAttachmentManager();
                holder.codextra_setAttachmentManager(manager);
            } else {
                AttachmentOps<T> newOps = new AttachmentOps<>(ops);
                newOps.codextra_setAttachmentManager(manager);
                ops = newOps;
            }
        }

        try {
            return wrapped.apply(ops);
        } finally {
            if (holder != null && oldManager != null) {
                holder.codextra_setAttachmentManager(oldManager);
            }
        }
    }

    public static <A, T> DynamicOps<T> push(DynamicOps<T> ops, AttachmentKey<A> key, A value) {
        CodextraAttachmentManagerHolder holder = getHolder(ops);
        if (holder == null) {
            ops = new AttachmentOps<>(ops);
            holder = (CodextraAttachmentManagerHolder) ops;
        }

        AttachmentManagerImpl manager = holder.codextra_getAttachmentManager();
        manager.push(key, value);

        return ops;
    }

    public static <A> void push(FriendlyByteBuf buf, AttachmentKey<A> key, A value) {
        CodextraAttachmentManagerHolder holder = (CodextraAttachmentManagerHolder) buf;
        AttachmentManagerImpl manager = holder.codextra_getAttachmentManager();
        manager.push(key, value);
    }

    public static <A> ByteBuf push(ByteBuf buf, AttachmentKey<A> key, A value) {
        CodextraAttachmentManagerHolder holder = getHolder(buf);
        if (holder == null) {
            buf = new FriendlyByteBuf(buf);
            holder = (CodextraAttachmentManagerHolder) buf;
        }

        AttachmentManagerImpl manager = holder.codextra_getAttachmentManager();
        manager.push(key, value);

        return buf;
    }

    public static <A> @Nullable A pop(DynamicOps<?> ops, AttachmentKey<A> key) {
        AttachmentManagerImpl manager = getAttachmentManager(ops);
        if (manager != null) {
            return manager.pop(key);
        }
        return null;
    }

    public static <A> @Nullable A pop(ByteBuf buf, AttachmentKey<A> key) {
        AttachmentManagerImpl manager = getAttachmentManager(buf);
        if (manager != null) {
            return manager.pop(key);
        }
        return null;
    }

    public static @Nullable AttachmentManagerImpl getAttachmentManager(DynamicOps<?> ops) {
        CodextraAttachmentManagerHolder holder = getHolder(ops);
        if (holder == null) return null;
        return holder.codextra_getAttachmentManager();
    }

    public static @Nullable AttachmentManagerImpl getAttachmentManager(ByteBuf buf) {
        CodextraAttachmentManagerHolder holder = getHolder(buf);
        if (holder == null) return null;
        return holder.codextra_getAttachmentManager();
    }

    private static @Nullable CodextraAttachmentManagerHolder getHolder(DynamicOps<?> ops) {
        if (ops instanceof CodextraAttachmentManagerHolder holder) return holder;

        // check the delegates of delegating ops, just in case someone wrapped our AttachmentOps
        while (ops instanceof DelegatingOps<?>) {
            ops = ((DelegatingOpsAccessor) ops).codextra_getDelegate();

            if (ops instanceof CodextraAttachmentManagerHolder holder) return holder;
        }

        return null;
    }

    private static @Nullable CodextraAttachmentManagerHolder getHolder(ByteBuf buf) {
        if (buf instanceof CodextraAttachmentManagerHolder holder) return holder;
        return null;
    }
}
