package org.oasis.datacore.core.entity.mongodb.joda;

/**
 * mongo conf for Joda DateTime (in MongoTemplateManager)
 * @author mdutoo
 *
 */
public class DateTimeCodecProvider implements org.bson.codecs.configuration.CodecProvider {
   @SuppressWarnings("unchecked")
   @Override
   public <T> org.bson.codecs.Codec<T> get(final Class<T> classToVerify, final org.bson.codecs.configuration.CodecRegistry registry) {
       if (classToVerify == org.joda.time.DateTime.class) {
           return (org.bson.codecs.Codec<T>) new DateTimeCodec();
       }

       return null;
   }
}