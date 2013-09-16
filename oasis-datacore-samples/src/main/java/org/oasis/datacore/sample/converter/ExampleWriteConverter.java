package org.oasis.datacore.sample.converter;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

/**
 * Shows how a converter can be used : for denormalization,
 * completing model (BUT lifecycle events are better at that),
 * custom serialization, while still having ex. id gen features 
 * 
 * see http://docs.spring.io/spring-data/data-document/docs/current/reference/html/#d0e2670
 * 
 * TODO reuse default converter !!!
 * 
 * @author mdutoo
 *
 */
public class ExampleWriteConverter implements Converter<Example, DBObject> {

   // TODO allow db operations in converter ????
   @Autowired
   private MongoTemplate mt;
   
  public DBObject convert(Example sourceExample) {
    DBObject dbo = new BasicDBObject();
    
    if (sourceExample.getId() == null) {
       //throw new RuntimeException("missing id");
       sourceExample.setId(ObjectId.get().toString());
    }
    dbo.put("_id", new ObjectId(sourceExample.getId()));
    
    dbo.put("text", sourceExample.getText());
    
    Document about = sourceExample.getAbout();
    if (about != null) {
       dbo.put("of", new DBRef(mt.getDb(), "documents", about.getId()));
       dbo.put("of_uri", about.getUri()); // denormalized, allows for query
       sourceExample.setAboutUri(about.getUri()); // NB. not required, just to ease up testing
    } else {
       // else fields can't be reset
       dbo.put("of", null);
       dbo.put("of_uri", null);
    }
    
    return dbo;
  }

}