package org.oasis.datacore.rest.server;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.oasis.datacore.common.context.SimpleRequestContextProvider;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.core.security.mock.LocalAuthenticationService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
import org.oasis.datacore.rest.client.QueryParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.ImmutableMap;


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
   private LocalAuthenticationService authenticationService;
   
   @Autowired
   private DataModelServiceImpl modelAdminService; // TODO rm refactor

   @Before
   public void setProject() {
      SimpleRequestContextProvider.setSimpleRequestContext(new ImmutableMap.Builder<String, Object>()
            .put(DatacoreApi.PROJECT_HEADER, DCProject.OASIS_SAMPLE).build());
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
      authenticationService.logout();
   }

   @Test
   public void testProvto() {
      Assert.assertNotNull(modelAdminService.getModelBase("coita:ateco_0"));
      List<DCResource> atecos = datacoreApiClient.findDataInType("coita:ateco_0", null, null, 10);
      Assert.assertTrue(atecos != null && !atecos.isEmpty());

      Assert.assertNotNull(modelAdminService.getModelBase("co:company_0"));
      List<DCResource> companies = datacoreApiClient.findDataInType("co:company_0", null, null, 10);
      Assert.assertTrue(companies != null && !companies.isEmpty());

      Assert.assertNotNull(modelAdminService.getModelBase("plo:country_0"));
      List<DCResource> italia = datacoreApiClient.findDataInType("plo:country_0",
            new QueryParameters().add("plo:name_i18n.v", "Italia"), null, 10);
      Assert.assertTrue(italia != null && !italia.isEmpty());

      Assert.assertNotNull(modelAdminService.getModelBase("pli:city_0"));
      List<DCResource> torino = datacoreApiClient.findDataInType("pli:city_0",
            new QueryParameters().add("pli:name_i18n.v", "Torino"), null, 10);
      Assert.assertTrue(torino != null && !torino.isEmpty());

      authenticationService.logout(); // NB. not required since followed by login
   }
}
