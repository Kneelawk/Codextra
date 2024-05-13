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

package com.kneelawk.codextra.api.attach;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import com.kneelawk.codextra.api.attach.codec.AttachingCodec;
import com.kneelawk.codextra.api.attach.codec.AttachingMapCodec;
import com.kneelawk.codextra.api.attach.codec.AttachmentDispatchCodec;
import com.kneelawk.codextra.api.attach.codec.AttachmentDispatchMapCodec;
import com.kneelawk.codextra.api.attach.codec.KeyAttachingCodec;
import com.kneelawk.codextra.api.attach.codec.RetrievalMapCodec;
import com.kneelawk.codextra.api.attach.codec.RetrieveWithCodec;
import com.kneelawk.codextra.api.attach.codec.RetrieveWithMapCodec;
import com.kneelawk.codextra.api.attach.stream.AttachingStreamCodec;
import com.kneelawk.codextra.api.attach.stream.AttachmentDispatchStreamCodec;
import com.kneelawk.codextra.api.attach.stream.ChildBufferFactory;
import com.kneelawk.codextra.api.attach.stream.MutReadAttachingStreamCodec;
import com.kneelawk.codextra.api.attach.stream.ReadAttachingStreamCodec;
import com.kneelawk.codextra.api.attach.stream.RetrievalStreamCodec;
import com.kneelawk.codextra.api.attach.stream.RetrieveWithStreamCodec;
import com.kneelawk.codextra.api.util.FunctionUtils;
import com.kneelawk.codextra.impl.CodextraImpl;
import com.kneelawk.codextra.impl.FieldNameHelper;

/**
 * A typed key allowing things to be attached to a {@link DynamicOps} or {@link FriendlyByteBuf}.
 * <p>
 * Attachments work like stacks, allowing you to push and pop them. Getting the current attachment always retrieves the
 * top-most value.
 * <p>
 * When attachment(s) are used in a codec, the required attachment(s) should be clearly noted in the codec's javadoc.
 *
 * @param <A> the type of thing this attaches.
 */
@SuppressWarnings("unused")
public class AttachmentKey<A> {
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private final String name;

    /**
     * Creates a new codec attachment key with the name of the field the attachment key is being assigned to.
     * <p>
     * This works by using a stack-walker to find the caller class, then iterating through the static fields in
     * initialization order to find the first one of the correct type that isn't initialized yet. This only works if
     * this instance is to be stored in a static field in the caller class.
     * <p>
     * <b>Note:</b> this won't work with Kotlin fields unless the field is annotated with {@code @JvmStatic}.
     * <p>
     * This is intended so that users can quickly and easily know where the attachment they're missing is located.
     *
     * @param <A> the type this attachment attaches.
     * @return the created attachment key.
     */
    public static <A> AttachmentKey<A> ofStaticFieldName() {
        Class<?> caller = STACK_WALKER.getCallerClass();
        String name = caller.getName() + "." +
            FieldNameHelper.getCurrentlyInitializingFieldName(caller, AttachmentKey.class);
        return new AttachmentKey<>(name);
    }

    /**
     * Creates a new codec attachment key.
     *
     * @param name the name of this attachment. This name is only used for printing error messages.
     * @param <A>  the type this attachment attaches.
     * @return the created attachment key.
     */
    public static <A> AttachmentKey<A> of(ResourceLocation name) {
        return of(name.toString());
    }

    /**
     * Creates a new codec attachment key.
     *
     * @param name the name of this attachment. This name is only used in printing error messages.
     * @param <A>  the type this attachment attaches.
     * @return the created attachment key.
     */
    public static <A> AttachmentKey<A> of(String name) {
        return new AttachmentKey<>(name);
    }

    private AttachmentKey(String name) {this.name = name;}

    /**
     * Gets this attachment's name.
     *
     * @return this attachment's name.
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "AttachmentKey[" + name + "]";
    }

    /**
     * Pushes an attachment value to the given {@link DynamicOps}.
     * <p>
     * Note: this <em>may</em> wrap the given ops and return the wrapper ops if the original ops did not support
     * attachments. Always use the returned ops.
     *
     * @param ops   the dynamic ops to attach to and possibly wrap.
     * @param value the value to attach.
     * @param <T>   the type of the dynamic ops.
     * @return the dynamic ops with the value attached.
     */
    public <T> DynamicOps<T> push(DynamicOps<T> ops, A value) {
        return CodextraImpl.push(ops, this, value);
    }

