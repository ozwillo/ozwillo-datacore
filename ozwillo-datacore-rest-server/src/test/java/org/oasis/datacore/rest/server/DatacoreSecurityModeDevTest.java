package org.oasis.datacore.rest.server;

import java.net.URI;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.WebApplicationException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.common.context.SimpleRequestContextProvider;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryServiceImpl;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.oasis.datacore.rest.api.util.UnitTestHelper;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
import org.oasis.datacore.rest.client.cxf.mock.AuthenticationHelper;
import org.oasis.datacore.rest.server.resource.ResourceException;
import org.oasis.datacore.rest.server.resource.ResourceNotFoundException;
import org.oasis.datacore.rest.server.resource.ResourceService;
import org.oasis.datacore.sample.CityCountrySample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.ImmutableMap;


/**
 * 
 * @author mdutoo
 *
 */
@Ignore // doesn't work because no HTTP server (had already failed to add jetty)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-deploy-context.xml" })
//@ContextConfiguration(locations = { "classpath:oasis-datacore-rest-server-test-context.xml" })
//@FixMethodOrder(MethodSorters.NAME_ASCENDING) // else random since java 7 NOT REQUIRED ANYMORE
public class DatacoreSecurityModeDevTest {

   @Value("${datacore.securitymode:devmode}")
   protected String securitymode;
   
   @Autowired
   @Qualifier("datacoreApiCachedJsonClient")
   private DatacoreCachedClient datacoreApiClient;

   /** to clean cache for tests */
   @Autowired
   @Qualifier("datacore.rest.client.cache.rest.api.DCResource")
   private Cache resourceCache; // EhCache getNativeCache
   
   /** to be able to build a full uri, to check in tests
    * TODO rather client-side DCURI or rewrite uri in server */
   ///@Value("${datacoreApiClient.baseUrl}") 
   ///private String baseUrl; // useless
   @Value("${datacoreApiClient.containerUrl}") 
   private String containerUrlString;
   @Value("#{new java.net.URI('${datacoreApiClient.containerUrl}')}")
   //@Value("#{uriService.getContainerUrl()}")
   private URI containerUrl;
   
   /** for testing purpose */
   @Autowired
   @Qualifier("datacoreApiImpl") 
   private DatacoreApiImpl datacoreApiImpl;
   /** for testing purpose */
   @Autowired
   private LdpEntityQueryServiceImpl ldpEntityQueryServiceImpl;
   @Autowired
   private ResourceService resourceService;
   
   @Autowired
   private CityCountrySample cityCountrySample;
   
   private DCResource testResource;
   
   
   @Before
   public void resetAndSetProject() throws ResourceException {
      // cleanDataAndCache :
      cityCountrySample.cleanDataOfCreatedModels(); // (was already called but this first cleans up data)
      datacoreApiClient.getCache().clear(); // to avoid side effects
      
      // set sample project :
      SimpleRequestContextProvider.setSimpleRequestContext(new ImmutableMap.Builder<String, Object>()
            .put(DatacoreApi.PROJECT_HEADER, DCProject.OASIS_SAMPLE).build());
      // TODO or rather DCRequestContextProvider.PROJECT ???
      testResource = DCResource.create(containerUrl,
            CityCountrySample.COUNTRY_MODEL_NAME, "UK")
            .set("n:name", "UK");
      try {
         AuthenticationHelper.loginAs("admin");
         DCResource testResourceFound = resourceService.get(testResource.getUri(), testResource.getModelType());
         resourceService.delete(testResource.getUri(), testResource.getModelType(), testResourceFound.getVersion());
      } catch (ResourceNotFoundException e) {
         // if no such test failed previously
      } catch (ResourceException e) {
         e.printStackTrace();
      } finally {
         AuthenticationHelper.logout();
      }
   }

   /**
    * Logout after tests to restore default unlogged state.
    * This is required in tests that use authentication,
    * else if last test fails, tests that don't login will use logged in user
    * rather than default one, which may trigger a different behaviour and
    * make some tests fail (ex. DatacoreApiServerTest.test3clientCache asserting
    * that creator is admin or guest).
    */
   @After
   public void logoutAfter() {
      AuthenticationHelper.logout();
   }
   
   @Test
   public void ensureSecurityMode() {
      if (!"devmode".equals(securitymode)) {
         Assert.fail("This test should be run with devmode=true");
      }
   }
   
   @Test
   public void testNotAuthentified() {
      try {
         datacoreApiClient.postDataInType(testResource, CityCountrySample.COUNTRY_MODEL_NAME);
         Assert.fail("Should not be able to update / PUT because of production security");
      } catch (NotAuthorizedException e) {
         Assert.assertTrue(true);
      /*} catch (WebApplicationException waex) {
         String responseContent = UnitTestHelper.readBodyAsString(waex);
         Assert.assertTrue(responseContent.toLowerCase().contains("strict"));*/
      } catch (Exception e) {
         Assert.fail("Bad exception");
      }
   }
   
   @Test
   public void testGuest() {
      AuthenticationHelper.loginAs("guest");
      try {
         datacoreApiClient.postDataInType(testResource, CityCountrySample.COUNTRY_MODEL_NAME);
         Assert.fail("Should not be able to update / PUT because of production security");
      } catch (NotAuthorizedException e) {
         Assert.assertTrue(true);
      /*} catch (WebApplicationException waex) {
         String responseContent = UnitTestHelper.readBodyAsString(waex);
         Assert.assertTrue(responseContent.toLowerCase().contains("strict"));*/
      } catch (Exception e) {
         Assert.fail("Bad exception");
      }
   }
   
   @Test
   public void testAdmin() {
      AuthenticationHelper.loginBasicAsAdmin();
      try {
         try {
            resourceService.get(testResource.getUri(), testResource.getModelType()); 
         } catch (Exception e) {
            ///Assert.fail("Bad exception");
         }
         datacoreApiClient.postDataInType(testResource, CityCountrySample.COUNTRY_MODEL_NAME);
         Assert.assertTrue(true);
      } catch (WebApplicationException waex) {
         String responseContent = UnitTestHelper.readBodyAsString(waex);
         System.err.println(responseContent);
         throw new RuntimeException(responseContent);
         //Assert.assertTrue(responseContent.toLowerCase().contains("strict"));
         //Assert.fail("Bad exception");
      } catch (Exception e) {
         Assert.fail("Bad exception");
      }
   }

}
