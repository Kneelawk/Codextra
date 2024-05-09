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

package com.kneelawk.codextra.api.util;

import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.DataResult;

import com.kneelawk.codextra.api.attach.AttachmentKey;

/**
 * Utilities for working with java functional programming.
 */
public final class FunctionUtils {
    private FunctionUtils() {}

    /**
     * Returns a function that massages a {@link DataResult} so that it returns the expected type.
     *
     * @param <R> the expected type.
     * @return the function for converting data results.
     */
    public static <R> Function<DataResult<? extends R>, DataResult<R>> dataIdentity() {
        return res -> res.map(Function.identity());
    }

    /**
     * Creates a function that converts everything into {@code null}.
     * <p>
     * This is useful when using {@link AttachmentKey#retrieveStream()} in a composite stream codec.
     *
     * @param <I> the input type.
     * @param <O> the output type.
     * @return the converter function.
     */
    public static <I, O> Function<I, @Nullable O> nullFunc() {
        return input -> null;
    }
}