    /**
     * Attaches a value to the given {@link FriendlyByteBuf}.
     *
     * @param buf   the buffer to attach to.
     * @param value the value to attach.
     */
    public void push(FriendlyByteBuf buf, A value) {
        CodextraImpl.push(buf, this, value);
    }

    /**
     * Attaches a value to the given {@link ByteBuf}.
     * <p>
     * Note: this <em>may</em> wrap the given buffer and return the wrapper buffer if the original buffer did not
     * support attachments. Always use the returned buffer.
     *
     * @param buf   the buffer to attach to and possibly wrap.
     * @param value the value to attach.
     * @return the buffer with the value attached.
     */
    public ByteBuf push(ByteBuf buf, A value) {
        return CodextraImpl.push(buf, this, value);
    }

    /**
     * Pops a value for this attachment from the given ops.
     *
     * @param ops the ops to pop the attachment from.
     * @return the popped value, or {@code null} if there was none.
     */
    public @Nullable A pop(DynamicOps<?> ops) {
        return CodextraImpl.pop(ops, this);
    }

    /**
     * Pops a value for this attachment from the given ops.
     *
     * @param buf the buffer to pop the attachment from.
     * @return the popped value, or {@code null} if there was none.
     */
    public @Nullable A pop(ByteBuf buf) {
        return CodextraImpl.pop(buf, this);
    }

    /**
     * Gets the current value for this attachment on the given ops.
     *
     * @param ops the dynamic ops to get the attached value from.
     * @return the current value, or {@code null} if there is none.
     */
    public @Nullable A getOrNull(DynamicOps<?> ops) {
        AttachmentManager manager = AttachmentManager.getAttachmentManager(ops);
        if (manager == null) return null;
        return manager.get(this);
    }

    /**
     * Gets the current value for this attachment on the given buffer.
     *
     * @param buf the buffer to get the attached value from.
     * @return the current value, or {@code null} if there is none.
     */
    public @Nullable A getOrNull(ByteBuf buf) {
        AttachmentManager manager = AttachmentManager.getAttachmentManager(buf);
        if (manager == null) return null;
        return manager.get(this);
    }

    /**
     * Gets the current value for this attachment on the given ops.
     * <p>
     * This is intended for use in custom {@link Codec}s and {@link MapCodec}s.
     *
     * @param ops the dynamic ops to get the attached value from.
     * @return the current value wrapped in {@link DataResult#success(Object)},
     * or {@link DataResult#error(Supplier)} if this attachment is not present.
     */
    public DataResult<A> getResult(DynamicOps<?> ops) {
        AttachmentManager manager = AttachmentManager.getAttachmentManager(ops);
        if (manager == null) {
            return DataResult.error(
                () -> "DynamicOps '" + ops + "' does not support attachments. Attachment [" + getName() +
                    "] not present.");
        }

        A value = manager.get(this);
        if (value == null) {
            Set<AttachmentKey<?>> presentAttachments = manager.getAttachments();
            return DataResult.error(() -> "Attachment [" + getName() + "] not present. Attachments present: [" +
                presentAttachments.stream().map(AttachmentKey::getName).collect(Collectors.joining(", ")) + "]");
        }

        return DataResult.success(value);
    }

    /**
     * Gets the current value for this attachment on the given buffer or throwing.
     * <p>
     * This is intended for use in custom {@link StreamCodec}s.
     *
     * @param buf the buffer to get the attached value from.
     * @return the current value.
     * @throws DecoderException if this attachment is not present.
     */
    public @NotNull A getOrThrow(ByteBuf buf) throws DecoderException {
        AttachmentManager manager = AttachmentManager.getAttachmentManager(buf);
        if (manager == null) {
            throw new DecoderException(
                "Buffer '" + buf + "' does not support attachments. Attachment [" + getName() + "] not present.");
        }

        A value = manager.get(this);
        if (value == null) {
            throw new DecoderException("Attachment [" + getName() + "] not present. Attachments present: [" +
                manager.getAttachments().stream().map(AttachmentKey::getName).collect(Collectors.joining(", ")) + "]");
        }

        return value;
    }

    /**
     * Creates a {@link Codec} that attaches the given values to the codec context when decoding/encoding the
     * wrapped codec.
     *
     * @param attachmentMap the map of attachments to attach.
     * @param toWrap        the codec which will receive the attached values.
     * @param <R>           the type the codec handles.
     * @return the created codec.
     */
    public static <R> Codec<R> attachingCodec(Map<AttachmentKey<?>, ?> attachmentMap, Codec<R> toWrap) {
        return new AttachingCodec<>(attachmentMap, toWrap);
    }

