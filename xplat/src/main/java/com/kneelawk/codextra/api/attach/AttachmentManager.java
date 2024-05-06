package com.kneelawk.codextra.api.attach;

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.ByteBuf;

import com.mojang.serialization.DynamicOps;

import com.kneelawk.codextra.impl.CodextraImpl;

/**
 * Manages {@link CodecAttachment}s.
 * <p>
 * This is usually part of a {@link DynamicOps} or {@link ByteBuf}.
 * <p>
 * This should generally only be used in implementing your own codecs.
 */
public interface AttachmentManager {
    /**
     * Gets the attachment manager for the given dynamic ops.
     *
     * @param ops the dynamic ops to get the attachment manager for.
     * @return the attachment manager for the given dynamic ops or
     * {@code null} if the given dynamic ops does not support attachments.
     */
    static @Nullable AttachmentManager getAttachmentManager(DynamicOps<?> ops) {
        return CodextraImpl.getAttachmentManager(ops);
    }

    /**
     * Gets the attachment manager for the given buffer.
     *
     * @param buf the buffer to get the attachment manager for.
     * @return the attachment manager for the given buffer or
     * {@code null} if the given buffer does not support attachments.
     */
    static @Nullable AttachmentManager getAttachmentManager(ByteBuf buf) {
        return CodextraImpl.getAttachmentManager(buf);
    }

    /**
     * Pushes an attachment.
     *
     * @param key   the attachment key.
     * @param value the value to push.
     * @param <A>   the type of attachment.
     */
    <A> void push(CodecAttachment<A> key, A value);

    /**
     * Pops an attachment.
     *
     * @param key the attachment key.
     * @param <A> the type of attachment.
     * @return the popped attachment or {@code null} if the attachment was not present.
     */
    <A> @Nullable A pop(CodecAttachment<A> key);

    /**
     * Gets the current value of an attachment.
     *
     * @param key the attachment key.
     * @param <A> the type of attachment.
     * @return the current value of the attachment or {@code null} if the attachment is not present.
     */
    <A> @Nullable A get(CodecAttachment<A> key);
}
