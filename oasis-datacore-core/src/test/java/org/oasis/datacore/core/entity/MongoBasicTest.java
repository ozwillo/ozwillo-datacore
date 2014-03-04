package org.oasis.datacore.core.entity;


import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;


/**
 * 
 * @author vvision
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-core-context.xml" })
@FixMethodOrder // else random since java 7
public class MongoBasicTest {
   
   @Autowired
   private EntityService dcEntityService;

   @Autowired
   private MongoTemplate mt; // to check mongo conf, last operation result...
  
   /**
    * testMongoJournal
    * 
    * WARNING: Takes some time !
    * This test takes time to execute as it must wait for mongod each time a new one is created.
    * 
    * LATER test it using Spring as client (hard because if done using @ContextConfiguration-injected
    * spring context, mongo client would start before the server)
    * LATER2 automate it in another, less frequent CI job
    * 
    * 
    * WARNING: It will fail if mongod is not initialized when trying to access admin db.
    * @Ignore
    */
   @Test
   @Ignore
   public void testMongoJournal() {
	   //Create dir for mongod
	   File tempDir = new File("/tmp/mongod");

	   if (!tempDir.exists()) {
	     boolean result = tempDir.mkdir();  
	      if(result) {    
	        System.out.println("/tmp/mongod created");  
	      }
	   }
	   
		//Start a clean mongod
		try {
			Runtime.getRuntime().exec("mongod --dbpath /tmp/mongod/ --logpath /tmp/mongod/testJournal.log --journal --port 4242");
			Thread.sleep(20000);//Ensure that mongo has time to start
		} catch (IOException | InterruptedException e) {
			Assert.fail("Bad exception");
		}
		
		try {
			MongoClient mongo = new MongoClient("localhost" , 4242);						
			DB admin = mongo.getDB("admin");
			CommandResult params = admin.command("getCmdLineOpts");
			
			Boolean argv = params.get("argv").toString().contains("--journal");			
			Boolean parsed = params.get("parsed").toString().contains("\"journal\" : true");			
			if(!argv || !parsed) {
				Assert.fail("Journaling is not enabled.");
			}		
			
			DB db = mongo.getDB("test");
			DBCollection coll = db.getCollection("journal");
			
			coll.remove(new BasicDBObject());
			Assert.assertTrue("collection should be empty.", !coll.find().hasNext());
		   
			DBObject city = new BasicDBObject(
					"_id_source", "42")
	               .append("_uri", "http://data.oasis-eu.org/city/France/Lyon")
	               .append("name", "Lyon")
	               .append("countryName", "France")
	               .append("inCountry", "http://data.oasis-eu.org/country/France");

			coll.insert(city);		
			mongo.close();
		   
		   
			//Immediately kill mongod
			String[] args = {"/usr/bin/pkill", "-9", "-f", "mongod --dbpath /tmp/mongod/ --logpath /tmp/mongod/testJournal.log --journal --port 4242"}; 
			Runtime.getRuntime().exec(args);
	
			File f = new File("/tmp/mongod/journal/j._0");
			Assert.assertTrue("Should a journal file j._0 exists.", f.exists());

			//Restart mongod and check if data is in db
			Runtime.getRuntime().exec("mongod --dbpath /tmp/mongod/ --logpath /tmp/mongod/testJournal.log --journal --port 2121");
			Thread.sleep(10000);
			
			mongo = new MongoClient("localhost" , 2121);
			db = mongo.getDB("test");
			coll = db.getCollection("journal");
			DBCursor findLyon = coll.find(new BasicDBObject("name", "Lyon"));
			Assert.assertTrue("Should have a document with name Lyon.", findLyon.count() == 1);
			mongo.close();
			//Clean the remaining mongod		
			String[] args2 = {"/usr/bin/pkill", "-9", "-f", "mongod --dbpath /tmp/mongod/ --logpath /tmp/mongod/testJournal.log --journal --port 2121"}; 
			Runtime.getRuntime().exec(args2);
			
			//Clean fs
			String[] rm = {"/bin/rm", "-r", "-f", "/tmp/mongod"}; 
			Runtime.getRuntime().exec(rm);
			   
		} catch (IOException | InterruptedException e) {
			Assert.fail("Bad exception");
		}
   }
   
   /**
    * WARNING: This test needs text search to be enabled.
    * WARNING: Takes some time !
    * 
    * According to Mongo website, we should not enable text search on production systems -> Beta feature.
    * It must be enabled on each and every mongod for replica sets and on each and every mongos for sharded clusters.
    * 
    * A collection can have at most one text index.
    * A sufficiently high limit on open file descriptors is needed when building a large text index.
    * 	Recommended (open files): ulimit -n 64000
    * 	http://docs.mongodb.org/manual/reference/ulimit/
    * Will impact insertion throughput.
    * Collection will use usePowerOf2Sizes to allocate space.
    * Each unique post-stemmed word in each indexed field for each document = one index entry.
    * 
    * http://docs.mongodb.org/manual/tutorial/enable-text-search/
    * http://docs.mongodb.org/manual/core/index-text/
    * 
    * LATER automate it in another, less frequent CI job
    * @Ignore
    */
   @Test
   @Ignore
   public void testMongoTextIndex() {
	 //Create dir for mongod
	   File tempDir = new File("/tmp/mongod");

	   if (!tempDir.exists()) {
	     boolean result = tempDir.mkdir();  
	      if(result) {    
	        System.out.println("/tmp/mongod created");  
	      }
	   }
	   
		//Start a clean mongod
		try {
			Runtime.getRuntime().exec("mongod --dbpath /tmp/mongod/ --logpath /tmp/mongod/textIndexTest.log --port 2424 --setParameter textSearchEnabled=true");
			Thread.sleep(20000);//Ensure that mongo has time to start
		} catch (IOException | InterruptedException e) {
			Assert.fail("Bad exception");
		}
	   
		try {
			MongoClient mongo = new MongoClient("localhost" , 2424);						
			DB admin = mongo.getDB("admin");
			CommandResult params = admin.command("getCmdLineOpts");
			
			Boolean argv = params.get("argv").toString().contains("textSearchEnabled=true");			
			Boolean parsed = params.get("parsed").toString().contains("textSearchEnabled=true");			
			if(!argv || !parsed) {
				Assert.fail("Text search is not enabled.");
			}
			
			DB db = mongo.getDB("test");
			DBCollection coll = db.getCollection("textSearch");
			
			coll.remove(new BasicDBObject());
			Assert.assertTrue("collection should be empty.", !coll.find().hasNext());
		   
			DBObject city = new BasicDBObject(
					"_id_source", "42")
	               .append("_uri", "http://data.oasis-eu.org/city/France/Lyon")
	               .append("name", "Lyon")
	               .append("countryName", "France")
	               .append("inCountry", "http://data.oasis-eu.org/country/France")
	               .append("description", "Lyon is known for its historical and architectural landmarks and is a UNESCO World Heritage Site.");
			
			coll.ensureIndex(new BasicDBObject("description", "text"), null, true);
			coll.insert(city);	
			
		    //Be sure that city has been persisted
		    DBCursor findLyon = coll.find(new BasicDBObject("name", "Lyon"));
		    Assert.assertTrue(findLyon.count() == 1);	    

		    DBObject textSearchCommand = new BasicDBObject();
		    textSearchCommand.put("text", "textSearch");
		    textSearchCommand.put("search", "historical");
		    CommandResult useTextQuery = db.command(textSearchCommand);
		    
		    Assert.assertTrue("Text search should have a language field.", useTextQuery.containsField("language"));
			
			mongo.close();
			//Clean the remaining mongod		
			String[] args = {"/usr/bin/pkill", "-9", "-f", "mongod --dbpath /tmp/mongod/ --logpath /tmp/mongod/textIndexTest.log --port 2424 --setParameter textSearchEnabled=true"}; 
			Runtime.getRuntime().exec(args);
			
			//Clean fs
			String[] rm = {"/bin/rm", "-r", "-f", "/tmp/mongod"}; 
			Runtime.getRuntime().exec(rm);
			   
		} catch (IOException e) {
			Assert.fail("Bad exception");
		}   

   }
   
}