    /**
     * Creates a {@link Codec} that attaches the given value to the codec context when decoding/encoding the
     * wrapped codec.
     *
     * @param toAttach the value to attach.
     * @param toWrap   the codec which will receive the attached value.
     * @param <R>      the type the codec handles.
     * @return the created codec.
     */
    public <R> Codec<R> attachingCodec(A toAttach, Codec<R> toWrap) {
        return AttachingCodec.single(this, toAttach, toWrap);
    }

    /**
     * Creates a {@link MapCodec} that attaches the given values to the codec context when decoding/encoding the
     * wrapped codec.
     *
     * @param attachmentMap the map of attachments to attach.
     * @param toWrap        the codec which will receive the attached values.
     * @param <R>           the type the codec handles.
     * @return the created map codec.
     */
    public static <R> MapCodec<R> attachingMapCodec(Map<AttachmentKey<?>, ?> attachmentMap, MapCodec<R> toWrap) {
        return new AttachingMapCodec<>(attachmentMap, toWrap);
    }

    /**
     * Creates a {@link MapCodec} that attaches the given value to the codec context when decoding/encoding the
     * wrapped codec.
     *
     * @param toAttach the value to attach.
     * @param toWrap   the codec which will receive the attached value.
     * @param <R>      the type the codec handles.
     * @return the created map codec.
     */
    public <R> MapCodec<R> attachingMapCodec(A toAttach, MapCodec<R> toWrap) {
        return AttachingMapCodec.single(this, toAttach, toWrap);
    }

    /**
     * Creates a {@link StreamCodec} that attaches the given values to the codec context when decoding/encoding the
     * wrapped codec.
     *
     * @param attachmentMap the map of attachments to attach.
     * @param toWrap        the codec which will receive the attached values.
     * @param <B>           the buffer type.
     * @param <V>           the type the codec handles.
     * @return the created stream codec.
     */
    public static <B extends FriendlyByteBuf, V> StreamCodec<B, V> attachingStreamCodec(
        Map<AttachmentKey<?>, ?> attachmentMap, StreamCodec<? super B, V> toWrap) {
        return new AttachingStreamCodec<>(attachmentMap, toWrap);
    }

    /**
     * Creates a {@link StreamCodec} that attaches the given value to the codec context when decoding/encoding the
     * wrapped codec.
     *
     * @param toAttach the value to attach.
     * @param toWrap   the codec which will receive the attached value.
     * @param <B>      the buffer type.
     * @param <V>      the type the codec handles.
     * @return the created stream codec.
     */
    public <B extends FriendlyByteBuf, V> StreamCodec<B, V> attachingStreamCodec(A toAttach,
                                                                                 StreamCodec<? super B, V> toWrap) {
        return AttachingStreamCodec.single(this, toAttach, toWrap);
    }

    /**
     * Creates a {@link MapCodec} that decodes one value and attaches it to the context when decoding the result value.
     *
     * @param keyCodec         the codec that decodes the attachment.
     * @param wrappedCodec     the codec that is invoked with the attachment attached.
     * @param attachmentGetter a function for getting the attachment when given the result type.
     * @param <R>              the result type.
     * @return the created map codec.
     */
    public <R> MapCodec<R> keyAttachingCodecResult(MapCodec<A> keyCodec, MapCodec<R> wrappedCodec,
                                                   Function<? super R, ? extends DataResult<? extends A>> attachmentGetter) {
        return new KeyAttachingCodec<>(this, keyCodec, wrappedCodec, attachmentGetter);
    }

    /**
     * Creates a {@link MapCodec} that decodes one value and attaches it to the context when decoding the result value.
     *
     * @param keyCodec         the codec that decodes the attachment.
     * @param wrappedCodec     the codec that is invoked with the attachment attached.
     * @param attachmentGetter a function for getting the attachment when given the result type.
     * @param <R>              the result type.
     * @return the created map codec.
     */
    public <R> MapCodec<R> keyAttachingCodec(MapCodec<A> keyCodec, MapCodec<R> wrappedCodec,
                                             Function<? super R, ? extends A> attachmentGetter) {
        return keyAttachingCodecResult(keyCodec, wrappedCodec, attachmentGetter.andThen(DataResult::success));
    }

