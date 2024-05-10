package com.kneelawk.codextra.impl.attach;

import org.jetbrains.annotations.NotNull;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;

import com.kneelawk.codextra.impl.CodextraImpl;

public class ManagerGrabberStreamCodec<B, V> implements StreamCodec<B, V> {
    private final StreamCodec<B, V> wrapped;

    public ManagerGrabberStreamCodec(StreamCodec<B, V> wrapped) {this.wrapped = wrapped;}

    @Override
    public @NotNull V decode(@NotNull B stream) {
        if (stream instanceof ByteBuf buf) {
            CodextraImpl.putStreamManager(buf);
        }
        V decoded = wrapped.decode(stream);
        CodextraImpl.removeStreamManager();
        return decoded;
    }

    @Override
    public void encode(@NotNull B stream, @NotNull V input) {
        if (stream instanceof ByteBuf buf) {
            CodextraImpl.putStreamManager(buf);
        }
        wrapped.encode(stream, input);
        CodextraImpl.removeStreamManager();
    }
}
