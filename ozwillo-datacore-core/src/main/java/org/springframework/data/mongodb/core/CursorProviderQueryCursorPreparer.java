package org.springframework.data.mongodb.core;

import java.util.concurrent.TimeUnit;

import org.springframework.data.mongodb.core.MongoTemplate.QueryCursorPreparer;
import org.springframework.data.mongodb.core.query.Query;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;


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
   
   private static final String MAX_SCAN = "$maxScan";
   
   private DBCursor cursorPrepared;
   private boolean doExplainQuery;
   private DBObject queryExplain;
   private int maxScan;
   /** microseconds, requires 2.6+ server http://docs.mongodb.org/manual/reference/method/cursor.maxTimeMS/#cursor.maxTimeMS */
   private int maxTime;

   /**
    * 
    * @param mongoTemplate
    * @param query
    * @param doExplainQuery
    * @param maxScan
    * @param maxTime microseconds, requires 2.6+ server http://docs.mongodb.org/manual/reference/method/cursor.maxTimeMS/#cursor.maxTimeMS
    */
   public CursorProviderQueryCursorPreparer(MongoTemplate mongoTemplate,
         Query query, boolean doExplainQuery, int maxScan, int maxTime) {
      mongoTemplate.super(query, null);
      this.doExplainQuery = doExplainQuery;
      this.maxScan = maxScan;
      this.maxTime = maxTime;
   }

   @Override
   public DBCursor prepare(DBCursor cursor) {
      if (doExplainQuery) {
         // cursor has just been created for query, let's get its explain() before
         // it gets changed by a possible sort
         queryExplain = cursor.explain();
      }
      
      this.cursorPrepared = super.prepare(cursor);
      
      // NB. not explaining sort because it can still be done afterwards using cursorPrepared
      
      if (maxScan > 0) {
         this.cursorPrepared.addSpecial(MAX_SCAN, maxScan);
      }
      if (maxTime > 0) {
         this.cursorPrepared.maxTime(maxTime, TimeUnit.MICROSECONDS);
      }
      return this.cursorPrepared;
   }

   public DBCursor getCursorPrepared() {
      return cursorPrepared;
   }

   public boolean isDoExplainQuery() {
      return doExplainQuery;
   }

   public DBObject getQueryExplain() {
      return queryExplain;
   }

   public int getMaxScan() {
      return maxScan;
   }

}