    /**
     * Creates a {@link StreamCodec} that decodes one value and then attaches it to the context when decoding the result value.
     *
     * @param attachmentCodec  the codec for decoding the attachment.
     * @param wrappedCodec     the codec that is invoked with the attachment attached.
     * @param attachmentGetter a function for getting the attachment when given the result type.
     * @param <B>              the buffer type.
     * @param <V>              the result type.
     * @return the created stream codec.
     */
    public <B extends FriendlyByteBuf, V> StreamCodec<B, V> readAttachingStreamCodec(
        StreamCodec<? super B, A> attachmentCodec,
        StreamCodec<? super B, V> wrappedCodec,
        Function<? super V, ? extends A> attachmentGetter) {
        return new ReadAttachingStreamCodec<>(this, attachmentCodec, wrappedCodec, attachmentGetter);
    }

    /**
     * Creates a {@link StreamCodec} that decodes one value and then attaches it to the context when decoding the result
     * value, but that also allows mutation of the attachment while encoding, making sure those changes show up in the
     * decoded attachment.
     * <p>
     * This is good for things like palettes.
     *
     * @param attachmentCodec   the attachment's codec.
     * @param wrappedBufferCtor for creating the buffer used by the wrapped codec, as writing wrapped codec values to
     *                          the main buffer must be delayed.
     * @param wrappedCodec      the codec that will be invoked with the attachment attached.
     * @param attachmentGetter  a function for getting the attachment when given the result type. This may simply create
     *                          a new attachment if the attachment is intended to get all its value from being mutated
     *                          while encoding.
     * @param <B1>              the buffer type of the returned stream codec.
     * @param <B2>              the buffer type of the wrapped stream codec.
     * @param <V>               the result type.
     * @return the created stream codec.
     */
    public <B1 extends FriendlyByteBuf, B2 extends FriendlyByteBuf, V> StreamCodec<B1, V> mutReadAttachingStreamCodec(
        StreamCodec<? super B1, A> attachmentCodec, ChildBufferFactory<? super B1, B2> wrappedBufferCtor,
        StreamCodec<? super B2, V> wrappedCodec, Function<? super V, ? extends A> attachmentGetter) {
        return new MutReadAttachingStreamCodec<>(this, attachmentCodec, wrappedBufferCtor, wrappedCodec,
            attachmentGetter);
    }

    /**
     * Creates a {@link RecordCodecBuilder} that acts as a field, but that only returns the retrieved value.
     * <p>
     * When attachment(s) are used in a codec, the required attachment(s) should be clearly noted in the codec's javadoc.
     *
     * @param retriever the function for retrieving the desired value form the attachment value.
     * @param <O>       the object the field will be a part of.
     * @param <R>       the field type.
     * @return the created {@link RecordCodecBuilder}.
     */
    public <O, R> RecordCodecBuilder<O, R> retrieveResult(
        Function<? super A, ? extends DataResult<? extends R>> retriever) {
        return new RetrievalMapCodec<>(this, retriever).forGetter(o -> null);
    }

    /**
     * Creates a {@link RecordCodecBuilder} that acts as a field, but that only returns the retrieved value.
     * <p>
     * When attachment(s) are used in a codec, the required attachment(s) should be clearly noted in the codec's javadoc.
     *
     * @param retriever the function for retrieving the desired value form the attachment value.
     * @param <O>       the object the field will be a part of.
     * @param <R>       the field type.
     * @return the created {@link RecordCodecBuilder}.
     */
    public <O, R> RecordCodecBuilder<O, R> retrieve(Function<? super A, ? extends R> retriever) {
        return retrieveResult(retriever.andThen(DataResult::success));
    }

    /**
     * Creates a {@link RecordCodecBuilder} that acts as a field, but that only returns the attached value.
     * <p>
     * When attachment(s) are used in a codec, the required attachment(s) should be clearly noted in the codec's javadoc.
     *
     * @param <O> the object type the field will be a part of.
     * @return the created {@link RecordCodecBuilder}.
     */
    public <O> RecordCodecBuilder<O, A> retrieve() {
        return retrieveResult(DataResult::success);
    }

