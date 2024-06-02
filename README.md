# Codextra

[![Github Release Status]][Github Release] [![Maven Status]][Maven] [![Javadoc Badge]][Javadoc] [![Discord Badge]][Discord] [![Ko-fi Badge]][Ko-fi]

[Github Release Status]: https://img.shields.io/github/v/release/Kneelawk/Codextra?include_prereleases&sort=semver&style=flat-square&logo=github

[Github Release]: https://github.com/Kneelawk/Codextra/releases/latest

[Maven Status]: https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fmaven.kneelawk.com%2Freleases%2Fcom%2Fkneelawk%2Fcodextra%2Fcodextra-xplat-intermediary%2Fmaven-metadata.xml&style=flat-square&logo=apachemaven&logoColor=blue

[Maven]: https://maven.kneelawk.com/#/releases/com/kneelawk/codextra

[Javadoc Badge]: https://img.shields.io/badge/-javadoc-green?style=flat-square

[Javadoc]: https://maven.kneelawk.com/javadoc/releases/com/kneelawk/codextra/codextra-xplat-intermediary/latest

[Discord Badge]: https://img.shields.io/discord/988299232731607110?style=flat-square&logo=discord

[Discord]: https://discord.gg/6vgpHcKmxg

[Ko-fi Badge]: https://img.shields.io/badge/ko--fi-donate-blue?style=flat-square&logo=kofi

[Ko-fi]: https://ko-fi.com/kneelawk

Minecraft Codec extras and utilities, including Attachments.

Attachments allow you to transfer context into your Codecs like what Minecraft does with `RegistryOps`.

## Depending on Codextra

Codextra can be added to your project's dependencies by adding the following to your project's `build.gradle`:

```groovy
repositories {
	maven {
		name = "Kneelawk"
		url = "https://maven.kneelawk.com/releases/"
	}
}

dependencies {
	// On Loom-based xplat projects like Architectury:
	modCompileOnly "com.kneelawk.codextra:codextra-xplat-intermediary:<version>"

	// On VanillaGradle-based xplat projects:
	modCompileOnly "com.kneelawk.codextra:codextra-xplat-mojmap:<version>"

	// On Loom Fabric projects:
	modImplementation "com.kneelawk.codextra:codextra-fabric:<version>"
	include "com.kneelawk.codextra:codextra-fabric:<version>"

	// On Userdev Neoforge projects:
	implementation "com.kneelawk.codextra:codextra-neoforge:<version>"
	jarJar "com.kneelawk.codextra:codextra-neoforge:<version>"
}
```

## Using Codextra Attachments

You can declare a new attachment type simply by creating an `AttachmentKey` of the desired type:

```java
public static final AttachmentKey<Map<Integer, ResourceLocation>> RL_LOOKUP_ATTACHMENT_KEY =
		AttachmentKey.ofStaticFieldName();

public static final AttachmentKey<String> NAME_ATTACHMENT_KEY = AttachmentKey.ofStaticFieldName();
```

Once you have created an attachment key, you can attach attachments of that type to your codecs.

### Attaching to `DynamicOps`

When starting an encode or decode, you can attach an attachment directly to a `DynamicOps` like so:

```java
DynamicOps<T> attachedOps = ATTACHMENT_KEY.push(oldOps);
```

When starting an encode or decode, you don't generally need to pop an attachment from your ops. However, if you are
writing your own `Codec` or `MapCodec` implementation, calling pop is generally good practice:

```java
DynamicOps<T> unattachedOps = ATTACHMENT_KEY.pop(attachedOps);
```

### Attaching from Within a Codec

When creating a `CODEC` for your object type, it may be useful to be able to attach an attachment so that `CODEC`s used
within your codec can make use of that attachment.

```java
public static Codec<MyObject> codec(String name) {
	return NAME_ATTACHMENT_KEY.attachingCodec(name, CODEC);
}
```

You can also decode a value and immediately attach it as an attachment:

```java
public static final MapCodec<WithLookup> MAP_CODEC =
		RL_LOOKUP_ATTACHMENT_KEY.keyAttachingCodec(LOOKUP_MAP_CODEC, WITH_LOOKUP_RECORD_MAP_CODEC, WithLookup::lookup);
```

### Using Attachments in Your Codecs

The simplest way to use an attachment in your own codec is by incorporating an attachment as an argument in
a `RegistryCodecBuilder`.

```java
public static final Codec<MyObject> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		// ...
		NAME_ATTACHMENT_KEY.retrieve()
		// ...
).apply(instance, MyObject::new));
```

You can also dispatch codecs based on attachments:

```java
public static final Codec<MyObject> CODEC1 = NAME_ATTACHMENT_KEY.dispatchCodec(name -> getCodecFromName(name));
```

Or you can use an attachment in conjunction with another codec:

```java
public static final Codec<MyObject> CODEC2 =
		NAME_ATTACHMENT_KEY.retrieveWithCodec(ResourceLocation.CODEC, (name, rl) -> new MyObject(name, rl));
```

See the [javadocs] for more ways to use attachments.

[javadocs]: https://maven.kneelawk.com/javadoc/releases/com/kneelawk/codextra/codextra-xplat-intermediary/latest

### Stream Codecs

Attachments also work for `StreamCodec`s.

You can attach an attachment to a `FriendlyByteBuf`:

```java
FriendlyByteBuf buf = getBuf();
ATTACHMENT_KEY.push(buf);
```

You can attach a value mid-`StreamCodec`:

```java
public static StreamCodec<FriendlyByteBuf, MyObject> streamCodec(String name) {
	return NAME_ATTACHMENT_KEY.attachingStreamCodec(name, STREAM_CODEC);
}
```

You can retrieve an attachment inside a composite `StreamCodec`:

```java
public static final StreamCodec<FriendlyByteBuf, MyObject> STREAM_CODEC = StreamCodec.composite(
		// ...
		NAME_ATTACHMENT_KEY.retrieveStream(), FunctionUtils.nullFunc(),
		// ...
		MyObject::new
);
```

Many of the other DFU codecs have stream-codec variants as well.
