package org.oasis.datacore.core.entity;


import java.net.URISyntaxException;
import java.util.Map;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.joda.time.DateTime;
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

import com.mongodb.MongoException;
import com.mongodb.WriteConcern;

import static org.junit.Assert.*;


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
      assertEquals("http://data.ozwillo.com/sample/1", sampleEntity.getUri());
      assertEquals("some text", props.get("string"));
      assertEquals(2, props.get("int"));
      assertEquals(true, props.get("boolean"));
      assertTrue(props.get("date") instanceof DateTime); // TODO Date
      System.out.println("sample data:\n" + sampleEntity);
   }

   @Test
   public void testMongoConfAcknowledged() {
      assertEquals(WriteConcern.ACKNOWLEDGED, mt.getDb().getWriteConcern());
      // TODO LATER for prod, check & test REPLICA_ACKNOWLEDGED (and not / also FSYNCED or JOURNALED ??)
      // TODO LATER2 for prod, test mongo auth
   }

   @Test
   public void testMongoNoDuplicateUriIndex() {
   		if (mt.collectionExists("test.unicity"))
   			mt.dropCollection("test.unicity");

	   MongoCollection<Document> coll = mt.createCollection("test.unicity");

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

	   assertEquals(0, coll.countDocuments());

	   IndexOptions indexOptions = new IndexOptions().unique(true);
	   coll.createIndex(Indexes.text("_uri"), indexOptions);
	   coll.insertOne(city1);

	   //Be sure that city1 has been persisted
	   assertEquals(1, coll.countDocuments());

	   try {
		   coll.insertOne(city2);
		   fail("Insert city2 should be impossible.");
	   } catch (MongoException e) {
		   assertTrue(true);
	   } catch (Exception e) {
		   fail("Bad exception");
	   }

	   assertEquals(1, coll.countDocuments());

	   if(mt.collectionExists("test.unicity")) {
		   mt.dropCollection("test.unicity");
	   }
   }
}