    /**
     * Creates a {@link StreamCodec} that retrieves the retrieved value.
     * <p>
     * This function does no encoding and does not need a valid value to encode. When creating a composite stream codec,
     * you can use {@link FunctionUtils#nullFunc()}.
     * <p>
     * When attachment(s) are used in a codec, the required attachment(s) should be clearly noted in the codec's javadoc.
     *
     * @param retriever the function for retrieving the desired value from the attachment value.
     * @param <B>       the buffer type.
     * @param <V>       the value type.
     * @return the created stream codec.
     */
    public <B extends ByteBuf, V> StreamCodec<B, V> retrieveStream(Function<? super A, ? extends V> retriever) {
        return new RetrievalStreamCodec<>(this, retriever);
    }

    /**
     * Creates a {@link StreamCodec} that retrieves the attached value.
     * <p>
     * This function does no encoding and does not need a valid value to encode. When creating a composite stream codec,
     * you can use {@link FunctionUtils#nullFunc()}.
     * <p>
     * When attachment(s) are used in a codec, the required attachment(s) should be clearly noted in the codec's javadoc.
     *
     * @param <B> the buffer type.
     * @return the created stream codec.
     */
    public <B extends ByteBuf> StreamCodec<B, A> retrieveStream() {
        return retrieveStream(Function.identity());
    }

    /**
     * Creates a {@link Codec} that decodes a value and combines it with an attachment to create a result.
     * <p>
     * When attachment(s) are used in a codec, the required attachment(s) should be clearly noted in the codec's javadoc.
     *
     * @param withCodec the codec for the value to be combined with the attachment.
     * @param retriever the function that combines the attachment with the decoded value.
     * @param reverse   the function that gets the value to be encoded when given the attachment and the combined value.
     * @param <O>       the type the attachment is combined with.
     * @param <R>       the result type.
     * @return the created codec.
     */
    public <O, R> Codec<R> retrieveWithCodecResult(Codec<O> withCodec,
                                                   BiFunction<? super A, ? super O, ? extends DataResult<? extends R>> retriever,
                                                   BiFunction<? super A, ? super R, ? extends DataResult<? extends O>> reverse) {
        return new RetrieveWithCodec<>(this, withCodec, retriever, reverse);
    }

    /**
     * Creates a {@link MapCodec} that decodes a value and combines it with an attachment to create a result.
     * <p>
     * When attachment(s) are used in a codec, the required attachment(s) should be clearly noted in the codec's javadoc.
     *
     * @param withCodec the codec for the value to be combined with the attachment.
     * @param retriever the function that combines the attachment with the decoded value.
     * @param reverse   the function that gets the value to be encoded when given the attachment and the combined value.
     * @param <O>       the type the attachment is combined with.
     * @param <R>       the result type.
     * @return the created map codec.
     */
    public <O, R> MapCodec<R> retrieveWithMapCodecResult(MapCodec<O> withCodec,
                                                         BiFunction<? super A, ? super O, ? extends DataResult<? extends R>> retriever,
                                                         BiFunction<? super A, ? super R, ? extends DataResult<? extends O>> reverse) {
        return new RetrieveWithMapCodec<>(this, withCodec, retriever, reverse);
    }

    /**
     * Creates a {@link Codec} that decodes a value and combines it with an attachment to create a result.
     * <p>
     * When attachment(s) are used in a codec, the required attachment(s) should be clearly noted in the codec's javadoc.
     *
     * @param withCodec the codec for the value to be combined with the attachment.
     * @param retriever the function that combines the attachment with the decoded value.
     * @param reverse   the function that gets the value to be encoded when given the attachment and the combined value.
     * @param <O>       the type the attachment is combined with.
     * @param <R>       the result type.
     * @return the created codec.
     */
    public <O, R> Codec<R> retrieveWithCodec(Codec<O> withCodec,
                                             BiFunction<? super A, ? super O, ? extends R> retriever,
                                             BiFunction<? super A, ? super R, ? extends O> reverse) {
        return retrieveWithCodecResult(withCodec, retriever.andThen(DataResult::success),
            reverse.andThen(DataResult::success));
    }

    /**
     * Creates a {@link MapCodec} that decodes a value and combines it with an attachment to create a result.
     * <p>
     * When attachment(s) are used in a codec, the required attachment(s) should be clearly noted in the codec's javadoc.
     *
     * @param withCodec the codec for the value to be combined with the attachment.
     * @param retriever the function that combines the attachment with the decoded value.
     * @param reverse   the function that gets the value to be encoded when given the attachment and the combined value.
     * @param <O>       the type the attachment is combined with.
     * @param <R>       the result type.
     * @return the created map codec.
     */
    public <O, R> MapCodec<R> retrieveWithMapCodec(MapCodec<O> withCodec,
                                                   BiFunction<? super A, ? super O, ? extends R> retriever,
                                                   BiFunction<? super A, ? super R, ? extends O> reverse) {
        return retrieveWithMapCodecResult(withCodec, retriever.andThen(DataResult::success),
            reverse.andThen(DataResult::success));
    }

