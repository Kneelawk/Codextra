Changes:

* Initial Codextra Release!
* Added general purpose codecs:
    * `errorHandlingMapCodec` - Catches decoding errors and allows you to log them.
    * `keyCheckingMapCodec` - Only decodes if a certain set of keys are present, otherwise returns `Optional.empty()`.
    * `mapKeyDispatchCodec` - Like a `KeyDispatchCodec` but allows the type-key to be a map-codec instead of a regular
      codec.
* Added Ops-Attachments with codecs:
    * `attachingCodec` - Attaches an attachment.
    * `keyAttachingCodec` - Attaches a decoded value as an attachment.
    * `mutKeyAttachingCodec` - Attaches a decoded value as an attachment, but allows that attachment to be mutated while
      encoding.
    * `retrieve` - Retrieves an attachment.
    * `retrieveWithCodec` - Combines an attachment with a decoded value and returns that.
    * `ifPresentCodec` - delegates to one codec if an attachment is present and another if the attachment is absent.
    * `dispatchCodec` - Dispatches to different codecs depending on an attachment value.
    * `dispatchIfPresentCodec` - Dispatches to different codecs depending on an attachment value or if the attachment is
      absent.
* Ops-Attachments can be transferred from a `StreamCodec` context to a DFU `Codec` context using Minecraft's
  built-in `ByteBufCodecs.fromCodec` and `ByteBufCodecs.fromCodecWithRegistries`.
* Added `CodecOrUnit` type which can hold either a codec or a unit supplier. This is useful for when a unit-codec should
  not write a field to its map at all.
