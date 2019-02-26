package org.oasis.datacore.core.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;


/**
 * FOR UNIT TEST PURPOSE ONLY !
 * Drops datacore's mongo db if datacore.devmode and system prop datacore.dropdb.
 * 
 * @author mdutoo
 *
 */
@Component
public class DropDbAtInit extends InitableBase {

   @Value("${datacore.devmode}")
   private boolean devmode;
   /** to drop db
    * TODO LATER rather in service, also for DatacoreSampleBase */
   @Autowired
   private /*static */MongoTemplate mgt;

   @Override
   protected void doInit() {
      if (devmode && "true".equals(System.getProperty("datacore.dropdb"))) {
         logger.info("dropping db...");
         mgt.getDb().drop();
      }
   }

   @Override
   public int getOrder() {
      return 10;
   }

}
