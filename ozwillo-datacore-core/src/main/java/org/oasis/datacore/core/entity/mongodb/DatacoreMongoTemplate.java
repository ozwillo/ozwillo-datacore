package org.oasis.datacore.core.entity.mongodb;

import java.util.List;

import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.CursorProviderQueryCursorPreparer;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Query;


/**
 * Allows :
 * * to manipulate DBCursor (ex. call explain()), by making visible cursorPreparer
 * 
 * @author mdutoo
 *
 */
public class DatacoreMongoTemplate extends MongoTemplate {

   public DatacoreMongoTemplate(MongoDbFactory mongoDbFactory, MongoConverter mongoConverter) {
      super(mongoDbFactory, mongoConverter);
   }

   /**
    * Makes visible cursorPreparer to manipulate DBCursor
    * @param query
    * @param entityClass
    * @param collectionName
    * @param cursorPreparer
    * @return
    */
   public <T> List<T> find(final Query query, Class<T> entityClass, String collectionName,
         CursorProviderQueryCursorPreparer cursorPreparer) {

      if (query == null) {
         return findAll(entityClass, collectionName);
      }

      return doFind(collectionName, query.getQueryObject(), query.getFieldsObject(), entityClass,
            cursorPreparer);
   }

}
