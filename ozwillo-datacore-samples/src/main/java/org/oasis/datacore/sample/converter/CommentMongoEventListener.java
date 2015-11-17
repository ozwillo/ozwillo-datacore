package org.oasis.datacore.sample.converter;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.stereotype.Component;

import com.mongodb.DBObject;

@Component
public class CommentMongoEventListener extends AbstractMongoEventListener<Comment> {

   // WARNING don't override else others below won't be called
   /*@Override
   public void onApplicationEvent(MongoMappingEvent<?> event) {
      System.out.println("onApplicationEvent Comment " + event);
   }*/

   @Override
   public void onBeforeConvert(Comment source) {
      System.out.println("onBeforeConvert Comment " + source);
   }

   @Override
   public void onBeforeSave(Comment source, DBObject dbo) {
      System.out.println("onBeforeSave Comment " +  source + " to dbo " + dbo);
   }

   @Override
   public void onAfterSave(Comment source, DBObject dbo) {
      System.out.println("onAfterSave Comment " +  source + " to dbo " + dbo);
   }

   @Override
   public void onAfterLoad(DBObject dbo) {
      System.out.println("onAfterLoad Comment " + dbo);
   }

   @Override
   public void onAfterConvert(DBObject dbo, Comment source) {
      System.out.println("onAfterConvert Comment dbo " +  dbo + " to " + source);
   }

}
