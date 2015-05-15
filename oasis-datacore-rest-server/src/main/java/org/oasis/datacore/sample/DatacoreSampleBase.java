package org.oasis.datacore.sample;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.codec.binary.Base64;
import org.oasis.datacore.common.context.DCRequestContextProvider;
import org.oasis.datacore.common.context.SimpleRequestContextProvider;
import org.oasis.datacore.contribution.service.ContributionService;
import org.oasis.datacore.core.entity.DatabaseSetupService;
import org.oasis.datacore.core.entity.NativeModelService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.query.QueryException;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryService;
import org.oasis.datacore.core.init.InitableBase;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.core.security.mock.MockAuthenticationService;
import org.oasis.datacore.historization.service.impl.HistorizationServiceImpl;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
import org.oasis.datacore.rest.server.DatacoreApiImpl;
import org.oasis.datacore.rest.server.event.EventService;
import org.oasis.datacore.rest.server.resource.ResourceEntityMapperService;
import org.oasis.datacore.rest.server.resource.ResourceService;
import org.oasis.datacore.server.uri.UriService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoOperations;

import com.google.common.collect.ImmutableMap;


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
   
   protected final Logger logger = LoggerFactory.getLogger(getClass());
   
   @Autowired
   protected MockAuthenticationService mockAuthenticationService;

   /** impl, to be able to modify it
    * TODO LATER extract interface */
   @Autowired
   protected DataModelServiceImpl modelAdminService;
   @Autowired
   protected NativeModelService nativeModelService;
   
   @Autowired
   @Qualifier("datacoreApiCachedJsonClient")
   protected /*DatacoreApi*/DatacoreCachedClient datacoreApiClient;
   /** for tests */
   @Autowired
   protected DatacoreApiImpl datacoreApiImpl;
   /** to cleanup db
    * TODO LATER rather in service */
   @Autowired
   protected MongoOperations mgo;
   /** to create indexes */
   @Autowired
   protected DatabaseSetupService databaseSetupService;
   
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

   protected HashSet<DCModelBase> storageModels = new HashSet<DCModelBase>();

   
   
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
            if (!neverCleanData()) {
               cleanDataOfCreatedModels(modelsToCreate);
            }
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

   /** default false, override to change ; requires to also override refreshBeforePost,
    * or to refresh explicitly before posting */
   protected boolean neverCleanData() {
      return false;
   }
   
   /** default false, override to change */
   protected boolean refreshBeforePost() {
      return false;
   }
   
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

   /** should only be storage */
   public HashSet<DCModelBase> getCreatedStorageModels() {
      return this.storageModels;
   }
   // for tests
   public void cleanModels() {
      cleanModels(new ArrayList<DCModelBase>(getCreatedStorageModels()));
   }
   public void cleanModels(DCModelBase ... models) {
      cleanModels(Arrays.asList(models));
   }
   public void cleanModels(List<DCModelBase> models) {
      List<DCModelBase> toBeRemovedModels = new ArrayList<DCModelBase>(models.size());
      for (DCModelBase model : models) {
         toBeRemovedModels.add(getCreatedModel(model));
         databaseSetupService.cleanModel(model);
         
         modelAdminService.removeModel(model.getName()); // remove model
      }
      this.storageModels.removeAll(toBeRemovedModels); // clean sampleBase state
   }

   public void cleanDataOfCreatedModels() {
      for (DCModelBase model : this.getCreatedStorageModels()) {
         this.databaseSetupService.cleanDataOfCreatedModel(model);
      }
   }
   public void cleanDataOfCreatedModels(DCModelBase ... models) {
      cleanDataOfCreatedModels(Arrays.asList(models));
   }
   public void cleanDataOfCreatedModels(List<DCModelBase> models) {
      for (DCModelBase model : models) {
         this.databaseSetupService.cleanDataOfCreatedModel(model);
      }
   }
   /**
    * Does nothing if not storage
    * @param storageModel
    */
   /*public void cleanDataOfCreatedModel(DCModelBase storageModel) {
      this.databaseSetupService.cleanDataOfCreatedModel(storageModel);
   }*/
   
   

   public DCModelBase getCreatedModel(DCModelBase dcModel, List<DCModelBase> modelsToCreate) {
      return getCreatedModel(dcModel.getName(), modelsToCreate, dcModel.getProjectName());
   }
   /**
    * 
    * @param modelType
    * @param modelsToCreate
    * @param projectName if null uses the current one
    * @return
    */
   public DCModelBase getCreatedModel(String modelType, List<DCModelBase> modelsToCreate, String projectName) {
      if (projectName == null) {
         projectName = modelAdminService.getProject().getName();
      }
      String modelAbsoluteName = projectName + "." + modelType;
      for (DCModelBase model : modelsToCreate) {
         if (model.getAbsoluteName().equals(modelAbsoluteName)) {
            return model;
         }
      }
      return getCreatedModel(modelAbsoluteName);
   }
   private DCModelBase getCreatedModel(DCModelBase dcModel) {
      return getCreatedModel(dcModel.getAbsoluteName());
   }
   private DCModelBase getCreatedModel(String modelAbsoluteName) {
      for (DCModelBase model : this.storageModels) {
         if (model.getAbsoluteName().equals(modelAbsoluteName)) {
            return model;
         }
      }
      return null;
   }
   
   
   protected boolean hasSomeModelsWithoutResource() throws RuntimeException {
      for (DCModelBase model : this.storageModels) {
         String modelType = model.getName();
         // set context project beforehands :
         List<DCEntity> resources = new SimpleRequestContextProvider<List<DCEntity>>() {
            protected List<DCEntity> executeInternal() throws QueryException {
               return ldpEntityQueryService.findDataInType(modelType, null, 0, 1);
            } // TODO better <T> T executeInternal()
         }.execInContext(new ImmutableMap.Builder<String, Object>()
               .put(DCRequestContextProvider.PROJECT, model.getProjectName()).build());
         if (resources == null || resources.isEmpty()) {
            return true;
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
   protected boolean createModel(DCModelBase model, boolean deleteCollectionsFirst) {
      if (model.isStorage()) {
         if (deleteCollectionsFirst) {
            modelAdminService.removeModel(model);
         }
         storageModels.remove(getCreatedModel(model)); // even if !deleteCollectionsFirst !!!
         // adding model
         modelAdminService.addModel(model);
         storageModels.add(model);
         
         return databaseSetupService.ensureCollectionAndIndices(model, deleteCollectionsFirst);
         
      } else { // mixin
         modelAdminService.addMixin(model);
         return true;
      }
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

   public List<DCResource> postAllDataInType(List<DCResource> resources) {
      return resources.stream().map(r -> postDataInType(r)).collect(Collectors.toList());
   }
   
   /**
    * @obsolete use rather resourceService
    * Requires to have logged in first
    */
   public DCResource postDataInType(DCResource resource) throws RuntimeException {
      if (refreshBeforePost()) {
         try {
            datacoreApiImpl.getData(resource.getModelType(),
                  UriHelper.parseUri(resource.getUri()).getId(), resource.getVersion());
         } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() / 100 == 2){ // successfully found
               DCResource existingResource = ((DCResource) e.getResponse().getEntity());
               resource.setVersion(existingResource.getVersion());
            } else if (e.getResponse().getStatus() == 304 // not modified
                  || e.getResponse().getStatus() == 404) { // not found, allow creation of new resource
            } else {
               throw e;
            }
         } catch (MalformedURLException e) {
            throw new RuntimeException(e);
         } catch (URISyntaxException e) {
            throw new RuntimeException(e);
         }
      }
      try {
         return datacoreApiImpl.postDataInType(resource, resource.getModelType());
      } catch (WebApplicationException e) {
         if (e.getResponse().getStatus() / 100 != 2) {
            throw e;
         }
         return (DCResource) e.getResponse().getEntity();
      }
   }
   /**
    * @obsolete use rather resourceService
    * Requires to have logged in first
    */
   public DCResource postDataInType(DCResource resource, String projectName) throws RuntimeException {
      return new SimpleRequestContextProvider<DCResource>() { // set context project beforehands :
         protected DCResource executeInternal() {
            return /*datacoreApiClient.*/postDataInType(resource);
         }
      }.execInContext(new ImmutableMap.Builder<String, Object>()
            .put(DCRequestContextProvider.PROJECT, projectName).build());
   }

   
   /////////////////////////////////////////////////////////////////////:
   //
   
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
         DCModelBase sourceModel = modelAdminService.getModelBase(source.getModelType()); // TODO service
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
      DCModelBase thisModel = modelAdminService.getModelBase(THIS.getModelType()); // TODO service
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
   
   
   /////////////////////////////////////////////////////////////////////
   // Helpers for building Models, TODO move
   
   public static String generateId(String tooComplicatedId) {
      try {
         return new String(Base64.encodeBase64( // else unreadable
               MessageDigest.getInstance("MD5").digest(tooComplicatedId.getBytes("UTF-8"))), "UTF-8");
      } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
         // should not happen
         // TODO log
         throw new RuntimeException(e);
      }
   }


   public static DCMixin createReferencingMixin(DCModelBase model, String ... embeddedFieldNames) {
      return createReferencingMixin(model, null, null, true, embeddedFieldNames);
   }
   /**
    * modelName_ref_0 (rather than modelName_0_ref, or modelName_0_ref_0)
    * because referencing mixin are strictly derived from models,
    * and its major version is auto derived because of its resource fields
    * pointing to majorVersioned models.
    * (HOWEVER this is a big constraint, because a new major version / model implies
    * new major versions / models in all resources indirectly linking to it,
    * so should rather add new fields OR VERSION THEM, OR VERSION MIXINS BUT NOT MODELS
    * (which is the same as having contributions from different sources / branches in the same Model)
    * and have new clients update new ones but also update them from older ones
    * changed by old clients)
    * NB. source model(s) are known by the resourceType resource fields pointing to them
    * TODO extract as helper
    * @param model
    * @param copyReferencingMixins
    * @param embeddedFieldNames
    * @return
    */
   public static DCMixin createReferencingMixin(DCModelBase model,
         DCMixin optInheritedReferencingMixin, DCMixin optDescendantMixin,
         boolean copyReferencingMixins, String ... embeddedFieldNames) {
      String refMixinNameRoot;
      
      // parsing model name TODO lifecycle
      String[] modelType = model.getName().split("_", 2); // TODO better
      String modelName = (modelType.length == 2) ? modelType[0] : model.getName();
      String modelVersionIfAny = (modelType.length == 2) ? modelType[1] : null;
      String modelNameWithVersionIfAny = model.getName();
      
      // parsing mixin name TODO lifecycle
      if (optDescendantMixin != null) {
         String[] optDescendantMixinType = optDescendantMixin.getName().split("_", 2); // TODO better
         String optDescendantMixinName = (optDescendantMixinType.length == 2) ?
               optDescendantMixinType[0] : optDescendantMixin.getName();
         String optDescendantMixinVersionIfAny = (optDescendantMixinType.length == 2) ?
               optDescendantMixinType[1] : null;
         refMixinNameRoot = optDescendantMixinName;
      } else {
         //refMixinNameRoot = modelNameWithVersionIfAny
         refMixinNameRoot = modelName;
      }
      
      String mixinVersion = "_0"; // has to be defined explicitly,
      // rather than ((modelVersionIfAny != null) ? "_" + modelVersionIfAny : "")
      DCMixin referencingMixin = (DCMixin) new DCMixin(refMixinNameRoot + "_ref" + mixinVersion,
            model.getPointOfViewAbsoluteName());
      
      if (copyReferencingMixins) {
         // copy referencing mixins :
         Collection<DCModelBase> referencingMixins;
         if (optInheritedReferencingMixin == null) {
            referencingMixins = model.getGlobalMixins();
         } else  {
            referencingMixins = new ArrayList<DCModelBase>(optInheritedReferencingMixin.getGlobalMixins());
            referencingMixins.add(optInheritedReferencingMixin);
         }
         for (DCModelBase mixin : referencingMixins) {
            if (mixin.getName().endsWith("_ref_" + mixin.getVersion())) { // TODO lifecycle
               referencingMixin.addMixin(mixin);
            }
         }
      }
      
      // add embedded & copied fields :
      for (String embeddedFieldName : embeddedFieldNames) {
         referencingMixin.addField(model.getGlobalField(embeddedFieldName));
      }
      
      // add actual resource reference field (if not yet in an inheriting mixin) :
      DCField existingField = referencingMixin.getGlobalField(modelName);
      if (existingField == null
            || !modelNameWithVersionIfAny.equals(existingField.getType())) {
         referencingMixin.addField(new DCResourceField(modelName, modelNameWithVersionIfAny, true, 100));
      }
      return referencingMixin;
   }
   
}
