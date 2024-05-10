package com.kneelawk.codextra.impl.mixin.impl;

import java.util.function.Supplier;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import io.netty.buffer.ByteBuf;

import com.mojang.serialization.Codec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.kneelawk.codextra.impl.attach.ManagerApplierCodec;
import com.kneelawk.codextra.impl.attach.ManagerGrabberStreamCodec;

/**
 * Transfers the AttachmentManager from the StreamCodec's context to the Codec's ops when calling
 * {@link ByteBufCodecs#fromCodec(Codec, Supplier)} or {@link ByteBufCodecs#fromCodecWithRegistries(Codec, Supplier)}.
 */
@Mixin(ByteBufCodecs.class)
public interface ByteBufCodecsMixin {
    @ModifyVariable(
        method = "fromCodec(Lcom/mojang/serialization/Codec;Ljava/util/function/Supplier;)Lnet/minecraft/network/codec/StreamCodec;",
        at = @At("HEAD"), argsOnly = true)
    private static Codec<Object> codextra_wrapFromCodecCodec(Codec<Object> value) {
        return new ManagerApplierCodec<>(value);
    }

    @ModifyReturnValue(
        method = "fromCodec(Lcom/mojang/serialization/Codec;Ljava/util/function/Supplier;)Lnet/minecraft/network/codec/StreamCodec;",
        at = @At("RETURN"))
    private static StreamCodec<ByteBuf, Object> codextra_wrapFromCodecReturn(StreamCodec<ByteBuf, Object> original) {
        return new ManagerGrabberStreamCodec<>(original);
    }

    @ModifyVariable(
        method = "fromCodecWithRegistries(Lcom/mojang/serialization/Codec;Ljava/util/function/Supplier;)Lnet/minecraft/network/codec/StreamCodec;",
        at = @At("HEAD"), argsOnly = true)
    private static Codec<Object> codextra_wrapFromCodecWithRegistriesCodec(Codec<Object> value) {
        return new ManagerApplierCodec<>(value);
    }

    @ModifyReturnValue(
        method = "fromCodecWithRegistries(Lcom/mojang/serialization/Codec;Ljava/util/function/Supplier;)Lnet/minecraft/network/codec/StreamCodec;",
        at = @At("RETURN"))
    private static StreamCodec<RegistryFriendlyByteBuf, Object> codextra_wrapFromCodecWithRegistriesReturn(
        StreamCodec<RegistryFriendlyByteBuf, Object> original) {
        return new ManagerGrabberStreamCodec<>(original);
    }
}
