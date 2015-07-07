package org.oasis.datacore.rest.server;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.oasis.datacore.common.context.SimpleRequestContextProvider;
import org.oasis.datacore.core.entity.EntityService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.query.QueryException;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryService;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCSecurity;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.core.security.EntityPermissionService;
import org.oasis.datacore.core.security.mock.LocalAuthenticationService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
import org.oasis.datacore.rest.client.QueryParameters;
import org.oasis.datacore.rest.server.event.EventService;
import org.oasis.datacore.rest.server.resource.ResourceException;
import org.oasis.datacore.rest.server.resource.ResourceService;
import org.oasis.datacore.sample.AltTourismPlaceAddressSample;
import org.oasis.datacore.sample.IgnCityhallSample;
import org.oasis.datacore.server.uri.BadUriException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;


/**
 * 
 * @author mdutoo
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-rest-server-test-context.xml" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING) // else random since java 7
public class DatacoreApiServerMixinTest {
   
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
   private LocalAuthenticationService authenticationService;
   
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
   private IgnCityhallSample ignCityhallSample;
   @Autowired
   private AltTourismPlaceAddressSample altTourismPlaceAddressSample;
   


   @Before
   public void cleanDataAndCacheAndSetProject() {
      ignCityhallSample.cleanDataOfCreatedModels(); // (was already called but this first cleans up data)
      altTourismPlaceAddressSample.cleanDataOfCreatedModels(); // (was already called but this first cleans up data)
      datacoreApiClient.getCache().clear(); // to avoid side effects

      SimpleRequestContextProvider.setSimpleRequestContext(new ImmutableMap.Builder<String, Object>()
            .put(DatacoreApi.PROJECT_HEADER, DCProject.OASIS_SAMPLE).build());
   }
   /**
    * Cleans up data of all Models
    */
   /*@Test // rather than @BeforeClass, else static and spring can't inject
   //@BeforeClass
   public void init1cleanupData() {
      for (DCModel model : modelServiceImpl.getModelMap().values()) {
         mgo.remove(new Query(), model.getCollectionName());
         Assert.assertEquals(0,  mgo.findAll(DCEntity.class, model.getCollectionName()).size());
      }
   }*/
   /**
    * Cleans up models
    */
   /*@Test // rather than @BeforeClass, else static and spring can't inject
   //@BeforeClass
   public void init1cleanupModel() {
      modelAdminService.getModelMap().clear();
   }*/
   
   @Test // rather than @BeforeClass, else static and spring can't inject
   //@BeforeClass
   public /*static */void init1setupModels() {
      
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
   public void testIgn() {
      /*ignCityhallSample.cleanCreatedModels();

      List<DCModelBase> modelsIgn = new ArrayList<DCModelBase>();
      ignCityhallSample.buildModelsIgn(modelsIgn);
      ignCityhallSample.initModels(modelsIgn);
      ignCityhallSample.fillDataIgn();*/
      // cleaning data AND models is required by this test :
      ignCityhallSample.initModels();
      
      authenticationService.loginAs("admin"); // else ign resources not writable

      ignCityhallSample.fillDataIgn();

      DCModelBase ignParcelleModel = modelAdminService.getModelBase(IgnCityhallSample.IGN_PARCELLE);
      Assert.assertEquals("numeroParcelle field should be original one",
            100, ignParcelleModel.getGlobalField("numeroParcelle").getQueryLimit());

      List<DCModelBase> modelsCityhallIgnV1Mixin = new ArrayList<DCModelBase>();
      ignCityhallSample.buildModelsCityhallIgnV1Mixin(modelsCityhallIgnV1Mixin);
      ignCityhallSample.initModels(modelsCityhallIgnV1Mixin);

      ignParcelleModel = modelAdminService.getModelBase(IgnCityhallSample.IGN_PARCELLE);
      Assert.assertEquals("numeroParcelle field should be overriding Cityhall Mixin's",
            101, ignParcelleModel.getGlobalField("numeroParcelle").getQueryLimit());

      List<DCModelBase> modelsCityhallIgnV2Inheritance = new ArrayList<DCModelBase>();
      ignCityhallSample.buildModelsCityhallIgnV2Inheritance(modelsCityhallIgnV2Inheritance);
      ignCityhallSample.initModels(modelsCityhallIgnV2Inheritance);

      DCModelBase cityhallIgnParcelleModel = modelAdminService.getModelBase(IgnCityhallSample.CITYHALL_IGN_PARCELLE);
      Assert.assertEquals("numeroParcelle field should be Cityhall Mixin's overriding original one copied / inherited using Mixin",
            102, cityhallIgnParcelleModel .getGlobalField("numeroParcelle").getQueryLimit());
      
      authenticationService.logout(); // NB. not required since followed by login
   }
   
   @Test
   public void testAddress() throws ResourceException, BadUriException, QueryException {
      // cleaning data AND models is required by this test :
      altTourismPlaceAddressSample.initModels();
      
      
      // Mixin for shared fields - use 1
      // MY_APP_PLACE
      
      DCResource myAppPlace1 = resourceService.create(AltTourismPlaceAddressSample.MY_APP_PLACE, "my_place_1").set("name", "my_place_1");
      Assert.assertNotNull(myAppPlace1);
      Assert.assertNull(myAppPlace1.getVersion());
      //Assert.assertEquals("...", myAppPlace1.getUri());
      Assert.assertEquals(1, myAppPlace1.getTypes().size());
      Assert.assertEquals(AltTourismPlaceAddressSample.MY_APP_PLACE, myAppPlace1.getModelType());
      Assert.assertEquals("my_place_1", myAppPlace1.getId());
      Assert.assertEquals(1, myAppPlace1.getProperties().size());
      Assert.assertEquals("my_place_1", myAppPlace1.get("name"));
      Assert.assertNull(myAppPlace1.get("zipCode")); // no address field yet
      
      DCResource myAppPlace1Posted = datacoreApiClient.postDataInType(myAppPlace1);
      Assert.assertNotNull(myAppPlace1Posted);
      Assert.assertEquals(myAppPlace1Posted.get("name"),  "my_place_1");
      Assert.assertNull(myAppPlace1Posted.get("zipCode")); // no address field yet
      
      // check that field without mixin fails, at update : 
      myAppPlace1Posted.set("zipCode", "69100");
      try {
         datacoreApiClient.putDataInType(myAppPlace1Posted);
         Assert.fail("Field without mixin should fail at update");
      } catch (BadRequestException brex) {
         Assert.assertTrue(true);
      }

      // check that field without mixin fails, same but already at creation :
      DCResource myAppPlace2 = resourceService.create(AltTourismPlaceAddressSample.MY_APP_PLACE, "my_place_2").set("name", "my_place_2")
            .set("zipCode", "69100");
      Assert.assertEquals("my_place_2", myAppPlace2.get("name"));
      try {
         datacoreApiClient.postDataInType(myAppPlace2);
         Assert.fail("Field without mixin should fail at creation");
      } catch (BadRequestException brex) {
         Assert.assertTrue(true);
      }
      
      // step 2 - now adding mixin
      modelAdminService.getModelBase(AltTourismPlaceAddressSample.MY_APP_PLACE)
         .addMixin(modelAdminService.getModelBase(AltTourismPlaceAddressSample.OASIS_ADDRESS));
      ///modelAdminService.addModel(myAppPlaceAddress); // LATER re-add...

      // check, at update :
      DCResource myAppPlace1Put = datacoreApiClient.putDataInType(myAppPlace1Posted);
      Assert.assertEquals("types should include mixin", 2, myAppPlace1Put.getTypes().size());
      Assert.assertNotNull(myAppPlace1Put);
      Assert.assertEquals(myAppPlace1Put.get("name"),  "my_place_1");
      Assert.assertEquals("69100", myAppPlace1Put.get("zipCode")); // now address field

      // check, same but already at creation :
      DCResource myAppPlace2Posted = datacoreApiClient.postDataInType(myAppPlace2);
      Assert.assertEquals("types should include mixin", 2, myAppPlace2Posted.getTypes().size());
      Assert.assertNotNull(myAppPlace2Posted);
      Assert.assertEquals(myAppPlace2Posted.get("name"),  "my_place_2");
      Assert.assertEquals("69100", myAppPlace2Posted.get("zipCode")); // now address field
      
      
      // Mixin for shared fields - use 2
      // ALTTOURISM_PLACEKIND

      DCResource altTourismHotelKind = resourceService.create(AltTourismPlaceAddressSample.ALTTOURISM_PLACEKIND, "hotel").set("name", "hotel");
      altTourismHotelKind = datacoreApiClient.postDataInType(altTourismHotelKind);

      // check that only ALTTOURISM_PLACEKIND is accepted as kind :
      // TODO LATER should explode already here
      DCResource altTourismPlaceJoWineryBad = resourceService.create(AltTourismPlaceAddressSample.ALTTOURISM_PLACE, "Jo_Winery")
            .set("name", "Jo_Winery")
            .set("kind", myAppPlace2Posted.getUri()); // not a kind
      try {
         datacoreApiClient.postDataInType(altTourismPlaceJoWineryBad);
         Assert.fail("Only " + AltTourismPlaceAddressSample.ALTTOURISM_PLACEKIND + " should be accepted as referenced type");
      } catch (BadRequestException brex) {
         Assert.assertTrue(true);
      }

      DCResource altTourismWineryKind = resourceService.create(AltTourismPlaceAddressSample.ALTTOURISM_PLACEKIND, "winery").set("name", "winery");
      altTourismWineryKind = datacoreApiClient.postDataInType(altTourismWineryKind);
      DCResource altTourismPlaceJoWinery = resourceService.create(AltTourismPlaceAddressSample.ALTTOURISM_PLACE, "Jo_Winery")
            .set("name", "Jo_Winery").set("kind", altTourismWineryKind.getUri());
      DCResource altTourismPlaceJoWineryPosted = datacoreApiClient.postDataInType(altTourismPlaceJoWinery);

      // check that field without mixin fails, at update : 
      altTourismPlaceJoWineryPosted.set("zipCode", "1000");
      try {
         datacoreApiClient.postDataInType(altTourismPlaceJoWineryPosted);
         Assert.fail("Field without mixin should fail at update");
      } catch (BadRequestException brex) {
         Assert.assertTrue(true);
      }

      // check that field without mixin fails, same but already at creation :
      DCResource altTourismMonasteryKind = resourceService.create(AltTourismPlaceAddressSample.ALTTOURISM_PLACEKIND, "monastery").set("name", "monastery");
      altTourismMonasteryKind = datacoreApiClient.postDataInType(altTourismMonasteryKind);
      DCResource altTourismPlaceSofiaMonastery = resourceService.create(AltTourismPlaceAddressSample.ALTTOURISM_PLACE, "Sofia_Monastery")
            .set("name", "Sofia_Monastery").set("kind", altTourismMonasteryKind.getUri())
            .set("zipCode", "1000");
      Assert.assertEquals("Sofia_Monastery", altTourismPlaceSofiaMonastery.get("name")); // TODO _ else space bad url !!!!!!!!!!!!!!!!
      try {
         datacoreApiClient.postDataInType(altTourismPlaceSofiaMonastery);
         Assert.fail("Field without mixin should fail at creation");
      } catch (BadRequestException brex) {
         Assert.assertTrue(true);
      }
         
      // step 2 - now adding mixin
      modelAdminService.getModelBase(AltTourismPlaceAddressSample.ALTTOURISM_PLACE)
         .addMixin(modelAdminService.getModelBase(AltTourismPlaceAddressSample.OASIS_ADDRESS));
      ///modelAdminService.addModel(altTourismPlace); // LATER re-add...

      // check, at update :
      DCResource altTourismPlaceJoWineryPut = datacoreApiClient.putDataInType(altTourismPlaceJoWineryPosted);
      Assert.assertEquals("types should include mixin", 2, altTourismPlaceJoWineryPut.getTypes().size());
      Assert.assertNotNull(altTourismPlaceJoWineryPut);
      Assert.assertEquals(altTourismPlaceJoWineryPut.get("name"), "Jo_Winery"); // TODO _ else space bad url !!!!!!!!!!!!!!!!
      Assert.assertEquals("1000", altTourismPlaceJoWineryPut.get("zipCode")); // now address field

      // check, same but already at creation :
      DCResource altTourismPlaceSofiaMonasteryPosted = datacoreApiClient.postDataInType(altTourismPlaceSofiaMonastery);
      Assert.assertEquals("types should include mixin", 2, altTourismPlaceSofiaMonasteryPosted.getTypes().size());
      Assert.assertNotNull(altTourismPlaceSofiaMonasteryPosted);
      Assert.assertEquals(altTourismPlaceSofiaMonasteryPosted.get("name"), "Sofia_Monastery"); // TODO _ else space bad url !!!!!!!!!!!!!!!!
      Assert.assertEquals("1000", altTourismPlaceSofiaMonasteryPosted.get("zipCode")); // now address field
      
      
      // security :
      // BEWARE don't use client (datacoreApiClient) else SecurityContextHolder won't work (different thread)
      // OR specify user in HTTP
      
      // check model defaults
      DCModelBase altTourismPlaceModel = modelServiceImpl.getModelBase(AltTourismPlaceAddressSample.ALTTOURISM_PLACE);
      Assert.assertTrue(altTourismPlaceModel.getSecurity().isAuthentifiedReadable());
      Assert.assertTrue(!altTourismPlaceModel.getSecurity().isAuthentifiedWritable());
      Assert.assertTrue(altTourismPlaceModel.getSecurity().isAuthentifiedCreatable());
      
      // TODO TODO test model type resource reader / writer / creator (& impl) !!!
      
      // preparing creation tests
      int i = 0;
      
      // logging in as guest
      authenticationService.loginAs("guest");
      
      // allow model to localauthdevmode guest
      altTourismPlaceModel.setSecurity(null);

      // check that find allowed as localauthdevmode guest
      List<DCEntity> allowedMonasteryRes = ldpEntityQueryService.findDataInType(altTourismPlaceModel.getName(),
            new ImmutableMap.Builder<String, List<String>>().put("name",
                  new ImmutableList.Builder<String>().add("Sofia_Monastery").build()).build(), 0, 10);
      Assert.assertTrue("query filtering should have allowed it because authentified",
            allowedMonasteryRes != null && allowedMonasteryRes.size() == 1);
      List<DCResource> allowedMonasteryClientRes = datacoreApiClient.findDataInType(AltTourismPlaceAddressSample.ALTTOURISM_PLACE,
            new QueryParameters().add("name", "Sofia_Monastery"), 0, 10); // client side
      Assert.assertTrue("query filtering should have allowed it because in guest type",
            allowedMonasteryClientRes != null && allowedMonasteryClientRes.size() == 1);
      
      // check that read (in addition to find) allowed as localauthdevmode guest
      try {
         resourceService.get(altTourismPlaceSofiaMonasteryPosted.getUri(),
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE);
         Assert.assertTrue(true);
      } catch (Exception e) {
         Assert.fail("Resource in guest type should be readable as guest");
      }
      try {
         datacoreApiClient.getData(AltTourismPlaceAddressSample.ALTTOURISM_PLACE,
               "Sofia_Monastery"); // client side
         Assert.assertTrue(true);
      } catch (Exception e) {
         Assert.fail("Resource in guest type should be readable as guest");
      }
      
      // check that write not allowed as localauthdevmode guest
      try {
         resourceService.createOrUpdate(altTourismPlaceSofiaMonasteryPosted,
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE, false, true, false);
         Assert.fail("Resource in guest type should not be writable as guest");
      } catch (Exception e) {
         Assert.assertTrue(true);
      }
      try {
         datacoreApiClient.postDataInType(altTourismPlaceSofiaMonasteryPosted); // client side
         Assert.fail("Resource in guest type should not be writable as guest");
      } catch (Exception e) {
         Assert.assertTrue(true);
      }
      
      // check that create not allowed as localauthdevmode guest
      try {
         resourceService.createOrUpdate(buildSofiaMonastery(++i),
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE, true, false, false);
         Assert.fail("Resource in guest type should not be creatable as guest");
      } catch (Exception e) {
         Assert.assertTrue(true);
      }
      try {
         datacoreApiClient.postDataInType(buildSofiaMonastery(++i)); // client side
         Assert.fail("Resource in guest type should not be creatable as guest");
      } catch (Exception e) {
         Assert.assertTrue(true);
      }
      
      // logging in as user with rights
      authenticationService.logout(); // NB. not required since followed by login
      authenticationService.loginAs("john");
      
      // remove localauthdevmode guest
      altTourismPlaceModel.setSecurity(new DCSecurity());
      altTourismPlaceModel.getSecurity().addResourceCreator("model_resource_creator_altTourism.place");
      altTourismPlaceModel.getSecurity().setAuthentifiedWritable(true); // NOT default

      // check that read (in addition to find) allowed as user as well
      try {
         resourceService.get(altTourismPlaceSofiaMonasteryPosted.getUri(),
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE);
         Assert.assertTrue(true);
      } catch (Exception e) {
         Assert.fail("Resource in guest type should be readable as user");
      }
      try {
         datacoreApiClient.getData(AltTourismPlaceAddressSample.ALTTOURISM_PLACE,
               "Sofia_Monastery"); // client side
         Assert.assertTrue(true);
      } catch (Exception e) {
         Assert.fail("Resource in guest type should be readable as user");
      }
      
      // check that writable by authentified user (NB. NOT default)
      try {
         altTourismPlaceSofiaMonasteryPosted = resourceService.createOrUpdate(altTourismPlaceSofiaMonasteryPosted,
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE, false, true, false);
         Assert.assertEquals("john", altTourismPlaceSofiaMonasteryPosted.getLastModifiedBy()); // check auditor
         Set<String> owners = entityService.getByUriUnsecured(altTourismPlaceSofiaMonasteryPosted.getUri(), altTourismPlaceModel).getOwners();
         Assert.assertTrue(owners != null && owners.size() == 1 &&  !"u_john".equals(owners.iterator().next())); // check creator as owner
      } catch (Exception e) {
         Assert.fail("Resource in authentified writable type should be writable as user");
      }
      try {
         altTourismPlaceSofiaMonasteryPosted = datacoreApiClient.postDataInType(altTourismPlaceSofiaMonasteryPosted); // client side
         Assert.assertEquals("john", altTourismPlaceSofiaMonasteryPosted.getLastModifiedBy()); // check auditor
         Set<String> owners = entityService.getByUriUnsecured(altTourismPlaceSofiaMonasteryPosted.getUri(), altTourismPlaceModel).getOwners();
         Assert.assertTrue(owners != null && owners.size() == 1 &&  !"u_john".equals(owners.iterator().next())); // check creator as owner
      } catch (Exception e) {
         Assert.fail("Resource in authentified writable type should be writable as user");
      }
      
      // check that creatable by authentified user
      try {
         DCResource resource = resourceService.createOrUpdate(buildSofiaMonastery(++i),
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE, true, false, false);
         Assert.assertEquals("john", resource.getCreatedBy()); // check auditor
         Set<String> owners = entityService.getByUriUnsecured(resource.getUri(), altTourismPlaceModel).getOwners();
         Assert.assertTrue(owners != null && owners.size() == 1 &&  "u_john".equals(owners.iterator().next())); // check creator as owner
      } catch (Exception e) {
         Assert.fail("Resource in authentified creatable type should be creatable as user");
      }
      try {
         DCResource resource = datacoreApiClient.postDataInType(buildSofiaMonastery(++i)); // client side
         Assert.assertEquals("john", resource.getCreatedBy()); // check auditor
         Set<String> owners = entityService.getByUriUnsecured(resource.getUri(), altTourismPlaceModel).getOwners();
         Assert.assertTrue(owners != null && owners.size() == 1 &&  "u_john".equals(owners.iterator().next())); // check creator as owner
      } catch (Exception e) {
         Assert.fail("Resource in authentified creatable type should be creatable as user");
      }
      
      // make model secured (still authentified readable)
      altTourismPlaceModel.getSecurity().setAuthentifiedWritable(false);
      altTourismPlaceModel.getSecurity().setAuthentifiedCreatable(false);
      
      // logging in as guest
      authenticationService.logout(); // NB. not required since followed by login
      authenticationService.loginAs("guest");

      // check that read not allowed anymore as localauthdevmode guest (because not null security)
      try {
         resourceService.get(altTourismPlaceSofiaMonasteryPosted.getUri(),
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE);
         Assert.fail("Resource in authentified type should not be readable as guest");
      } catch (Exception e) {
         Assert.assertTrue(true);
      }
      try {
         datacoreApiClient.getData(AltTourismPlaceAddressSample.ALTTOURISM_PLACE,
               "Sofia_Monastery", 0l); // client side
         Assert.fail("Resource in authentified type should not be readable as guest");
      } catch (Exception e) {
         Assert.assertTrue(true);
      }
      try {
         datacoreApiClient.getData(AltTourismPlaceAddressSample.ALTTOURISM_PLACE,
               "Sofia_Monastery"); // client side
         Assert.assertTrue(true);
      } catch (Exception e) {
         Assert.fail("Get cached up-to-date data is always allowed (only requires entityService.isUpToDate()");
      }
      
      // check that write still not allowed as localauthdevmode guest (because not null security)
      try {
         resourceService.createOrUpdate(altTourismPlaceSofiaMonasteryPosted,
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE, false, true, false);
         Assert.fail("Resource in authentified type should not be writable as guest");
      } catch (AccessDeniedException e) {
         Assert.assertTrue(true);
      }
      try {
         datacoreApiClient.postDataInType(altTourismPlaceSofiaMonasteryPosted); // client side
         Assert.fail("Resource in authentified type should not be writable as guest");
      } catch (ForbiddenException e) {
         Assert.assertTrue(true);
      }
      
      // check that create still not allowed as localauthdevmode guest (because not null security)
      try {
         resourceService.createOrUpdate(buildSofiaMonastery(++i),
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE, true, false, false);
         Assert.fail("Resource in authentified type should not be creatable as guest");
      } catch (AccessDeniedException e) {
         Assert.assertTrue(true);
      }
      try {
         datacoreApiClient.postDataInType(buildSofiaMonastery(++i)); // client side
         Assert.fail("Resource in authentified type should not be creatable as guest");
      } catch (ForbiddenException e) {
         Assert.assertTrue(true);
      }
      
      // check that not found anymore as localauthdevmode guest (because not null security)
      authenticationService.loginAs("guest");
      List<DCEntity> forbiddenMonasteryRes = ldpEntityQueryService.findDataInType(altTourismPlaceModel.getName(),
            new ImmutableMap.Builder<String, List<String>>().put("name",
                  new ImmutableList.Builder<String>().add("Sofia_Monastery").build()).build(), 0, 10);
      Assert.assertTrue("query filtering should have forbidden authentified model",
            forbiddenMonasteryRes == null || forbiddenMonasteryRes.isEmpty());
      List<DCResource> forbiddenMonasteryClientRes = datacoreApiClient.findDataInType(AltTourismPlaceAddressSample.ALTTOURISM_PLACE,
            new QueryParameters().add("name", "Sofia_Monastery"), 0, 10); // client side
      Assert.assertTrue("query filtering should have forbidden authentified model",
            forbiddenMonasteryClientRes == null || forbiddenMonasteryClientRes.isEmpty());
      authenticationService.logout(); // NB. not required since followed by login

      // logging in as user with not yet set rights
      authenticationService.logout(); // NB. not required since followed by login
      authenticationService.loginAs("john");

      // check that found because authentified
      allowedMonasteryRes = ldpEntityQueryService.findDataInType(altTourismPlaceModel.getName(),
            new ImmutableMap.Builder<String, List<String>>().put("name",
                  new ImmutableList.Builder<String>().add("Sofia_Monastery").build()).build(), 0, 10);
      Assert.assertTrue("query filtering should have allowed it because authentified",
            allowedMonasteryRes != null && allowedMonasteryRes.size() == 1);
      allowedMonasteryClientRes = datacoreApiClient.findDataInType(AltTourismPlaceAddressSample.ALTTOURISM_PLACE,
            new QueryParameters().add("name", "Sofia_Monastery"), 0, 10); // client side
      Assert.assertTrue("query filtering should have allowed it because authentified",
            allowedMonasteryClientRes != null && allowedMonasteryClientRes.size() == 1);

      // check that readable because authentified
      try {
         resourceService.get(altTourismPlaceSofiaMonasteryPosted.getUri(),
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE);
         Assert.assertTrue(true);
      } catch (Exception e) {
         Assert.fail("Resource in authentified type should be readable because authentified");
      }
      try {
         datacoreApiClient.getData(AltTourismPlaceAddressSample.ALTTOURISM_PLACE,
               "Sofia_Monastery"); // client side
         Assert.assertTrue(true);
      } catch (Exception e) {
         Assert.fail("Resource in authentified type should be readable because authentified");
      }
      
      // check that not writable because not in writer group
      try {
         resourceService.createOrUpdate(altTourismPlaceSofiaMonasteryPosted,
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE, false, true, false);
         Assert.fail("Resource in authentified type should not be writable because not yet in writer group");
      } catch (AccessDeniedException e) {
         Assert.assertTrue(true);
      }
      try {
         datacoreApiClient.postDataInType(altTourismPlaceSofiaMonasteryPosted); // client side
         Assert.fail("Resource in authentified type should not be writable because not yet in writer group");
      } catch (ForbiddenException e) {
         Assert.assertTrue(true);
      }
      
      // check that not creatable because not in writer group
      try {
         resourceService.createOrUpdate(buildSofiaMonastery(++i),
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE, true, false, false);
         Assert.fail("Resource in authentified type should not be creatable because not yet in writer group");
      } catch (AccessDeniedException e) {
         Assert.assertTrue(true);
      }
      try {
         datacoreApiClient.postDataInType(buildSofiaMonastery(++i)); // client side
         Assert.fail("Resource in authentified type should not be creatable because not yet in writer group");
      } catch (ForbiddenException e) {
         Assert.assertTrue(true);
      }
      
      // make model more secured (not authentified readable)
      altTourismPlaceModel.getSecurity().setAuthentifiedReadable(false);
      
      // check that not found because in not yet set reader group
      forbiddenMonasteryRes = ldpEntityQueryService.findDataInType(altTourismPlaceModel.getName(),
            new ImmutableMap.Builder<String, List<String>>().put("name",
                  new ImmutableList.Builder<String>().add("Sofia_Monastery").build()).build(), 0, 10);
      Assert.assertTrue("query filtering should have forbidden it because in not yet set reader group",
            forbiddenMonasteryRes == null || forbiddenMonasteryRes.isEmpty());
      forbiddenMonasteryClientRes = datacoreApiClient.findDataInType(AltTourismPlaceAddressSample.ALTTOURISM_PLACE,
            new QueryParameters().add("name", "Sofia_Monastery"), 0, 10); // client side
      Assert.assertTrue("query filtering should have forbidden it because in not yet set reader group",
            forbiddenMonasteryClientRes == null || forbiddenMonasteryClientRes.isEmpty());

      // check that not readable because in not yet set reader group
      try {
         resourceService.get(altTourismPlaceSofiaMonasteryPosted.getUri(),
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE);
         Assert.fail("Resource in private type should not be readable as GUEST");
      } catch (AccessDeniedException e) {
         Assert.assertTrue(true);
      }
      try {
         datacoreApiClient.getData(AltTourismPlaceAddressSample.ALTTOURISM_PLACE,
               "Sofia_Monastery", 0l); // client side
         Assert.fail("Resource in private type should not be readable as GUEST");
      } catch (ForbiddenException e) {
         Assert.assertTrue(true);
      }
      try {
         datacoreApiClient.getData(AltTourismPlaceAddressSample.ALTTOURISM_PLACE,
               "Sofia_Monastery"); // client side
         Assert.assertTrue(true);
      } catch (Exception e) {
         Assert.fail("Get cached up-to-date data is always allowed (only requires entityService.isUpToDate()");
      }
      
      // check that not writable because still not in writer group
      try {
         resourceService.createOrUpdate(altTourismPlaceSofiaMonasteryPosted,
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE, false, true, false);
         Assert.fail("Resource in private type should not be writable as GUEST");
      } catch (AccessDeniedException e) {
         Assert.assertTrue(true);
      }
      try {
         datacoreApiClient.postDataInType(altTourismPlaceSofiaMonasteryPosted); // client side
         Assert.fail("Resource in private type should not be writable as GUEST");
      } catch (ForbiddenException e) {
         Assert.assertTrue(true);
      }
      
      // check that not creatable because still not in writer group
      try {
         resourceService.createOrUpdate(buildSofiaMonastery(++i),
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE, true, false, false);
         Assert.fail("Resource in private type should not be creatable as GUEST");
      } catch (AccessDeniedException e) {
         Assert.assertTrue(true);
      }
      try {
         datacoreApiClient.postDataInType(buildSofiaMonastery(++i)); // client side
         Assert.fail("Resource in private type should not be creatable as GUEST");
      } catch (ForbiddenException e) {
         Assert.assertTrue(true);
      }
      
      authenticationService.logout(); // NB. not required since followed by login
      
      // set reader rights
      authenticationService.loginAs("admin");
      DCEntity altTourismPlaceSofiaMonasteryEntity = entityService.getByUri(altTourismPlaceSofiaMonastery.getUri(), altTourismPlaceModel);
      entityPermissionService.setReaders(altTourismPlaceSofiaMonasteryEntity,
            new ImmutableSet.Builder<String>().add("rm_altTourism.place.SofiaMonastery_readers").build());
      entityService.changeRights(altTourismPlaceSofiaMonasteryEntity);
      // get with updated version :
      altTourismPlaceSofiaMonasteryPosted = datacoreApiClient.getData(AltTourismPlaceAddressSample.ALTTOURISM_PLACE, "Sofia_Monastery");
      authenticationService.logout(); // NB. not required since followed by login
      ///altTourismPlaceSofiaMonastery = datacoreApiClient.postDataInType(altTourismPlaceSofiaMonastery);

      // logging in as user with rights
      authenticationService.loginAs("john");
      
      // check that found by user in group
      /*SecurityContext sc = new SecurityContextImpl();
      Authentication authentication = new TestingAuthenticationToken("john", "pass", "group");
      sc.setAuthentication(authentication);
      SecurityContextHolder.setContext(sc);*/
      allowedMonasteryRes = ldpEntityQueryService.findDataInType(altTourismPlaceModel.getName(),
            new ImmutableMap.Builder<String, List<String>>().put("name",
                  new ImmutableList.Builder<String>().add("Sofia_Monastery").build()).build(), 0, 10);
      Assert.assertTrue("query filtering should have allowed it because in reader group",
            allowedMonasteryRes != null && allowedMonasteryRes.size() == 1);
      allowedMonasteryClientRes = datacoreApiClient.findDataInType(AltTourismPlaceAddressSample.ALTTOURISM_PLACE,
            new QueryParameters().add("name", "Sofia_Monastery"), 0, 10); // client side
      Assert.assertTrue("query filtering should have allowed it because in reader group",
            allowedMonasteryClientRes != null && allowedMonasteryClientRes.size() == 1);

      // check that readable by user in reader group
      try {
         resourceService.get(altTourismPlaceSofiaMonasteryPosted.getUri(),
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE);
         Assert.assertTrue(true);
      } catch (Exception e) {
         Assert.fail("Resource in private type should be readable by user in reader group");
      }
      try {
         datacoreApiClient.getData(AltTourismPlaceAddressSample.ALTTOURISM_PLACE,
               "Sofia_Monastery"); // client side
         Assert.assertTrue(true);
      } catch (Exception e) {
         Assert.fail("Resource in private type should be readable by user in reader group");
      }
      
      // check that not writable by user not in writer group
      try {
         resourceService.createOrUpdate(altTourismPlaceSofiaMonasteryPosted,
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE, false, true, false);
         Assert.fail("Resource in private type should not be writable by user not in writer group");
      } catch (AccessDeniedException e) {
         Assert.assertTrue(true);
      }
      try {
         datacoreApiClient.postDataInType(altTourismPlaceSofiaMonasteryPosted); // client side
         Assert.fail("Resource in private type should not be writable by user not in writer group");
      } catch (ForbiddenException e) {
         Assert.assertTrue(true);
      }
      
      // check that not creatable by user not in writer group
      try {
         resourceService.createOrUpdate(buildSofiaMonastery(++i),
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE, true, false, false);
         Assert.fail("Resource in private type should not be creatable by user not in writer group");
      } catch (AccessDeniedException e) {
         Assert.assertTrue(true);
      }
      try {
         datacoreApiClient.postDataInType(buildSofiaMonastery(++i)); // client side
         Assert.fail("Resource in private type should not be creatable by user not in writer group");
      } catch (ForbiddenException e) {
         Assert.assertTrue(true);
      }
      
      authenticationService.logout(); // NB. not required since followed by login

      authenticationService.loginAs("jim");
      
      // check that not writable by user in not yet set writer group
      try {
         resourceService.createOrUpdate(altTourismPlaceSofiaMonasteryPosted,
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE, false, true, false);
         Assert.fail("Resource in private type should not be writable by user in not yet set writer group");
      } catch (AccessDeniedException e) {
         Assert.assertTrue(true);
      }
      try {
         datacoreApiClient.postDataInType(altTourismPlaceSofiaMonasteryPosted); // client side
         Assert.fail("Resource in private type should not be writable by user in not yet set writer group");
      } catch (ForbiddenException e) {
         Assert.assertTrue(true);
      }
      
      // check that creatable by user in writer group
      try {
         DCResource resource = resourceService.createOrUpdate(buildSofiaMonastery(++i),
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE, true, false, false);
         Assert.assertEquals("jim", resource.getCreatedBy()); // check auditor
         Set<String> owners = entityService.getByUriUnsecured(resource.getUri(), altTourismPlaceModel).getOwners();
         Assert.assertTrue(owners != null && owners.size() == 1 &&  "u_jim".equals(owners.iterator().next())); // check creator as owner
      } catch (Exception e) {
         Assert.fail("Resource in private type should be creatable by user in writer group");
      }
      try {
         DCResource resource = datacoreApiClient.postDataInType(buildSofiaMonastery(++i)); // client side
         Assert.assertEquals("jim", resource.getCreatedBy()); // check auditor
      } catch (Exception e) {
         Assert.fail("Resource in private type should be creatable by user in writer group");
      }
      
      authenticationService.logout(); // NB. not required since followed by login
      
      // set writer rights
      authenticationService.loginAs("admin");
      entityPermissionService.setWriters(altTourismPlaceSofiaMonasteryEntity,
            new ImmutableSet.Builder<String>().add("rm_altTourism.place.SofiaMonastery_writers").build());
      entityService.changeRights(altTourismPlaceSofiaMonasteryEntity);
      // get with updated version :
      altTourismPlaceSofiaMonasteryPosted = datacoreApiClient.getData(AltTourismPlaceAddressSample.ALTTOURISM_PLACE, "Sofia_Monastery");
      authenticationService.logout(); // NB. not required since followed by login
      ///altTourismPlaceSofiaMonastery = datacoreApiClient.postDataInType(altTourismPlaceSofiaMonastery); // TODO also test

      // check that writable by user in writer group
      authenticationService.loginAs("jim");
      
      try {
         altTourismPlaceSofiaMonasteryPosted = resourceService.createOrUpdate(altTourismPlaceSofiaMonasteryPosted,
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE, false, true, false);
         Assert.assertEquals("jim", altTourismPlaceSofiaMonasteryPosted.getLastModifiedBy()); // check auditor
      } catch (Exception e) {
         Assert.fail("Resource in private type should be writable by user in writer group");
      }
      try {
         altTourismPlaceSofiaMonasteryPosted = datacoreApiClient.postDataInType(altTourismPlaceSofiaMonasteryPosted); // client side
         Assert.assertEquals("jim", altTourismPlaceSofiaMonasteryPosted.getLastModifiedBy()); // check auditor
      } catch (Exception e) {
         Assert.fail("Resource in private type should be writable by user in writer group");
      }

      // check that still creatable by user in writer group
      try {
         DCResource resource = resourceService.createOrUpdate(buildSofiaMonastery(++i),
               AltTourismPlaceAddressSample.ALTTOURISM_PLACE, true, false, false);
         Assert.assertEquals("jim", resource.getCreatedBy()); // check auditor
         Set<String> owners = entityService.getByUriUnsecured(resource.getUri(), altTourismPlaceModel).getOwners();
         Assert.assertTrue(owners != null && owners.size() == 1 &&  "u_jim".equals(owners.iterator().next())); // check creator as owner
      } catch (Exception e) {
         Assert.fail("Resource in private type should be creatable by user in writer group");
      }
      try {
         DCResource resource = datacoreApiClient.postDataInType(buildSofiaMonastery(++i)); // client side
         Assert.assertEquals("jim", resource.getCreatedBy()); // check auditor
      } catch (Exception e) {
         Assert.fail("Resource in private type should be creatable by user in writer group");
      }
      
      authenticationService.logout();
      
      // revert model to default (public)
      altTourismPlaceModel.setSecurity(null);
   }
   
   private DCResource buildSofiaMonastery(int i) {
      return resourceService.create(AltTourismPlaceAddressSample.ALTTOURISM_PLACE,
            "Sofia_Monastery_" + i).set("name", "Sofia_Monastery_" + i)
            .set("kind", UriHelper.buildUri(containerUrl, AltTourismPlaceAddressSample.ALTTOURISM_PLACEKIND,
                  "monastery")).set("zipCode", "1000");
   }

   /*public void doAs(String login, Callback callback) {
      mockAuthenticationService.login("john");
      callback.execute();
      mockAuthenticationService.logout();
   }*/

   public DCResource buildAltTourismPlaceKind(String kind) {
      return resourceService.create(AltTourismPlaceAddressSample.ALTTOURISM_PLACEKIND, kind).set("name", kind);
      // TODO iri ??
      //resource.setVersion(-1l);
   }
   
   public DCResource buildAltTourismPlaceKindObsolete(String kind) {
      DCResource resource = new DCResource();
      String type = AltTourismPlaceAddressSample.ALTTOURISM_PLACEKIND;
      String iri = kind;
      resource.setUri(UriHelper.buildUri(containerUrl, type, iri));
      //resource.setVersion(-1l);
      resource.setProperty("type", type);
      resource.setProperty("iri", iri);
      resource.setProperty("name", kind);
      return resource;
   }


}
