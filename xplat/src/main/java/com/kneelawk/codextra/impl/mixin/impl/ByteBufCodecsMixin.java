package com.kneelawk.codextra.impl.mixin.impl;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.mojang.serialization.Codec;

import net.minecraft.network.codec.ByteBufCodecs;

import com.kneelawk.codextra.impl.attach.ManagerApplicationCodec;

/**
 * Transfers the AttachmentManager from the thread-local context to the Codec's ops when calling
 * {@link ByteBufCodecs#fromCodec(Codec, Supplier)} or {@link ByteBufCodecs#fromCodecWithRegistries(Codec, Supplier)}.
 */
@Mixin(ByteBufCodecs.class)
public interface ByteBufCodecsMixin {
    @ModifyVariable(
        method = "fromCodec(Lcom/mojang/serialization/Codec;Ljava/util/function/Supplier;)Lnet/minecraft/network/codec/StreamCodec;",
        at = @At("HEAD"), argsOnly = true)
    private static Codec<Object> codextra_wrapFromCodecCodec(Codec<Object> value) {
        return new ManagerApplicationCodec<>(value);
    }

    @ModifyVariable(
        method = "fromCodecWithRegistries(Lcom/mojang/serialization/Codec;Ljava/util/function/Supplier;)Lnet/minecraft/network/codec/StreamCodec;",
        at = @At("HEAD"), argsOnly = true)
    private static Codec<Object> codextra_wrapFromRegistryCodecCodec(Codec<Object> value) {
        return new ManagerApplicationCodec<>(value);
    }
}
