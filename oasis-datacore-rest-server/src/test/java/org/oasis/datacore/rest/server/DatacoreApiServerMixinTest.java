package org.oasis.datacore.rest.server;

import javax.ws.rs.BadRequestException;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.client.DatacoreClientApi;
import org.oasis.datacore.rest.server.event.DCInitIdEventListener;
import org.oasis.datacore.rest.server.event.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
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
   private /*DatacoreApi*/DatacoreClientApi datacoreApiClient;
   
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

   @Autowired
   private DataModelServiceImpl modelAdminService; // TODO rm refactor
   
   // .bdparcellaire
   public static final String IGN_COMMUNE = "ign.commune";
   public static final String IGN_DEPARTEMENT = "ign.departement";
   public static final String IGN_CANTON = "ign.canton";
   public static final String IGN_ARRONDISSEMENT = "ign.arrondissement";
   public static final String IGN_DIVISION = "ign.division"; // Cadastrale
   public static final String IGN_LOCALISANT = "ign.localisant"; // Parcellaire
   public static final String IGN_PARCELLE = "ign.parcelle";
   
   public static final String CITYHALL_IGN_PARCELLE = "cityhall.ign.parcelle";

   public static final String OASIS_ADDRESS = "oasis.address";
   
   public static final String MY_APP_PLACE = "my.app.place";

   public static final String ALTTOURISM_PLACEKIND = "altTourism.placeKind";
   public static final String ALTTOURISM_PLACE = "altTourism.place";
   
   @Test // rather than @BeforeClass, else static and spring can't inject
   //@BeforeClass
   public /*static */void init1setupModels() {
      ///cityCountrySample.init(); // auto called
      
      // because IGN Model governance say so (but this could be forbidden by OASIS) :
      // field names are in French 
      // all fields are required
      
      DCModel ignCommuneModel = new DCModel(IGN_COMMUNE); // rgc., pdp.
      /*ignCommuneModel.setDocumentation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
            + "\"name\": \"France\" }");*/
      ///ignCommuneModel.addField(new DCField("name", "string", true, 100)); // TODO cityCountry's ??!!
      ignCommuneModel.addField(new DCField("departementCode", "int", true, 100)); // DEP
      ignCommuneModel.addField(new DCField("departement", "resource", true, 100));
      ignCommuneModel.addField(new DCField("communeCode", "int", true, 100)); // COM ; code, commune ?
      ignCommuneModel.addField(new DCField("arrondissementCode", "int", true, 100)); // ARRD
      ignCommuneModel.addField(new DCField("cantonCode", "int", true, 100)); // CANT
      ignCommuneModel.addField(new DCField("statutAdministratif", "int", true, 100)); // ADMI
      ignCommuneModel.addField(new DCField("population", "int", true, 0)); // POPU ; query ?
      ignCommuneModel.addField(new DCField("surface", "int", true, 0)); // SURFACE ; query ?
      ignCommuneModel.addField(new DCField("nom", "string", true, 100)); // NOM ; com.nom ? ; majuscules ; cityCountry's ??
      ignCommuneModel.addField(new DCField("longitudeDMS", "int", true, 100)); // LONGI_DMS
      ignCommuneModel.addField(new DCField("latitudeDMS", "int", true, 100)); // LATI_DMS

      DCModel ignDepartementModel = new DCModel(IGN_DEPARTEMENT); // rgc., pdp.
      ignDepartementModel.addField(new DCField("nom", "string", true, 100)); // DEP ; dep.nom ?
      ignDepartementModel.addField(new DCField("departementCode", "int", true, 100)); // DEP

      DCModel ignCantonModel = new DCModel(IGN_CANTON); // rgc.
      ignCantonModel.addField(new DCField("nom", "string", true, 100)); // ?? ; dep.nom ?
      ignCantonModel.addField(new DCField("departementCode", "int", true, 100)); // DEP
      ignCantonModel.addField(new DCField("arrondissementCode", "int", true, 100)); // ARRD
      ignCantonModel.addField(new DCField("cantonCode", "int", true, 100)); // CANT

      DCModel ignArrondissementModel = new DCModel(IGN_ARRONDISSEMENT); // rgc., bdparcellaire.
      ignArrondissementModel.addField(new DCField("commune", "resource", true, 100));
      ignArrondissementModel.addField(new DCField("departementCode", "int", true, 100)); // DEP
      ignArrondissementModel.addField(new DCField("arrondissementCode", "int", true, 100)); // ARRD
      ignArrondissementModel.addField(new DCField("absorbeeCode", "int", true, 100)); // commune
      
      DCModel ignDivisionModel = new DCModel(IGN_DIVISION); // bdparcellaire. ; cadastrale
      ignDivisionModel.addField(new DCField("sectionCadastrale", "string", true, 100)); // section
      ignDivisionModel.addField(new DCField("feuilleCadastrale", "int", true, 100)); // feuille
      
      DCModel ignLocalisantModel = new DCModel(IGN_LOCALISANT); // bdparcellaire. ; parcellaire
      ignLocalisantModel.addField(new DCField("sectionCadastrale", "string", true, 100)); // section
      ignLocalisantModel.addField(new DCField("feuilleCadastrale", "int", true, 100)); // feuille
      ignLocalisantModel.addField(new DCField("numeroParcelle", "int", true, 100)); // numero

      DCModel ignParcelleModel = new DCModel(IGN_PARCELLE); // bdparcellaire.
      ignParcelleModel.addField(new DCField("sectionCadastrale", "string", true, 100)); // section
      ignParcelleModel.addField(new DCField("feuilleCadastrale", "int", true, 100)); // feuille
      ignLocalisantModel.addField(new DCField("numeroParcelle", "int", true, 100)); // numero
      
      modelAdminService.addModel(ignCommuneModel);
      modelAdminService.addModel(ignDepartementModel);
      modelAdminService.addModel(ignArrondissementModel);
      modelAdminService.addModel(ignCantonModel);
      modelAdminService.addModel(ignParcelleModel);
      modelAdminService.addModel(ignDivisionModel);
      modelAdminService.addModel(ignLocalisantModel);
      
      // IGN patched by cityhalls - v1 (using Mixin, so in same collection)
      DCMixin cityhallIgnParcelleMixin = new DCMixin(CITYHALL_IGN_PARCELLE); // bdparcellaire.
      cityhallIgnParcelleMixin.addField(new DCField("cityhall.numeroParcelle", "int", true, 100));
      ///modelAdminService.addMixin(cityhallIgnParcelleMixin); // LATER ?
            // numeroParcelle ?? => TODO PREFIXES !!!! OR prefix using declaring Model / Mixin ?!
      ignParcelleModel.addMixin(cityhallIgnParcelleMixin); // NB. doesn't change the Model version ?!?!?!
      ///modelAdminService.addModel(ignParcelleModel); // LATER re-add...
      
      // IGN patched by cityhalls - v2 (using inheritance, so in separate collection)
      ///DCModel cityhallIgnParcelleModel = new DCModel("cityhall.ign.parcelle", ignParcelleModel.getName()); // bdparcellaire.
      DCModel cityhallIgnParcelleModel = new DCModel(CITYHALL_IGN_PARCELLE);
      cityhallIgnParcelleModel.addMixin(ignParcelleModel); // inheritance = parent model (copy) as mixin NOO OPPOSITE OVERRIDE
      ///modelAdminService.addMixin(ignParcelleModel); // LATER ??
      cityhallIgnParcelleModel.addField(new DCField("cityhall.numeroParcelle", "int", true, 100)); // alt1 new field along the existing one
      cityhallIgnParcelleModel.addField(new DCField("numeroParcelle", "int", true, 100)); // alt2 override (of def ?? or only of value, or auto ?)
      ///modelAdminService.addModel(cityhallIgnParcelleModel); // LATER re-add...

      // Mixin for shared fields
      DCMixin oasisAddress = new DCMixin(OASIS_ADDRESS);
      oasisAddress.addField(new DCField("streetAndNumber", "string", false, 100)); // my.app.place.address.streetAndNumber ?!
      oasisAddress.addField(new DCField("zipCode", "string", true, 100)); // "string" for cedex / po box
      oasisAddress.addField(new DCField("cityName", "string", false, 100)); // OR only resource ??
      oasisAddress.addField(new DCField("city", "resource", false, 100));
      oasisAddress.addField(new DCField("countryName", "string", false, 100)); // OR only resource ??
      oasisAddress.addField(new DCField("country", "resource", false, 100));
      ///modelAdminService.addMixin(oasisAddress); // LATER ?

      
      // Mixin for shared fields - use 1
      DCModel myAppPlaceAddress = new DCModel(MY_APP_PLACE);
      myAppPlaceAddress.addField(new DCField("name", "string", true, 100)); // my.app.place.name ?!
      modelAdminService.addModel(myAppPlaceAddress);
      
      DCResource myAppPlace1 = resourceService.create(MY_APP_PLACE, "my_place_1").set("name", "my_place_1");
      Assert.assertNotNull(myAppPlace1);
      //Assert.assertEquals("...", myAppPlace1.getUri());
      Assert.assertEquals(1, myAppPlace1.getTypes().size());
      Assert.assertEquals(MY_APP_PLACE, myAppPlace1.getTypes().get(0));
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
      DCResource myAppPlace2 = resourceService.create(MY_APP_PLACE, "my_place_2").set("name", "my_place_2")
            .set("zipCode", "69100");
      Assert.assertEquals("my_place_2", myAppPlace2.get("name"));
      try {
         datacoreApiClient.postDataInType(myAppPlace2);
         Assert.fail("Field without mixin should fail at creation");
      } catch (BadRequestException brex) {
         Assert.assertTrue(true);
      }
      
      // step 2 - now adding mixin
      myAppPlaceAddress.addMixin(oasisAddress);
      ///modelAdminService.addModel(myAppPlaceAddress); // LATER re-add...

      // check, at update :
      DCResource myAppPlace1Put = datacoreApiClient.putDataInType(myAppPlace1Posted);
      Assert.assertNotNull(myAppPlace1Put);
      Assert.assertEquals(myAppPlace1Put.get("name"),  "my_place_1");
      Assert.assertEquals("69100", myAppPlace1Put.get("zipCode")); // now address field

      // check, same but already at creation :
      DCResource myAppPlace2Posted = datacoreApiClient.postDataInType(myAppPlace2);
      Assert.assertNotNull(myAppPlace2Posted);
      Assert.assertEquals(myAppPlace2Posted.get("name"),  "my_place_2");
      Assert.assertEquals("69100", myAppPlace2Posted.get("zipCode")); // now address field
      
      
      // Mixin for shared fields - use 2
      DCModel altTourismPlaceKind = new DCModel(ALTTOURISM_PLACEKIND);
      altTourismPlaceKind.addField(new DCField("name", "string", true, 100));
      altTourismPlaceKind.addListener(eventService.initialize(new DCInitIdEventListener(ALTTOURISM_PLACEKIND, "name"))); // TODO null
      DCModel altTourismPlace = new DCModel(ALTTOURISM_PLACE);
      altTourismPlace.addField(new DCField("name", "string", true, 100));
      altTourismPlace.addField(new DCResourceField("kind", ALTTOURISM_PLACEKIND, true, 100)); // hotel... ; alternativeTourismPlaceKind
      modelAdminService.addModel(altTourismPlaceKind);
      modelAdminService.addModel(altTourismPlace);

      DCResource altTourismHotelKind = resourceService.create(ALTTOURISM_PLACEKIND, "hotel").set("name", "hotel");
      altTourismHotelKind = datacoreApiClient.postDataInType(altTourismHotelKind);

      // check that only ALTTOURISM_PLACEKIND is accepted as kind :
      // TODO LATER should explode already here
      DCResource altTourismPlaceJoWineryBad = resourceService.create(ALTTOURISM_PLACE, "Jo_Winery")
            .set("name", "Jo_Winery")
            .set("kind", myAppPlace2Posted.getUri()); // not a kind
      try {
         datacoreApiClient.postDataInType(altTourismPlaceJoWineryBad);
         Assert.fail("Only " + ALTTOURISM_PLACEKIND + " should be accepted as referenced type");
      } catch (BadRequestException brex) {
         Assert.assertTrue(true);
      }

      DCResource altTourismWineryKind = resourceService.create(ALTTOURISM_PLACEKIND, "winery").set("name", "winery");
      altTourismWineryKind = datacoreApiClient.postDataInType(altTourismWineryKind);
      DCResource altTourismPlaceJoWinery = resourceService.create(ALTTOURISM_PLACE, "Jo_Winery")
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
      DCResource altTourismMonasteryKind = resourceService.create(ALTTOURISM_PLACEKIND, "monastery").set("name", "monastery");
      altTourismMonasteryKind = datacoreApiClient.postDataInType(altTourismMonasteryKind);
      DCResource altTourismPlaceSofiaMonastery = resourceService.create(ALTTOURISM_PLACE, "Sofia_Monastery")
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
      altTourismPlace.addMixin(oasisAddress);
      ///modelAdminService.addModel(altTourismPlace); // LATER re-add...

      // check, at update :
      DCResource altTourismPlaceJoWineryPut = datacoreApiClient.putDataInType(altTourismPlaceJoWineryPosted);
      Assert.assertNotNull(altTourismPlaceJoWineryPut);
      Assert.assertEquals(altTourismPlaceJoWineryPut.get("name"), "Jo_Winery"); // TODO _ else space bad url !!!!!!!!!!!!!!!!
      Assert.assertEquals("1000", altTourismPlaceJoWineryPut.get("zipCode")); // now address field

      // check, same but already at creation :
      DCResource altTourismPlaceSofiaMonasteryPosted = datacoreApiClient.postDataInType(altTourismPlaceSofiaMonastery);
      Assert.assertNotNull(altTourismPlaceSofiaMonasteryPosted);
      Assert.assertEquals(altTourismPlaceSofiaMonasteryPosted.get("name"), "Sofia_Monastery"); // TODO _ else space bad url !!!!!!!!!!!!!!!!
      Assert.assertEquals("1000", altTourismPlaceSofiaMonasteryPosted.get("zipCode")); // now address field
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
   
   @Test
   public void test() {
      
   }

   public DCResource buildAltTourismPlaceKind(String kind) {
      return resourceService.create(ALTTOURISM_PLACEKIND, kind).set("name", kind);
      // TODO iri ??
      //resource.setVersion(-1l);
   }
   
   public DCResource buildAltTourismPlaceKindObsolete(String kind) {
      DCResource resource = new DCResource();
      String type = ALTTOURISM_PLACEKIND;
      String iri = kind;
      resource.setUri(UriHelper.buildUri(containerUrl, type, iri));
      //resource.setVersion(-1l);
      resource.setProperty("type", type);
      resource.setProperty("iri", iri);
      resource.setProperty("name", kind);
      return resource;
   }


}
