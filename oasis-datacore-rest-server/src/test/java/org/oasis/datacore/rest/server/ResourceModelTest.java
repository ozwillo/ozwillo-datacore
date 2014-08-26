package org.oasis.datacore.rest.server;

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
import org.oasis.datacore.sample.ResourceModelIniter;
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
public class ResourceModelTest {
   
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
   private String containerUrl;

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
   private ResourceModelIniter ResourceModelIniter;
   
   
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
   public void testResourceModel() {
      Assert.assertNotNull(modelAdminService.getModel("dcmo:model_0"));
      List<DCResource> models = datacoreApiClient.findDataInType("dcmo:model_0", null, null, 10);
      Assert.assertTrue(models != null && !models.isEmpty());

      Assert.assertNotNull(modelAdminService.getModel("dcmi:mixin_0"));
      List<DCResource> mixins = datacoreApiClient.findDataInType("dcmi:mixin_0", null, null, 10);
      Assert.assertTrue(mixins != null && !mixins.isEmpty());

      List<DCResource> arePlaceModels = datacoreApiClient.findDataInType("dcmo:model_0",
            new QueryParameters().add("dcmo:globalMixins", "pl:place_0"), null, 10);
      Assert.assertTrue(arePlaceModels != null && !arePlaceModels.isEmpty());

      List<DCResource> haveCountryModels = datacoreApiClient.findDataInType("dcmo:model_0",
            new QueryParameters().add("dcmo:globalFields.dcmf:name", "plo:name"), null, 10);
      Assert.assertTrue(haveCountryModels != null && !haveCountryModels.isEmpty());
      
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
   }
   

}
