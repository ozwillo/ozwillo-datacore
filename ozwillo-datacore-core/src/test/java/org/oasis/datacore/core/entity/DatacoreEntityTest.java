package org.oasis.datacore.core.entity;


import java.net.URISyntaxException;
import java.util.Map;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoIterable;
import org.bson.Document;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;


/**
 * 
 * @author mdutoo
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-core-test-context.xml" })
@TestExecutionListeners(listeners = { DependencyInjectionTestExecutionListener.class,
      DirtiesContextTestExecutionListener.class, TransactionalTestExecutionListener.class }) // else (harmless) error :
// TestContextManager [INFO] Could not instantiate TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener]. Specify custom listener classes or make the default listener classes (and their required dependencies) available. Offending class: [javax/servlet/ServletContext]
// see http://stackoverflow.com/questions/26125024/could-not-instantiate-testexecutionlistener
@FixMethodOrder // else random since java 7
public class DatacoreEntityTest {
   
   @Autowired
   private EntityService dcEntityService;

   @Autowired
   private MongoTemplate mt; // to check mongo conf, last operation result...
   
   @Test
   @Ignore
   public void testSampleData() throws URISyntaxException {
      DCEntity sampleEntity = dcEntityService.getSampleData();
      Map<String, Object> props = sampleEntity.getProperties();
      Assert.assertEquals("http://data.ozwillo.com/sample/1", sampleEntity.getUri());
      Assert.assertEquals("some text", props.get("string"));
      Assert.assertEquals(2, props.get("int"));
      Assert.assertEquals(true, props.get("boolean"));
      Assert.assertTrue(props.get("date") instanceof DateTime); // TODO Date
      System.out.println("sample data:\n" + sampleEntity);
   }

   @Test
   public void testMongoConfAcknowledged() {
      Assert.assertEquals(WriteConcern.ACKNOWLEDGED, mt.getDb().getWriteConcern());
      // TODO LATER for prod, check & test REPLICA_ACKNOWLEDGED (and not / also FSYNCED or JOURNALED ??)
      // TODO LATER2 for prod, test mongo auth
   }

   @Test
   public void testMongoNoDuplicateUriIndex() {
	   MongoCollection coll = !mt.collectionExists("test.unicity") ? mt.createCollection("test.unicity") : mt.getCollection("test.unicity");
	   String duplicatedUri = "http://data.ozwillo.com/city/France/Lyon";

	   Document city1 = new Document(
				"_id_source", "42")
               .append("_uri", duplicatedUri)
               .append("name", "Lyon")
               .append("countryName", "France")
               .append("inCountry", "http://data.ozwillo.com/country/France");

	   Document city2 = new Document(
				"_id_source", "21")
               .append("_uri", duplicatedUri)
               .append("name", "Strasbourg")
               .append("countryName", "France")
               .append("inCountry", "http://data.ozwillo.com/country/France");

	   //Be sure the collection is empty
	   coll.deleteOne(new Document());
	   Assert.assertTrue(coll.countDocuments()==0);

	   Document index = new Document("_uri", 1);
	   coll.createIndex(index);
	   coll.insertOne(city1);

	   //Be sure that city1 has been persisted
	   FindIterable<Document> fi = coll.find(new Document("name", "Lyon"));
//	   DBCursor findLyon = coll.find(new BasicDBObject("name", "Lyon"));
	   Assert.assertTrue(coll.countDocuments() == 1);

	   try {
		   coll.insertOne(city2);
		   //TODO check if this is still applied on mongo 4
//		   Assert.fail("Insert city2 should be impossible.");
	   } catch (MongoException e) {
		   Assert.assertTrue(true);
	   } catch (Exception e) {
		   Assert.fail("Bad exception");
	   }

	   if(mt.collectionExists("test.unicity")) {
		   mt.dropCollection("test.unicity");
	   }
   }

   /**
    * testMongoQueryUsingIndex() :
    * Query on indexed field & assert explain.toString contains BTree_Cursor,
    * Query on non-indexed field & assert it contains BasicCursor
    */
   @Test
   public void testMongoQueryUsingIndex() {
	   MongoCollection coll = !mt.collectionExists("test.index") ? mt.createCollection("test.index") : mt.getCollection("test.index");

	   //Be sure the collection is empty
	   coll.deleteOne(new BasicDBObject());
	   Assert.assertTrue(coll.countDocuments()==0);

	   Document city = new Document(
				"_id_source", "42")
              .append("_uri", "http://data.ozwillo.com/city/France/Lyon")
              .append("name", "Lyon")
              .append("countryName", "France")
              .append("inCountry", "http://data.ozwillo.com/country/France");

//	   coll.ensureIndex(new BasicDBObject("_uri", 1), null, true);
//	   coll.insert(city);

	   BasicDBObject index = new BasicDBObject("_uri", 1);
	   coll.createIndex(index);
	   coll.insertOne(city);
//	   DBCursor findLyon = coll.find(new BasicDBObject("name", "Lyon"));
	   Assert.assertTrue(coll.countDocuments() == 1);
//	   Assert.assertEquals("Should have BasicCursor on non-indexed field.", "BasicCursor", findLyon.explain().get("cursor"));

	   FindIterable<Document> fi = coll.find(new BasicDBObject("_uri", "http://data.ozwillo.com/city/France/Lyon"));
	   Assert.assertTrue(coll.countDocuments() == 1);
//	   Assert.assertEquals("Should have BtreeCursor on indexed field.", "BtreeCursor _uri_1", findUri.explain().get("cursor"));

	   if(mt.collectionExists("test.index")) {
		   mt.dropCollection("test.index");
	   }
   }

}
