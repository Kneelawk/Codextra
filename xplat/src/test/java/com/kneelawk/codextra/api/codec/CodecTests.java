package com.kneelawk.codextra.api.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import com.kneelawk.codextra.api.Codextra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CodecTests {
    @Test
    void unitHandlingMapCodec() {
        Codec<String> codec = Codextra.unitHandlingFieldOf("test", Codec.unit("Hello World")).codec();
        String input = """
            {
            }
            """;

        DataResult<String> res = codec.parse(JsonOps.INSTANCE, JsonParser.parseString(input));
        assertTrue(res.isSuccess());
        assertEquals("Hello World", res.getOrThrow());
    }

    @Test
    void unitHandlingMapCodecEncoding() {
        Codec<String> codec = Codextra.unitHandlingFieldOf("test", Codec.unit("Hello World")).codec();
        DataResult<JsonElement> res = codec.encodeStart(JsonOps.INSTANCE, "Hello World");
        assertTrue(res.isSuccess());
        assertEquals("{}", res.getOrThrow().toString());
    }

    @Test
    void unitHandlingMapCodecEncoding2() {
        Codec<String> codec = Codextra.unitHandlingFieldOf("test", Codec.STRING).codec();
        DataResult<JsonElement> res = codec.encodeStart(JsonOps.INSTANCE, "Hello World");
        assertTrue(res.isSuccess());
        assertEquals("{\"test\":\"Hello World\"}", res.getOrThrow().toString());
    }
}
