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
import java.util.LinkedHashSet;
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
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.core.meta.model.DCSecurity;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.historization.service.impl.HistorizationServiceImpl;
import org.oasis.datacore.model.resource.ModelResourceMappingService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.server.DatacoreApiImpl;
import org.oasis.datacore.rest.server.event.EventService;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.resource.ResourceEntityMapperService;
import org.oasis.datacore.rest.server.resource.ResourceException;
import org.oasis.datacore.rest.server.resource.ResourceNotFoundException;
import org.oasis.datacore.rest.server.resource.ResourceService;
import org.oasis.datacore.server.uri.UriService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

   /** impl, to be able to modify it
    * TODO LATER extract interface */
   @Autowired
   protected DataModelServiceImpl modelAdminService;
   @Autowired
   protected NativeModelService nativeModelService;
   
   /*@Autowired NOO CXF server not yet inited, rather use ResourceService or datacoreApiImpl
   @Qualifier("datacoreApiCachedJsonClient")
   protected /DatacoreApi/DatacoreCachedClient datacoreApiClient;*/
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

   /** for forking */
   @Autowired
   private ModelResourceMappingService mrMappingService;
   
   
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
      // NB. logged in as admin by Initable above
      try {
         doInitInternal();

      } catch (WebApplicationException waex) {
         logger.error("HTTP " + waex.getResponse().getStatus()
               + " web app error initing " + this.getClass().getName() + ":\n"
               + waex.getResponse().getStringHeaders()
               + ((waex.getResponse().getEntity() != null) ? "\n" + waex.getResponse().getEntity() + "\n" : ""), waex);
               
      } // NB. other exceptions are caught by InitService above
   }

   protected void doInitInternal() {
      List<DCModelBase> modelsToCreate = new ArrayList<DCModelBase>();
      buildModels(modelsToCreate);
      boolean allCollectionsAlreadyExist = createModels(modelsToCreate, false);
      
      if (/*enableFillDataAtInit() && */(!allCollectionsAlreadyExist
            || alwaysFillData() || hasSomeModelsWithoutResource())) { // sample possibly incomplete
         if (!neverCleanData()) {
            cleanDataOfCreatedModels(modelsToCreate);
            // NB. data only samples (i.e. no modelsToCreate) uses dependent sample models
         } // else guard
         fillData(); // always fill sample data if any
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
   /** ex. for metamodel, in order to refill all models' resources */
   protected boolean alwaysFillData() {
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

   /** overriden for samples */
   @Override
   protected DCProject getProject() {
      return getSampleProject();
   }
   
   /**
    * To be called by tests only if they require to init all data explicitly,
    * and even if there already are there (cleans first) ;
    * otherwise, do cleanDataOfCreatedModels(someModels) then createSomeData()
    */
   public void initData() {
      login();
      try {
         DCProject project = getProject();
         if (project != null && !project.getName().equals(modelAdminService.getProject().getName())) { // not current
            new SimpleRequestContextProvider<Object>() { // set context project beforehands :
               protected Object executeInternal() throws ResourceException {
                  cleanDataOfCreatedModels();
                  fillData();
                  return null;
               }
            }.execInContext(new ImmutableMap.Builder<String, Object>()
                  .put(DCRequestContextProvider.PROJECT, project.getName()).build());
         } else {
            cleanDataOfCreatedModels();
            fillData();
         }
      } finally {
         logout();
      }
   }
   public void flushData() {
      login();
      try {
         DCProject project = getProject();
         if (project != null && !project.getName().equals(modelAdminService.getProject().getName())) { // not current
            new SimpleRequestContextProvider<Object>() { // set context project beforehands :
               protected DCResource executeInternal() throws ResourceException {
                  cleanDataOfCreatedModels();
                  return null;
               }
            }.execInContext(new ImmutableMap.Builder<String, Object>()
                  .put(DCRequestContextProvider.PROJECT, project.getName()).build());
         } else {
            cleanDataOfCreatedModels();
         }
      } finally {
         logout();
      }
   }

   /**
    * To be called by tests only if they require to init some models explictly.
    */
   public void initModels() {
      login();
      try {
         DCProject project = getProject();
         if (project != null && !project.getName().equals(modelAdminService.getProject().getName())) { // not current
            new SimpleRequestContextProvider<Object>() { // set context project beforehands :
               protected DCResource executeInternal() throws ResourceException {
                  List<DCModelBase> modelsToCreate = new ArrayList<DCModelBase>();
                  buildModels(modelsToCreate);
                  for (DCModelBase model : modelsToCreate) {
                     // NB. NOT this.models but new ones else might have been modified in previous initData()
                     createModel(model, true);
                  }
                  return null;
               }
            }.execInContext(new ImmutableMap.Builder<String, Object>()
                  .put(DCRequestContextProvider.PROJECT, project.getName()).build());
         } else {
            List<DCModelBase> modelsToCreate = new ArrayList<DCModelBase>();
            buildModels(modelsToCreate);
            for (DCModelBase model : modelsToCreate) {
               // NB. NOT this.models but new ones else might have been modified in previous initData()
               createModel(model, true);
            }
         }
      } finally {
         logout();
      }
   }
   public void initModels(DCModelBase ... modelsToCreate) {
      initModels(Arrays.asList(modelsToCreate));
   }
   public void initModels(List<DCModelBase> modelsToCreate) {
      login();
      try {
         DCProject project = getProject();
         if (project != null && !project.getName().equals(modelAdminService.getProject().getName())) { // not current
            new SimpleRequestContextProvider<Object>() { // set context project beforehands :
               protected DCResource executeInternal() throws ResourceException {
                  createModels(modelsToCreate, true);
                  return null;
               }
            }.execInContext(new ImmutableMap.Builder<String, Object>()
                  .put(DCRequestContextProvider.PROJECT, project.getName()).build());
         } else {
            createModels(modelsToCreate, true);
         }
      } finally {
         logout();
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
         DCModelBase existingModel = modelAdminService.getModelBase(model.getName());
         storageModels.remove(getCreatedModel(model)); // even if !deleteCollectionsFirst !!!
         if (deleteCollectionsFirst && existingModel != null && existingModel.isStorage()) {
            if (!existingModel.isMultiProjectStorage()) {
               mgo.dropCollection(existingModel.getCollectionName());
            }
         }
         
         // adding model
         modelAdminService.addModel(model);
         storageModels.add(model);
         
         return databaseSetupService.ensureCollectionAndIndices(model, deleteCollectionsFirst); // applied on its storage
         
      } else { // mixin
         modelAdminService.addModel(model);
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
    * uses resourceService
    * Requires to have logged in first
    * @return existing if any, else created
    */
   public DCResource postDataInTypeIfNotExists(DCResource resource) throws RuntimeException {
      try {
         return resourceService.get(resource.getUri(), resource.getModelType());
         /*if (refreshBeforePost()) {
            resource.setVersion(existing.getVersion());
         }*/// TODO NOO strictPostMode

      } catch (ResourceNotFoundException ex) {
         // allow creation of new resource
      } catch (RuntimeException rex) {
         throw rex;
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
      return /*datacoreApiClient.*/postDataInType(resource);
   }
   public DCResource getData(DCResource resource) throws RuntimeException {
      try {
         return resourceService.get(resource.getUri(), resource.getModelType());
      } catch (ResourceException rex) {
         throw new RuntimeException(rex);
      }
   }
   public DCResource getData(DCResource resource, String projectName) throws RuntimeException {
      return new SimpleRequestContextProvider<DCResource>() { // set context project beforehands :
         protected DCResource executeInternal() {
            return getData(resource);
         }
      }.execInContext(new ImmutableMap.Builder<String, Object>()
            .put(DCRequestContextProvider.PROJECT, projectName).build());
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
   /**
    * uses resourceService
    * Requires to have logged in first
    */
   public DCResource postDataInTypeIfNotExists(DCResource resource, String projectName) {
      return new SimpleRequestContextProvider<DCResource>() { // set context project beforehands :
         protected DCResource executeInternal() throws ResourceException {
            return postDataInTypeIfNotExists(resource);
         }
      }.execInContext(new ImmutableMap.Builder<String, Object>()
            .put(DCRequestContextProvider.PROJECT, projectName).build());
   }

   
   protected final DCProject getSampleProject() {
      DCProject project = modelAdminService.getProject(DCProject.OASIS_SAMPLE);
      if (project == null) {
         project = new DCProject(DCProject.OASIS_SAMPLE);
         project.setModelLevelSecurityEnabled(true); // let unit tests tweak model-level security as necessary
         modelAdminService.addProject(project);
         ///dataForkMetamodelIn(project); // NO still store them all in oasis.meta, TODO LATER add projectName in their URI
         project.addLocalVisibleProject(getMetamodelProject()); // ... so for now rather this
      }
      return project;
   }
   
   protected final DCProject getMainProject() {
      DCProject project = modelAdminService.getProject(DCProject.OASIS_MAIN);
      if (project == null) {
         project = buildProjectDefaultConf(DCProject.OASIS_MAIN,
               "(facade) Makes visible all published projects", ozwilloMainAdmins);
         // TODO TODO add ALL other projects ; & should not have any local models
      }
      return project;
   }
   
   /**
    * i.e. main's sandbox
    * @return
    */
   protected final DCProject getSandboxProject() { // TODO use it in playground
      DCProject project = modelAdminService.getProject(DCProject.OASIS_SANBOX);
      if (project == null) {
         project = new DCProject(DCProject.OASIS_SANBOX);
         project.setModelLevelSecurityEnabled(true); // to each sandbox user its own models
         modelAdminService.addProject(project);
         // NB. no need to fork metamodel, sandbox custom models will be stored
         // in multiProjectStorage oasis.meta.dcmi:mixin_0 collection
         // and their resources in oasis.sandbox.* collections
         // (not even hard forks i.e. copies, only soft forks i.e. inheriting overrides would be a problem)
         ///dataForkMetamodelIn(project); // TODO LATER still store them also in oasis.meta with projectName in their URI
         project.addLocalVisibleProject(getMainProject());
         project.addLocalVisibleProject(getSampleProject());
      }
      return project;
   }
   
   
   ///////////////////////////////////////////////////
   // PROJECT CREATION HELPERS

   /**
    * Adds oasis.main
    * @param unversionedName
    * @param majorVersion 0, 1...
    * @param doc
    * @param owners default resourceOwners, therefore also of model resources
    * @param visibleProjects (oasis.meta is implicitly added)
    * @return
    */
   public DCProject buildContainerVersionedProjectDefaultConf(
         String unversionedName, long majorVersion,
         String doc, LinkedHashSet<String> owners,
         DCProject ... visibleProjects) {
      return buildProjectDefaultConf(unversionedName + '_' + majorVersion, // geo_1 // NB. in geo_1.0, 0 would be minorVersion
            doc, owners, visibleProjects);
      // NB. versioned projects aren't in main, rather their facade, to allow transparent migration
   }

   /**
    * Adds to oasis.main
    * @param containerVersionedProject
    * @return
    */
   public DCProject buildFacadeProjectDefaultConf(DCProject containerVersionedProject) {
      if (containerVersionedProject.getMajorVersion() < 0) {
         throw new RuntimeException("Not a versioned project " + containerVersionedProject);
      }
      DCProject orgProject = new DCProject(containerVersionedProject.getUnversionedName());
      orgProject.setDocumentation("(Unversioned facade) " + containerVersionedProject.getDocumentation());
      orgProject.addLocalVisibleProject(containerVersionedProject);
      
      // override rights policy (though projects not restored from persistence for now) :
      // HOWEVER SINCE THERE ARE NO LOCAL MODELS, MOST OF THIS IS USELESS
      // TODO LATER better : prevent from writing locally model resources (dcmo:model_0 modelType)
      setDefaultGlobalPublicSecurity(orgProject, containerVersionedProject.getSecurityDefaults().getResourceOwners());
      // XXX NB. PermissionEvaluator hack allows to write in visible container project, TODO better here
      
      modelAdminService.addProject(orgProject);
      
      getMainProject().addLocalVisibleProject(orgProject); // because facade, allows transparent migration
      return orgProject;
   }

   /**
    * Adds meta
    * @param unversionedName
    * @param majorVersion 0, 1...
    * @param doc
    * @param owners default resourceOwners, therefore also of model resources
    * @param visibleProjects (oasis.meta is implicitly added)
    * @return
    */
   public DCProject buildProjectDefaultConf(
         String nameWithVersionIfAny,
         String doc, LinkedHashSet<String> owners,
         DCProject ... visibleProjects) {
      DCProject org1Project = new DCProject(nameWithVersionIfAny); // geo_1 // NB. in geo_1.0, 0 would be minorVersion
      org1Project.setDocumentation(doc);
      org1Project.addLocalVisibleProject(getMetamodelProject()); // else metamodel not visible
      // TODO prevent local models
      
      for (DCProject visibleProject : visibleProjects) {
         org1Project.addLocalVisibleProject(visibleProject); // geo // ! could be rather geo1, but then could not be visible in org !
      }

      // override rights policy (though projects not restored from persistence for now) :
      setDefaultGlobalPublicSecurity(org1Project, owners);
      
      modelAdminService.addProject(org1Project);
      // NB. versioned projects aren't in main, rather their facade, to allow transparent migration
      return org1Project;
   }
   
   
   public void dataForkMetamodelIn(DCProject project) {
      dataForkLocalModels(getMetamodelProject(), project);
   }
   public void dataForkLocalModels(DCProject forkedProject, DCProject targetProject) {
      if (targetProject.getLocalVisibleProject(forkedProject.getName()) == null) {
         targetProject.addLocalVisibleProject(forkedProject);
      }
      for (DCModelBase model : forkedProject.getLocalModels()) {
         if (targetProject.getLocalModel(model.getName()) != null) {
            continue; // assume already forked, LATER support changes
         }
         // only fork storage models :
         // (because modelService/project.getStorageModel(model) uses getModel(parentName) to visit hierarchy)
         if (model.isStorage()) {
            this.fork(model, targetProject, false, false, true, false);
         }
      }
   }
   
   /**
    * NB. can't have several at once
    * @param model
    * @param forkModel hard fork i.e. copy
    * @param overrideModel
    * @param forkData i.e. fork modelStorage
    * @param readonlyData
    * @return
    * @throws RuntimeException : ResourceException if hard fork & fails
    */
   public DCModelBase fork(DCModelBase model, DCProject inProject, boolean forkModel, boolean overrideModel,
         boolean forkData, boolean readonlyData) throws RuntimeException {
      DCModelBase forkedModel = inProject.getLocalModel(model.getName());
      if (forkedModel != null) {
         throw new RuntimeException("Already forked " + model.getAbsoluteName()
               + " in project " + inProject.getName()
               + " to " + forkedModel.getAbsoluteName()); // TODO LATER allow changes
      }
      
      if (!forkModel && !overrideModel && !forkData && !readonlyData) {
         // i.e. fully visible : same model
         return model;
      }
      
      if (forkModel) { // and data
         DCResource mr;
         try {
            mr = mrMappingService.modelToResource(model, null);
            forkedModel = mrMappingService.toModelOrMixin(mr);
         } catch (ResourceParsingException | ResourceException e) {
            throw new RuntimeException("Error while hard forking model "
                  + model.getName() + " in project " + inProject.getName(), e);
         }
         
      } else {
         forkedModel = new DCModel(model.getName(), inProject.getName());
         forkedModel.addMixin(model);
         
         // TODO TODO make storage only if was storage, same instanciable !!
      
         if (overrideModel) { // and data
            forkedModel.setStorage(true);
            forkedModel.setInstanciable(true);
            forkedModel.setDocumentation("model fork");
         } else if (forkData) {
            // TODO TODO mark this model's resource as readonly in this project
            forkedModel.setStorage(true);
            forkedModel.setInstanciable(true);
            forkedModel.setDocumentation("data fork");
         } else if (readonlyData) {
            DCSecurity modelSecurity = modelAdminService.getSecurity(model);
            DCSecurity forkedSecurity = (modelSecurity == null) ? new DCSecurity() : new DCSecurity(modelSecurity);
            // TODO mark this model's resources as readonly from this project
            ///forkedSecurity.setResourceReadonly(true); // TODO
            forkedModel.setSecurity(forkedSecurity);
            forkedSecurity.setResourceCreationOwners(new LinkedHashSet<String>(0)); // NOT instanciable
            forkedModel.setInstanciable(false); // NOT instanciable (TODO ???????????????)
            forkedModel.setDocumentation("security fork");
         } // else already handled
      }
      
      inProject.addLocalModel(forkedModel);
      return forkedModel;
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
      DCMixin referencingMixin = (DCMixin) new DCMixin(refMixinNameRoot + "_ref" + mixinVersion);
      // NB. not filling projectName in case model's had not been set yet
      
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
            if (mixin.getName().endsWith("_ref_" + 0)) { // mixin.getMajorVersion() BUT not yet created ; TODO lifecycle
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
