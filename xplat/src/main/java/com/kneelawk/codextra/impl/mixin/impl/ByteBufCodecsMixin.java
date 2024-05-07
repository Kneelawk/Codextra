package com.kneelawk.codextra.impl.mixin.impl;

import java.util.function.Supplier;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import io.netty.buffer.ByteBuf;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.kneelawk.codextra.impl.attach.AttachmentManagerImpl;
import com.kneelawk.codextra.impl.attach.AttachmentOps;
import com.kneelawk.codextra.impl.attach.ManagerGrabberStreamCodec;
import com.kneelawk.codextra.impl.mixin.api.CodextraAttachmentManagerHolder;

/**
 * Transfers the AttachmentManager from the ByteBuf to the Codec's ops when calling {@link ByteBufCodecs#fromCodec(Codec, Supplier)}.
 */
@Mixin(ByteBufCodecs.class)
public interface ByteBufCodecsMixin {
    @ModifyReturnValue(
        method = "fromCodec(Lcom/mojang/serialization/Codec;Ljava/util/function/Supplier;)Lnet/minecraft/network/codec/StreamCodec;",
        at = @At("RETURN"))
    private static StreamCodec<ByteBuf, ?> codextra_wrapFromCodec(StreamCodec<ByteBuf, Object> original) {
        return new ManagerGrabberStreamCodec<>(original);
    }

    @WrapOperation(method = "method_56370", at = @At(value = "INVOKE",
        target = "Lcom/mojang/serialization/Codec;parse(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;", remap = false))
    private static DataResult<?> codextra_wrapFromCodecParse(Codec<?> instance, DynamicOps<Object> dynamicOps, Object o,
                                                             Operation<DataResult<?>> original) {
        AttachmentManagerImpl manager = ManagerGrabberStreamCodec.CURRENT_MANAGER.get();
        if (manager != null) {
            if (dynamicOps instanceof CodextraAttachmentManagerHolder holder) {
                holder.codextra_setAttachmentManager(manager);
            } else {
                dynamicOps = new AttachmentOps<>(dynamicOps);
            }
        }
        return original.call(instance, dynamicOps, o);
    }
    
    @WrapOperation(method = "method_56369", at = @At(value = "INVOKE",
        target = "Lcom/mojang/serialization/Codec;encodeStart(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;", remap = false))
    private static DataResult<?> codextra_wrapFromCodecEncodeStart(Codec<?> instance, DynamicOps<Object> dynamicOps, Object o,
                                                                Operation<DataResult<?>> original) {
        AttachmentManagerImpl manager = ManagerGrabberStreamCodec.CURRENT_MANAGER.get();
        if (manager != null) {
            if (dynamicOps instanceof CodextraAttachmentManagerHolder holder) {
                holder.codextra_setAttachmentManager(manager);
            } else {
                dynamicOps = new AttachmentOps<>(dynamicOps);
            }
        }
        return original.call(instance, dynamicOps, o);
    }
}
