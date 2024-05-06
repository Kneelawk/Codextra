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

package com.kneelawk.codextra.api.attach;

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.ByteBuf;

import com.mojang.serialization.DynamicOps;

import net.minecraft.network.FriendlyByteBuf;

import com.kneelawk.codextra.impl.CodextraImpl;
import com.kneelawk.codextra.impl.attach.AttachmentManager;

/**
 * A key for a kind of thing that can be attached to a {@link DynamicOps} or {@link FriendlyByteBuf}.
 * <p>
 * Attachments work like stacks, allowing you to push and
 *
 * @param <A> the type of thing this attaches.
 */
@SuppressWarnings("unused")
public class CodecAttachment<A> {
    private final String name;

    /**
     * Creates a new codec attachment key.
     *
     * @param name the name of this attachment. This name is only used in printing error messages.
     * @param <A>  the type this attachment attaches.
     */
    public static <A> CodecAttachment<A> of(String name) {
        return new CodecAttachment<>(name);
    }

    private CodecAttachment(String name) {this.name = name;}

    /**
     * Gets this attachment's name.
     *
     * @return this attachment's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Pushes an attachment value to the given {@link DynamicOps}.
     * <p>
     * Note: this <em>may</em> wrap the given ops and return the wrapped ops if the original ops did not support
     * attachments. Always use the returned ops.
     *
     * @param ops   the dynamic ops to attach to and possibly wrap.
     * @param value the value to attach.
     * @param <T>   the type of the dynamic ops.
     * @return the dynamic ops with the value attached.
     */
    public <T> DynamicOps<T> push(DynamicOps<T> ops, A value) {
        return CodextraImpl.push(ops, this, value);
    }

    /**
     * Attaches a value to the given {@link FriendlyByteBuf}.
     *
     * @param buf   the buffer to attach to.
     * @param value the value to attach.
     */
    public void push(FriendlyByteBuf buf, A value) {
        CodextraImpl.push(buf, this, value);
    }

    /**
     * Attaches a value to the given {@link ByteBuf}.
     * <p>
     * Note: this <em>may</em> wrap the given buffer and return the wrapped buffer if the original buffer did not
     * support attachments. Always use the returned buffer.
     *
     * @param buf   the buffer to attach to and possibly wrap.
     * @param value the value to attach.
     * @return the buffer with the value attached.
     */
    public ByteBuf push(ByteBuf buf, A value) {
        return CodextraImpl.push(buf, this, value);
    }

    /**
     * Pops a value for this attachment from the given ops.
     *
     * @param ops the ops to pop the attachment from.
     * @return the popped value or {@code null} if there was none.
     */
    public @Nullable A pop(DynamicOps<?> ops) {
        return CodextraImpl.pop(ops, this);
    }

    /**
     * Pops a value for this attachment from the given ops.
     *
     * @param buf the buffer to pop the attachment from.
     * @return the popped value or {@code null} if there was none.
     */
    public @Nullable A pop(ByteBuf buf) {
        return CodextraImpl.pop(buf, this);
    }

    /**
     * Gets the current value for this attachment on the given ops.
     *
     * @param ops the dynamic ops to get the attached value from.
     * @return the current value or {@code null} if there is none.
     */
    public @Nullable A get(DynamicOps<?> ops) {
        AttachmentManager manager = CodextraImpl.getAttachmentManager(ops);
        if (manager == null) return null;
        return manager.get(this);
    }

    /**
     * Gets the current value for this attachment on the given buffer.
     *
     * @param buf the buffer to get the attached value from.
     * @return the current value or {@code null} if there is none.
     */
    public @Nullable A get(ByteBuf buf) {
        AttachmentManager manager = CodextraImpl.getAttachmentManager(buf);
        if (manager == null) return null;
        return manager.get(this);
    }
}
