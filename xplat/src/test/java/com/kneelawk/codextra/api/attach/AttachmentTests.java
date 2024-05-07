package com.kneelawk.codextra.api.attach;

import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

import com.kneelawk.codextra.impl.CodextraConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AttachmentTests {
    private static final AttachmentKey<String> TEST_ATTACHMENT = AttachmentKey.ofStaticFieldName();

    private record BasicTest(String test, String hello) {
        public static final Codec<BasicTest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("test").forGetter(BasicTest::test),
            TEST_ATTACHMENT.retrieve()
        ).apply(instance, BasicTest::new));
    }

    @Test
    void basicTest() {
        DynamicOps<JsonElement> ops = JsonOps.INSTANCE;
        ops = TEST_ATTACHMENT.push(ops, "Hello World");
        String testString = """
            {
              "test": "Testing!"
            }
            """;
        BasicTest test = BasicTest.CODEC.parse(ops, JsonParser.parseString(testString)).getOrThrow();
        assertEquals("Testing!", test.test);
        assertEquals("Hello World", test.hello);
    }

    @Test
    void failureTest() {
        DynamicOps<JsonElement> ops = JsonOps.INSTANCE;
        String testString = """
            {
              "test": "Testing!"
            }
            """;
        DataResult<BasicTest> result = BasicTest.CODEC.parse(ops, JsonParser.parseString(testString));
        assertTrue(result.isError());
        System.out.println("Result: " + result);
    }

    private static final AttachmentKey<Map<ResourceLocation, DispatchTest>> DISPATCH_ATTACHMENT =
        AttachmentKey.ofStaticFieldName();

    private record DispatchTest(String str, ResourceLocation name) {
        public static final Codec<DispatchTest> CODEC = DISPATCH_ATTACHMENT.dispatchCodec(
            map -> ResourceLocation.CODEC.flatXmap(rl -> map.containsKey(rl) ? DataResult.success(map.get(rl)) :
                DataResult.error(() -> "Map missing key [" + rl + "]"), test -> DataResult.success(test.name)));
    }

    @Test
    void dispatchTest() {
        DynamicOps<JsonElement> ops = JsonOps.INSTANCE;
        DispatchTest test = new DispatchTest("Hello World!", CodextraConstants.rl("test"));
        Map<ResourceLocation, DispatchTest> map = Map.of(test.name, test);

        ops = DISPATCH_ATTACHMENT.push(ops, map);

        String testString = "\"codextra:test\"";

        DispatchTest result = DispatchTest.CODEC.parse(ops, JsonParser.parseString(testString)).getOrThrow();
        assertSame(test, result);
    }
}
