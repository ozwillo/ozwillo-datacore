package org.oasis.datacore.core.entity.mongodb.joda;

/**
 * mongo conf for Joda DateTime (in MongoTemplateManager)
 * @author mdutoo
 * @deprecated
 *
 */
public class DateTimeTransformer implements org.bson.Transformer {
   @Override
   public Object transform(Object objectToTransform) {
       org.joda.time.DateTime value = (org.joda.time.DateTime) objectToTransform;
       return value;
   }
}