package org.oasis.datacore.rest.server;

import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.client.DatacoreClientApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
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
   
   @Test // rather than @BeforeClass, else static and spring can't inject
   //@BeforeClass
   public /*static */void init1setupModels() {
      ///cityCountrySample.init(); // auto called
   }
   
   /**
    * Cleans up data of all Models
    */
   @Test // rather than @BeforeClass, else static and spring can't inject
   //@BeforeClass
   public /*static */void init2cleanupDbFirst() {
      for (DCModel model : modelServiceImpl.getModelMap().values()) {
         mgo.remove(new Query(), model.getCollectionName());
         Assert.assertEquals(0,  mgo.findAll(DCEntity.class, model.getCollectionName()).size());
      }
   }

   @Ignore
   @Test
   public void test1CreateFailInStrictModeWithVersion() {
      DCResource londonCityData = null;//datacoreApiClient.getData("city", "UK/London", null);
      Assert.assertNull(londonCityData);

      londonCityData = buildCityData("London", "UK", true);
      londonCityData.setVersion(0l);
      boolean oldStrictPostMode = datacoreApiImpl.isStrictPostMode();
      datacoreApiImpl.setStrictPostMode(true);
      try {
         datacoreApiClient.postDataInType(londonCityData, "city");
         Assert.fail("POST creation in strict mode should not be allowed when version provided");
      } catch (WebApplicationException waex) {
         Assert.assertTrue((waex.getResponse().getEntity() + "").toLowerCase().contains("strict"));
      } finally {
         datacoreApiImpl.setStrictPostMode(oldStrictPostMode);
      }
   }
   
   // TODO test missing uri
   
   // TODO test uri : replaceBaseUrlMode, normalizeUrlMode

   @Ignore
   @Test
   public void test1CreateFailWithoutReferencedData() {
      DCResource londonCityData = null;//datacoreApiClient.getData("city", "UK/London", null);
      Assert.assertNull(londonCityData);

      londonCityData = buildCityData("London", "UK", false);
      try {
         datacoreApiClient.postDataInType(londonCityData, "city");
         Assert.fail("Creation should fail when referenced data doesn't exist");
      } catch (WebApplicationException waex) {
         Assert.assertTrue((waex.getResponse().getEntity() + "").contains(
               this.containerUrl + "dc/type/country/UK")); // http://localhost:8180/
      }
   }

   // TODO LATER
   //@Test
   public void test2CreateWithReferencedDataInGraph() {
      DCResource londonCityData = null;//datacoreApiClient.getData("city", "UK/London", null);
      Assert.assertNull(londonCityData);

      londonCityData = buildCityData("London", "UK", false);
      DCResource postedLondonCityData = datacoreApiClient.postDataInType(londonCityData, "city");
   }

   @Test
   public void test2Create() {
      test2Create("UK", "London");
   }
   /**
    * Creates city & country & checks them
    * @param country
    * @param city
    * @return
    */
   public DCResource test2Create(String country, String city) {
      DCResource ukCountryData = datacoreApiClient.getData("country", country, null);
      Assert.assertNull(ukCountryData);
      ukCountryData = buildNamedData("country", country);
      DCResource postedUkCountryData = datacoreApiClient.postDataInType(ukCountryData, "country");
      ///List<DCResource> countryDatas = new ArrayList<DCResource>();
      ///countryDatas.add(ukCountryData);
      ///DCResource postedUkCountryData = datacoreApiClient.postAllDataInType(countryDatas, "country").get(0);
      Assert.assertNotNull(postedUkCountryData);
      
      String iri = country + '/' + city;
      DCResource cityData = datacoreApiClient.getData("city", iri, null);
      Assert.assertNull(cityData);

      cityData = buildCityData(city, country, false);
      DCResource postedLondonCityData = datacoreApiClient.postDataInType(cityData, "city");
      Assert.assertNotNull(postedLondonCityData);
      Assert.assertEquals(cityData.getProperties().get("description"),
            postedUkCountryData.getProperties().get("description"));
      DCResource gottenLondonCityData = datacoreApiClient.getData("city", iri, null);
      Assert.assertNotNull(gottenLondonCityData);
      Assert.assertEquals(cityData.getProperties().get("description"),
            postedUkCountryData.getProperties().get("description"));
      return cityData;
   }

   ///@Test
   public void test2CreateEmbedded() {
      DCResource bordeauxCityData = datacoreApiClient.getData("city", "France/Bordeaux", null);
      Assert.assertNull(bordeauxCityData);

      bordeauxCityData = buildCityData("Bordeaux", "France", true);
      DCResource postedBordeauxCityData = datacoreApiClient.postDataInType(bordeauxCityData, "city");
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
      String type = "city";
      String iri = countryName + '/' + name;
      DCResource cityResource = new DCResource();
      cityResource.setUri(UriHelper.buildUri(containerUrl, type, iri));
      //cityResource.setVersion(-1l);
      /*cityResource.setProperty("type", type);
      cityResource.setProperty("iri", iri);*/
      cityResource.setProperty("name", name);
      
      String countryType = "country";
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
   public void test3GetPostVersion() throws Exception {
      DCResource data = datacoreApiClient.getData("city", "UK/London", null);
      Assert.assertNotNull(data);
      Assert.assertNotNull(data.getVersion());
      long version = data.getVersion();
      Assert.assertEquals(this.containerUrl + "dc/type/city/UK/London", data.getUri()); // http://localhost:8180/
      ///Assert.assertEquals("city", data.getProperties().get("type"));
      ///Assert.assertEquals("UK/London", data.getProperties().get("iri"));
      DCResource postedData = datacoreApiClient.postDataInType(data, "city");
      Assert.assertNotNull(postedData);
      Assert.assertEquals(version + 1, (long) postedData.getVersion());
   }

   @Test
   public void test3clientCache() throws Exception {
      String bordeauxUriToEvict = test2Create("France", "Bordeaux").getUri();
      resourceCache.evict(bordeauxUriToEvict); // create with country but clean cache

      try {
         datacoreApiClient.deleteData("city", "France/Bordeaux", null);
         Assert.fail("Should not be able to delete without (having cache allowing) "
               + "sending cuttent version as ETag");
      } catch (Exception e) {
         Assert.assertTrue(true);
      }
      
      // GET
      // first call, sends ETag which should put result in cache :
      DCResource bordeauxCityResource = datacoreApiClient.getData("city", "France/Bordeaux", null);
      bordeauxCityResource = checkCachedBordeauxCityDataAndDelete(bordeauxCityResource);

      // post with bad version
      bordeauxCityResource.setVersion(1l);
      try {
         datacoreApiClient.postDataInType(bordeauxCityResource, "city"); // create
         Assert.fail("POST creation with bad version should fail");
      } catch (WebApplicationException waex) {
         Assert.assertTrue((waex.getResponse().getEntity() + "").toLowerCase().contains("version"));
      }
      bordeauxCityResource.setVersion(null); // else mongo optimistic locking exception
      
      // post
      DCResource postBordeauxCityResource = datacoreApiClient.postDataInType(bordeauxCityResource, "city"); // create
      bordeauxCityResource = checkCachedBordeauxCityDataAndDelete(postBordeauxCityResource);
      
      // put (& patch)
      bordeauxCityResource = datacoreApiClient.postDataInType(bordeauxCityResource, "city"); // first create...
      resourceCache.evict(bordeauxCityResource.getUri()); /// ... and clean cache
      DCResource putBordeauxCityResource = datacoreApiClient.putDataInType(bordeauxCityResource, "city", "France/Bordeaux");
      bordeauxCityResource = checkCachedBordeauxCityDataAndDelete(putBordeauxCityResource);
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
      DCResource putBordeauxCityResource = datacoreApiClient.postDataInType(bordeauxCityResource, "city");
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
      DCResource putBordeauxCityResource = datacoreApiClient.postDataInType(bordeauxCityResource, "city");
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
      DCResource cachedBordeauxCityResource = datacoreApiClient.getData("city", "France/Bordeaux", null);
      Assert.assertTrue("Should be same (cached) object", expectedCachedBordeauxCityResource == cachedBordeauxCityResource);
      
      // deleting, will send ETag which must be current version :
      datacoreApiClient.deleteData("city", "France/Bordeaux", null);
      Assert.assertNull(resourceCache.get(cachedBordeauxCityResource.getUri())); // check that cache has been cleaned
      
      DCResource renewBordeauxCityResource = datacoreApiClient.getData("city", "France/Bordeaux", null);
      Assert.assertFalse("Should not be cached object anymore", expectedCachedBordeauxCityResource == renewBordeauxCityResource);

      cachedBordeauxCityResource.setVersion(null); // else mongo optimistic locking exception
      return cachedBordeauxCityResource;
   }

   @Test
   public void test3find() throws Exception {
      List<DCResource> resources = datacoreApiClient.findDataInType("city", "", null, null);
      Assert.assertEquals(1, resources.size());
      Assert.assertEquals(this.containerUrl + "dc/type/city/UK/London", resources.get(0).getUri()); // http://localhost:8180/
      
      DCResource bordeauxCityData = buildCityData("Bordeaux", "France", true);
      DCResource postedBordeauxCityData = datacoreApiClient.postDataInType(bordeauxCityData, "city");

      resources = datacoreApiClient.findDataInType("city", "", null, null);
      Assert.assertEquals(2, resources.size());
      
      resources = datacoreApiClient.findDataInType("city", "name=Bordeaux", null, 10);
      Assert.assertEquals(1, resources.size());
      Assert.assertEquals(postedBordeauxCityData.getUri(), resources.get(0).getUri());
   }
}
