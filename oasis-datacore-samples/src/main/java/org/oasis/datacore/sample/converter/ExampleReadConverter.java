package org.oasis.datacore.sample.converter;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.mongodb.DBObject;
import com.mongodb.DBRef;

/**
 * Shows how a converter can be used : for denormalization,
 * completing model (BUT lifecycle events are better at that),
 * custom serialization, while still having ex. id gen features 
 * 
 * see http://docs.spring.io/spring-data/data-document/docs/current/reference/html/#d0e2670
 * 
 * TODO reuse default converter ???
 * 
 * @author mdutoo
 *
 */
public class ExampleReadConverter implements Converter<DBObject, Example> {
   
   // TODO allow db operations in converter ????
   @Autowired
   private MongoOperations mop;

   public Example convert(DBObject source) {
      Example example = new Example();
      example.setId(((ObjectId) source.get("_id")).toString());
      example.setId((String) source.get("text"));
      DBRef of = (DBRef) source.get("of");
      if (of != null) {
         Document about = mop.findOne(new Query(new Criteria("_id").is(of.getId())), Document.class);
         if (about != null) {
            example.setAbout(about);
            example.setAboutUri(about.getUri()); // NB. not required, just to ease up testing
         } // else TODO error
      }
      return example;
   }

 }