package org.oasis.datacore.rest.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.BadRequestException;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.oasis.datacore.core.entity.DCEntityService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.query.QueryException;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryService;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.security.mock.MockAuthenticationService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
import org.oasis.datacore.rest.server.event.EventService;
import org.oasis.datacore.rest.server.resource.ResourceService;
import org.oasis.datacore.sample.AltTourismPlaceAddressSample;
import org.oasis.datacore.sample.IgnCityhallSample;
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
public class DatacoreApiServerMixinTest {
   
   @Autowired
   @Qualifier("datacoreApiCachedClient")
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
   private DCEntityService entityService;
   @Autowired
   private MockAuthenticationService mockAuthenticationService;
   
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
   private IgnCityhallSample ignCityhallSample;
   @Autowired
   private AltTourismPlaceAddressSample altTourismPlaceAddressSample;
   


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

   @Test
   public void testIgn() {
      ignCityhallSample.initIgn();

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
   }
   
   @Test
   public void testAddress() throws QueryException {
      altTourismPlaceAddressSample.initModel();
      
      
      // Mixin for shared fields - use 1
      // MY_APP_PLACE
      
      DCResource myAppPlace1 = resourceService.create(AltTourismPlaceAddressSample.MY_APP_PLACE, "my_place_1").set("name", "my_place_1");
      Assert.assertNotNull(myAppPlace1);
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
      modelAdminService.getModel(AltTourismPlaceAddressSample.MY_APP_PLACE)
         .addMixin(modelAdminService.getMixin(AltTourismPlaceAddressSample.OASIS_ADDRESS));
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
      modelAdminService.getModel(AltTourismPlaceAddressSample.ALTTOURISM_PLACE)
         .addMixin(modelAdminService.getMixin(AltTourismPlaceAddressSample.OASIS_ADDRESS));
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

      // check that GET allowed as guest
      //resourceService.get();
      
      // check that write not allowed as guest
      //resourceService.createOrUpdate(resource, modelType, canCreate, canUpdate)
      
      // make model secured
      DCModel altTourismPlaceModel = modelServiceImpl.getModel(AltTourismPlaceAddressSample.ALTTOURISM_PLACE);
      altTourismPlaceModel.getSecurity().setPublicRead(false);

      // check that not found anymore as GUEST
      mockAuthenticationService.login("guest");
      List<DCEntity> forbiddenMonasteryRes = ldpEntityQueryService.findDataInType(altTourismPlaceModel,
            new HashMap<String,List<String>>() {{ put("name", new ArrayList<String>() {{ add("Sofia_Monastery"); }}); }}, 0, 10);
      mockAuthenticationService.logout();
      Assert.assertTrue("query filtering should have forbidden non public model",
            forbiddenMonasteryRes == null || forbiddenMonasteryRes.isEmpty());
      
      // set rights
      DCEntity altTourismPlaceSofiaMonasteryEntity = entityService.getByUriId(altTourismPlaceSofiaMonastery.getUri(), altTourismPlaceModel);
      altTourismPlaceSofiaMonasteryEntity.setReaders(new ArrayList<String>() {{ add("group"); }});
      entityService.update(altTourismPlaceSofiaMonasteryEntity, altTourismPlaceModel);
      altTourismPlaceSofiaMonastery = datacoreApiClient.postDataInType(altTourismPlaceSofiaMonastery);
      
      // check that not found (because not in group)
      mockAuthenticationService.login("guest");
      forbiddenMonasteryRes = ldpEntityQueryService.findDataInType(altTourismPlaceModel,
            new HashMap<String,List<String>>() {{ put("name", new ArrayList<String>() {{ add("Sofia_Monastery"); }}); }}, 0, 10);
      mockAuthenticationService.logout();
      Assert.assertTrue("query filtering should have forbidden it because not in group",
            forbiddenMonasteryRes == null || forbiddenMonasteryRes.isEmpty());
      
      // check that found when using rights
      /*SecurityContext sc = new SecurityContextImpl();
      Authentication authentication = new TestingAuthenticationToken("john", "pass", "group");
      sc.setAuthentication(authentication);
      SecurityContextHolder.setContext(sc);*/
      mockAuthenticationService.login("john");
      List<DCEntity> allowedMonasteryRes = ldpEntityQueryService.findDataInType(altTourismPlaceModel,
            new HashMap<String,List<String>>() {{ put("name", new ArrayList<String>() {{ add("Sofia_Monastery"); }}); }}, 0, 10);
      mockAuthenticationService.logout();
      Assert.assertTrue("query filtering should have allowed it", allowedMonasteryRes != null && allowedMonasteryRes.size() == 1);
      
      // revert model to default (public)
      altTourismPlaceModel.getSecurity().setPublicRead(true);
   }
   
   @Test
   public void test() {
      
   }

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
