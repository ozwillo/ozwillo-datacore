package org.oasis.datacore.sample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.oasis.datacore.contribution.service.ContributionService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.query.QueryException;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryService;
import org.oasis.datacore.core.init.InitableBase;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
import org.oasis.datacore.core.meta.model.DCI18nField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.security.mock.MockAuthenticationService;
import org.oasis.datacore.historization.exception.HistorizationException;
import org.oasis.datacore.historization.service.impl.HistorizationServiceImpl;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
import org.oasis.datacore.rest.server.DatacoreApiImpl;
import org.oasis.datacore.rest.server.event.EventService;
import org.oasis.datacore.rest.server.resource.ResourceEntityMapperService;
import org.oasis.datacore.rest.server.resource.ResourceService;
import org.oasis.datacore.rest.server.resource.UriService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;


/**
 * To write a sample class, make it extend this class and annotate it with @Component.
 * If your sample class depends on other sample classes (ex. data one on its model one),
 * tell Spring about it ex. add @DependsOn("citizenKinModel").
 * 
 * To work with Datacore Resources, use resourceService, or server side datacoreApiImpl
 * and the helper methods here
 * (both requiring to have logged in, which is done within init() as admin by default).
 * 
 * Calling it using datacoreApiCachedClient (DatacoreClientApi) works SAVE from appserver
 * (tomcat) and therefore can only be used in tests (or if deploying CXF on jetty within tomcat)
 * but NOT in samples init.
 * 
 * See why InitService (and not mere @PostConstruct or independent ApplicationListeners)
 * in InitService comments.
 *
 * @author mdutoo
 *
 */
public abstract class DatacoreSampleBase extends InitableBase/*implements ApplicationListener<ContextRefreshedEvent> */{

   private static final Logger logger = LoggerFactory.getLogger(DatacoreSampleBase.class);
   
   @Autowired
   protected MockAuthenticationService mockAuthenticationService;

   /** impl, to be able to modify it
    * TODO LATER extract interface */
   @Autowired
   protected DataModelServiceImpl modelAdminService;

   @Autowired
   @Qualifier("datacoreApiCachedJsonClient")
   protected /*DatacoreApi*/DatacoreCachedClient datacoreApiClient;
   /** for tests */
   @Autowired
   protected DatacoreApiImpl datacoreApiImpl;
   /** to cleanup db
    * TODO LATER rather in service */
   @Autowired
   protected /*static */MongoOperations mgo;
   
   @Autowired
   protected ResourceService resourceService;
   @Autowired
   protected UriService uriService; // to build URIs when using ResourceService
   @Autowired
   protected LdpEntityQueryService ldpEntityQueryService;
   @Autowired
   protected ResourceEntityMapperService resourceEntityMapperService; // to unmap LdpEntityQueryService results

   @Autowired
   protected EventService eventService;

   @Autowired
   private HistorizationServiceImpl historizationService;
   
   @Autowired
   private ContributionService contributionService;

   protected HashSet<DCModel> models = new HashSet<DCModel>();

   
   
   ///////////////////////////////////////////////////////////////////////////:
   // Initable impl
   // 

   /**
    * Override to come before or after
    */
   @Override
   public int getOrder() {
      return 1000;
   }
   
   /*@Override
   public void onApplicationEvent(ContextRefreshedEvent event) {
      this.init();
   }*/
   //@PostConstruct // NOO deadlock, & same for ApplicationContextAware
   @Override
   protected void doInit() {
      mockAuthenticationService.loginAs("admin");
      try {
         List<DCModelBase> modelsToCreate = new ArrayList<DCModelBase>();
         buildModels(modelsToCreate);
         boolean allCollectionsAlreadyExist = createModels(modelsToCreate, false);
         
         if (modelsToCreate.isEmpty()) { // data only sample
            cleanDataOfCreatedModels(); // uses dependent sample models
         } else if (/*enableFillDataAtInit() && */(!allCollectionsAlreadyExist || hasSomeModelsWithoutResource())) {
            cleanDataOfCreatedModels(modelsToCreate);
            fillData();
         }

      } catch (WebApplicationException waex) {
         logger.error("HTTP " + waex.getResponse().getStatus()
               + " web app error initing " + this.getClass().getName() + ":\n"
               + waex.getResponse().getStringHeaders()
               + ((waex.getResponse().getEntity() != null) ? "\n" + waex.getResponse().getEntity() + "\n" : ""), waex);

      } catch (Throwable t) {
         logger.error("Error initing " + this.getClass().getName(), t);
               
      } finally {
         mockAuthenticationService.logout();
      }
   }
   
   

