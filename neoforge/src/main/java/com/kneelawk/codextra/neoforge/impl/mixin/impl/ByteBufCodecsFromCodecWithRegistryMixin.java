package com.kneelawk.codextra.neoforge.impl.mixin.impl;

import java.util.function.Supplier;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;

import com.kneelawk.codextra.impl.attach.AttachmentManagerImpl;
import com.kneelawk.codextra.impl.mixin.api.CodextraAttachmentManagerHolder;

/**
 * Transfers the AttachmentManager from the RegistryFriendlyByteBuf to the Codec's ops when calling
 * {@link net.minecraft.network.codec.ByteBufCodecs#fromCodecWithRegistries(Codec, Supplier)}
 */
@Mixin(targets = "net.minecraft.network.codec.ByteBufCodecs$19")
public class ByteBufCodecsFromCodecWithRegistryMixin {
    @WrapOperation(method = "decode(Lnet/minecraft/network/RegistryFriendlyByteBuf;)Ljava/lang/Object;", at = @At(
        value = "INVOKE",
        target = "Lcom/mojang/serialization/Codec;parse(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;", remap = false))
    private DataResult<?> codextra_wrapParse(Codec<?> instance, DynamicOps<Object> dynamicOps, Object o,
                                             Operation<DataResult<?>> original, RegistryFriendlyByteBuf buf) {
        // this cast should *always* succeed, as RegistryFriendlyByteBuf extends FriendlyByteBuf which is mixin'd to implement CodextraAttachmentManagerHolder
        AttachmentManagerImpl manager = ((CodextraAttachmentManagerHolder) buf).codextra_getAttachmentManager();
        // I sure hope nobody wraps the dynamic ops to cause it to not be a RegistryOps, but I'd rather be safe
        if (dynamicOps instanceof RegistryOps<Object> ops) {
            ((CodextraAttachmentManagerHolder) ops).codextra_setAttachmentManager(manager);
        }
        return original.call(instance, dynamicOps, o);
    }

    @WrapOperation(method = "encode(Lnet/minecraft/network/RegistryFriendlyByteBuf;Ljava/lang/Object;)V", at = @At(
        value = "INVOKE",
        target = "Lcom/mojang/serialization/Codec;encodeStart(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;", remap = false))
    private DataResult<?> codextra_wrapEncodeStart(Codec<?> instance, DynamicOps<Object> dynamicOps, Object o,
                                                   Operation<DataResult<?>> original, RegistryFriendlyByteBuf buf,
                                                   Object value) {
        // this cast should *always* succeed, as RegistryFriendlyByteBuf extends FriendlyByteBuf which is mixin'd to implement CodextraAttachmentManagerHolder
        AttachmentManagerImpl manager = ((CodextraAttachmentManagerHolder) buf).codextra_getAttachmentManager();
        // I sure hope nobody wraps the dynamic ops to cause it to not be a RegistryOps, but I'd rather be safe
        if (dynamicOps instanceof RegistryOps<Object> ops) {
            ((CodextraAttachmentManagerHolder) ops).codextra_setAttachmentManager(manager);
        }
        return original.call(instance, dynamicOps, o);
    }
}
