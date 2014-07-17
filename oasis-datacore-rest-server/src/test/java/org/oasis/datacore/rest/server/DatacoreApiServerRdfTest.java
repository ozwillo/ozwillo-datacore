package org.oasis.datacore.rest.server;

import java.util.ArrayList;
import java.util.List;

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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-rest-server-test-context.xml" })
public class DatacoreApiServerRdfTest {

   @Autowired
   @Qualifier("datacoreApiCachedRdfClient")
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

   private DCResource buildNamedData(String type, String name) {
      DCResource resource = DCResource.create(containerUrl, type, name).set("n:name", name);
      return resource;
   }

   private DCResource buildCityData(String name, String countryName,
         int populationCount, boolean embeddedCountry) {
      String type = CityCountrySample.CITY_MODEL_NAME;
      String iri = countryName + '/' + name;
      DCResource cityResource = DCResource.create(containerUrl, type, iri).set("n:name", name);
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
   public void getRdf() {
      cityCountrySample.initData();
      
      QueryParameters params = new QueryParameters().add("i18n:name.v", "Moscow");
      List<DCResource> resources = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            params, null, 10);
      Assert.assertEquals(1, resources.size());
   }
   
   @Test
   public void sendSimpleRdf() {
      DCResource ukCountry = buildNamedData(CityCountrySample.COUNTRY_MODEL_NAME, "UK");
      DCResource resource = datacoreApiClient.postDataInType(ukCountry);
      Assert.assertEquals(ukCountry.getUri(), resource.getUri());
   }
   
   @Test
   public void sendLessSimpleRdf() {
      datacoreApiClient.postDataInType(buildNamedData(CityCountrySample.COUNTRY_MODEL_NAME, "UK"));
      
      DateTime londonFoundedDate = new DateTime(-43, 4, 1, 0, 0, DateTimeZone.UTC);
      DCResource londonCityData = buildCityData("London", "UK", 10000000, false);
      londonCityData.setProperty("city:founded", londonFoundedDate);
      londonCityData.setProperty("i18n:name", DCResource.listBuilder()
            .add(DCResource.propertiesBuilder().put("l", "fr").put("v", "Londres").build())
            .add(DCResource.propertiesBuilder().put("l", "en").put("v", "London").build())
            .add(DCResource.propertiesBuilder().put("l", "us").put("v", "London").build()).build());
      DCResource resource = datacoreApiClient.postDataInType(londonCityData);
      
      Assert.assertEquals(londonCityData.getUri(), resource.getUri());
   }
   
   @Test
   public void sendMultipleRdf() {
      ArrayList<DCResource> resourceList = new ArrayList<DCResource>();
      DCResource ukCountry = buildNamedData(CityCountrySample.COUNTRY_MODEL_NAME, "UK");
      DCResource frCountry = buildNamedData(CityCountrySample.COUNTRY_MODEL_NAME, "FR");
      resourceList.add(ukCountry);
      resourceList.add(frCountry);
      List<DCResource> resources = datacoreApiClient.postAllDataInType(resourceList, CityCountrySample.COUNTRY_MODEL_NAME);
      Assert.assertEquals(frCountry.getUri(), resources.get(0).getUri());
   }
   
}