   ///////////////////////////////////////////////////////////////////////////:
   // For tests : methods to use & implement
   // 
   
   protected abstract void buildModels(List<DCModelBase> models); // TODO TODOOOOO buildModels
   
   /**
    * If called explicitly, better to clean data first (else Conflict ?!) ;
    * IF IT CHANGES MODELS, models must be reinited first in tests !
    */
   protected abstract void fillData();
   
   /**
    * To be called by tests only if they require to init all data explicitly,
    * and even if there already are there (cleans first) ;
    * otherwise, do cleanDataOfCreatedModels(someModels) then createSomeData()
    */
   public void initData() {
      mockAuthenticationService.loginAs("admin");
      try {
         cleanDataOfCreatedModels();
         fillData();
      } finally {
         mockAuthenticationService.logout();
      }
   }
   public void flushData() {
      mockAuthenticationService.loginAs("admin");
      try {
         cleanDataOfCreatedModels();
      } finally {
         mockAuthenticationService.logout();
      }
   }

   /**
    * To be called by tests only if they require to init some models explictly.
    */
   public void initModels() {
      mockAuthenticationService.loginAs("admin");
      try {
         List<DCModelBase> modelsToCreate = new ArrayList<DCModelBase>();
         buildModels(modelsToCreate);
         for (DCModelBase model : modelsToCreate) {
            // NB. NOT this.models but new ones else might have been modified in previous initData()
            createModel(model, true);
         }
      } finally {
         mockAuthenticationService.logout();
      }
   }
   public void initModels(DCModelBase ... modelsToCreate) {
      initModels(Arrays.asList(modelsToCreate));
   }
   public void initModels(List<DCModelBase> modelsToCreate) {
      mockAuthenticationService.loginAs("admin");
      try {
         createModels(modelsToCreate, true);
      } finally {
         mockAuthenticationService.logout();
      }
   }
   
   /**
    * To be overriden if required
    * @return 
    * @return
    */
   /*protected boolean enableFillDataAtInit() {
      return true;
   }*/

   public HashSet<DCModel> getCreatedModels() {
      return this.models;
   }
   // for tests
   public void cleanCreatedModels() {
      for (DCModel model : this.getCreatedModels()) {
         mgo.dropCollection(model.getCollectionName()); // delete data
         modelAdminService.removeModel(model.getName()); // remove model
      }
      this.models.clear();
   }
   public void cleanModels(DCModelBase ... models) {
      cleanModels(Arrays.asList(models));
   }
   public void cleanModels(List<DCModelBase> models) {
      List<DCModelBase> toBeRemovedModels = new ArrayList<DCModelBase>(models.size());
      for (DCModelBase model : models) {
         toBeRemovedModels.add(getCreatedModel(model.getName()));
         if (model instanceof DCModel) {
            DCModel dcModel = (DCModel) model;
            mgo.dropCollection(dcModel.getCollectionName()); // delete data
            
            if (dcModel.isHistorizable()) {
               try {
                  String historizationCollectionName = historizationService.getHistorizedCollectionNameFromOriginalModel(dcModel);
                  //mgo.remove(new Query(), historizationCollectionName);
                  mgo.dropCollection(historizationCollectionName);
               } catch (HistorizationException e) {
                  logger.error("error while dropping (historization of) model "
                        + model.getName(), e);
               }
            }
            
            if (dcModel.isContributable()) {
               String contributionCollectionName = model.getName() + ".c"; // TODO TODOOOOOO move
               //mgo.remove(new Query(), historizationCollectionName);
               mgo.dropCollection(contributionCollectionName);
            }
            
            modelAdminService.removeModel(model.getName()); // remove model
         }
      }
      this.models.removeAll(toBeRemovedModels); // clean sampleBase state
   }

