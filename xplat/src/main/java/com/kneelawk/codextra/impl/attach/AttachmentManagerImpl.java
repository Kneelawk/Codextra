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

package com.kneelawk.codextra.impl.attach;

import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import com.kneelawk.codextra.api.attach.AttachmentKey;
import com.kneelawk.codextra.api.attach.AttachmentManager;

public class AttachmentManagerImpl implements AttachmentManager {
    private final Map<AttachmentKey<?>, Holder<?>> holders = new Reference2ObjectLinkedOpenHashMap<>();

    public AttachmentManagerImpl() {}

    @Override
    @SuppressWarnings("unchecked")
    public <A> void push(AttachmentKey<A> key, A value) {
        Holder<A> cur = new Holder<>(value);
        cur.prev = (Holder<A>) holders.put(key, cur);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> @Nullable A pop(AttachmentKey<A> key) {
        Holder<A> popped = (Holder<A>) holders.remove(key);
        if (popped == null) return null;
        if (popped.prev != null) {
            holders.put(key, popped.prev);
        }
        return popped.value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> @Nullable A get(AttachmentKey<A> key) {
        Holder<A> holder = (Holder<A>) holders.get(key);
        if (holder == null) return null;
        return holder.value;
    }

    @Override
    public Set<AttachmentKey<?>> getAttachments() {
        return new ReferenceOpenHashSet<>(holders.keySet());
    }

    @Override
    public boolean isEmpty() {
        return holders.isEmpty();
    }

    private static class Holder<A> {
        final A value;
        Holder<A> prev = null;

        private Holder(A value) {this.value = value;}
    }
}
