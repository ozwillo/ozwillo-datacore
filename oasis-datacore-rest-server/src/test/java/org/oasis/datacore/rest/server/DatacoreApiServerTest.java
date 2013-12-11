package org.oasis.datacore.rest.server;

import java.util.List;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.security.OasisAuthAuditor;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.client.DatacoreClientApi;
import org.oasis.datacore.rest.client.QueryParameters;
import org.oasis.datacore.sample.CityCountrySample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * Tests CXF client with mock server : simple get / post / put version & cache
 * 
 * @author mdutoo
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-rest-server-test-context.xml" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING) // else random since java 7
public class DatacoreApiServerTest {
   
   @Autowired
   @Qualifier("datacoreApiCachedClient")
   private /*DatacoreApi*/DatacoreClientApi datacoreApiClient;
   
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

   @Autowired
   private CityCountrySample cityCountrySample;
   
   
   @Test // rather than @BeforeClass, else static and spring can't inject
   //@BeforeClass
   public /*static */void init1setupModels() {
      cityCountrySample.cleanDataOfCreatedModels(); // (was already called but this first cleans up data)
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

   @Test
   public void test1CreateFailInStrictModeWithVersion() {
      checkNoResource(CityCountrySample.CITY_MODEL_NAME, "UK/London");

      DCResource londonCityData = buildCityData("London", "UK", true);
      londonCityData.setVersion(0l);
      boolean oldStrictPostMode = datacoreApiImpl.isStrictPostMode();
      datacoreApiImpl.setStrictPostMode(true);
      try {
         datacoreApiClient.postDataInType(londonCityData, CityCountrySample.CITY_MODEL_NAME);
         Assert.fail("POST creation in strict mode should not be allowed when version provided");
      } catch (WebApplicationException waex) {
         Assert.assertTrue((waex.getResponse().getEntity() + "").toLowerCase().contains("strict"));
      } finally {
         datacoreApiImpl.setStrictPostMode(oldStrictPostMode);
      }
   }
   
   // TODO test missing uri
   
   // TODO test uri : replaceBaseUrlMode, normalizeUrlMode

   @Test
   public void test1CreateFailWithoutReferencedData() {
      checkNoResource(CityCountrySample.CITY_MODEL_NAME, "UK/London");

      DCResource londonCityData = buildCityData("London", "UK", false);
      try {
         datacoreApiClient.postDataInType(londonCityData, CityCountrySample.CITY_MODEL_NAME);
         Assert.fail("Creation should fail when referenced data doesn't exist");
      } catch (WebApplicationException waex) {
         Assert.assertTrue((waex.getResponse().getEntity() + "").contains(
               this.containerUrl + "dc/type/" + CityCountrySample.COUNTRY_MODEL_NAME + "/UK")); // http://localhost:8180/
      }
   }

   /**
    * 
    */
   @Test
   public void test2Create() {
      test2Create("UK", "London");
   }

   // TODO LATER
   //@Test
   public void test2CreateWithReferencedDataInGraph() {
      checkNoResource(CityCountrySample.CITY_MODEL_NAME, "UK/London");

      DCResource londonCityData = buildCityData("London", "UK", false);
      DCResource postedLondonCityData = datacoreApiClient.postDataInType(londonCityData, CityCountrySample.CITY_MODEL_NAME);
   }
   
   /**
    * Creates city & country & checks them
    * @param country
    * @param city
    * @return
    */
   public DCResource test2Create(String country, String city) {
      checkNoResource(CityCountrySample.COUNTRY_MODEL_NAME, country);
      DCResource ukCountryData = buildNamedData(CityCountrySample.COUNTRY_MODEL_NAME, country);
      DCResource postedUkCountryData = datacoreApiClient.postDataInType(ukCountryData, CityCountrySample.COUNTRY_MODEL_NAME);
      ///List<DCResource> countryDatas = new ArrayList<DCResource>();
      ///countryDatas.add(ukCountryData);
      ///DCResource postedUkCountryData = datacoreApiClient.postAllDataInType(countryDatas, CityCountrySample.COUNTRY_MODEL_NAME).get(0);
      Assert.assertNotNull(postedUkCountryData);
      
      String iri = country + '/' + city;
      checkNoResource(CityCountrySample.CITY_MODEL_NAME, iri);

      DCResource cityData = buildCityData(city, country, false);
      DCResource postedLondonCityData = datacoreApiClient.postDataInType(cityData, CityCountrySample.CITY_MODEL_NAME);
      Assert.assertNotNull(postedLondonCityData);
      Assert.assertEquals(cityData.getProperties().get("description"),
            postedUkCountryData.getProperties().get("description"));
      DCResource gottenLondonCityData = datacoreApiClient.getData(CityCountrySample.CITY_MODEL_NAME, iri, null);
      Assert.assertNotNull(gottenLondonCityData);
      Assert.assertEquals(cityData.getProperties().get("description"),
            postedUkCountryData.getProperties().get("description"));
      return cityData;
   }

   private void checkNoResource(String modelType, String id) {
      try {
         datacoreApiClient.getData(modelType, id, null);
         Assert.fail("There shouldn't be any " + modelType + " with id " + id + " yet");
      } catch (NotFoundException e) {
         Assert.assertTrue(true);
      }
   }

   ///@Test
   public void test2CreateEmbedded() {
      checkNoResource(CityCountrySample.CITY_MODEL_NAME, "France/Bordeaux");

      DCResource bordeauxCityData = buildCityData("Bordeaux", "France", true);
      DCResource postedBordeauxCityData = datacoreApiClient.postDataInType(bordeauxCityData, CityCountrySample.CITY_MODEL_NAME);
      Assert.assertNotNull(postedBordeauxCityData);
   }

   private DCResource buildNamedData(String type, String name) {
      DCResource resource = new DCResource();
      String iri = name;
      resource.setUri(UriHelper.buildUri(containerUrl, type, iri));
      //resource.setVersion(-1l);
      /*resource.setProperty("type", type);
      resource.setProperty("iri", iri);*/
      resource.setProperty("name", name);
      return resource;
   }
   
   private DCResource buildCityData(String name, String countryName, boolean embeddedCountry) {
      String type = CityCountrySample.CITY_MODEL_NAME;
      String iri = countryName + '/' + name;
      DCResource cityResource = new DCResource();
      cityResource.setUri(UriHelper.buildUri(containerUrl, type, iri));
      //cityResource.setVersion(-1l);
      /*cityResource.setProperty("type", type);
      cityResource.setProperty("iri", iri);*/
      cityResource.setProperty("name", name);
      
      String countryType = CityCountrySample.COUNTRY_MODEL_NAME;
      String countryUri = UriHelper.buildUri(containerUrl, countryType, countryName);
      if (embeddedCountry) {
         DCResource countryResource = buildNamedData(countryType, countryName);
         cityResource.setProperty("inCountry", countryResource);
      } else {
         cityResource.setProperty("inCountry", countryUri);
      }
      return cityResource;
   }

   /**
    * Tests the CXF client with the DatacoreApi service
    * @throws Exception If a problem occurs
    */
   @Test
   public void test2GetUpdateVersion() throws Exception {
      DCResource data = datacoreApiClient.getData(CityCountrySample.CITY_MODEL_NAME, "UK/London", null);
      Assert.assertNotNull(data);
      Assert.assertNotNull(data.getVersion());
      long version = data.getVersion();
      Assert.assertEquals(this.containerUrl + "dc/type/" + CityCountrySample.CITY_MODEL_NAME + "/UK/London", data.getUri()); // http://localhost:8180/
      ///Assert.assertEquals(CityCountrySample.CITY_MODEL_NAME, data.getProperties().get("type"));
      ///Assert.assertEquals("UK/London", data.getProperties().get("iri"));
      
      // test using POST update
      DCResource postedData = datacoreApiClient.postDataInType(data, CityCountrySample.CITY_MODEL_NAME);
      Assert.assertNotNull(postedData);
      Assert.assertEquals(version + 1, (long) postedData.getVersion());
      
      // test using PUT update
      DCResource putData = datacoreApiClient.putDataInType(postedData, CityCountrySample.CITY_MODEL_NAME, "UK/London");
      Assert.assertNotNull(putData);
      Assert.assertEquals(version + 2, (long) putData.getVersion());
   }

   @Test
   public void test3clientCache() throws Exception {
      String bordeauxUriToEvict = test2Create("France", "Bordeaux").getUri();
      resourceCache.evict(bordeauxUriToEvict); // create with country but clean cache

      try {
         datacoreApiClient.deleteData(CityCountrySample.CITY_MODEL_NAME, "France/Bordeaux", null);
         Assert.fail("Should not be able to delete without (having cache allowing) "
               + "sending cuttent version as ETag");
      } catch (Exception e) {
         Assert.assertTrue(true);
      }
      
      // GET
      // first call, sends ETag which should put result in cache :
      DCResource bordeauxCityResource = datacoreApiClient.getData(CityCountrySample.CITY_MODEL_NAME, "France/Bordeaux", null);
      bordeauxCityResource = checkCachedBordeauxCityDataAndDelete(bordeauxCityResource);

      // post with bad version
      bordeauxCityResource.setVersion(1l);
      try {
         datacoreApiClient.postDataInType(bordeauxCityResource, CityCountrySample.CITY_MODEL_NAME); // create
         Assert.fail("POST creation with bad version should fail");
      } catch (WebApplicationException waex) {
         Assert.assertTrue((waex.getResponse().getEntity() + "").toLowerCase().contains("version"));
      }
      bordeauxCityResource.setVersion(null); // else mongo optimistic locking exception
      
      // post
      DCResource postBordeauxCityResource = datacoreApiClient.postDataInType(bordeauxCityResource, CityCountrySample.CITY_MODEL_NAME); // create
      bordeauxCityResource = checkCachedBordeauxCityDataAndDelete(postBordeauxCityResource);
      // check audit data
      DateTime creationDate = postBordeauxCityResource.getCreated();
      Assert.assertNotNull("creation date should not be null", creationDate);
      String creationAuditor = postBordeauxCityResource.getCreatedBy();
      Assert.assertEquals(OasisAuthAuditor.TEST_AUDITOR, creationAuditor);
      Assert.assertEquals("at creation, created & modified dates should be the same",
            creationDate, postBordeauxCityResource.getLastModified());
      Assert.assertEquals("at creation, created & modified auditors should be the same",
            creationAuditor, postBordeauxCityResource.getLastModifiedBy());
      
      // put (& patch)
      bordeauxCityResource = datacoreApiClient.postDataInType(bordeauxCityResource, CityCountrySample.CITY_MODEL_NAME); // first create...
      resourceCache.evict(bordeauxCityResource.getUri()); /// ... and clean cache
      DCResource putBordeauxCityResource = datacoreApiClient.putDataInType(bordeauxCityResource, CityCountrySample.CITY_MODEL_NAME, "France/Bordeaux");
      bordeauxCityResource = checkCachedBordeauxCityDataAndDelete(putBordeauxCityResource);
      // check audit data
      Assert.assertEquals("at modification, created date should not change",
            creationDate, postBordeauxCityResource.getCreated());
      Assert.assertEquals("at modification, created auditor should not change",
            creationAuditor, postBordeauxCityResource.getCreatedBy());
      Assert.assertNotSame("at modification, modified date should differ from create date",
            creationDate, postBordeauxCityResource.getLastModified());
      Assert.assertEquals(OasisAuthAuditor.TEST_AUDITOR, postBordeauxCityResource.getLastModifiedBy());
   }
   
   /**
    * For now, client doesn't know when to parse String as Date
    * but we can still check if this String is OK
    * (would require (cached) Models for that)
    * @throws Exception
    */
   @Test
   public void test3propDateString() throws Exception {
      DCResource bordeauxCityResource = buildCityData("Bordeaux", "France", false);
      DateTime bordeauxFoundedDate = new DateTime(300, 4, 1, 0, 0);
      bordeauxCityResource.setProperty("founded", bordeauxFoundedDate); // testing date field
      DCResource putBordeauxCityResource = datacoreApiClient.postDataInType(bordeauxCityResource, CityCountrySample.CITY_MODEL_NAME);
      bordeauxCityResource = checkCachedBordeauxCityDataAndDelete(putBordeauxCityResource);
      Assert.assertTrue("returned date field should mean the same date as the one put",
            putBordeauxCityResource.getProperties().get("founded").toString().contains("300"));
   }

   /**
    * For now, client doesn't know when to parse String as Date
    * (would require (cached) Models for that)
    * @throws Exception
    */
   @Test
   @Ignore // LATER
   public void test3propDateJoda() throws Exception {
      DCResource bordeauxCityResource = buildCityData("Bordeaux", "France", false);
      DateTime bordeauxFoundedDate = new DateTime(300, 4, 1, 0, 0);
      bordeauxCityResource.setProperty("founded", bordeauxFoundedDate); // testing date field
      DCResource putBordeauxCityResource = datacoreApiClient.postDataInType(bordeauxCityResource, CityCountrySample.CITY_MODEL_NAME);
      bordeauxCityResource = checkCachedBordeauxCityDataAndDelete(putBordeauxCityResource);
      Assert.assertEquals("returned date field should be the Joda one put", bordeauxFoundedDate,
            putBordeauxCityResource.getProperties().get("founded"));
   }

   /**
    * 
    * @param expectedCachedBordeauxCityResource
    * @return without version, so can be used in POST creation again
    */
   private DCResource checkCachedBordeauxCityDataAndDelete(DCResource expectedCachedBordeauxCityResource) {
      // second call, should return 308
      DCResource cachedBordeauxCityResource = datacoreApiClient.getData(CityCountrySample.CITY_MODEL_NAME, "France/Bordeaux", null);
      Assert.assertTrue("Should be same (cached) object", expectedCachedBordeauxCityResource == cachedBordeauxCityResource);
      
      // deleting, will send ETag which must be current version :
      datacoreApiClient.deleteData(CityCountrySample.CITY_MODEL_NAME, "France/Bordeaux", null);
      Assert.assertNull(resourceCache.get(cachedBordeauxCityResource.getUri())); // check that cache has been cleaned
      
      checkNoResource(CityCountrySample.CITY_MODEL_NAME, "France/Bordeaux");

      cachedBordeauxCityResource.setVersion(null); // else mongo optimistic locking exception
      return cachedBordeauxCityResource;
   }

   @Test
   public void test3find() throws Exception {
      List<DCResource> resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters(), null, null);
      Assert.assertEquals(1, resources.size());
      Assert.assertEquals(this.containerUrl + "dc/type/" + CityCountrySample.CITY_MODEL_NAME + "/UK/London", resources.get(0).getUri()); // http://localhost:8180/
      
      DCResource bordeauxCityData = buildCityData("Bordeaux", "France", true);
      DCResource postedBordeauxCityData = datacoreApiClient.postDataInType(bordeauxCityData, CityCountrySample.CITY_MODEL_NAME);

      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters(), null, null);
      Assert.assertEquals(2, resources.size());
      
      // unquoted regex
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("name", "$regex.*Bord.*"), null, 10);
      Assert.assertEquals(1, resources.size());
      Assert.assertEquals(postedBordeauxCityData.getUri(), resources.get(0).getUri());
      
      // unquoted equals (empty)
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("name", "Bordeaux"), null, 10);
      Assert.assertEquals(1, resources.size());
      Assert.assertEquals(postedBordeauxCityData.getUri(), resources.get(0).getUri());

      // unquoted equals (SQL)
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("name", "=Bordeaux"), null, 10);
      Assert.assertEquals(1, resources.size());
      Assert.assertEquals(postedBordeauxCityData.getUri(), resources.get(0).getUri());

      // unquoted equals (java)
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("name", "==Bordeaux"), null, 10);
      Assert.assertEquals(1, resources.size());
      Assert.assertEquals(postedBordeauxCityData.getUri(), resources.get(0).getUri());

      // JSON (quoted) equals (empty)
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("name", "\"Bordeaux\""), null, 10);
      Assert.assertEquals(1, resources.size());
      Assert.assertEquals(postedBordeauxCityData.getUri(), resources.get(0).getUri());
   }
}