   public void cleanDataOfCreatedModels() {
      for (DCModel model : this.getCreatedModels()) {
         cleanDataOfCreatedModel(model);
      }
   }
   public void cleanDataOfCreatedModels(DCModelBase ... models) {
      cleanDataOfCreatedModels(Arrays.asList(models));
   }
   public void cleanDataOfCreatedModels(List<DCModelBase> models) {
      for (DCModelBase model : models) {
         if (model instanceof DCModel) {
            cleanDataOfCreatedModel((DCModel) model);
         }
      }
   }
   public void cleanDataOfCreatedModel(DCModel model) {
      // delete (rather than drop & recreate !) : 
      mgo.remove(new Query(), model.getCollectionName());

      if (model.isHistorizable()) {
         try {
            DCModel historizedModel = historizationService.getHistorizationModel(model);
            if (historizedModel == null) {
               historizedModel = historizationService.createHistorizationModel(model); // TODO ??????
            }
            mgo.remove(new Query(), historizedModel.getCollectionName());
         } catch (HistorizationException e) {
            throw new RuntimeException("Historization init error of Model " + model.getName(), e);
         }
      }
      
      if (model.isContributable()) {
         String contributionCollectionName = model.getName() + ".c"; // TODO TODOOOOOO move
         mgo.remove(new Query(), contributionCollectionName);
      }
   }
   
   
   
   public DCModelBase getCreatedModel(String modelType, List<DCModelBase> modelsToCreate) {
      for (DCModelBase model : modelsToCreate) {
         if (model.getName().equals(modelType)) {
            return model;
         }
      }
      for (DCModelBase model : this.models) {
         if (model.getName().equals(modelType)) {
            return model;
         }
      }
      return null;
   }
   private DCModelBase getCreatedModel(String modelType) {
      for (DCModelBase model : this.models) {
         if (model.getName().equals(modelType)) {
            return model;
         }
      }
      return null;
   }
   
   
   protected boolean hasSomeModelsWithoutResource() {
      for (DCModel model : this.models) {
         try {
            List<DCEntity> resources = ldpEntityQueryService.findDataInType(model,
                  new HashMap<String,List<String>>(0), 0, 1);
            if (resources == null || resources.isEmpty()) {
               return true;
            }
         } catch (QueryException e) {
            throw new RuntimeException("Init error of resources of model " + model.getName(), e);
         }
      }
      return false;
   }
   
   /*protected void createModels(DCModelBase ... modelOrMixins) {
      createModels(false, modelOrMixins);
   }*/
   /**
    * 
    * @param deleteCollectionsFirst
    * @param modelOrMixins
    * @return allCollectionsAlreadyExist
    */
   protected boolean createModels(List<DCModelBase> modelOrMixins, boolean deleteCollectionsFirst) {
      boolean allCollectionsAlreadyExist = true;
      for (DCModelBase modelOrMixin : modelOrMixins) {
         allCollectionsAlreadyExist = createModel(modelOrMixin, deleteCollectionsFirst)
               && allCollectionsAlreadyExist;
      }
      return allCollectionsAlreadyExist;
   }
   /**
    * 
    * @param deleteCollectionsFirst
    * @param modelOrMixin
    * @return collectionAlreadyExists
    */
   protected boolean createModel(DCModelBase modelOrMixin, boolean deleteCollectionsFirst) {
      if (modelOrMixin instanceof DCModel) {
         DCModel model = (DCModel) modelOrMixin;
         if (deleteCollectionsFirst) {
            modelAdminService.removeModel(model.getName());
            models.remove(getCreatedModel(model.getName()));
         }
         // adding model
         modelAdminService.addModel(model);
         models.add(model);
         
         return ensureCollectionAndIndices(model, deleteCollectionsFirst);
         
      } else { // mixin
         DCMixin mixin = (DCMixin) modelOrMixin;
         modelAdminService.addMixin(mixin);
         return true;
      }
   }

   
   
