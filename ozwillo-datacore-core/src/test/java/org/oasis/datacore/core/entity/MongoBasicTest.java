package org.oasis.datacore.core.entity;


import java.io.File;
import java.io.IOException;
import java.util.Collections;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.core.entity.mongodb.joda.DateTimeCodec;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;

import static org.junit.Assert.*;


/**
 * 
 * @author vvision
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-core-test-context.xml" })
@TestExecutionListeners(listeners = { DependencyInjectionTestExecutionListener.class,
      DirtiesContextTestExecutionListener.class, TransactionalTestExecutionListener.class }) // else (harmless) error :
// TestContextManager [INFO] Could not instantiate TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener]. Specify custom listener classes or make the default listener classes (and their required dependencies) available. Offending class: [javax/servlet/ServletContext]
// see http://stackoverflow.com/questions/26125024/could-not-instantiate-testexecutionlistener
@FixMethodOrder // else random since java 7
public class MongoBasicTest {
   
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
			fail("Bad exception");
		}
		
		try {
			MongoClient mongo = createMongoClient("localhost" , 4242);
			MongoDatabase admin = mongo.getDatabase("admin");
			Document params = admin.runCommand(new Document("getCmdLineOpts", 1));
			
			Boolean argv = params.get("argv").toString().contains("--journal");			
			Boolean parsed = params.get("parsed").toString().contains("\"journal\" : true");			
			if(!argv || !parsed) {
				fail("Journaling is not enabled.");
			}
			
			MongoDatabase db = mongo.getDatabase("test");
			MongoCollection<Document> coll = db.getCollection("journal");
			
			coll.deleteOne(new BasicDBObject());
			assertEquals(0, coll.countDocuments());
		   
			Document city = new Document("_id_source", "42")
	               .append("_uri", "http://data.ozwillo.com/city/France/Lyon")
	               .append("name", "Lyon")
	               .append("countryName", "France")
	               .append("inCountry", "http://data.ozwillo.com/country/France");

			coll.insertOne(city);
			mongo.close();
		   
		   
			//Immediately kill mongod
			String[] args = {"/usr/bin/pkill", "-9", "-f", "mongod --dbpath /tmp/mongod/ --logpath /tmp/mongod/testJournal.log --journal --port 4242"}; 
			Runtime.getRuntime().exec(args);
	
			File f = new File("/tmp/mongod/journal/j._0");
			assertTrue("Should a journal file j._0 exists.", f.exists());

			//Restart mongod and check if data is in db
			Runtime.getRuntime().exec("mongod --dbpath /tmp/mongod/ --logpath /tmp/mongod/testJournal.log --journal --port 2121");
			Thread.sleep(10000);
			
			mongo = createMongoClient("localhost" , 2121);
			db = mongo.getDatabase("test");
			coll = db.getCollection("journal");
			long findLyonCount = coll.countDocuments(new BasicDBObject("name", "Lyon"));
			assertEquals("Should have a document with name Lyon.", 1, findLyonCount);
			mongo.close();
			//Clean the remaining mongod		
			String[] args2 = {"/usr/bin/pkill", "-9", "-f", "mongod --dbpath /tmp/mongod/ --logpath /tmp/mongod/testJournal.log --journal --port 2121"}; 
			Runtime.getRuntime().exec(args2);
			
			//Clean fs
			String[] rm = {"/bin/rm", "-r", "-f", "/tmp/mongod"}; 
			Runtime.getRuntime().exec(rm);
			   
		} catch (IOException | InterruptedException e) {
			fail("Bad exception");
		}
   }
   
   private MongoClient createMongoClient(String host, int port) {
      return MongoClients.create(MongoClientSettings.builder()
            .codecRegistry(
                  CodecRegistries.fromRegistries(
                        MongoClientSettings.getDefaultCodecRegistry(),
                        CodecRegistries.fromCodecs(new DateTimeCodec())
                        )
                  )
            .readPreference(ReadPreference.secondaryPreferred())
            //.writeConcern(legacyMongoClient.getWriteConcern())
            ///.readConcern(legacyMongoClient.getReadConcern())
            ///.retryWrites(mongoClientOptions.getRetryWrites())
            .applyToClusterSettings(clusterSettings -> {
               clusterSettings.hosts(Collections.singletonList(new ServerAddress(host , port)));
            }).build());
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
			fail("Bad exception");
		}
	   
		try {
			MongoClient mongo = createMongoClient("localhost" , 2424);						
			MongoDatabase admin = mongo.getDatabase("admin");
			Document params = admin.runCommand(new Document("getCmdLineOpts", 1));
			
			Boolean argv = params.get("argv").toString().contains("textSearchEnabled=true");			
			Boolean parsed = params.get("parsed").toString().contains("textSearchEnabled=true");			
			if(!argv || !parsed) {
				fail("Text search is not enabled.");
			}
			
			MongoDatabase db = mongo.getDatabase("test");
			MongoCollection<Document> coll = db.getCollection("textSearch");
			
			coll.deleteOne(new BasicDBObject());
			assertEquals(0, coll.countDocuments());

			Document city = new Document("_id_source", "42")
	               .append("_uri", "http://data.ozwillo.com/city/France/Lyon")
	               .append("name", "Lyon")
	               .append("countryName", "France")
	               .append("inCountry", "http://data.ozwillo.com/country/France")
	               .append("description", "Lyon is known for its historical and architectural landmarks and is a UNESCO World Heritage Site.");
			BasicDBObject index = new BasicDBObject("description", "text");
			coll.createIndex(index);
			coll.insertOne(city);
			
		    //Be sure that city has been persisted
			assertEquals(1, coll.countDocuments(new BasicDBObject("name", "Lyon")));

			BsonDocument bsonDocument = new BsonDocument();
			bsonDocument.append("text", new BsonString("textSearch"));
			bsonDocument.append("search", new BsonString("historical"));
		    Document useTextQuery = db.runCommand(bsonDocument);
		    
		    assertTrue("Text search should have a language field.", useTextQuery.containsKey("language"));
			
			mongo.close();
			//Clean the remaining mongod		
			String[] args = {"/usr/bin/pkill", "-9", "-f", "mongod --dbpath /tmp/mongod/ --logpath /tmp/mongod/textIndexTest.log --port 2424 --setParameter textSearchEnabled=true"}; 
			Runtime.getRuntime().exec(args);
			
			//Clean fs
			String[] rm = {"/bin/rm", "-r", "-f", "/tmp/mongod"}; 
			Runtime.getRuntime().exec(rm);
			   
		} catch (IOException e) {
			fail("Bad exception");
		}   

   }
   
}

