package org.oasis.datacore.rest.server;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.client.DatacoreClientApi;
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
      /*
      // IGN patched by cityhalls - v1 (using Mixin, so in same collection)
      DCMixin cityhallIgnParcelleMixin = new DCMixin("cityhall.ign.parcelle"); // bdparcellaire.
      cityhallIgnParcelleMixin.addField(new DCField("cityhall.numeroParcelle", "int", true, 100));
            // numeroParcelle ?? => TODO PREFIXES !!!! OR prefix using declaring Model / Mixin ?!
      ignParcelleModel.addMixin(cityhallIgnParcelleMixin); // NB. doesn't change the Model version ?!?!?!
      
      // IGN patched by cityhalls - v2 (using inheritance, so in separate collection)
      DCModel cityhallIgnParcelleModel = new DCModel("cityhall.ign.parcelle", ignParcelleModel.getName()); // bdparcellaire.
      cityhallIgnParcelleMixin.addField(new DCField("cityhall.numeroParcelle", "int", true, 100)); // alt1 new field along the existing one
      cityhallIgnParcelleMixin.addField(new DCField("numeroParcelle", "int", true, 100)); // alt2 override (of def ?? or only of value, or auto ?)
      
      DCMixin oasisAddress = new DCMixin("oasis.address");
      oasisAddress.addField(new DCField("streetAndNumber", "string", true, 100)); // my.app.place.address.streetAndNumber ?!
      oasisAddress.addField(new DCField("zipCode", "string", true, 100)); // "string" for cedex / po box
      oasisAddress.addField(new DCField("cityName", "string", true, 100)); // OR only resource ??
      oasisAddress.addField(new DCField("city", "resource", true, 100));
      oasisAddress.addField(new DCField("countryName", "string", true, 100)); // OR only resource ??
      oasisAddress.addField(new DCField("country", "resource", true, 100));
      DCModel myAppPlaceAddress = new DCModel("my.app.place");
      myAppPlaceAddress.addField(new DCField("name", "string", true, 100)); // my.app.place.name ?!
      myAppPlaceAddress.addMixin(oasisAddress);
      
      // Mixin for shared fields
      DCModel alternativeTourismPlaceKind = new DCModel("alt.tourism.placeKind");
      alternativeTourismPlaceKind.addField(new DCField("name", "string", true, 100));
      DCModel alternativeTourismPlace = new DCModel("alt.tourism.place");
      alternativeTourismPlace.addField(new DCField("name", "string", true, 100));
      alternativeTourismPlace.addField(new DCField("type", "resource", true, 100)); // hotel... ; alternativeTourismPlaceKind
      alternativeTourismPlace.addMixin(oasisAddress);
      */
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
   public void testBuild() {
      String modelType = "alt.tourism.placeKind";
      final String kind = "hotel";
      @SuppressWarnings("serial")
      DCResource r = build(modelType, kind, new HashMap<String,Object>() {{
         put("name", kind);
      }});
      Assert.assertNotNull(r);
      Assert.assertEquals(r.getUri(), this.containerUrl + "dc/type/" + modelType + '/' + kind);
      Assert.assertEquals(r.getProperties().get("name"), kind);
   }

   @Test
   public void testFluentCreate() {
      String modelType = "alt.tourism.placeKind";
      String kind = "winery";
      DCResource r = DCResource.create(this.containerUrl, modelType, kind).set("name", kind);
      Assert.assertNotNull(r);
      Assert.assertEquals(r.getUri(), this.containerUrl + "dc/type/" + modelType + '/' + kind);
      Assert.assertEquals(r.getProperties().get("name"), kind);
   }

   @Test
   public void testFluentCreateService() {
      String modelType = "alt.tourism.placeKind";
      String kind = "monastery";
      DCResource r = create(modelType, kind).set("name", kind);
      Assert.assertNotNull(r);
      Assert.assertEquals(r.getUri(), this.containerUrl + "dc/type/" + modelType + '/' + kind);
      Assert.assertEquals(r.getProperties().get("name"), kind);
   }
   
   
   
   private DCResource create(String modelType, String iri) {
      return DCResource.create(this.containerUrl, modelType, iri);
   }

   public DCResource buildAltTourismPlaceKind(String kind) {
      DCResource resource = new DCResource();
      String type = "alt.tourism.placeKind";
      String iri = kind;
      resource.setUri(UriHelper.buildUri(containerUrl, type, iri));
      //resource.setVersion(-1l);
      resource.setProperty("type", type);
      resource.setProperty("iri", iri);
      resource.setProperty("kind", kind);
      return resource;
   }
   
   public DCResource build(String type, String iri, Map<String,Object> fieldValues) {
      DCResource resource = new DCResource();
      resource.setUri(UriHelper.buildUri(containerUrl, type, iri));
      //resource.setVersion(-1l);
      resource.setProperty("type", type);
      resource.setProperty("iri", iri);
      for (Map.Entry<String, Object> fieldValueEntry : fieldValues.entrySet()) {
         resource.setProperty(fieldValueEntry.getKey(), fieldValueEntry.getValue());
      }
      return resource;
   }

}