   ///////////////////////////////////////////////////////////////////////////:
   // Resource collection & index creation
   // TODO move
   
   public boolean ensureCollectionAndIndices(DCModel model, boolean deleteCollectionsFirst) {
      if (deleteCollectionsFirst) {
         // cleaning data first
         mgo.dropCollection(model.getCollectionName());
      }
      boolean collectionAlreadyExists = ensureCollectionAndIndices(model);
      
      if(model.isHistorizable()) {
         collectionAlreadyExists = ensureHistorizedCollectionAndIndices(model, deleteCollectionsFirst)
               || collectionAlreadyExists;
      }
      
      if (model.isContributable()) {
         collectionAlreadyExists = ensureContributedCollectionAndIndices(model, deleteCollectionsFirst);
      }
      
      return collectionAlreadyExists;
   }

   public boolean ensureHistorizedCollectionAndIndices(DCModel model, boolean deleteCollectionsFirst) {
      DCModel historizedModel;
      try {
         historizedModel = historizationService.createHistorizationModel(model);
         if (deleteCollectionsFirst) {
            // cleaning data first
            mgo.dropCollection(historizedModel.getCollectionName());
         }
         return ensureCollectionAndIndices(historizedModel);
      } catch (HistorizationException e) {
         throw new RuntimeException("Historization init error of Model " + model.getName(), e);
      }
   }

   public boolean ensureContributedCollectionAndIndices(DCModel model, boolean deleteCollectionsFirst) {
      //contributiondModel = contributionService.createContributionModel(model); // TODO TODOOO
      if (deleteCollectionsFirst) {
         // cleaning data first
         String contributionCollectionName = model.getName() + ".c"; // TODO TODOOOOOO move
         mgo.dropCollection(contributionCollectionName);
      }
      return false; // ensureCollectionAndIndices(historizedModel); // TODO TODOOOO
   }

   
   private boolean ensureCollectionAndIndices(DCModel model) {
      DBCollection coll;
      boolean collectionAlreadyExists = mgo.collectionExists(model.getCollectionName()); 
      if (collectionAlreadyExists) {
         coll = mgo.getCollection(model.getCollectionName());
      } else {
         coll = mgo.createCollection(model.getCollectionName());
      }
      
      // generating static indexes
      coll.ensureIndex(new BasicDBObject(DCEntity.KEY_URI, 1), null, true);
      coll.ensureIndex(new BasicDBObject(DCEntity.KEY_AR, 1)); // for query security
      coll.ensureIndex(new BasicDBObject(DCEntity.KEY_CH_AT, 1)); // for default order
      
      // generating field indices
      ensureFieldIndices(coll, DCEntity.KEY_P + ".", model.getGlobalFieldMap().values());
      
      return collectionAlreadyExists;
   }

   private void ensureFieldIndices(DBCollection coll, String prefix, Collection<DCField> globalFields) {
      for (DCField globalField : globalFields) {
         ensureFieldIndices(coll, prefix, globalField);
      }
   }

