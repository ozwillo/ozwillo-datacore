package org.oasis.datacore.core.entity.mongodb.joda;

/**
 * mongo conf for Joda DateTime (in MongoTemplateManager)
 * @author mdutoo
 *
 */
public class DateTimeCodec implements org.bson.codecs.Codec<org.joda.time.DateTime> {
   public DateTimeCodec() {
      
   }
   
   @Override
   public void encode(final org.bson.BsonWriter writer, final org.joda.time.DateTime value, final org.bson.codecs.EncoderContext encoderContext) {
       writer.writeDateTime(value.getMillis());
   }

   @Override
   public org.joda.time.DateTime decode(final org.bson.BsonReader reader, final org.bson.codecs.DecoderContext decoderContext) {
       return new org.joda.time.DateTime(reader.readDateTime());
   }

   @Override
   public Class<org.joda.time.DateTime> getEncoderClass() {
       return org.joda.time.DateTime.class;
   }
}