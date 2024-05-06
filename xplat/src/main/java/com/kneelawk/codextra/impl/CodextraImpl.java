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

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.ByteBuf;

import com.mojang.serialization.DynamicOps;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.DelegatingOps;

import com.kneelawk.codextra.api.attach.CodecAttachment;
import com.kneelawk.codextra.impl.attach.AttachmentManager;
import com.kneelawk.codextra.impl.attach.AttachmentOps;
import com.kneelawk.codextra.impl.mixin.api.CodextraAttachmentManagerHolder;
import com.kneelawk.codextra.impl.mixin.impl.DelegatingOpsAccessor;

public class CodextraImpl {
    public static <A, T> DynamicOps<T> push(DynamicOps<T> ops, CodecAttachment<A> key, A value) {
        CodextraAttachmentManagerHolder holder = getHolder(ops);
        if (holder == null) {
            ops = new AttachmentOps<>(ops);
            holder = (CodextraAttachmentManagerHolder) ops;
        }

        AttachmentManager manager = holder.codextra_getAttachmentManager();
        manager.push(key, value);

        return ops;
    }

    public static <A> void push(FriendlyByteBuf buf, CodecAttachment<A> key, A value) {
        CodextraAttachmentManagerHolder holder = (CodextraAttachmentManagerHolder) buf;
        AttachmentManager manager = holder.codextra_getAttachmentManager();
        manager.push(key, value);
    }

    public static <A> ByteBuf push(ByteBuf buf, CodecAttachment<A> key, A value) {
        CodextraAttachmentManagerHolder holder = getHolder(buf);
        if (holder == null) {
            buf = new FriendlyByteBuf(buf);
            holder = (CodextraAttachmentManagerHolder) buf;
        }

        AttachmentManager manager = holder.codextra_getAttachmentManager();
        manager.push(key, value);

        return buf;
    }

    public static <A> @Nullable A pop(DynamicOps<?> ops, CodecAttachment<A> key) {
        AttachmentManager manager = getAttachmentManager(ops);
        if (manager != null) {
            return manager.pop(key);
        }
        return null;
    }

    public static <A> @Nullable A pop(ByteBuf buf, CodecAttachment<A> key) {
        AttachmentManager manager = getAttachmentManager(buf);
        if (manager != null) {
            return manager.pop(key);
        }
        return null;
    }

    public static @Nullable AttachmentManager getAttachmentManager(DynamicOps<?> ops) {
        CodextraAttachmentManagerHolder holder = getHolder(ops);
        if (holder == null) return null;
        return holder.codextra_getAttachmentManager();
    }

    public static @Nullable AttachmentManager getAttachmentManager(ByteBuf buf) {
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
