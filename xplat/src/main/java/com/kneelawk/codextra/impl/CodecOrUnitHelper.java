package com.kneelawk.codextra.impl;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;

@Deprecated
public class CodecOrUnitHelper {
    public static <A> @Nullable A getUnitValue(Codec<A> codec) {
        if (codec instanceof MapCodec.MapCodecCodec<A> map && map.codec().keys(JsonOps.INSTANCE).findAny().isEmpty())
            return codec.parse(JsonOps.INSTANCE, JsonOps.INSTANCE.createMap(Map.of())).result().orElse(null);
        return null;
    }
}
