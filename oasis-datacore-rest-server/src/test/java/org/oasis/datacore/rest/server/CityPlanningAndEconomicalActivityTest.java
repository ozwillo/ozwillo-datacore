package org.oasis.datacore.rest.server;

import java.net.URI;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.oasis.datacore.core.entity.EntityService;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryService;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.security.EntityPermissionService;
import org.oasis.datacore.core.security.mock.MockAuthenticationService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
import org.oasis.datacore.rest.client.QueryParameters;
import org.oasis.datacore.rest.server.event.EventService;
import org.oasis.datacore.rest.server.resource.ResourceService;
import org.oasis.datacore.sample.CityPlanningAndEconomicalActivitySample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * 
 * @author mdutoo
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-rest-server-test-context.xml" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING) // else random since java 7
public class CityPlanningAndEconomicalActivityTest {
   
   @Autowired
   @Qualifier("datacoreApiCachedJsonClient")
   private /*DatacoreApi*/DatacoreCachedClient datacoreApiClient;
   
   @Autowired
   private ResourceService resourceService;
   @Autowired
   private EventService eventService;
   
   /** to init models */
   @Autowired
   private /*static */DataModelServiceImpl modelServiceImpl;
   ///@Autowired
   ///private CityCountrySample cityCountrySample;
   
   /** to cleanup db
    * TODO LATER rather in service */
   @Autowired
   private /*static */MongoOperations mgo;
   /** to setup security tests */
   @Autowired
   private EntityService entityService;
   @Autowired
   private EntityPermissionService entityPermissionService;
   @Autowired
   private MockAuthenticationService authenticationService;
   
   /** to be able to build a full uri, to check in tests
    * TODO rather client-side DCURI or rewrite uri in server */
   ///@Value("${datacoreApiClient.baseUrl}") 
   ///private String baseUrl; // useless
   @Value("${datacoreApiClient.containerUrl}")
   private String containerUrlString;
   @Value("#{new java.net.URI('${datacoreApiClient.containerUrl}')}")
   //@Value("#{uriService.getContainerUrl()}")
   private URI containerUrl;

   /** for testing purpose, including of security */
   @Autowired
   @Qualifier("datacoreApiImpl") 
   private DatacoreApiImpl datacoreApiImpl;
   /** for security testing purpose */
   @Autowired
   private LdpEntityQueryService ldpEntityQueryService;

   @Autowired
   private DataModelServiceImpl modelAdminService; // TODO rm refactor

   @Autowired
   private CityPlanningAndEconomicalActivitySample cityPlanningAndEconomicalActivitySample;
   
   
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
      authenticationService.logout();
   }

   @Test
   public void testProvto() {
      Assert.assertNotNull(modelAdminService.getModel("coita:ateco_0"));
      List<DCResource> atecos = datacoreApiClient.findDataInType("coita:ateco_0", null, null, 10);
      Assert.assertTrue(atecos != null && !atecos.isEmpty());

      Assert.assertNotNull(modelAdminService.getModel("co:company_0"));
      List<DCResource> companies = datacoreApiClient.findDataInType("co:company_0", null, null, 10);
      Assert.assertTrue(companies != null && !companies.isEmpty());

      Assert.assertNotNull(modelAdminService.getModel("plo:country_0"));
      List<DCResource> italia = datacoreApiClient.findDataInType("plo:country_0",
            new QueryParameters().add("plo:name_i18n.v", "Italia"), null, 10);
      Assert.assertTrue(italia != null && !italia.isEmpty());

      Assert.assertNotNull(modelAdminService.getModel("pli:city_0"));
      List<DCResource> torino = datacoreApiClient.findDataInType("pli:city_0",
            new QueryParameters().add("pli:name_i18n.v", "Torino"), null, 10);
      Assert.assertTrue(torino != null && !torino.isEmpty());
      
      /*
      cityPlanningAndEconomicalActivitySample.init();
      
      authenticationService.loginAs("admin"); // else ign resources not writable
      
      cityPlanningAndEconomicalActivitySample.doInit();

      DCModelBase ignParcelleModel = modelAdminService.getModel(IgnCityhallSample.IGN_PARCELLE);
      Assert.assertEquals("numeroParcelle field should be original one",
            100, ignParcelleModel.getGlobalField("numeroParcelle").getQueryLimit());
      
      ignCityhallSample.initCityhallIgnV1Mixin();

      ignParcelleModel = modelAdminService.getModel(IgnCityhallSample.IGN_PARCELLE);
      Assert.assertEquals("numeroParcelle field should be overriding Cityhall Mixin's",
            101, ignParcelleModel.getGlobalField("numeroParcelle").getQueryLimit());
      
      ignCityhallSample.initCityhallIgnV2Inheritance();

      DCModelBase cityhallIgnParcelleModel = modelAdminService.getModel(IgnCityhallSample.CITYHALL_IGN_PARCELLE);
      Assert.assertEquals("numeroParcelle field should be Cityhall Mixin's overriding original one copied / inherited using Mixin",
            102, cityhallIgnParcelleModel .getGlobalField("numeroParcelle").getQueryLimit());
      */
      authenticationService.logout(); // NB. not required since followed by login
   }
   

}
