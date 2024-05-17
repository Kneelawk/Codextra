package com.kneelawk.codextra.api.codec;

import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.kneelawk.codextra.api.Codextra;
import com.kneelawk.codextra.api.attach.AttachmentKey;
import com.kneelawk.codextra.api.attach.codec.RetrievalMapCodec;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Deprecated
public class CodecOrUnitTests {
    @Test
    void unitDetection() {
        Codec<String> codec = Codec.unit("Hello World");
        CodecOrUnit<String> codecOrUnit = CodecOrUnit.codec(codec);
        assertInstanceOf(CodecOrUnit.Unit.class, codecOrUnit);
    }

    @Test
    void nonUnitDetection() {
        Codec<String> codec = Codec.STRING;
        CodecOrUnit<String> codecOrUnit = CodecOrUnit.codec(codec);
        assertInstanceOf(CodecOrUnit.Wrapper.class, codecOrUnit);
    }

    @Test
    void optionalFieldDetection() {
        Codec<String> codec = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("str", "default").forGetter(Function.identity())
        ).apply(instance, Function.identity()));
        CodecOrUnit<String> codecOrUnit = CodecOrUnit.codec(codec);
        assertInstanceOf(CodecOrUnit.Wrapper.class, codecOrUnit);
    }

    private static final AttachmentKey<String> STRING_ATTACHMENT = AttachmentKey.ofStaticFieldName();

    @Test
    @Disabled("CodecOrUnit is deprecated because it cannot solve this problem")
    void optionalAttachmentDetection() {
        // this codec is specially crafted to break the unit detection
        Codec<String> codec =
            Codextra.errorHandlingMapCodec(new RetrievalMapCodec<>(STRING_ATTACHMENT, DataResult::success), str -> {},
                true).xmap(opt -> opt.orElse("else"), Optional::of).codec();
        CodecOrUnit<String> codecOrUnit = CodecOrUnit.codec(codec);
        assertInstanceOf(CodecOrUnit.Wrapper.class, codecOrUnit);
    }
}
