package org.oasis.datacore.rest.api.client;

import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
import org.oasis.datacore.rest.client.QueryParameters;
import org.oasis.datacore.rest.client.cxf.mock.MockClientAuthenticationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * Tests CXF client with mock server : simple get / post / put version & cache
 * 
 * @author mdutoo
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-rest-client-test-context.xml" })
public class DatacoreApiCXFClientTest {

   public static final String TEST_USER_JOHN = "john";
   
   //@Autowired
   //@Qualifier("datacoreApiClient")
   //private /DatacoreApi/DatacoreClientApi datacoreApiClient;
   @Autowired // injection brings control (?)
   @Qualifier("datacoreApiCachedClient") // rather than...
   private DatacoreCachedClient datacoreApiClient; // NOT DatacoreApiCachedClientImpl ; Datacore(Client)Api ??
   
   /** to be able to build a full uri, to check in tests
    * TODO rather client-side DCURI or rewrite uri in server */
   ///@Value("${datacoreApiClient.baseUrl}") 
   ///private String baseUrl; // useless
   @Value("${datacoreApiClient.containerUrl}") 
   private String containerUrl;

   @Test
   public void testEncodingLibs() throws Exception {
      String value = "\"Bordeaux&= +.;#~_\"";
      String cxfEncodedValue = "name=" + HttpUtils.encodePartiallyEncoded(value, true);
      String jdkEncodedValue = "name=" + URLEncoder.encode(value, "UTF-8");
      Assert.assertEquals("JDK and CXF should do same URL encoding", jdkEncodedValue, cxfEncodedValue);
   }
   
   /**
    * Tests the CXF client with the DatacoreApi service
    * @throws Exception If a problem occurs
    */
   @Test
   public void getPostVersionTest() throws Exception {
      DCResource resource = datacoreApiClient.getData("city", "UK/London");
      Assert.assertNotNull(resource);
      Assert.assertTrue("should be different object, because in mock HTTP 308 (which triggers cache) is disabled save for Bordeaux",
            resource != datacoreApiClient.getData("city", "UK/London"));
      Assert.assertNotNull(resource.getVersion());
      long version = resource.getVersion();
      Assert.assertEquals(this.containerUrl + "dc/type/city/UK/London", resource.getUri());
      /*Assert.assertEquals("city", resource.getProperties().get("type"));
      Assert.assertEquals("UK/London", resource.getProperties().get("iri"));*/
      Assert.assertEquals("London", resource.getProperties().get("name"));
      Object inCountryFound = resource.getProperties().get("inCountry");
      Assert.assertTrue(inCountryFound instanceof Map<?,?>);
      @SuppressWarnings("unchecked")
      Map<String,Object> inCountry = (Map<String,Object>) inCountryFound;
      Assert.assertEquals(this.containerUrl + "dc/type/country/UK", inCountry.get("uri"));
      Assert.assertEquals("UK", inCountry.get("name"));
      DCResource postedResource = datacoreApiClient.postDataInType(resource, "city");
      Assert.assertNotNull(postedResource);
      Assert.assertEquals(version + 1, (long) postedResource.getVersion());
   }

   @Test
   public void clientCacheTest() throws Exception {
      try {
         datacoreApiClient.deleteData("city", "France/Bordeaux");
         Assert.fail("Should not be able to delete without (having cache allowing) "
               + "sending cuttent version as ETag");
      } catch (Exception e) {
         Assert.assertTrue(true);
      }
      
      // GET
      // first call, sends ETag which should put result in cache :
      DCResource bordeauxCityResource = datacoreApiClient.getData("city", "France/Bordeaux");
      checkCachedBordeauxCityResource(bordeauxCityResource);

      // post
      DCResource postBordeauxCityResource = datacoreApiClient.postDataInType(bordeauxCityResource, "city");
      checkCachedBordeauxCityResource(postBordeauxCityResource);
      // put (patch)
      DCResource putBordeauxCityResource = datacoreApiClient.putDataInType(bordeauxCityResource, "city", "France/Bordeaux");
      checkCachedBordeauxCityResource(putBordeauxCityResource);
   }

   private void checkCachedBordeauxCityResource(DCResource expectedCachedBordeauxCityResource) {
      // second call, should return 308
      DCResource cachedBordeauxCityResource = datacoreApiClient.getData("city", "France/Bordeaux");
      Assert.assertTrue("Should be same (cached) object", expectedCachedBordeauxCityResource == cachedBordeauxCityResource);
      datacoreApiClient.deleteData("city", "France/Bordeaux", null);
      DCResource renewBordeauxCityData = datacoreApiClient.getData("city", "France/Bordeaux");
      Assert.assertFalse("Should not be cached object anymore", expectedCachedBordeauxCityResource == renewBordeauxCityData);
   }
   
   @Test
   public void testMockAuthenticationTestUser() {
      MockClientAuthenticationHelper.loginAs(TEST_USER_JOHN);
      try {
         List<DCResource> res = datacoreApiClient.findDataInType(DatacoreApiMockServerImpl.TEST_USER_MODEL_TYPE_QUERY_TRIGGER,
               new QueryParameters(), 0, 0);
         Assert.assertTrue("Test user login should have been found in testUser header",
               res != null && !res.isEmpty() && TEST_USER_JOHN.equals(res.get(0).get(DatacoreApi.TEST_USER)));
      } finally {
         MockClientAuthenticationHelper.logout();
      }
   }
}
