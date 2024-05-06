package com.kneelawk.codextra.impl.attach;

import org.jetbrains.annotations.NotNull;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;

import com.kneelawk.codextra.impl.CodextraImpl;

public class ManagerGrabberStreamCodec<B, V> implements StreamCodec<B, V> {
    public static final ThreadLocal<AttachmentManagerImpl> CURRENT_MANAGER = new ThreadLocal<>();

    private final StreamCodec<B, V> wrapped;

    public ManagerGrabberStreamCodec(StreamCodec<B, V> wrapped) {this.wrapped = wrapped;}

    @Override
    public @NotNull V decode(B object) {
        if (object instanceof ByteBuf buf) {
            CURRENT_MANAGER.set(CodextraImpl.getAttachmentManager(buf));
        }
        V decoded = wrapped.decode(object);
        CURRENT_MANAGER.remove();
        return decoded;
    }

    @Override
    public void encode(B object, V object2) {
        if (object instanceof ByteBuf buf) {
            CURRENT_MANAGER.set(CodextraImpl.getAttachmentManager(buf));
        }
        wrapped.encode(object, object2);
        CURRENT_MANAGER.remove();
    }
}
