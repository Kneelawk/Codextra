Changes:

* Initial Codextra Release!
* Added general purpose codecs:
    * `ErrorHandlingMapCodec` - Catches decoding errors and allows you to log them.
    * `KeyCheckingMapCodec` - Only decodes if a certain set of keys are present, otherwise returns `Optional.empty()`.
    * `MapKeyDispatchCodec` - Like a `KeyDispatchCodec` but allows the type-key to be a map-codec instead of a regular
      codec.
* Added Ops-Attachments with codecs:
  * `attachingCodec` - Attaches an attachment.
  * `keyAttachingCodec` - Attaches a decoded value as an attachment.
  * `retrieve` - Retrieves an attachment.
  * `retrieveWithCodec` - Combines an attachment with a decoded value and returns that.
  * `dispatchCodec` - Dispatches to different codecs depending on an attachment value.