    /**
     * Creates a {@link StreamCodec} that decodes a value and combines it with an attachment to create a result.
     * <p>
     * When attachment(s) are used in a codec, the required attachment(s) should be clearly noted in the codec's javadoc.
     *
     * @param withCodec the codec for the value to be combined with the attachment.
     * @param retriever the function that combines the attachment with the decoded value.
     * @param reverse   the function that gets the value to be encoded when given the attachment and the combined value.
     * @param <B>       the buffer type.
     * @param <O>       the type the attachment is combined with.
     * @param <R>       the result type.
     * @return the created stream codec.
     */
    public <B extends ByteBuf, O, R> StreamCodec<B, R> retrieveWithStreamCodec(StreamCodec<? super B, O> withCodec,
                                                                               BiFunction<? super A, ? super O, ? extends R> retriever,
                                                                               BiFunction<? super A, ? super R, ? extends O> reverse) {
        return new RetrieveWithStreamCodec<>(this, withCodec, retriever, reverse);
    }

    /**
     * Creates a {@link Codec} that dispatches based on the retrieved attachment.
     * <p>
     * When attachment(s) are used in a codec, the required attachment(s) should be clearly noted in the codec's javadoc.
     *
     * @param dispatcher the function for retrieving the correct codec based on the retrieved attachment.
     * @param <R>        the codec type.
     * @return the created codec.
     */
    public <R> Codec<R> dispatchCodecResult(
        Function<? super A, ? extends DataResult<? extends Codec<? extends R>>> dispatcher) {
        return new AttachmentDispatchCodec<>(this, dispatcher);
    }

    /**
     * Creates a {@link Codec} that dispatches based on the retrieved attachment.
     * <p>
     * When attachment(s) are used in a codec, the required attachment(s) should be clearly noted in the codec's javadoc.
     *
     * @param dispatcher the function for retrieving the correct codec based on the retrieved attachment.
     * @param <R>        the codec type.
     * @return the created codec.
     */
    public <R> Codec<R> dispatchCodec(Function<? super A, ? extends Codec<? extends R>> dispatcher) {
        return dispatchCodecResult(dispatcher.andThen(DataResult::success));
    }

    /**
     * Creates a {@link MapCodec} that dispatches based on the retrieved attachment.
     * <p>
     * When attachment(s) are used in a codec, the required attachment(s) should be clearly noted in the codec's javadoc.
     *
     * @param dispatcher the function for retrieving the correct codec based on the retrieved attachment.
     * @param <R>        the map codec type.
     * @return the created map codec.
     */
    public <R> MapCodec<R> dispatchMapCodecResult(
        Function<? super A, ? extends DataResult<? extends MapCodec<? extends R>>> dispatcher) {
        return new AttachmentDispatchMapCodec<>(this, dispatcher);
    }

    /**
     * Creates a {@link MapCodec} that dispatches based on the retrieved attachment.
     * <p>
     * When attachment(s) are used in a codec, the required attachment(s) should be clearly noted in the codec's javadoc.
     *
     * @param dispatcher the function for retrieving the correct codec based on the retrieved attachment.
     * @param <R>        the map codec type.
     * @return the created map codec.
     */
    public <R> MapCodec<R> dispatchMapCodec(Function<? super A, ? extends MapCodec<? extends R>> dispatcher) {
        return dispatchMapCodecResult(dispatcher.andThen(DataResult::success));
    }

    /**
     * Creates a {@link StreamCodec} that dispatches based on the retrieved attachment.
     * <p>
     * When attachment(s) are used in a codec, the required attachment(s) should be clearly noted in the codec's javadoc.
     *
     * @param dispatcher the function for retrieving the correct codec based on the retrieved attachment.
     * @param <B>        the buffer type.
     * @param <R>        the result type.
     * @return the created stream codec.
     */
    public <B extends ByteBuf, R> StreamCodec<B, R> dispatchStreamCodec(
        Function<? super A, ? extends StreamCodec<? super B, ? extends R>> dispatcher) {
        return new AttachmentDispatchStreamCodec<>(this, dispatcher);
    }
}
