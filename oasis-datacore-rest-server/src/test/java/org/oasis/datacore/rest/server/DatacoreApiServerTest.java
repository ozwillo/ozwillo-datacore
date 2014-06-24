package org.oasis.datacore.rest.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.core.entity.EntityQueryService;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryServiceImpl;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.ResourceParsingHelper;
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


/**
 * Tests CXF client with mock server : simple get / post / put version & cache
 * 
 * @author mdutoo
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-rest-server-test-context.xml" })
//@FixMethodOrder(MethodSorters.NAME_ASCENDING) // else random since java 7 NOT REQUIRED ANYMORE
public class DatacoreApiServerTest {
   
   @Autowired
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

   @Test
   public void testCreateFailInStrictModeWithVersion() {
      checkNoResource(CityCountrySample.CITY_MODEL_NAME, "UK/London");

      DCResource londonCityData = buildCityData("London", "UK", 10000000, true);
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
   public void testCreateFailWithoutReferencedData() {
      checkNoResource(CityCountrySample.CITY_MODEL_NAME, "UK/London");

      DCResource londonCityData = buildCityData("London", "UK", 10000000, false);
      try {
         datacoreApiClient.postDataInType(londonCityData, CityCountrySample.CITY_MODEL_NAME);
         Assert.fail("Creation should fail when referenced data doesn't exist");
      } catch (WebApplicationException waex) {
         Assert.assertTrue((waex.getResponse().getEntity() + "").contains(
               this.containerUrl + "dc/type/" + CityCountrySample.COUNTRY_MODEL_NAME + "/UK")); // http://localhost:8180/
      }
   }

   @Test
   public void testCreateFailWithWrongLocalReferenceModel() {
      checkNoResource(CityCountrySample.CITY_MODEL_NAME, "UK/London");

      DCResource londonCityData = buildCityData("London", "UK", 10000000, false);
      londonCityData.set("inCountry", londonCityData.getUri());
      try {
         datacoreApiClient.postDataInType(londonCityData, CityCountrySample.CITY_MODEL_NAME);
         Assert.fail("Creation should fail when referenced data is of the wrong (here, same) model");
      } catch (BadRequestException brex) {
         String responseContent = String.valueOf(brex.getResponse().getEntity());
         Assert.assertTrue(responseContent.contains(CityCountrySample.CITY_MODEL_NAME + " does not match"));
      }
   }

   @Test
   public void testCreateFailWithWrongExternalDatacoreReferenceModel() throws Exception {
      String knownExternalDatacoreContainerUrl = "http://otherknowndatacore.org/";

      // create a city with a country stored externally, in another kown Datacore
      DCResource torinoCityData = buildCityData("Torino", "IT", 3000000, false);
      String countryUri = UriHelper.buildUri(knownExternalDatacoreContainerUrl, CityCountrySample.COUNTRY_MODEL_NAME, "IT");
      torinoCityData.setProperty("inCountry", countryUri);
      DCResource postedTorinoCityResource = datacoreApiClient.postDataInType(torinoCityData, CityCountrySample.CITY_MODEL_NAME);
      Assert.assertEquals("External known Datacore resource references "
            + "should not fail when non-existing (unlike local references)",
            postedTorinoCityResource.get("inCountry"), countryUri);

      // create a city with a country stored externally in another kown Datacore, but of illegal city type
      DCResource londonCityData = buildCityData("London", "UK", 10000000, false);
      String externalKnownDatacoreCityUri = UriHelper.buildUri(knownExternalDatacoreContainerUrl,
            CityCountrySample.CITY_MODEL_NAME, "IT/Torino");
      londonCityData.set("inCountry", externalKnownDatacoreCityUri);
      try {
         datacoreApiClient.postDataInType(londonCityData, CityCountrySample.CITY_MODEL_NAME);
         Assert.fail("Creation should fail when referenced data in external known Datacore "
               + "is of the wrong (here, same) model");
      } catch (BadRequestException brex) {
         String responseContent = String.valueOf(brex.getResponse().getEntity());
         Assert.assertTrue(responseContent.contains(CityCountrySample.CITY_MODEL_NAME + " does not match"));
      }
   }

   /**
    * 
    */
   @Test
   public void testCreate() {
      testCreate("UK", "London", 10000000);
   }

   // TODO LATER
   //@Test
   public void testCreateWithReferencedDataInGraph() {
      checkNoResource(CityCountrySample.CITY_MODEL_NAME, "UK/London");

      DCResource londonCityData = buildCityData("London", "UK", 10000000, false);
      DCResource postedLondonCityData = datacoreApiClient.postDataInType(londonCityData, CityCountrySample.CITY_MODEL_NAME);
   }
   
   /**
    * Creates city & country & checks them
    * @param country
    * @param city
    * @return client resource BUT NOT POSTed one (no version)
    */
   public DCResource testCreate(String country, String city, int populationCount) {
      checkNoResource(CityCountrySample.COUNTRY_MODEL_NAME, country);
      
      DCResource ukCountryData = buildNamedData(CityCountrySample.COUNTRY_MODEL_NAME, country);

      try {
         datacoreApiClient.putDataInType(ukCountryData, CityCountrySample.COUNTRY_MODEL_NAME, "UK");
         Assert.fail("Should not be able to update / PUT a non existing Resource");
      } catch (BadRequestException e) {
         Assert.assertTrue(true);
      } catch (Exception e) {
         Assert.fail("Bad exception");
      }
      
      DCResource postedUkCountryData = datacoreApiClient.postDataInType(ukCountryData, CityCountrySample.COUNTRY_MODEL_NAME);
      Assert.assertNotNull(postedUkCountryData);

      DCResource putUkCountryData = datacoreApiClient.putDataInType(postedUkCountryData, CityCountrySample.COUNTRY_MODEL_NAME, "UK");
      Assert.assertNotNull("Should be able to update / PUT an existing Resource", putUkCountryData);
      
      String iri = country + '/' + city;
      checkNoResource(CityCountrySample.CITY_MODEL_NAME, iri);

      DCResource cityData = buildCityData(city, country, 10000000, false);
      DCResource postedLondonCityData = datacoreApiClient.postDataInType(cityData, CityCountrySample.CITY_MODEL_NAME);
      Assert.assertNotNull(postedLondonCityData);
      Assert.assertEquals(cityData.getProperties().get("populationCount"),
            postedLondonCityData.getProperties().get("populationCount"));
      DCResource gottenLondonCityData = datacoreApiClient.getData(CityCountrySample.CITY_MODEL_NAME, iri);
      Assert.assertNotNull(gottenLondonCityData);
      Assert.assertEquals(cityData.getProperties().get("populationCount"),
            postedLondonCityData.getProperties().get("populationCount"));
      
      try {
         datacoreApiClient.postDataInType(cityData, CityCountrySample.CITY_MODEL_NAME);
         Assert.fail("Should not be able to recreate / rePOST a Resource even without a version (unicity)");
      } catch (BadRequestException e) {
         Assert.assertTrue(true);
      } catch (Exception e) {
         Assert.fail("Bad exception");
      }
      
      return cityData;
   }

   private void checkNoResource(String modelType, String id) {
      try {
         datacoreApiClient.getData(modelType, id);
         Assert.fail("There shouldn't be any " + modelType + " with id " + id + " yet");
      } catch (NotFoundException e) {
         Assert.assertTrue(true);
      } catch (Exception e) {
         Assert.fail("Bad exception");
      }
   }

   ///@Test
   public void testCreateEmbedded() {
      checkNoResource(CityCountrySample.CITY_MODEL_NAME, "France/Bordeaux");

      DCResource bordeauxCityData = buildCityData("Bordeaux", "France", 300000, true);
      DCResource postedBordeauxCityData = datacoreApiClient.postDataInType(bordeauxCityData, CityCountrySample.CITY_MODEL_NAME);
      Assert.assertNotNull(postedBordeauxCityData);
   }

   private DCResource buildNamedData(String type, String name) {
      DCResource resource = DCResource.create(containerUrl, type, name).set("name", name);
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
      DCResource cityResource = DCResource.create(containerUrl, type, iri).set("name", name);
      /*DCResource cityResource = new DCResource();
      cityResource.setUri(UriHelper.buildUri(containerUrl, type, iri));
      cityResource.setProperty("name", name);*/
      //cityResource.setVersion(-1l);
      /*cityResource.setProperty("type", type);
      cityResource.setProperty("iri", iri);*/
      cityResource.set("populationCount", populationCount);
      
      String countryUri = UriHelper.buildUri(containerUrl, CityCountrySample.COUNTRY_MODEL_NAME, countryName);
      if (embeddedCountry) {
         DCResource countryResource = buildNamedData(CityCountrySample.COUNTRY_MODEL_NAME, countryName);
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
   public void testGetUpdateVersion() throws Exception {
      // first fill some data
      testCreate();
      
      DCResource data = datacoreApiClient.getData(CityCountrySample.CITY_MODEL_NAME, "UK/London");
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
   public void testClientCache() throws Exception {
      String bordeauxUriToEvict = testCreate("France", "Bordeaux", 300000).getUri();
      resourceCache.evict(bordeauxUriToEvict); // create with country but clean cache

      try {
         datacoreApiClient.deleteData(CityCountrySample.CITY_MODEL_NAME, "France/Bordeaux");
         Assert.fail("Should not be able to delete without (having cache allowing) "
               + "sending content version as ETag");
      } catch (Exception e) {
         Assert.assertTrue(true);
      }
      
      // GET
      // first call, sends ETag which should put result in cache :
      DCResource bordeauxCityResource = datacoreApiClient.getData(CityCountrySample.CITY_MODEL_NAME, "France/Bordeaux");
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
      String creator = postBordeauxCityResource.getCreatedBy();
      Assert.assertTrue("admin".equals(creator) || "guest".equals(creator)); // prod or dev
      Assert.assertEquals("at creation, created & modified dates should be the same",
            creationDate, postBordeauxCityResource.getLastModified());
      Assert.assertEquals("at creation, created & modified auditors should be the same",
            creator, postBordeauxCityResource.getLastModifiedBy());
      
      // put (& patch)
      bordeauxCityResource = datacoreApiClient.postDataInType(bordeauxCityResource, CityCountrySample.CITY_MODEL_NAME); // first create...
      resourceCache.evict(bordeauxCityResource.getUri()); /// ... and clean cache
      DCResource putBordeauxCityResource = datacoreApiClient.putDataInType(bordeauxCityResource, CityCountrySample.CITY_MODEL_NAME, "France/Bordeaux");
      bordeauxCityResource = checkCachedBordeauxCityDataAndDelete(putBordeauxCityResource);
      // check audit data
      Assert.assertEquals("at modification, created date should not change",
            creationDate, postBordeauxCityResource.getCreated());
      Assert.assertEquals("at modification, created auditor should not change",
            creator, postBordeauxCityResource.getCreatedBy());
      Assert.assertNotSame("at modification, modified date should differ from create date",
            creationDate, postBordeauxCityResource.getLastModified());
      String modifier = postBordeauxCityResource.getLastModifiedBy();
      Assert.assertTrue("admin".equals(modifier) || "guest".equals(modifier)); // prod or dev
   }

   @Test
   public void testRemoveProperty() throws Exception {
      // fill some data
      datacoreApiClient.postDataInType(buildNamedData(CityCountrySample.COUNTRY_MODEL_NAME, "UK"));
      DCResource londonCityData = buildCityData("London", "UK", 10000000, false);
      DCResource postedLondonCityResource = datacoreApiClient.postDataInType(londonCityData, CityCountrySample.CITY_MODEL_NAME);
      
      // check return, server's & without cache
      Assert.assertEquals(10000000, postedLondonCityResource.get("populationCount"));
      Assert.assertEquals(10000000, datacoreApiClient.getData(postedLondonCityResource)
            .get("populationCount")); // re-get in cache
      //datacoreApiClient.getCache().clear();
      Assert.assertEquals(10000000, datacoreApiClient.getData(londonCityData)
            .get("populationCount")); // without cache
      
      // removing, but using POST/PATCH so not on server
      postedLondonCityResource.getProperties().remove("populationCount");
      Assert.assertEquals(null, postedLondonCityResource.get("populationCount"));
      postedLondonCityResource = datacoreApiClient.postDataInType(postedLondonCityResource);
      // check return, server's & without cache
      Assert.assertEquals(10000000, postedLondonCityResource.get("populationCount"));
      Assert.assertEquals(10000000, datacoreApiClient.getData(postedLondonCityResource)
            .get("populationCount")); // re-get in cache
      datacoreApiClient.getCache().clear();
      Assert.assertEquals(10000000, datacoreApiClient.getData(londonCityData)
            .get("populationCount")); // without cache
      
      // actually removing, using PUT so also on server
      postedLondonCityResource.getProperties().remove("populationCount");
      Assert.assertEquals(null, postedLondonCityResource.get("populationCount"));
      DCResource putLondonCityResource = datacoreApiClient.putDataInType(postedLondonCityResource);
      // check return, server's & without cache
      Assert.assertEquals(null, putLondonCityResource.get("populationCount"));
      Assert.assertEquals(null, datacoreApiClient.getData(putLondonCityResource)
            .get("populationCount")); // re-get in cache
      datacoreApiClient.getCache().clear();
      Assert.assertEquals(null, datacoreApiClient.getData(londonCityData)
            .get("populationCount")); // without cache
   }

   @Test
   public void testExternalReferencedResource() throws Exception {
      // create a city with a country stored externally, in another unkown Datacore or on the web
      String externalContainerUrl = "http://myotherunkowncontainerorwebsite.org/";
      DCResource torinoCityData = buildCityData("Torino", "IT", 3000000, false);
      String countryUri = UriHelper.buildUri(externalContainerUrl, CityCountrySample.COUNTRY_MODEL_NAME, "IT");
      torinoCityData.setProperty("inCountry", countryUri);
      DCResource postedTorinoCityResource = datacoreApiClient.postDataInType(torinoCityData, CityCountrySample.CITY_MODEL_NAME);
      Assert.assertEquals("External resource references should not fail when non-existing (unlike local references)",
            postedTorinoCityResource.get("inCountry"), countryUri);
      
      // now making it fail, to see external resource check warning :
      postedTorinoCityResource.set("populationCount", "illegal population count");
      try {
         postedTorinoCityResource = datacoreApiClient.postDataInType(postedTorinoCityResource, CityCountrySample.CITY_MODEL_NAME);
         Assert.fail("POST creation of city with String populationCount should fail");
      } catch (BadRequestException brex) {
         String responseContent = String.valueOf(brex.getResponse().getEntity());
         Assert.assertTrue(responseContent.contains(externalContainerUrl)
               && responseContent.contains("UnknownHostException"));
      }
   }
   
   /**
    * For now, client doesn't know when to parse String as Date
    * but we can still check if this String is OK
    * (would require (cached) Models for that)
    * @throws Exception
    */
   @Test
   public void testPropDateStringUtcBC() throws Exception {
      datacoreApiClient.postDataInType(buildNamedData(CityCountrySample.COUNTRY_MODEL_NAME, "UK"));
      DCResource londonCityData = buildCityData("London", "UK", 10000000, false);
      DateTime londonFoundedDate = new DateTime(-43, 4, 1, 0, 0, DateTimeZone.UTC);
      // NB. if created without timezone, the default one is weird : "0300-04-01T00:00:00.000+00:09:21"
      // because http://stackoverflow.com/questions/2420527/odd-results-in-joda-datetime-for-01-04-1893
      londonCityData.setProperty("founded", londonFoundedDate); // testing date field
      
      DCResource postedLondonCityResource = datacoreApiClient.postDataInType(londonCityData, CityCountrySample.CITY_MODEL_NAME);
      String postedlondonFoundedDateString = (String) postedLondonCityResource.get("founded");
      DateTime postedlondonFoundedDateStringParsed = ResourceParsingHelper.parseDate(postedlondonFoundedDateString);
      Assert.assertEquals("POST returned date field should be UTC",
            DateTimeZone.UTC, postedlondonFoundedDateStringParsed.getZone());
      Assert.assertEquals("POST returned date field should be the same date as the one sent",
            londonFoundedDate, postedlondonFoundedDateStringParsed);

      DCResource gottenLondonCityResource = datacoreApiClient.getData(CityCountrySample.CITY_MODEL_NAME,
            "UK/London", -1l); // to force refresh
      String gottenLondonCityResourceDateString = (String) gottenLondonCityResource.get("founded");
      DateTime gottenLondonCityResourceDateStringParsed = ResourceParsingHelper.parseDate(gottenLondonCityResourceDateString);
      Assert.assertEquals("GET returned date field should be UTC",
            DateTimeZone.UTC, gottenLondonCityResourceDateStringParsed.getZone());
      Assert.assertEquals("GET returned date field should be the same date as the one POSTed",
            londonFoundedDate, gottenLondonCityResourceDateStringParsed);
      
      DCResource putLondonCityResource = datacoreApiClient.putDataInType(postedLondonCityResource, CityCountrySample.CITY_MODEL_NAME, "UK/London");
      String putLondonCityResourceDateString = (String) putLondonCityResource.get("founded");
      DateTime putLondonCityResourceDateStringParsed = ResourceParsingHelper.parseDate(putLondonCityResourceDateString);
      Assert.assertEquals("PUT returned date field should be UTC",
            DateTimeZone.UTC, putLondonCityResourceDateStringParsed.getZone());
      Assert.assertEquals("PUT returned date field should be the same date as the one POSTed",
            londonFoundedDate, putLondonCityResourceDateStringParsed);
      
      List<DCResource> foundLondonCityResources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("name", "London"), 0, 10);
      Assert.assertEquals(1, foundLondonCityResources.size());
      DCResource foundLondonCityResource = foundLondonCityResources.get(0);
      String foundLondonCityResourceDateString = (String) foundLondonCityResource.get("founded");
      DateTime foundLondonCityResourceDateStringParsed = ResourceParsingHelper.parseDate(foundLondonCityResourceDateString);
      Assert.assertEquals("GET returned date field should be UTC",
            DateTimeZone.UTC, foundLondonCityResourceDateStringParsed.getZone());
      Assert.assertEquals("GET returned date field should be the same date as the one POSTed",
            londonFoundedDate, foundLondonCityResourceDateStringParsed);
      
      datacoreApiClient.deleteData(CityCountrySample.CITY_MODEL_NAME, "UK/London");
   }
   
   /**
    * For now, client doesn't know when to parse String as Date
    * but we can still check if this String is OK
    * (would require (cached) Models for that)
    * @throws Exception
    */
   @Test
   public void testPropDateStringPlusOneTimezone() throws Exception {
      datacoreApiClient.postDataInType(buildNamedData(CityCountrySample.COUNTRY_MODEL_NAME, "France"));
      DCResource bordeauxCityData = buildCityData("Bordeaux", "France", 10000000, false);
      DateTime bordeauxFoundedDate = new DateTime(300, 4, 1, 0, 0, DateTimeZone.forID("+01:00"));
      // NB. if created without timezone, the default one (i.e. DateTimeZone.forID("Europe/Paris"))
      // is weird : "0300-04-01T00:00:00.000+00:09:21" ; see explanation :
      // http://stackoverflow.com/questions/2420527/odd-results-in-joda-datetime-for-01-04-1893
      bordeauxCityData.setProperty("founded", bordeauxFoundedDate); // testing date field
      
      DCResource postedBordeauxCityResource = datacoreApiClient.postDataInType(bordeauxCityData, CityCountrySample.CITY_MODEL_NAME);
      String postedBordeauxFoundedDateString = (String) postedBordeauxCityResource.get("founded");
      DateTime postedBordeauxFoundedDateStringParsed = ResourceParsingHelper.parseDate(postedBordeauxFoundedDateString);
      Assert.assertEquals("POST returned date field should be UTC",
            DateTimeZone.UTC, postedBordeauxFoundedDateStringParsed.getZone());
      Assert.assertEquals("POST returned date field should mean the same date as the one sent",
            bordeauxFoundedDate.toDateTime(DateTimeZone.UTC), postedBordeauxFoundedDateStringParsed);
      
      DCResource gottenBordeauxCityResource = datacoreApiClient.getData(CityCountrySample.CITY_MODEL_NAME,
            "France/Bordeaux", -1l); // to force refresh
      String gottenBordeauxCityResourceDateString = (String) gottenBordeauxCityResource.get("founded");
      DateTime gottenBordeauxCityResourceDateStringParsed = ResourceParsingHelper.parseDate(gottenBordeauxCityResourceDateString);
      Assert.assertEquals("PUT returned date field should be UTC",
            DateTimeZone.UTC, postedBordeauxFoundedDateStringParsed.getZone());
      Assert.assertEquals("GET returned date field should mean the same date as the one POSTed",
            bordeauxFoundedDate.toDateTime(DateTimeZone.UTC), gottenBordeauxCityResourceDateStringParsed);
      
      DCResource putBordeauxCityResource = datacoreApiClient.putDataInType(postedBordeauxCityResource, CityCountrySample.CITY_MODEL_NAME, "France/Bordeaux");
      String putBordeauxCityResourceDateString = (String) putBordeauxCityResource.get("founded");
      DateTime putBordeauxCityResourceDateStringParsed = ResourceParsingHelper.parseDate(putBordeauxCityResourceDateString);
      Assert.assertEquals("PUT returned date field should be UTC",
            DateTimeZone.UTC, postedBordeauxFoundedDateStringParsed.getZone());
      Assert.assertEquals("PUT returned date field should mean the same date as the one POSTed",
            bordeauxFoundedDate.toDateTime(DateTimeZone.UTC), putBordeauxCityResourceDateStringParsed);
      
      List<DCResource> foundBordeauxCityResources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("name", "Bordeaux"), 0, 10);
      Assert.assertEquals(1, foundBordeauxCityResources.size());
      DCResource foundBordeauxCityResource = foundBordeauxCityResources.get(0);
      String foundBordeauxCityResourceDateString = (String) foundBordeauxCityResource.get("founded");
      DateTime foundBordeauxCityResourceDateStringParsed = ResourceParsingHelper.parseDate(foundBordeauxCityResourceDateString);
      Assert.assertEquals("GET returned date field should be UTC",
            DateTimeZone.UTC, foundBordeauxCityResourceDateStringParsed.getZone());
      Assert.assertEquals("GET returned date field should be the same date as the one POSTed",
            bordeauxFoundedDate.toDateTime(DateTimeZone.UTC), foundBordeauxCityResourceDateStringParsed);
      
      checkCachedBordeauxCityDataAndDelete(putBordeauxCityResource);
   }

   /**
    * For now, client doesn't know when to parse String as Date
    * (would require (cached) Models for that)
    * @throws Exception
    */
   @Test
   @Ignore // LATER
   public void testPropDateJoda() throws Exception {
      DCResource bordeauxCityResource = buildCityData("Bordeaux", "France", 10000000, false);
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
      DCResource cachedBordeauxCityResource = datacoreApiClient.getData(CityCountrySample.CITY_MODEL_NAME, "France/Bordeaux");
      Assert.assertTrue("Should be same (cached) object", expectedCachedBordeauxCityResource == cachedBordeauxCityResource);
      
      // deleting, will send ETag which must be current version :
      datacoreApiClient.deleteData(CityCountrySample.CITY_MODEL_NAME, "France/Bordeaux");
      Assert.assertNull(resourceCache.get(cachedBordeauxCityResource.getUri())); // check that cache has been cleaned
      
      checkNoResource(CityCountrySample.CITY_MODEL_NAME, "France/Bordeaux");

      cachedBordeauxCityResource.setVersion(null); // else mongo optimistic locking exception
      return cachedBordeauxCityResource;
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
      londonCityData.setProperty("founded", londonFoundedDate);
      datacoreApiClient.postDataInType(londonCityData );
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters(), null, null);
      Assert.assertEquals(1, resources.size());
      Assert.assertEquals(this.containerUrl + "dc/type/" + CityCountrySample.CITY_MODEL_NAME + "/UK/London", resources.get(0).getUri()); // http://localhost:8180/

      // query all - two resource
      datacoreApiClient.postDataInType(buildNamedData(CityCountrySample.COUNTRY_MODEL_NAME, "France"));
      DCResource bordeauxCityData = buildCityData("Bordeaux", "France", 10000000, false);
      DateTime bordeauxFoundedDate = new DateTime(300, 4, 1, 0, 0, DateTimeZone.forID("+01:00"));
      bordeauxCityData.setProperty("founded", bordeauxFoundedDate);
      DCResource postedBordeauxCityData = datacoreApiClient.postDataInType(bordeauxCityData);
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

      // More query case - see #9 :
      
      // JSON $in
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("name", "$in[\"Bordeaux\"]"), null, 10);
      Assert.assertEquals(1, resources.size());
      Assert.assertEquals(postedBordeauxCityData.getUri(), resources.get(0).getUri());
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("name", "$in[]"), null, 10);
      Assert.assertEquals(0, resources.size());
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("name", "$in[\"Bordeaux\",\"NotThere\"]"), null, 10);
      Assert.assertEquals(1, resources.size());
      Assert.assertEquals(postedBordeauxCityData.getUri(), resources.get(0).getUri());
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("name", "$in[\"Bordeaux\",\"London\"]"), null, 10);
      Assert.assertEquals(2, resources.size());
      
      // JSON +01:00 date period
      // on year :
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("founded", ">\"0200-04-01T00:00:00.000+01:00\"")
            .add("founded", "<\"0300-04-02T00:00:00.000+01:00\""), null, 10);
      Assert.assertEquals(1, resources.size());
      // on second :
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("founded", ">\"0200-04-01T00:00:00.000+01:00\"")
            .add("founded", "<\"0300-04-01T00:00:00.001+01:00\""), null, 10);
      Assert.assertEquals(1, resources.size());
      // strict :
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("founded", ">\"0300-04-01T00:00:00.000+01:00\"")
            .add("founded", "<\"0300-04-02T00:00:00.000+01:00\""), null, 10);
      Assert.assertEquals(0, resources.size());
      // gte :
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("founded", ">=\"0300-04-01T00:00:00.000+01:00\"")
            .add("founded", "<\"0300-04-02T00:00:00.000+01:00\""), null, 10);
      Assert.assertEquals(1, resources.size());
      // lte :
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("founded", ">\"0200-04-01T00:00:00.000+01:00\"")
            .add("founded", "<=\"0300-04-01T00:00:00.000+01:00\""), null, 10);
      Assert.assertEquals(1, resources.size());
      // on year, using LDP query :
      resources = datacoreApiClient.queryDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("founded", ">\"0200-04-01T00:00:00.000+01:00\"")
            .add("founded", "<\"0300-04-02T00:00:00.000+01:00\"").add("limit", "10").toString(),
            EntityQueryService.LANGUAGE_LDPQL);
      Assert.assertEquals(1, resources.size());
      
      // JSON UTC date period
      // on year :
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("founded", ">\"-0143-04-01T00:00:00.000Z\"")
            .add("founded", "<\"-0043-04-02T00:00:00.000Z\""), null, 10);
      Assert.assertEquals(1, resources.size());
      // on second :
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("founded", ">\"-0143-04-01T00:00:00.000Z\"")
            .add("founded", "<\"-0043-04-01T00:00:00.001Z\""), null, 10);
      Assert.assertEquals(1, resources.size());
      // strict :
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("founded", ">\"-0043-04-01T00:00:00.000Z\"")
            .add("founded", "<\"-0043-04-02T00:00:00.000Z\""), null, 10);
      Assert.assertEquals(0, resources.size());
      // gte :
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("founded", ">=\"-0043-04-01T00:00:00.000Z\"")
            .add("founded", "<\"-0043-04-02T00:00:00.000Z\""), null, 10);
      Assert.assertEquals(1, resources.size());
      // lte :
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("founded", ">\"-0143-04-01T00:00:00.000Z\"")
            .add("founded", "<=\"-0043-04-01T00:00:00.000Z\""), null, 10);
      Assert.assertEquals(1, resources.size());
      // on year, using LDP query :
      resources = datacoreApiClient.queryDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("founded", ">\"-0143-04-01T00:00:00.000Z\"")
            .add("founded", "<\"-0043-04-02T00:00:00.000Z\"").add("limit", "10").toString(),
            EntityQueryService.LANGUAGE_LDPQL);
      Assert.assertEquals(1, resources.size());

      // equals (empty) starting with number
      DCResource cityStartingWithNumberCityData = buildCityData("7henextcity", "France", 10000000, false);
      DCResource postedCityStartingWithNumberCityData = datacoreApiClient.postDataInType(cityStartingWithNumberCityData);
      // works unquoted (not parsed anymore as starting JSON number 7)...
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("name", "7henextcity"), null, 10);
      Assert.assertEquals(1, resources.size());
      Assert.assertEquals(postedCityStartingWithNumberCityData.getUri(), resources.get(0).getUri());
      // and works quoted alright
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("name", "\"7henextcity\""), null, 10);
      Assert.assertEquals(1, resources.size());
      Assert.assertEquals(postedCityStartingWithNumberCityData.getUri(), resources.get(0).getUri());
   }


   @Test
   public void testQueryMaxScanError() throws Exception {
      ldpEntityQueryServiceImpl.setMaxScan(0); // unlimited, default in test
      
      // no resource
      
      // query all
      List<DCResource> resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters(), null, null);
      // should not fail : query has no non indexed field
      Assert.assertEquals(0, resources.size());

      String nonExistingFoundedDate = "=\"1200-04-01T00:00:00.000+01:00\"";

      // unquoted equals (empty)
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("founded", nonExistingFoundedDate), null, 10);
      // should not fail : less documents in db than any maxScan for now
      Assert.assertEquals(0, resources.size());
      
      // two resources
      datacoreApiClient.postDataInType(buildNamedData(CityCountrySample.COUNTRY_MODEL_NAME, "UK"));
      DateTime londonFoundedDate = new DateTime(-43, 4, 1, 0, 0, DateTimeZone.UTC);
      DCResource londonCityData = buildCityData("London", "UK", 10000000, false);
      londonCityData.setProperty("founded", londonFoundedDate);
      datacoreApiClient.postDataInType(londonCityData );
      datacoreApiClient.postDataInType(buildNamedData(CityCountrySample.COUNTRY_MODEL_NAME, "France"));
      DCResource bordeauxCityData = buildCityData("Bordeaux", "France", 10000000, false);
      DateTime bordeauxFoundedDate = new DateTime(300, 4, 1, 0, 0, DateTimeZone.forID("+01:00"));
      bordeauxCityData.setProperty("founded", bordeauxFoundedDate);
      DCResource postedBordeauxCityData = datacoreApiClient.postDataInType(bordeauxCityData);
      
      // query all
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters(), null, null);
      // should not fail : query has no non indexed field
      Assert.assertEquals(2, resources.size());
      // limiting maxScan to less than document nb :
      ldpEntityQueryServiceImpl.setMaxScan(1);
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters(), null, null);
      // should not fail : query has no non indexed field
      Assert.assertEquals(1, resources.size());

      // limiting maxScan to (less or equal than) document nb :
      ldpEntityQueryServiceImpl.setMaxScan(2);
      try {
         // unquoted equals (empty)
         resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
               new QueryParameters().add("founded", nonExistingFoundedDate), null, 10);
         //Assert.assertEquals(0, resources.size());
         //Assert.assertEquals(postedBordeauxCityData.getUri(), resources.get(0).getUri());
         Assert.fail("Should have raised exception because query on non indexed field reached "
               + "maxScan before document limit");
      } catch (BadRequestException brex) {
         Assert.assertTrue((brex.getResponse().getEntity() + "").contains("maxScan"));
      }

   }


   @Test
   public void testFindDcEntityIndexedFields() throws Exception {
      // two resources
      datacoreApiClient.postDataInType(buildNamedData(CityCountrySample.COUNTRY_MODEL_NAME, "UK"));
      DCResource londonCityData = datacoreApiClient.postDataInType(buildCityData("London", "UK", 10000000, false));
      datacoreApiClient.postDataInType(buildNamedData(CityCountrySample.COUNTRY_MODEL_NAME, "France"));
      DCResource bordeauxCityData = datacoreApiClient.postDataInType(buildCityData("Bordeaux", "France", 10000000, false));

      // requiring index
      ldpEntityQueryServiceImpl.setMaxScan(1);

      // unquoted equals (empty)
      List<DCResource> resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("_uri", londonCityData.getUri()), null, 10);
      Assert.assertEquals(1, resources.size());
      Assert.assertEquals(londonCityData.getUri(), resources.get(0).getUri());
      
      resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            new QueryParameters().add("_uri", bordeauxCityData.getUri()), null, 10);
      Assert.assertEquals(1, resources.size());
      Assert.assertEquals(bordeauxCityData.getUri(), resources.get(0).getUri());
   }
   
   @Test
   public void testQueryInList() {
      cityCountrySample.initData();

      QueryParameters paramsPoi = new QueryParameters().add("name", "Mole Antonelliana");
      List<DCResource> poi = datacoreApiClient.findDataInType(CityCountrySample.POI_MODEL_NAME,
            paramsPoi, null, 10);
      Assert.assertEquals(1, poi.size());

      // in city :
      QueryParameters params = new QueryParameters().add("pointsOfInterest", poi.get(0).getUri());
      List<DCResource> resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            params, null, 10);
      Assert.assertEquals(1, resources.size());
   }

   @Test
   public void testI18nDraft() {
      //Create Russia
      //checkNoResource(CityCountrySample.COUNTRY_MODEL_NAME, "Russia");
      DCResource russiaCountryData = buildNamedData(CityCountrySample.COUNTRY_MODEL_NAME, "Russia");
      DCResource postedRussiaCountryData = datacoreApiClient.postDataInType(russiaCountryData, CityCountrySample.COUNTRY_MODEL_NAME);
      Assert.assertNotNull(postedRussiaCountryData);

      //Create Moscow
      DateTime moscowFoundedDate = new DateTime(1147, 4, 4, 0, 0, DateTimeZone.UTC);
      DCResource moscowCityData = buildCityData("Moscow", "Russia", 10000000, false).set("founded", moscowFoundedDate);

      //i18n
      HashMap<String, String> fr = new HashMap<String, String>();
      fr.put("t", "fr");
      fr.put("v", "Moscou");
      HashMap<String, String> en = new HashMap<String, String>();
      en.put("t", "en");
      en.put("v", "Moscow");
      HashMap<String, String> us = new HashMap<String, String>();
      us.put("t", "us");
      us.put("v", "Moscow");
      HashMap<String, String> ru = new HashMap<String, String>();
      ru.put("t", "ru");
      ru.put("v", "Моskva");
      //ru.put("v", "Москва");
      ArrayList<HashMap<String, String>> i18n = new ArrayList<HashMap<String, String>>();
      i18n.add(fr);
      i18n.add(en);
      i18n.add(us);
      i18n.add(ru);
      moscowCityData.setProperty("i18Name", i18n);

      datacoreApiClient.postDataInType(moscowCityData);

      QueryParameters params = new QueryParameters();
      params.add("i18Name.v", "Moscow");
      //params.add("debug", "true");
      List<DCResource> resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            params, null, 10);
      Assert.assertEquals(1, resources.size());
      Assert.assertEquals(moscowCityData.getUri(), resources.get(0).getUri());
      Assert.assertEquals(moscowCityData.get("i18Name"), resources.get(0).get("i18Name"));
   }

}
