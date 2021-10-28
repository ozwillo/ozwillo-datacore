package org.oasis.datacore.core.entity;


import static org.junit.Assert.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.common.context.DCRequestContextProvider;
import org.oasis.datacore.common.context.SimpleRequestContextProvider;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.SimpleUriService;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import com.google.common.collect.ImmutableMap;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;


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

   // To create a conf'd MongoTemplate.
   // NB. neither EntityService (because no project available yet)
   // nor MongoTemplateManager can't be used in -core.
   @Qualifier("mappingConverter")
   @Autowired
   private MongoConverter mongoConverter;
   @Qualifier("mongoDbFactory")
   @Autowired
   private MongoDatabaseFactory mongoDbFactory;
   
   /** to check mongo conf, last operation result... */
   @Autowired
   private MongoTemplate mt;

   private String collName = "test";

   
   @Before
   public void setup() {
      if (mt.collectionExists(collName)) {
         mt.dropCollection(collName);
      }

      // set sample project :
      SimpleRequestContextProvider.setSimpleRequestContext(new ImmutableMap.Builder<String, Object>()
            .put(DCRequestContextProvider.PROJECT, DCProject.OASIS_SAMPLE).build());
      SimpleRequestContextProvider.getSimpleRequestContext().get(DCRequestContextProvider.PROJECT);
   }
   
   
   @Test
   public void testSampleData() throws URISyntaxException {
      DCEntity sampleEntity = getSampleData();
      Map<String, Object> props = sampleEntity.getProperties();
      assertEquals("http://data.ozwillo.com/city/France/Lyon", sampleEntity.getUri());
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
	   MongoCollection<Document> coll = mt.createCollection(collName);

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
   }

   @Test
   public void testMongoOptimisticLocking() throws URISyntaxException {
      DCEntity lyonEntity = getSampleData();

      MongoCollection<Document> coll = mt.createCollection(collName);
      assertEquals(0, coll.countDocuments());
      
      MongoTemplate mtt = new MongoTemplate(mongoDbFactory, mongoConverter);
      // as in MongoTemplateManager :
      // first disable auto index creation else error Index with name: _uri already exists with different options, in collection dcEntity
      // index are created FOR EACH MODEL'S COLLECTION in DatabaseSetupService
      ((MongoMappingContext) mtt.getConverter().getMappingContext()).setApplicationEventPublisher(null);
      
      DCEntity lyonEntityRes = mtt.insert(lyonEntity, collName);
      assertEquals(1, coll.countDocuments());
      
      DCEntity badVersionLyonEntity = new DCEntity(lyonEntityRes); // even with same _id
      badVersionLyonEntity.setVersion(12097589l);

      try {
         mtt.insert(badVersionLyonEntity, collName);
         fail("Insert badVersionLyonEntity should be impossible.");
      } catch (DuplicateKeyException e) {
         assertTrue(true);
      } catch (Exception e) {
         fail("Bad exception");
      }

      assertEquals(1, coll.countDocuments());
   }

   
   public DCEntity getSampleData() throws URISyntaxException {
      DCEntity dcEntity = new DCEntity();
      dcEntity.setUri("http://data.ozwillo.com/city/France/Lyon");
      dcEntity.setTypes(Arrays.asList("city"));
      Map<String, Object> props = dcEntity.getProperties(); // else properties are null

      props.put("name", "Lyon");
      
      props.put("string", "some text");
      props.put("int", 2);
      props.put("boolean", true);
      props.put("date", new DateTime());
      
      props.put("geoloc", "SAMPLE_GEOLOC_WKT_FORMAT");

      Map<String,String> i18nMap = new HashMap<String,String>();
      i18nMap.put("en", "London");
      i18nMap.put("fr", "Londres"); // NB. fallbacks might be defined on type data governance...
      props.put("i18nAlt1Field", i18nMap);
      props.put("i18nAlt2Field", "London"); // "real" value
      props.put("i18nAlt2Field__i18n", i18nMap); // TODO __i ??
      
      props.put("dcRef", SimpleUriService.buildUri(null, "city", "London"));
      props.put("scRef", SimpleUriService.buildUri(new URI("http://social.ozwillo.com"), "user", "john"));
      
      List<String> stringList = new ArrayList<String>();
      stringList.add("a");
      stringList.add("b");
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("a", "a");
      map.put("b", 2);
      map.put("c", new ArrayList<String>(stringList));
      map.put("dcRef", SimpleUriService.buildUri(null, "city", "London"));
      map.put("scRef", SimpleUriService.buildUri(new URI("http://social.ozwillo.com"), "user", "john"));
      List<Map<String,Object>> mapList = new ArrayList<Map<String,Object>>();
      mapList.add(new HashMap<String,Object>(map));
      props.put("stringList", stringList);
      props.put("map", map);
      props.put("mapList", mapList);
      
      // TODO partial copy
      // OPT if used / modeled : URL...
      // OPT if modeled : BigInteger/Decimal, Locale, Serializing (NOT mongo binary)
      
      return dcEntity;
   }
}
