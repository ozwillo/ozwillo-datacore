package org.oasis.datacore.rest.server;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.oasis.datacore.core.entity.EntityService;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryService;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.security.EntityPermissionService;
import org.oasis.datacore.core.security.mock.MockAuthenticationService;
import org.oasis.datacore.model.resource.ModelResourceMappingService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
import org.oasis.datacore.rest.client.QueryParameters;
import org.oasis.datacore.rest.server.event.EventService;
import org.oasis.datacore.rest.server.resource.ResourceService;
import org.oasis.datacore.sample.CityCountrySample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.ImmutableList;


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
   private ModelResourceMappingService mrMappingService;
   
   /** for tests */
   @Autowired
   private CityCountrySample cityCountrySample;
   
   
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
   public void testResourceModel() throws Exception {
      Assert.assertNotNull(modelAdminService.getModelBase("dcmo:model_0"));
      List<DCResource> models = datacoreApiClient.findDataInType("dcmo:model_0", null, null, 10);
      Assert.assertTrue(models != null && !models.isEmpty());

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
   
   @Test
   public void testResourceModelUpdateThroughREST() throws Exception {
      // put in initial state (delete stuff) in case test was aborted :
      DCResource mr = datacoreApiClient.getData("dcmo:model_0", CityCountrySample.CITY_MODEL_NAME);
      DCModelBase m = mrMappingService.toModelOrMixin(mr);
      m.getField("city:founded").setRequired(false);
      datacoreApiClient.postDataInType(mrMappingService.modelToResource(m, mr));
      Assert.assertFalse(modelAdminService.getModelBase(
            CityCountrySample.CITY_MODEL_NAME).getField("city:founded").isRequired());
      
      // change a model on client side, post it and check that ResourceService parsing changes :
      // (while testing java to resource methods and back)
      // checking that putting a Resource without city:founded is for now OK
      DCResource villeurbanneCity = resourceService.create(CityCountrySample.CITY_MODEL_NAME, "France/Villeurbanne")
            .set("n:name", "Villeurbanne").set("city:inCountry", getFranceCountry().getUri());
      deleteExisting(villeurbanneCity);
      
      villeurbanneCity = datacoreApiClient.postDataInType(villeurbanneCity);
      Assert.assertNotNull(villeurbanneCity);
      // getting model and changing it
      DCResource cityModelResource = datacoreApiClient.getData("dcmo:model_0", CityCountrySample.CITY_MODEL_NAME);
      resourceService.resourceToEntity(cityModelResource); // cleans up resource, else ex. Long props are rather Integers,
      // which toModelOrMixin doesn't support 
      DCModel clientCityModel = (DCModel) mrMappingService.toModelOrMixin(cityModelResource);
      DCField clientCityFoundedField = clientCityModel.getField("city:founded");
      Assert.assertTrue(!clientCityFoundedField.isRequired());
      clientCityFoundedField.setRequired(true);
      mrMappingService.modelToResource(clientCityModel, cityModelResource); // mrMappingService1.modelToResource(clientCityModel)
      try {
      
      // updating model & check that changed
      cityModelResource = datacoreApiClient.putDataInType(cityModelResource);
      @SuppressWarnings("unchecked")
      Map<String,DCField> cityModelFields1 = mrMappingService.propsToFields(
            (List<Map<String, Object>>) cityModelResource.get("dcmo:fields"));
      Assert.assertTrue(cityModelFields1.get("city:founded").isRequired());
      // checking that DCModel has been updated :
      DCModelBase cityModel = modelAdminService.getModelBase(CityCountrySample.CITY_MODEL_NAME);
      Assert.assertNotNull(cityModel);
      Assert.assertTrue(cityModel.getField("city:founded").isRequired());
      // checking that putting a Resource without city:founded is now forbidden
      try {
         datacoreApiClient.putDataInType(villeurbanneCity);
         Assert.fail("Should not be able to update a Resource without a now required field");
      } catch (BadRequestException brex) {
         Assert.assertTrue(true);
      }
      // but that it works with it
      villeurbanneCity.set("city:founded", new DateTime(-6000, 4, 1, 0, 0, DateTimeZone.UTC));
      villeurbanneCity = datacoreApiClient.putDataInType(villeurbanneCity);
      Assert.assertNotNull(villeurbanneCity);
      
      } finally {
         // putting it back in default state
         clientCityFoundedField.setRequired(false);
         mrMappingService.modelToResource(clientCityModel, cityModelResource); // mrMappingService1.modelToResource(clientCityModel)
         cityModelResource = datacoreApiClient.putDataInType(cityModelResource);
         deleteExisting(villeurbanneCity);
      }
   }

   @Test
   public void testCountryLanguageSpecificModel() throws Exception {
      String frCityModelName = "sample.city.cité"; // with problem-bound é
      ArrayList<String> frCityModelMixins = new ArrayList<String>();
      DCResource frCityModel = resourceService.create("dcmo:model_0", frCityModelName)
            .set("dcmo:name", frCityModelName).set("dcmo:mixins", frCityModelMixins).set("dcmo:maxScan", 0);
      deleteExisting(frCityModel);
      
      try {
         datacoreApiClient.postDataInType(frCityModel);
         // test fails on bad practice non-country / language specific modelType
         // (characters beyond $-_.() AND +!*' i.e. reserved $&+,/:;=?@ & unsafe  "<>#%{}|\^~[]`)
         Assert.fail("é should be bad practice in non country / language specific modelType");
      } catch (BadRequestException iaex) {
         Assert.assertTrue(true);
      }

      frCityModelMixins.add("dcmls:CountryLanguageSpecific");
      frCityModel.set("dcmls:code", "FR");
      try {
         datacoreApiClient.postDataInType(frCityModel);
         Assert.fail("country / language specific modelType should have generic mixin");
      } catch (BadRequestException iaex) {
         Assert.assertTrue(true);
      }

      frCityModelMixins.add(CityCountrySample.CITY_MODEL_NAME);
      frCityModel = datacoreApiClient.postDataInType(frCityModel);
      Assert.assertNotNull(frCityModel); // no more é or generic mixin error
   }

   @Test
   public void testImpactedModelUpdate() throws Exception {
      // put in initial state (delete stuff) in case test was aborted :
      DCResource mr = datacoreApiClient.getData("dcmo:model_0", CityCountrySample.CITY_MODEL_NAME);
      DCModelBase m = mrMappingService.toModelOrMixin(mr);
      m.getField("city:founded").setRequired(false);
      datacoreApiClient.postDataInType(mrMappingService.modelToResource(m, mr));
      Assert.assertFalse(modelAdminService.getModelBase(
            CityCountrySample.CITY_MODEL_NAME).getField("city:founded").isRequired());
      
      // create referring model :
      String frCityModelName = "sample.city.cityFR";
      ArrayList<String> frCityModelMixins = new ArrayList<String>();
      DCResource frCityModelResource = resourceService.create("dcmo:model_0", frCityModelName)
            .set("dcmo:name", frCityModelName).set("dcmo:mixins", new ImmutableList.Builder<String>()
                  .add(CityCountrySample.CITY_MODEL_NAME).build()).set("dcmo:maxScan", 0);
      deleteExisting(frCityModelResource);

      frCityModelMixins.add(CityCountrySample.CITY_MODEL_NAME);
      frCityModelResource = datacoreApiClient.postDataInType(frCityModelResource);
      Assert.assertNotNull(frCityModelResource);
      // check initial referring model :
      // in server :
      DCModelBase frCityModel = modelAdminService.getModelBase(frCityModelName);
      Assert.assertNotNull(frCityModel);
      Assert.assertTrue(!frCityModel.getGlobalField("city:founded").isRequired());
      // in served model resource :
      frCityModelResource = datacoreApiClient.getData(frCityModelResource);
      Assert.assertNotNull(frCityModelResource);
      resourceService.resourceToEntity(frCityModelResource); // cleans up resource, else ex. Long props are rather Integers,
      // which toModelOrMixin doesn't support 
      frCityModel = (DCModel) mrMappingService.toModelOrMixin(frCityModelResource);
      Assert.assertTrue(!frCityModel.getGlobalField("city:founded").isRequired());
      
      // update referred model :
      
      // getting referred model and changing it
      DCResource cityModelResource = datacoreApiClient.getData("dcmo:model_0", CityCountrySample.CITY_MODEL_NAME);
      resourceService.resourceToEntity(cityModelResource); // cleans up resource, else ex. Long props are rather Integers,
      // which toModelOrMixin doesn't support 
      DCModel clientCityModel = (DCModel) mrMappingService.toModelOrMixin(cityModelResource);
      DCField clientCityFoundedField = clientCityModel.getField("city:founded");
      Assert.assertTrue(!clientCityFoundedField.isRequired());
      clientCityFoundedField.setRequired(true);
      mrMappingService.modelToResource(clientCityModel, cityModelResource);
      try {
         
         // updating referred model using REST & check that changed
         cityModelResource = datacoreApiClient.putDataInType(cityModelResource);
         @SuppressWarnings("unchecked")
         Map<String,DCField> cityModelFields1 = mrMappingService.propsToFields(
               (List<Map<String, Object>>) cityModelResource.get("dcmo:fields"));
         Assert.assertTrue(cityModelFields1.get("city:founded").isRequired());
         // checking that DCModel has been updated :
         DCModelBase cityModel = modelAdminService.getModelBase(CityCountrySample.CITY_MODEL_NAME);
         Assert.assertNotNull(cityModel);
         Assert.assertTrue(cityModel.getField("city:founded").isRequired());

         // check that referring model has changed :
         // in server :
         frCityModel = modelAdminService.getModelBase(frCityModelName);
         Assert.assertNotNull(frCityModel);
         Assert.assertTrue(frCityModel.getGlobalField("city:founded").isRequired());
         // in served model resource :
         frCityModelResource = datacoreApiClient.getData(frCityModelResource);
         Assert.assertNotNull(frCityModelResource);
         resourceService.resourceToEntity(frCityModelResource); // cleans up resource, else ex. Long props are rather Integers,
         // which toModelOrMixin doesn't support 
         frCityModel = (DCModel) mrMappingService.toModelOrMixin(frCityModelResource);
         Assert.assertTrue(frCityModel.getGlobalField("city:founded").isRequired());
         
         } finally {
            // putting it back in default state
            clientCityFoundedField.setRequired(false);
            mrMappingService.modelToResource(clientCityModel, cityModelResource);
            cityModelResource = datacoreApiClient.putDataInType(cityModelResource);
         }
   }

   /** puts back in initial state ; TODO in base helper */
   public void deleteExisting(DCResource resource) {
      if (resource == null) {
         return;
      }
      try {
         DCResource existingResource = datacoreApiClient.getData(resource);
         // if already exists, delete it first :
         datacoreApiClient.deleteData(existingResource); // and not resource,
         // else could keep its city;founded value
      } catch (NotFoundException nfex) {
         // expected the first time
      }
   }

   @Test
   public void testResourceModelCreateAndIndex() throws Exception {
      cityCountrySample.cleanDataOfCreatedModels();
      cityCountrySample.initData(); // else can't find France country on CI server
      
      String newName =  "sample.city.city_1";
      /*// put in initial state (delete stuff) in case test was aborted :
      mgo.dropCollection(newName);
      modelAdminService.removeModel(newName);*/
      // putting back to default state, in case test was aborted :
      /*if (newVilleurbanneCity != null) {
         deleteExisting(newVilleurbanneCity);
      }*/
      try {
         deleteExisting(datacoreApiClient.getData("dcmo:model_0", newName));
      } catch (NotFoundException nfex) {
         // expected the first time
      }
      
      // now post it as new model with other name, and check
      // that uri index is there to ensure unicity :
      DCModelBase newCityModel = null;
      DCResource newVilleurbanneCity = null;
      DCResource newCityModelResource = datacoreApiClient
            .getData("dcmo:model_0", CityCountrySample.CITY_MODEL_NAME);
      newCityModelResource.setVersion(null);
      newCityModelResource.setUri(newCityModelResource.getUri().replace("sample.city.city", newName));
      newCityModelResource.set("dcmo:name", newName);
      newCityModelResource = datacoreApiClient.postDataInType(newCityModelResource);
      // checking that DCModel has been updated :
      newCityModel = modelAdminService.getModelBase(newName);
      Assert.assertNotNull(newCityModel);
      Assert.assertNotNull(modelAdminService.getMixin(newName)); // and that also available among reusable mixins
      // POSTing a new Resource in it :
      newVilleurbanneCity = resourceService.create(newName, "France/Villeurbanne")
            .set("n:name", "Villeurbanne").set("city:inCountry", getFranceCountry().getUri());
      deleteExisting(newVilleurbanneCity); // putting back in initial state if necessary
      
      newVilleurbanneCity = datacoreApiClient.postDataInType(newVilleurbanneCity);
      Assert.assertNotNull(newVilleurbanneCity);
      Assert.assertEquals(1, datacoreApiClient.findDataInType(newName, null, 0, 10).size());
      // attempt to rePOST it as a new Resource should fail :
      DCResource newNewVilleurbanneCity = newVilleurbanneCity;
      newNewVilleurbanneCity.setVersion(null);
      try {
         datacoreApiClient.postDataInType(newNewVilleurbanneCity);
         Assert.fail("URI unique index should forbid rePOSTing an existing Resource");
      } catch (BadRequestException brex) {
         Assert.assertTrue(true);
      }
      
      // putting back to default state, while checking delete :
      datacoreApiClient.deleteData(newCityModelResource);
      Assert.assertNull(modelAdminService.getModelBase(newName));
      Assert.assertNull(modelAdminService.getMixin(newName)); // checked that also removed
      Assert.assertFalse(mgo.collectionExists(newName)); // check that dropped
      try {
         datacoreApiClient.getData(newNewVilleurbanneCity);
         Assert.fail("resource should have been deleted along with model");
      } catch (NotFoundException nfex) {
         Assert.assertTrue(true);
      }
   }

   private DCResource getFranceCountry() {
      return datacoreApiClient.getData(CityCountrySample.COUNTRY_MODEL_NAME, "France"); 
   }

}
