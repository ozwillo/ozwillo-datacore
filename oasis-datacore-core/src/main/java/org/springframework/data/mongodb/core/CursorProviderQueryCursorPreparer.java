package org.springframework.data.mongodb.core;

import org.springframework.data.mongodb.core.MongoTemplate.QueryCursorPreparer;
import org.springframework.data.mongodb.core.query.Query;

import com.mongodb.DBCursor;


/**
 * Helps DatacoreMongoTemplate to manipulate DBCursor (ex. call explain()), by making visible cursorPreparer
 * NB. can't be in another package, else QueryCursorPreparer not visible
 * 
 * Impl notes :
 * Has to extend QueryCursorPreparer, custom code must be written in it after calling its super. 
 * 
 * @author mdutoo
 *
 */
public class CursorProviderQueryCursorPreparer extends QueryCursorPreparer {
   
   private DBCursor cursorPrepared;

   public CursorProviderQueryCursorPreparer(MongoTemplate mongoTemplate, Query query) {
      mongoTemplate.super(query);
   }

   @Override
   public DBCursor prepare(DBCursor cursor) {
      this.cursorPrepared = super.prepare(cursor);
      return this.cursorPrepared;
   }

   public DBCursor getCursorPrepared() {
      return cursorPrepared;
   }

}