   private void ensureFieldIndices(DBCollection coll, String prefix, DCField globalField) {
      String prefixedGlobalFieldName = prefix + globalField.getName();
      if (globalField.getQueryLimit() > 0) {
         coll.ensureIndex(prefixedGlobalFieldName);
      }
      switch (DCFieldTypeEnum.getEnumFromStringType(globalField.getType())) {
      case LIST:
         DCField listField = ((DCListField) globalField).getListElementField();
         ensureFieldIndices(coll, prefixedGlobalFieldName + ".", listField);
         break;
      case MAP:
         Map<String, DCField> mapFields = ((DCMapField) globalField).getMapFields();
         // TODO WARNING : single map field can't be indexed !!!
         ensureFieldIndices(coll, prefixedGlobalFieldName + ".", mapFields.values());
         break;
      case I18N:
         DCField listI18nField = ((DCI18nField) globalField);
         DCField map = ((DCListField) listI18nField).getListElementField();
         Map<String, DCField> mapContent = ((DCMapField) map).getMapFields();
         ensureFieldIndices(coll, prefixedGlobalFieldName + ".", mapContent.values());
         break;
      default:
         break;
      }
      // TODO LATER embedded resources
   }


   /**
    * @obsolete use rather resourceService
    * Requires to have logged in first
    */
   public DCResource putDataInType(DCResource resource) {
      try {
         return datacoreApiImpl.putDataInType(resource, resource.getModelType(),
               uriService.parseUri(resource.getUri()).getId());
      } catch (WebApplicationException e) {
         if (e.getResponse().getStatus() / 100 != 2) {
            throw e;
         }
         return (DCResource) e.getResponse().getEntity();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * @obsolete use rather resourceService
    * Requires to have logged in first
    */
   public DCResource postDataInType(DCResource resource) {
      try {
         return datacoreApiImpl.postDataInType(resource, resource.getModelType());
      } catch (WebApplicationException e) {
         if (e.getResponse().getStatus() / 100 != 2) {
            throw e;
         }
         return (DCResource) e.getResponse().getEntity();
      }
   }

   /***
    * helper method to build new DCResources FOR TESTING ; 
    * copies the given Resource's field that are among the given modelOrMixins
    * (or all if modelOrMixins is null or empty) to this Resource
    * @param source
    * @param modelOrMixins
    * @return
    */
   public DCResource copy(DCResource THIS, DCResource source, DCModelBase ... modelOrMixins) {
      if (modelOrMixins == null || modelOrMixins.length == 0) {
         /*for (Object modelOrMixin : source.getProperties().keySet()) {
            
         }*/
         DCModel sourceModel = modelAdminService.getModel(source.getModelType()); // TODO service
         modelOrMixins = new DCModelBase[] { sourceModel };
         /*int sourceTypeNb = sourceTypes.size();
         // TODO or only mail Model (and its own mixins) ?
         if (sourceTypeNb > 1) {
            List<DCModelBase> modelOrMixinList = new ArrayList<DCModelBase>(sourceTypeNb);
            modelOrMixinList.add(sourceModel);
            for (int i = 1; i < sourceTypeNb; i++) {
               DCModelBase mixin = modelAdminService.getMixinOrModel(sourceTypes.get(i));
               modelOrMixinList.add(mixin);
            }
            modelOrMixins = modelOrMixinList.toArray(new DCModelBase[sourceTypeNb]);
         }*/
      }
      DCModel thisModel = modelAdminService.getModel(THIS.getModelType()); // TODO service
      for (DCModelBase modelOrMixin : modelOrMixins) {
         boolean hasModelOrMixin = thisModel.getName().equals(modelOrMixin.getName())
               /*|| modelAdminService.hasMixin(THIS, modelOrMixin)*/; // TODO service
         Map<String, DCField> fieldMap = modelOrMixin.getGlobalFieldMap();
         for (String fieldName : fieldMap.keySet()) {
            //DCField field = fieldMap.get(fieldName);
            if (hasModelOrMixin || thisModel.getField(fieldName) != null) {
               Object sourceValue = source.get(fieldName);
               THIS.set(fieldName, sourceValue);
            }
         }
      }
      return THIS;
   }
   public DCResource copy(DCResource THIS, DCResource source) {
      return copy(THIS, source, (DCModelBase[]) null);
   }
   
}
