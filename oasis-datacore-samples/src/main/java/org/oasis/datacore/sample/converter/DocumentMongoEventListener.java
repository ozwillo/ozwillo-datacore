package org.oasis.datacore.sample.converter;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.stereotype.Component;

import com.mongodb.DBObject;

@Component
public class DocumentMongoEventListener extends AbstractMongoEventListener<Document> {

   // WARNING don't override else others below won't be called
   /*@Override
   public void onApplicationEvent(MongoMappingEvent<?> event) {
      System.out.println("onApplicationEvent Document " + event);
   }*/

   @Override
   public void onBeforeConvert(Document source) {
      System.out.println("onBeforeConvert Document " + source);
   }

   @Override
   public void onBeforeSave(Document source, DBObject dbo) {
      System.out.println("onBeforeSave Document " +  source + " to dbo " + dbo);
   }

   @Override
   public void onAfterSave(Document source, DBObject dbo) {
      System.out.println("onAfterSave Document " +  source + " to dbo " + dbo);
   }

   @Override
   public void onAfterLoad(DBObject dbo) {
      System.out.println("onAfterLoad Document " + dbo);
   }

   @Override
   public void onAfterConvert(DBObject dbo, Document source) {
      System.out.println("onAfterConvert Document dbo " +  dbo + " to " + source);
   }

}
