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

package com.kneelawk.codextra.impl.mixin.impl;

import java.util.Objects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.FriendlyByteBuf;

import com.kneelawk.codextra.impl.CodextraImpl;
import com.kneelawk.codextra.impl.attach.AttachmentManagerImpl;
import com.kneelawk.codextra.impl.mixin.api.CodextraAttachmentManagerHolder;

@Mixin(FriendlyByteBuf.class)
public class FriendlyByteBufMixin implements CodextraAttachmentManagerHolder {
    @Unique
    private AttachmentManagerImpl codextra_attachmentManager;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void codextra_onCreate(ByteBuf source, CallbackInfo ci) {
        AttachmentManagerImpl manager = CodextraImpl.getAttachmentManager(source);
        codextra_attachmentManager = Objects.requireNonNullElseGet(manager, AttachmentManagerImpl::new);
    }

    @Override
    public AttachmentManagerImpl codextra_getAttachmentManager() {
        return codextra_attachmentManager;
    }

    @Override
    public void codextra_setAttachmentManager(AttachmentManagerImpl manager) {
        this.codextra_attachmentManager = manager;
    }
}
