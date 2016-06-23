package org.oasis.datacore.rest.server;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.oasis.datacore.common.context.DCRequestContextProvider;
import org.oasis.datacore.common.context.SimpleRequestContextProvider;
import org.oasis.datacore.core.entity.EntityModelService;
import org.oasis.datacore.core.entity.EntityService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.query.QueryException;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryService;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.core.security.EntityPermissionService;
import org.oasis.datacore.core.security.mock.LocalAuthenticationService;
import org.oasis.datacore.model.resource.LoadPersistedModelsAtInit;
import org.oasis.datacore.model.resource.ModelResourceMappingService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.oasis.datacore.rest.api.util.UnitTestHelper;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
import org.oasis.datacore.rest.client.QueryParameters;
import org.oasis.datacore.rest.server.event.EventService;
import org.oasis.datacore.rest.server.resource.ResourceException;
import org.oasis.datacore.rest.server.resource.ResourceService;
import org.oasis.datacore.sample.CityCountrySample;
import org.oasis.datacore.sample.PublicPrivateOrganizationSample;
import org.oasis.datacore.sample.ResourceModelIniter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;


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
   private DatacoreCachedClient datacoreApiClient;
   
   @Autowired
   private ResourceService resourceService;
   @Autowired
   private EventService eventService;
   
   /** to init models */
   @Autowired
   private /*static */DataModelServiceImpl modelServiceImpl;
   /** to be tested */
   @Autowired
   private LoadPersistedModelsAtInit loadPersistedModelsAtInitService;
   
   /** to cleanup db
    * TODO LATER rather in service */
   @Autowired
   private /*static */MongoOperations mgo;
   /** to setup security tests & test multiProjectStorage */
   @Autowired
   private EntityService entityService;
   /** to enable index testing NOO only find in models */
   @Autowired
   private EntityModelService entityModelService;
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
   private ModelResourceMappingService mrMappingService;
   
   /** for tests */
   @Autowired
   private CityCountrySample cityCountrySample;
   
   
   @Before
   public void setProject() {
      ///cityCountrySample.initData();
      
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
   public void testResourceModel() throws Exception {
      Assert.assertNotNull(modelAdminService.getModelBase(ResourceModelIniter.MODEL_MODEL_NAME));
      List<DCResource> models = datacoreApiClient.findDataInType(ResourceModelIniter.MODEL_MODEL_NAME, null, null, 10);
      Assert.assertTrue(models != null && !models.isEmpty());

      List<DCResource> arePlaceModels = new SimpleRequestContextProvider<List<DCResource>>() { // set context project beforehands :
         protected List<DCResource> executeInternal() throws ResourceException {
            return datacoreApiClient.findDataInType(ResourceModelIniter.MODEL_MODEL_NAME,
                  new QueryParameters().add("dcmo:globalMixins", "pl:place_0"), null, 10); // from CityPlanningAndEconomicalActivitySample
         }
      }.execInContext(new ImmutableMap.Builder<String, Object>()
            .put(DCRequestContextProvider.PROJECT, DCProject.OASIS_SAMPLE).build());
      Assert.assertTrue(arePlaceModels != null && !arePlaceModels.isEmpty());

      List<DCResource> haveCountryModels = datacoreApiClient.findDataInType(ResourceModelIniter.MODEL_MODEL_NAME,
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
      DCResource mr = datacoreApiClient.getData(ResourceModelIniter.MODEL_MODEL_NAME, CityCountrySample.CITY_MODEL_NAME);
      DCModelBase m = mrMappingService.toModelOrMixin(mr);
      m.getField("city:founded").setRequired(false);
      datacoreApiClient.postDataInType(mrMappingService.modelToResource(m, mr));
      Assert.assertFalse(modelAdminService.getModelBase(
            CityCountrySample.CITY_MODEL_NAME).getField("city:founded").isRequired());
      
      // change a model on client side, post it and check that ResourceService parsing changes :
      // (while testing java to resource methods and back)
      // checking that putting a Resource without city:founded is for now OK
      DCResource villeurbanneCity = resourceService.create(CityCountrySample.CITY_MODEL_NAME, "France/Villeurbanne")
            .set("n:name", "Villeurbanne")
            .set("city:inCountry", getFranceCountry().getUri())
            .set("city:i18nname", DCResource.listBuilder()
              .add(DCResource.propertiesBuilder().put("@language", "fr").put("@value", "Villeurbanne").build())
              .build());
      deleteExisting(villeurbanneCity);
      
      villeurbanneCity = datacoreApiClient.postDataInType(villeurbanneCity);
      Assert.assertNotNull(villeurbanneCity);
      // getting model and changing it
      DCResource cityModelResource = datacoreApiClient.getData(ResourceModelIniter.MODEL_MODEL_NAME, CityCountrySample.CITY_MODEL_NAME);
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
         // get it first to update version if required :
         DCResource latestCityModelResource = datacoreApiClient.getData(cityModelResource);
         cityModelResource.setVersion(latestCityModelResource.getVersion());
         cityModelResource = datacoreApiClient.putDataInType(cityModelResource);
         deleteExisting(villeurbanneCity);
      }
   }

   @Test
   public void testCountryLanguageSpecificModel() throws Exception {
      String frCityModelName = "sample.city.cité"; // with problem-bound é
      ArrayList<String> frCityModelMixins = new ArrayList<String>();
      DCResource frCityModel = resourceService.create(ResourceModelIniter.MODEL_MODEL_NAME, frCityModelName)
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

      frCityModelMixins.add(ResourceModelIniter.MODEL_COUNTRYLANGUAGESPECIFIC_NAME);
      frCityModel.set("dcmls:code", "FR");
      try {
         datacoreApiClient.postDataInType(frCityModel);
         Assert.fail("country / language specific modelType should have a generic mixin");
      } catch (BadRequestException iaex) {
         Assert.assertTrue(true);
      }

      frCityModelMixins.add(CityCountrySample.CITY_MODEL_NAME);
      frCityModel = datacoreApiClient.postDataInType(frCityModel);
      Assert.assertNotNull(frCityModel); // no more é or generic mixin error
   }

   @Test
   public void testImpactedModelAndIndexUpdate() throws Exception {
      // put in initial state in case test was aborted :
      DCResource mr = datacoreApiClient.getData(ResourceModelIniter.MODEL_MODEL_NAME, CityCountrySample.CITY_MODEL_NAME);
      DCModelBase m = mrMappingService.toModelOrMixin(mr);
      m.getField("city:founded").setRequired(false);
      m.getField("city:founded").setQueryLimit(0);
      datacoreApiClient.postDataInType(mrMappingService.modelToResource(m, mr));
      // checking initial state :
      Assert.assertFalse(modelAdminService.getModelBase(
            CityCountrySample.CITY_MODEL_NAME).getField("city:founded").isRequired());
      Assert.assertEquals(0, modelAdminService.getModelBase(
            CityCountrySample.CITY_MODEL_NAME).getField("city:founded").getQueryLimit());
      // and that has not index :
      //String moscowCityUri = UriHelper.buildUri(this.containerUrl,
      //      CityCountrySample.CITY_MODEL_NAME, "Russia/Moscow");
      QueryParameters cityFoundedDebugParams = new QueryParameters().add("city:founded", new DateTime().toString())
            .add("city:founded", "+") // to override default sort on _chAt which could blur results
            .add(DatacoreApi.DEBUG_PARAM, "true");
      List<DCResource> debugRes = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
            cityFoundedDebugParams, null, 10);
      Assert.assertFalse("Should not have used index on city:founded",
            TestHelper.getDebugCursor(debugRes).startsWith("BtreeCursor _p.city:founded"));
      
      // create referring model :
      String frCityModelName = "sample.city.cityFR";
      ArrayList<String> frCityModelMixins = new ArrayList<String>();
      DCResource frCityModelResource = resourceService.create(ResourceModelIniter.MODEL_MODEL_NAME, frCityModelName)
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
      // and has not index :
      debugRes = datacoreApiClient.findDataInType(frCityModelName,
            cityFoundedDebugParams, null, 10);
      Assert.assertFalse("cityFR should not have index on city:founded",
            TestHelper.getDebugCursor(debugRes).startsWith("BtreeCursor _p.city:founded"));
      
      // update referred model :
      
      // getting referred model and changing it
      DCResource cityModelResource = datacoreApiClient.getData(ResourceModelIniter.MODEL_MODEL_NAME, CityCountrySample.CITY_MODEL_NAME);
      resourceService.resourceToEntity(cityModelResource); // cleans up resource, else ex. Long props are rather Integers,
      // which toModelOrMixin doesn't support 
      DCModel clientCityModel = (DCModel) mrMappingService.toModelOrMixin(cityModelResource);
      DCField clientCityFoundedField = clientCityModel.getField("city:founded");
      Assert.assertTrue(!clientCityFoundedField.isRequired());
      clientCityFoundedField.setRequired(true);
      Assert.assertEquals(0, clientCityFoundedField.getQueryLimit());
      clientCityFoundedField.setQueryLimit(100);
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
         Assert.assertEquals(100, cityModel.getField("city:founded").getQueryLimit());

         // check that referring model has changed :
         // in server :
         frCityModel = modelAdminService.getModelBase(frCityModelName);
         Assert.assertNotNull(frCityModel);
         Assert.assertTrue(frCityModel.getGlobalField("city:founded").isRequired());
         Assert.assertEquals(100, frCityModel.getGlobalField("city:founded").getQueryLimit());
         // in served model resource :
         frCityModelResource = datacoreApiClient.getData(frCityModelResource);
         Assert.assertNotNull(frCityModelResource);
         resourceService.resourceToEntity(frCityModelResource); // cleans up resource, else ex. Long props are rather Integers,
         // which toModelOrMixin doesn't support 
         frCityModel = (DCModel) mrMappingService.toModelOrMixin(frCityModelResource);
         Assert.assertTrue(frCityModel.getGlobalField("city:founded").isRequired());
         Assert.assertEquals(100, frCityModel.getGlobalField("city:founded").getQueryLimit());
         // and has indexes :
         //entityModelService.setDisableMultiProjectStorageCriteriaForTesting(true); // NOO only find in models

         /*
          * SCH : commenting this out. It doesn't make sense to test on a plan for a query that returns no
          * data. The query actually hits against two properties (city:founded and _aliasOf: {$exists: false})
          * both have indices. Even with index intersection, Mongo will first scan the _aliasOf index, find
          * there's no data, then return an empty result set without even opening the city:founded index
          */

//         debugRes = datacoreApiClient.findDataInType(frCityModelName,
//               cityFoundedDebugParams, null, 10);
//         Assert.assertTrue("cityFR should have index on city:founded",
//               TestHelper.getDebugCursor(debugRes).startsWith("BtreeCursor _p.city:founded"));
         //entityModelService.setDisableMultiProjectStorageCriteriaForTesting(false); // NOO only find in models
         
         // removing index from referrer (by overriding it) :
         frCityModelResource = datacoreApiClient.getData(frCityModelResource); // update
         frCityModel = mrMappingService.toModelOrMixin(frCityModelResource);
         DCField frCityFoundedFieldOverride = frCityModel.getGlobalField("city:founded");
         frCityFoundedFieldOverride.setQueryLimit(0);
         frCityModel.addField(frCityFoundedFieldOverride);
         frCityModelResource = datacoreApiClient.postDataInType(
               mrMappingService.modelToResource(frCityModel, frCityModelResource));
         //entityModelService.setDisableMultiProjectStorageCriteriaForTesting(true); // NOO only find in models
         // and checking that referrer has not, but referred still has :
         debugRes = datacoreApiClient.findDataInType(frCityModelName,
               cityFoundedDebugParams, null, 10);
         Assert.assertFalse("cityFR should have index on city:founded",
               TestHelper.getDebugCursor(debugRes).startsWith("BtreeCursor _p.city:founded"));
         debugRes = datacoreApiClient.findDataInType(CityCountrySample.CITY_MODEL_NAME,
               cityFoundedDebugParams, null, 10);
         Assert.assertTrue("city should also have index on city:founded",
               TestHelper.getDebugCursor(debugRes).startsWith("BtreeCursor _p.city:founded"));
         ///entityModelService.setDisableMultiProjectStorageCriteriaForTesting(false); // NOO only find in models
         
         
         } finally {
            // putting it back in default state
            clientCityFoundedField.setRequired(false);
            clientCityFoundedField.setQueryLimit(0);
            mrMappingService.modelToResource(clientCityModel, cityModelResource);
            // get it first to update version if required :
            DCResource latestCityModelResource = datacoreApiClient.getData(cityModelResource);
            cityModelResource.setVersion(latestCityModelResource.getVersion());
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
         // else could keep its city:founded value
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
         deleteExisting(datacoreApiClient.getData(ResourceModelIniter.MODEL_MODEL_NAME, newName));
      } catch (NotFoundException nfex) {
         // expected the first time
      }
      
      // now post it as new model with other name, and check
      // that uri index is there to ensure unicity :
      DCModelBase newCityModel = null;
      DCResource newVilleurbanneCity = null;
      DCResource newCityModelResource = datacoreApiClient
            .getData(ResourceModelIniter.MODEL_MODEL_NAME, CityCountrySample.CITY_MODEL_NAME);
      newCityModelResource.setVersion(null);
      newCityModelResource.setUri(newCityModelResource.getUri().replace("sample.city.city", newName));
      newCityModelResource.set("dcmo:name", newName);
      newCityModelResource = datacoreApiClient.postDataInType(newCityModelResource);
      // checking that DCModel has been updated :
      newCityModel = modelAdminService.getModelBase(newName);
      Assert.assertNotNull(newCityModel);
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

   /**
    * i.e. testCantWriteModelOutsideProject
    * @throws Exception
    */
   @Test
   public void testMultiProjectStoragePreventsHidingResource() throws Exception {
      DCResource metaDisplayableModel = datacoreApiClient.getData(
            ResourceModelIniter.MODEL_MODEL_NAME, ResourceModelIniter.MODEL_DISPLAYABLE_NAME);
      Assert.assertNotNull("oasis.meta resources can be seen from oasis.sample project", metaDisplayableModel);
      Assert.assertEquals("model resources should bear their creation project name", DCProject.OASIS_META, metaDisplayableModel.get("dcmo:pointOfViewAbsoluteName"));
      
      authenticationService.loginAs("admin"); // else AuthenticationCredentialsNotFoundException in calling entityService
      try {
      DCEntity metaDisplayableModelEntity = entityService.getByUri(metaDisplayableModel.getUri(),
            modelServiceImpl.getModelBase(ResourceModelIniter.MODEL_MODEL_NAME));
      Assert.assertEquals("entity should be tagged by their creation project name",
            DCProject.OASIS_META, metaDisplayableModelEntity.getProjectName());
      List<DCResource> metaDisplayableModelFound = datacoreApiClient.findDataInType(ResourceModelIniter.MODEL_MODEL_NAME,
            new QueryParameters().add("dcmo:name", ResourceModelIniter.MODEL_DISPLAYABLE_NAME), null, 10);
      Assert.assertTrue(metaDisplayableModelFound.size() == 1);
      Assert.assertEquals(metaDisplayableModel.getUri(), metaDisplayableModelFound.get(0).getUri());
      
      try {
         datacoreApiClient.postDataInType(metaDisplayableModel);
         Assert.fail("(oasis.meta.)dcmi:mixin_0 resources can't be written"
               + " even by admin from oasis.sample project because it is "
               + "multiProjectStorage, otherwise they could wrongly be hidden "
               + "(which would be a data - or model here - fork) and anyway require "
               + "post filtering for queries");
      } catch (BadRequestException brex) {
         Assert.assertTrue(UnitTestHelper.readBodyAsString(brex).contains("belonging to another project"));
         // (actually not Forbidden but ModelResourceMappingService.getAndCheckModelResourceProject()
         // on ABOUT_TO_BUILD event before that)
      }
      
      // see test of different model resources (stored in same collection) with same uri in ProjectTest
      } finally {
         authenticationService.logout();
      }
   }
   
   @Test
   public void testLoadPersistedModels() throws QueryException {
      DCModelBase origCityModel = modelAdminService.getModelBase(CityCountrySample.CITY_MODEL_NAME);
      Assert.assertNotNull(origCityModel);
      Assert.assertNotNull(datacoreApiClient
            .getData(ResourceModelIniter.MODEL_MODEL_NAME, CityCountrySample.CITY_MODEL_NAME));
      
      // removing model then reloading its project's models :
      modelAdminService.removeModel(origCityModel);
      Assert.assertNull("model removed", modelAdminService.getModelBase(CityCountrySample.CITY_MODEL_NAME));
      Assert.assertNotNull("model removed but its resource should still be there", datacoreApiClient
            .getData(ResourceModelIniter.MODEL_MODEL_NAME, CityCountrySample.CITY_MODEL_NAME));
      // reloading :
      try {
         authenticationService.loginAs("admin"); // else AuthenticationCredentialsNotFoundException in calling entityService
         loadPersistedModelsAtInitService.loadModels(DCProject.OASIS_SAMPLE);
      } finally {
         authenticationService.logout();
      }
      Assert.assertNotNull(modelAdminService.getModelBase(CityCountrySample.CITY_MODEL_NAME));

      // removing model then reloading all models :
      modelAdminService.removeModel(origCityModel);
      Assert.assertNull("model removed", modelAdminService.getModelBase(CityCountrySample.CITY_MODEL_NAME));
      Assert.assertNotNull("model removed but its resource should still be there", datacoreApiClient
            .getData(ResourceModelIniter.MODEL_MODEL_NAME, CityCountrySample.CITY_MODEL_NAME));
      // then reloading its project's models :
      try {
         authenticationService.loginAs("admin"); // else AuthenticationCredentialsNotFoundException in calling entityService
         loadPersistedModelsAtInitService.loadProjects();
      } finally {
         authenticationService.logout();
      }
      Assert.assertNotNull(modelAdminService.getModelBase(CityCountrySample.CITY_MODEL_NAME));
      
      // TODO also test removing project
   }

   
   @Test
   public void testFrozenModels() throws Exception {
      // put in initial state in case test was aborted AND check that model can then be changed :
      DCResource pr = datacoreApiClient.getData(ResourceModelIniter.MODEL_PROJECT_NAME, DCProject.OASIS_SAMPLE);
      DCProject p = mrMappingService.toProject(pr);
      p.setFrozenModelNames(new LinkedHashSet<String>());
      pr = datacoreApiClient.putDataInTypeInProject(mrMappingService.projectToResource(p, pr),
            DCProject.OASIS_META); // can't write outside project ; using PUT to replace set list
      DCResource mr = datacoreApiClient.getData(ResourceModelIniter.MODEL_MODEL_NAME, CityCountrySample.CITY_MODEL_NAME);
      DCModelBase m = mrMappingService.toModelOrMixin(mr);
      m.getField("city:founded").setRequired(false);
      mr = datacoreApiClient.postDataInType(mrMappingService.modelToResource(m, mr));
      m = mrMappingService.toModelOrMixin(mr); // to update version
      // checking initial state :
      Assert.assertTrue(modelAdminService.getProject(
            DCProject.OASIS_SAMPLE).getFrozenModelNames().isEmpty());
      Assert.assertFalse(modelAdminService.getModelBase(
            CityCountrySample.CITY_MODEL_NAME).getField("city:founded").isRequired());
      
      // add frozen model :
      //pr.set("dcmp:frozenModelNames", DCResource.listBuilder().add(CityCountrySample.CITY_MODEL_NAME).build());
      p.getFrozenModelNames().add(CityCountrySample.CITY_MODEL_NAME);
      try {
      p.setVersion(datacoreApiClient.getData(ResourceModelIniter.MODEL_PROJECT_NAME,
            DCProject.OASIS_SAMPLE).getVersion()); // else 409 conflict
      pr = datacoreApiClient.postDataInTypeInProject(mrMappingService.projectToResource(p, pr),
            DCProject.OASIS_META); // can't write outside project
      p = mrMappingService.toProject(pr);
      Assert.assertEquals("frozen model should have been added",
            1, modelAdminService.getProject(DCProject.OASIS_SAMPLE).getFrozenModelNames().size());
      Assert.assertEquals(modelAdminService.getProject(DCProject.OASIS_SAMPLE).getFrozenModelNames()
            .iterator().next(), CityCountrySample.CITY_MODEL_NAME);
      // try to change frozen model :
      m.getField("city:founded").setRequired(true);
      try {
         mr = datacoreApiClient.postDataInType(mrMappingService.modelToResource(m, mr));
         m = mrMappingService.toModelOrMixin(mr); // to update version in case fails
         Assert.fail("Should not be able to change a frozen model");
      } catch (ForbiddenException fex) {
         Assert.assertTrue(UnitTestHelper.readBodyAsString(fex).contains("Access denied error (Can't update frozen model")); // Access denied error (Can't update frozen model sample.city.city in project oasis.sample) while converting or enriching to model or mixin sample.city.city with dcmo:pointOfViewAbsoluteName=oasis.sample in project oasis.sample, aborting POSTing resource (as admin).
         Assert.assertEquals("The frozen model shouldn't have been persisted", mr.getVersion(), datacoreApiClient
               .getData(ResourceModelIniter.MODEL_MODEL_NAME, CityCountrySample.CITY_MODEL_NAME).getVersion());
      }
      
      // rather use wildcard
      //pr.set("dcmp:frozenModelNames", DCResource.listBuilder().add(DCProject.MODEL_NAMES_WILDCARD).build());
      p.setVersion(datacoreApiClient.getData(ResourceModelIniter.MODEL_PROJECT_NAME,
            DCProject.OASIS_SAMPLE).getVersion()); // else 409 conflict
      p.getFrozenModelNames().clear();
      p.getFrozenModelNames().add(DCProject.MODEL_NAMES_WILDCARD);
      p.getFrozenModelNames().add("anyother:Modelname");
      pr = datacoreApiClient.putDataInTypeInProject(mrMappingService.projectToResource(p, pr),
            DCProject.OASIS_META); // can't write outside project ; using PUT to replace set list
      p = mrMappingService.toProject(pr);
      Assert.assertEquals("frozen models should have been added",
            2, modelAdminService.getProject(DCProject.OASIS_SAMPLE).getFrozenModelNames().size());
      Assert.assertEquals(modelAdminService.getProject(DCProject.OASIS_SAMPLE).getFrozenModelNames()
            .iterator().next(), DCProject.MODEL_NAMES_WILDCARD); // (order is preserved)
      // try to change frozen model :
      m.getField("city:founded").setRequired(true);
      try {
         mr = datacoreApiClient.postDataInType(mrMappingService.modelToResource(m, mr));
         m = mrMappingService.toModelOrMixin(mr); // to update version in case fails
         Assert.fail("Should not be able to change a frozen model");
      } catch (ForbiddenException fex) {
         Assert.assertTrue(UnitTestHelper.readBodyAsString(fex).contains("Access denied error (Can't update frozen model")); // Access denied error (Can't update frozen model sample.city.city in project oasis.sample) while converting or enriching to model or mixin sample.city.city with dcmo:pointOfViewAbsoluteName=oasis.sample in project oasis.sample, aborting POSTing resource (as admin).
         Assert.assertEquals("The frozen model shouldn't have been persisted", mr.getVersion(), datacoreApiClient
               .getData(ResourceModelIniter.MODEL_MODEL_NAME, CityCountrySample.CITY_MODEL_NAME).getVersion());
      }

      // clearing and reloading :
      modelAdminService.getProject(p.getName()).getFrozenModelNames().clear();
      try {
         authenticationService.loginAs("admin"); // else AuthenticationCredentialsNotFoundException in calling entityService
         loadPersistedModelsAtInitService.loadProject(datacoreApiClient.getData(ResourceModelIniter
               .MODEL_PROJECT_NAME, DCProject.OASIS_SAMPLE), DCProject.OASIS_SAMPLE);
      } finally {
         authenticationService.logout();
      }
      Assert.assertEquals("frozen model names should have been loaded from persistence",
            2, modelAdminService.getProject(p.getName()).getFrozenModelNames().size());
      Assert.assertEquals(modelAdminService.getProject(p.getName()).getFrozenModelNames()
            .iterator().next(), DCProject.MODEL_NAMES_WILDCARD);
      
      // restore initial state :
      } finally {
         p.setVersion(datacoreApiClient.getData(ResourceModelIniter.MODEL_PROJECT_NAME,
               DCProject.OASIS_SAMPLE).getVersion()); // else 409 conflict
         p.getFrozenModelNames().clear();
         pr = datacoreApiClient.putDataInTypeInProject(mrMappingService.projectToResource(p, pr),
               DCProject.OASIS_META); // can't write outside project ; using PUT to replace set list
         p = mrMappingService.toProject(pr);
         Assert.assertTrue(modelAdminService.getProject(
               DCProject.OASIS_SAMPLE).getFrozenModelNames().isEmpty());
         m.getField("city:founded").setRequired(false);
         mr = datacoreApiClient.postDataInType(mrMappingService.modelToResource(m, mr));
      }
   }

   
   @Test
   public void testAllowedModelPrefixes() throws Exception {
      // put in initial state in case test was aborted AND check that model can then be changed :
      DCResource pr = datacoreApiClient.getData(ResourceModelIniter.MODEL_PROJECT_NAME, DCProject.OASIS_SAMPLE);
      DCProject p = mrMappingService.toProject(pr);
      p.setAllowedModelPrefixes(new LinkedHashSet<String>());
      pr = datacoreApiClient.putDataInTypeInProject(mrMappingService.projectToResource(p, pr),
            DCProject.OASIS_META); // can't write outside project ; using PUT to replace set list
      DCResource mr = datacoreApiClient.getData(ResourceModelIniter.MODEL_MODEL_NAME, CityCountrySample.CITY_MODEL_NAME);
      DCModelBase m = mrMappingService.toModelOrMixin(mr);
      m.getField("city:founded").setRequired(false);
      mr = datacoreApiClient.postDataInType(mrMappingService.modelToResource(m, mr));
      m = mrMappingService.toModelOrMixin(mr); // to update version
      // checking initial state :
      Assert.assertTrue(modelAdminService.getProject(
            DCProject.OASIS_SAMPLE).getAllowedModelPrefixes().isEmpty());
      Assert.assertFalse(modelAdminService.getModelBase(
            CityCountrySample.CITY_MODEL_NAME).getField("city:founded").isRequired());
      
      // add another allowed prefix :
      //pr.set("dcmp:allowedModelPrefixes", DCResource.listBuilder().add("anyotherprefix").build());
      p.getAllowedModelPrefixes().add("anyotherprefix");
      try {
      p.setVersion(datacoreApiClient.getData(ResourceModelIniter.MODEL_PROJECT_NAME,
            DCProject.OASIS_SAMPLE).getVersion()); // else 409 conflict
      pr = datacoreApiClient.postDataInTypeInProject(mrMappingService.projectToResource(p, pr),
            DCProject.OASIS_META); // can't write outside project
      p = mrMappingService.toProject(pr);
      Assert.assertEquals("allowed prefix should have been added",
            1, modelAdminService.getProject(DCProject.OASIS_SAMPLE).getAllowedModelPrefixes().size());
      Assert.assertEquals(modelAdminService.getProject(DCProject.OASIS_SAMPLE).getAllowedModelPrefixes()
            .iterator().next(), "anyotherprefix");
      // try to change model without any allowed prefix :
      m.getField("city:founded").setRequired(true);
      try {
         mr = datacoreApiClient.postDataInType(mrMappingService.modelToResource(m, mr));
         m = mrMappingService.toModelOrMixin(mr); // to update version in case fails
         Assert.fail("Should not be able to change a model without any allowed prefix");
      } catch (ForbiddenException fex) {
         Assert.assertTrue(UnitTestHelper.readBodyAsString(fex).contains("Access denied error (Can't update model without any allowed model prefix")); // Access denied error (Can't update model without any allowed model prefix ([anyotherprefix]) sample.city.city in project oasis.sample) while converting or enriching to model or mixin sample.city.city with dcmo:pointOfViewAbsoluteName=oasis.sample in project oasis.sample, aborting POSTing resource (as admin).
         Assert.assertEquals("The model without any allowed prefix shouldn't have been persisted",
               mr.getVersion(), datacoreApiClient.getData(ResourceModelIniter.MODEL_MODEL_NAME,
                     CityCountrySample.CITY_MODEL_NAME).getVersion());
      }
      
      // add the right allowed prefix :
      //pr.set("dcmp:allowedModelPrefixes", DCResource.listBuilder().add("sample.city.").build());
      p.setVersion(datacoreApiClient.getData(ResourceModelIniter.MODEL_PROJECT_NAME,
            DCProject.OASIS_SAMPLE).getVersion()); // else 409 conflict
      p.getAllowedModelPrefixes().add("sample.city.");
      pr = datacoreApiClient.postDataInTypeInProject(mrMappingService.projectToResource(p, pr),
            DCProject.OASIS_META); // can't write outside project
      p = mrMappingService.toProject(pr);
      Assert.assertEquals("allowed prefix should have been added",
            2, modelAdminService.getProject(DCProject.OASIS_SAMPLE).getAllowedModelPrefixes().size());
      // try to change model with allowed prefix :
      m.getField("city:founded").setRequired(true);
      mr = datacoreApiClient.postDataInType(mrMappingService.modelToResource(m, mr));
      Assert.assertNotEquals("Model with allowed prefix should have been updated",
            (long) m.getVersion(), (long) mr.getVersion());
      m = mrMappingService.toModelOrMixin(mr); // to update version
      
      // rather use wildcard
      //pr.set("dcmp:allowedModelPrefixes", DCResource.listBuilder().add(DCProject.MODEL_NAMES_WILDCARD).build());
      p.setVersion(datacoreApiClient.getData(ResourceModelIniter.MODEL_PROJECT_NAME,
            DCProject.OASIS_SAMPLE).getVersion()); // else 409 conflict
      p.getAllowedModelPrefixes().clear();
      p.getAllowedModelPrefixes().add("anyotherprefix");
      p.getAllowedModelPrefixes().add(DCProject.MODEL_NAMES_WILDCARD);
      pr = datacoreApiClient.putDataInTypeInProject(mrMappingService.projectToResource(p, pr),
            DCProject.OASIS_META); // can't write outside project ; using PUT to replace set list
      p = mrMappingService.toProject(pr);
      Assert.assertEquals("allowed prefixes should have been added",
            2, modelAdminService.getProject(DCProject.OASIS_SAMPLE).getAllowedModelPrefixes().size());
      // try to change model with allowed prefix :
      m.getField("city:founded").setRequired(true);
      mr = datacoreApiClient.postDataInType(mrMappingService.modelToResource(m, mr));
      Assert.assertNotEquals("Model should have been updated because of allowed prefix wildcard",
            (long) m.getVersion(), (long) mr.getVersion());
      m = mrMappingService.toModelOrMixin(mr); // to update version

      // clearing and reloading :
      modelAdminService.getProject(p.getName()).getAllowedModelPrefixes().clear();
      try {
         authenticationService.loginAs("admin"); // else AuthenticationCredentialsNotFoundException in calling entityService
         loadPersistedModelsAtInitService.loadProject(datacoreApiClient.getData(ResourceModelIniter
               .MODEL_PROJECT_NAME, DCProject.OASIS_SAMPLE), DCProject.OASIS_SAMPLE);
      } finally {
         authenticationService.logout();
      }
      Assert.assertEquals("allowed prefixes should have been loaded from persistence",
            2, modelAdminService.getProject(p.getName()).getAllowedModelPrefixes().size());
      Assert.assertEquals(modelAdminService.getProject(p.getName()).getAllowedModelPrefixes()
            .iterator().next(), "anyotherprefix");
      
      // restore initial state :
      } finally {
         p.setVersion(datacoreApiClient.getData(ResourceModelIniter.MODEL_PROJECT_NAME,
               DCProject.OASIS_SAMPLE).getVersion()); // else 409 conflict
         p.getAllowedModelPrefixes().clear();
         pr = datacoreApiClient.putDataInTypeInProject(mrMappingService.projectToResource(p, pr),
               DCProject.OASIS_META); // can't write outside project ; using PUT to replace set list
         p = mrMappingService.toProject(pr);
         Assert.assertTrue(modelAdminService.getProject(
               DCProject.OASIS_SAMPLE).getAllowedModelPrefixes().isEmpty());
         m.getField("city:founded").setRequired(false);
         mr = datacoreApiClient.postDataInType(mrMappingService.modelToResource(m, mr));
      }
   }
   

   @Test
   public void testProjectReloadSecurityAndSetList() throws Exception {
      // preparing
      @SuppressWarnings("serial")
      LinkedHashSet<String> adminRco = new LinkedHashSet<String>() {{ add("u_admin"); }}; // in local dev only !
      @SuppressWarnings("serial")
      LinkedHashSet<String> testRco = new LinkedHashSet<String>() {{ add("test"); addAll(adminRco); }}; // merged
      DCResource pr = datacoreApiClient.getData(ResourceModelIniter.MODEL_PROJECT_NAME, DCProject.OASIS_SAMPLE);
      String sampleProjectUri = pr.getUri();
      DCProject p = mrMappingService.toProject(pr);
      @SuppressWarnings("serial")
      ArrayList<String> sampleProjectLocalVisibleProjectNames = new ArrayList<String>() {{
         add(DCProject.OASIS_META); add(PublicPrivateOrganizationSample.ORG1_PROJECT);
         add(PublicPrivateOrganizationSample.ORG2_PROJECT); add(PublicPrivateOrganizationSample.ORG3_PROJECT); }};
      DCResource metaPr = datacoreApiClient.getData(ResourceModelIniter.MODEL_PROJECT_NAME, DCProject.OASIS_META);
      String metaProjectUri = metaPr.getUri();
      DCResource mainPr = datacoreApiClient.getData(ResourceModelIniter.MODEL_PROJECT_NAME, DCProject.OASIS_MAIN);
      String mainProjectUri = mainPr.getUri();
      
      // put in initial state in case test was aborted AND check that model can then be changed :
      pr = datacoreApiClient.getData(ResourceModelIniter.MODEL_PROJECT_NAME, DCProject.OASIS_SAMPLE);
      p = mrMappingService.toProject(pr);
      //pr.set("dcmp:securityConstraints", null); // NOT p.setSecurityConstraints(null); because merged in POST-like behaviour
      ///p.getSecurityConstraints().getResourceCreationOwners().clear();
      //pr.set("dcmp:securityDefaults", null); // NOT p.setSecurityDefaults(null); because merged in POST-like behaviour
      p.getSecurityDefaults().setResourceCreationOwners(adminRco);
      //pr.set("dcmp:visibleSecurityConstraints", null); // NOT p.setVisibleSecurityConstraints(null); because merged in POST-like behaviour
      p.getVisibleSecurityConstraints().getResourceCreationOwners().clear();
      // and set list :
      p.getLocalVisibleProjects().clear();
      for (String pn : sampleProjectLocalVisibleProjectNames) {
         p.addLocalVisibleProject(modelAdminService.getProject(pn));
      }
      pr = datacoreApiClient.putDataInTypeInProject(mrMappingService.projectToResource(p, pr),
            DCProject.OASIS_META); // can't write outside project ; using PUT to replace set lists
      p = mrMappingService.toProject(pr);
      // checking initial state :
      DCProject actualProject = modelAdminService.getProject(DCProject.OASIS_SAMPLE);
      ///Assert.assertTrue(actualProject.getSecurityConstraints().getResourceCreationOwners().isEmpty());
      Assert.assertTrue(actualProject.getSecurityDefaults().getResourceCreationOwners().equals(adminRco));
      Assert.assertTrue(actualProject.getVisibleSecurityConstraints().getResourceCreationOwners().isEmpty());
      // and set list :
      Assert.assertEquals(sampleProjectLocalVisibleProjectNames, actualProject
            .getLocalVisibleProjects().stream().map(p1 -> p1.getName()).collect(Collectors.toList()));
      
      // set project security data :
      try {
      ///p.setSecurityConstraints(security);
      p.getSecurityDefaults().setResourceCreationOwners(testRco);
      p.getVisibleSecurityConstraints().setResourceCreationOwners(testRco);
      // and set list :
      p.getLocalVisibleProjects().clear();
      p.addLocalVisibleProject(modelAdminService.getProject(DCProject.OASIS_META)); // BUT never remove meta else model 404 not found !
      p.addLocalVisibleProject(mrMappingService.toProject(datacoreApiClient
            .getData(ResourceModelIniter.MODEL_PROJECT_NAME, DCProject.OASIS_MAIN)));
      pr = datacoreApiClient.postDataInTypeInProject(mrMappingService.projectToResource(p, pr),
            DCProject.OASIS_META); // can't write outside project ; using POST to merge set lists
      p = mrMappingService.toProject(pr);
      // checking changes :
      actualProject = modelAdminService.getProject(DCProject.OASIS_SAMPLE);
      /*Assert.assertTrue("security constraints should have been set",
            modelAdminService.getProject(DCProject.OASIS_SAMPLE).getSecurityConstraints() != null
            && modelAdminService.getProject(DCProject.OASIS_SAMPLE).getSecurityConstraints()
            .getResourceCreationOwners().equals(testRco));*/
      Assert.assertEquals("security defaults should have been set", testRco,
            actualProject.getSecurityDefaults().getResourceCreationOwners());
      Assert.assertEquals("visible security constraints should have been set", testRco,
            actualProject.getVisibleSecurityConstraints().getResourceCreationOwners());
      // and set list :
      Assert.assertEquals("local visible projects should have been added", new HashSet<String>() {{
         // (not ArrayList because merge produces different order)
         addAll(sampleProjectLocalVisibleProjectNames); add(DCProject.OASIS_MAIN); }},
            actualProject.getLocalVisibleProjects().stream().map(p1 -> p1.getName()).collect(Collectors.toSet()));

      // clearing and reloading :
      ///modelAdminService.getProject(DCProject.OASIS_SAMPLE).setSecurityConstraints(null);
      actualProject.getSecurityDefaults().setResourceCreationOwners(adminRco);
      actualProject.getVisibleSecurityConstraints().getResourceCreationOwners().clear();
      // and set list :
      actualProject.getLocalVisibleProjects().clear();
      actualProject.addLocalVisibleProject(modelAdminService.getProject(DCProject.OASIS_META)); // BUT never remove meta else model 404 not found !
      try {
         authenticationService.loginAs("admin"); // else AuthenticationCredentialsNotFoundException in calling entityService
         loadPersistedModelsAtInitService.loadProject(datacoreApiClient.getData(ResourceModelIniter
               .MODEL_PROJECT_NAME, DCProject.OASIS_SAMPLE), DCProject.OASIS_SAMPLE);
      } finally {
         authenticationService.logout();
      }
      // checking reload :
      actualProject = modelAdminService.getProject(DCProject.OASIS_SAMPLE);
      /*Assert.assertTrue("security constraints should have been reloaded",
            modelAdminService.getProject(DCProject.OASIS_SAMPLE).getSecurityConstraints() != null
            && modelAdminService.getProject(DCProject.OASIS_SAMPLE).getSecurityConstraints()
            .getResourceCreationOwners().equals(testRco));*/
      Assert.assertTrue("security defaults should have been reloaded", testRco.equals(
            actualProject.getSecurityDefaults().getResourceCreationOwners()));
      Assert.assertTrue("visible security constraints should have been reloaded", testRco.equals(
            actualProject.getVisibleSecurityConstraints().getResourceCreationOwners()));
      // and set list :
      Assert.assertEquals("local visible project should have been reloaded", new HashSet<String>() {{
         // (not ArrayList because merge produces different order)
         addAll(sampleProjectLocalVisibleProjectNames); add(DCProject.OASIS_MAIN); }},
               actualProject.getLocalVisibleProjects().stream().map(p1 -> p1.getName()).collect(Collectors.toSet()));
      
      // restore initial state :
      } finally {
         //pr.set("dcmp:securityConstraints", null); // NOT p.setSecurityConstraints(null); because merged in POST-like behaviour
         ///p.getSecurityConstraints().getResourceCreationOwners().clear();
         //pr.set("dcmp:securityDefaults", null); // NOT p.setSecurityDefaults(null); because merged in POST-like behaviour
         p.getSecurityDefaults().setResourceCreationOwners(adminRco);
         //pr.set("dcmp:visibleSecurityConstraints", null); // NOT p.setVisibleSecurityConstraints(null); because merged in POST-like behaviour
         p.getVisibleSecurityConstraints().getResourceCreationOwners().clear();
         // and set list :
         p.getLocalVisibleProjects().clear();
         for (String pn : sampleProjectLocalVisibleProjectNames) {
            p.addLocalVisibleProject(modelAdminService.getProject(pn));
         }
         pr = datacoreApiClient.putDataInTypeInProject(mrMappingService.projectToResource(p, pr),
               DCProject.OASIS_META); // can't write outside project ; using PUT to replace set list
         p = mrMappingService.toProject(pr);
         // checking initial state :
         actualProject = modelAdminService.getProject(DCProject.OASIS_SAMPLE);
         ///Assert.assertTrue(actualProject.getSecurityConstraints().getResourceCreationOwners().isEmpty());
         Assert.assertTrue(actualProject.getSecurityDefaults().getResourceCreationOwners().equals(adminRco));
         Assert.assertTrue(actualProject.getVisibleSecurityConstraints().getResourceCreationOwners().isEmpty());
         // and set list (including checking PUT replace instead of POST merge) :
         Assert.assertEquals(sampleProjectLocalVisibleProjectNames, actualProject
               .getLocalVisibleProjects().stream().map(p1 -> p1.getName()).collect(Collectors.toList()));
      }
   }
   
}
