package org.oasis.datacore.rest.server;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryServiceImpl;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.binding.DatacoreObjectMapper;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
import org.oasis.datacore.rest.client.QueryParameters;
import org.oasis.datacore.sample.CityCountrySample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;


/**
 * Tests CXF client with mock server : simple get / post / put version & cache
 * 
 * @author mdutoo
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-rest-server-test-context.xml" })
//@FixMethodOrder(MethodSorters.NAME_ASCENDING) // else random since java 7 NOT REQUIRED ANYMORE
public class DatacoreApiServerJsonLdTest {
   
   @Autowired
   @Qualifier("datacoreApiCachedJsonClient")
   private /*DatacoreApi*/DatacoreCachedClient datacoreApiClient;
   
   /** to init models */
   @Autowired
   private /*static */DataModelServiceImpl modelServiceImpl;
   ///@Autowired
   ///private CityCountrySample cityCountrySample;
   
   /** to cleanup db
    * TODO LATER rather in service */
   @Autowired
   private /*static */MongoOperations mgo;

   /** to clean cache for tests */
   @Autowired
   @Qualifier("datacore.rest.client.cache.rest.api.DCResource")
   private Cache resourceCache; // EhCache getNativeCache
   
   /** to be able to build a full uri, to check in tests
    * TODO rather client-side DCURI or rewrite uri in server */
   ///@Value("${datacoreApiClient.baseUrl}") 
   ///private String baseUrl; // useless
   @Value("${datacoreApiClient.containerUrl}") 
   private String containerUrl;

   /** for testing purpose */
   @Autowired
   @Qualifier("datacoreApiImpl") 
   private DatacoreApiImpl datacoreApiImpl;
   /** for testing purpose */
   @Autowired
   private LdpEntityQueryServiceImpl ldpEntityQueryServiceImpl;
   
   @Autowired
   private CityCountrySample cityCountrySample;
   
   
   @Before
   public void cleanDataAndCache() {
      cityCountrySample.cleanDataOfCreatedModels(); // (was already called but this first cleans up data)
      datacoreApiClient.getCache().clear(); // to avoid side effects
   }
   @After
   public void resetDefaults() {
      ldpEntityQueryServiceImpl.setMaxScan(0); // unlimited, default in test
   }
   
   /**
    * Cleans up data of all Models
    */
   /*@Test // rather than @BeforeClass, else static and spring can't inject
   //@BeforeClass
   public void init2cleanupDbFirst() {
      for (DCModel model : modelServiceImpl.getModelMap().values()) {
         mgo.remove(new Query(), model.getCollectionName());
         Assert.assertEquals(0,  mgo.findAll(DCEntity.class, model.getCollectionName()).size());
      }
   }*/

   private DCResource buildNamedData(String type, String name) {
      DCResource resource = DCResource.create(containerUrl, type, name).set("n:name", name);
      /*String iri = name;
      resource.setUri(UriHelper.buildUri(containerUrl, type, iri));*/
      //resource.setVersion(-1l);
      /*resource.setProperty("type", type);
      resource.setProperty("iri", iri);*/
      return resource;
   }
   
   private DCResource buildCityData(String name, String countryName,
         int populationCount, boolean embeddedCountry) {
      String type = CityCountrySample.CITY_MODEL_NAME;
      String iri = countryName + '/' + name;
      DCResource cityResource = DCResource.create(containerUrl, type, iri).set("n:name", name);
      /*DCResource cityResource = new DCResource();
      cityResource.setUri(UriHelper.buildUri(containerUrl, type, iri));
      cityResource.setProperty("name", name);*/
      //cityResource.setVersion(-1l);
      /*cityResource.setProperty("type", type);
      cityResource.setProperty("iri", iri);*/
      cityResource.set("city:populationCount", populationCount);
      
      String countryUri = UriHelper.buildUri(containerUrl, CityCountrySample.COUNTRY_MODEL_NAME, countryName);
      if (embeddedCountry) {
         DCResource countryResource = buildNamedData(CityCountrySample.COUNTRY_MODEL_NAME, countryName);
         cityResource.setProperty("city:inCountry", countryResource);
      } else {
         cityResource.setProperty("city:inCountry", countryUri);
      }
      return cityResource;
   }

   @Test
   public void testFind() throws Exception {
      // query all - no resource
      List<DCResource> resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters(), null, null);
      Assert.assertEquals(0, resources.size());
      
      // query all - one resource
      datacoreApiClient.postDataInType(buildNamedData(CityCountrySample.COUNTRY_MODEL_NAME, "UK"));
      DateTime londonFoundedDate = new DateTime(-43, 4, 1, 0, 0, DateTimeZone.UTC);
      DCResource londonCityData = buildCityData("London", "UK", 10000000, false);
      londonCityData.setProperty("city:founded", londonFoundedDate);

      //i18n
      HashMap<String, String> fr = new HashMap<String, String>();
      fr.put("@language", "fr");
      fr.put("@value", "Londres");
      HashMap<String, String> en = new HashMap<String, String>();
      en.put("@language", "en");
      en.put("@value", "London");
      HashMap<String, String> us = new HashMap<String, String>();
      us.put("@language", "us");
      us.put("@value", "London");
      ArrayList<HashMap<String, String>> i18n = new ArrayList<HashMap<String, String>>();
      i18n.add(fr);
      i18n.add(en);
      i18n.add(us);
      londonCityData.setProperty("i18n:name", i18n);

      datacoreApiClient.postDataInType(londonCityData);
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters(), null, null);
      DatacoreObjectMapper dcObjectMapper = new DatacoreObjectMapper();
      ///JacksonJsonProvider dcJacksonJsonProvider = new JacksonJsonProvider(dcObjectMapper);
      ///ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ///dcJacksonJsonProvider.writeTo(resources, type, genericType, annotations, mediaType, httpHeaders, bos);
      String json = dcObjectMapper.writeValueAsString(resources)
            .replaceAll("\"l\"", "\"@language\"")
            .replaceAll("\"v\"", "\"@value\"");
      // now json SHOULD be json-ld
      // Read the file into an Object (The type of this object will be a List, Map, String, Boolean,
      // Number or null depending on the root object in the file).
      Object jsonObject = JsonUtils.fromInputStream(new ByteArrayInputStream(json.getBytes()));
      // Create a context JSON map containing prefixes and definitions
      Map context = new HashMap();
      // Customise context...
      ///context.put("dc", "http://dc");
      context.put("i18n:name", "{\"@container\": \"@language\"}");
      // Create an instance of JsonLdOptions with the standard JSON-LD options
      JsonLdOptions options = new JsonLdOptions();
      ///options.set
      // Customise options...
      // Call whichever JSONLD function you want! (e.g. compact)
      Object compact = JsonLdProcessor.frame(jsonObject, context, options);
      // Print out the result (or don't, it's your call!)
      System.out.println(JsonUtils.toPrettyString(compact));
      options.format = "text/turtle";
      Object turtlefRdf = JsonLdProcessor.toRDF(jsonObject, options); // compact
      System.out.println(JsonUtils.toPrettyString(turtlefRdf));
      
      /*Assert.assertEquals(1, resources.size());
      Assert.assertEquals(this.containerUrl + "dc/type/" + CityCountrySample.CITY_MODEL_NAME + "/UK/London", resources.get(0).getUri()); // http://localhost:8180/

      // query all - two resource
      datacoreApiClient.postDataInType(buildNamedData(CityCountrySample.COUNTRY_MODEL_NAME, "France"));
      DCResource bordeauxCityData = buildCityData("Bordeaux", "France", 10000000, false);
      DateTime bordeauxFoundedDate = new DateTime(300, 4, 1, 0, 0, DateTimeZone.forID("+01:00"));
      bordeauxCityData.setProperty("founded", bordeauxFoundedDate);
      DCResource postedBordeauxCityData = datacoreApiClient.postDataInType(bordeauxCityData);
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters(), null, null);
      Assert.assertEquals(2, resources.size());*/
   }
   
}
