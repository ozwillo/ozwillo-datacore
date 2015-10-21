package org.oasis.datacore.core.entity;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.model.DCEntityBase;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.ModelException;
import org.oasis.datacore.core.meta.ModelNotFoundException;
import org.oasis.datacore.core.meta.SimpleUriService;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;


/**
 * Provides helpers on Entities
 * - to manage their models, by managing their request-scoped caching on each entity
 * (TODO LATER better archi & impl)
 * - and that have been taken out of EntityService
 * else there is a circular dependency at Spring context initialization because
 * EntityPermissionEvaluator relies on them (instead of relying on external ACL
 * storage, such as JDBC for default AclPermissionEvaluator) :
 * 
 * (ldpEntityQueryServiceImpl to) mongoTemplate to mongoDbFactory to inner through
 * Infrastructure... to org.springframework.cache.config.internalCacheAdvisor
 * to org.springframework.cache.annotation.AnnotationCacheOperationSource#0
 * through InfrastructureAdvisorAutoProxyCreator.getAdvicesAndAdvisorsForBean()
 * to org.springframework.security.methodSecurityMetadataSourceAdvisor
 * to DelegatingMethodSecurityMetadataSource to inners to entityExpressionHandler
 * to EntityPermissionEvaluator to DCEntityService/Impl
 * 
 * which triggers silent (!) BeanCreationException-wrapped BeanCurrentlyInCreationException
 * on mongoTemplate.
 * 
 * @author mdutoo
 *
 */
@Component
public class EntityModelService {
   
   private static final Logger logger = LoggerFactory.getLogger(EntityModelService.class);
   
   @Autowired
   private DCModelService modelService;

   @Autowired
   private DataModelServiceImpl projectService;

   /** for index testing ON MODELS ONLY in ResourceModelTest, else index is always _b_1__uri_1 */
   private boolean disableMultiProjectStorageCriteriaForTesting = false;
      
   /** shortcut, to avoid having to inject also ModelService in EntityService */
   public DCProject getProject() {
      return modelService.getProject();
   }
   /**
    * Handles case of multi project storage model
    * TODO store entity project in it ?? 
    * @param dataEntity
    * @param model to be used if null dataEntity
    * @return
    */
   public DCProject getProject(DCEntityBase dataEntity, DCModelBase model) {
      if (dataEntity == null || dataEntity.getProjectName() == null) { // none yet (been queried by LDP)
         return modelService.getProject(model.getProjectName());
      }
      DCModelBase storageModel = this.getStorageModel(dataEntity);
      if (storageModel != null && storageModel.isMultiProjectStorage()) { // especially oasis.meta.dcmi:mixin_0 in case of model resources !
         // (even oasis.sandbox models are stored in oasis.meta.dcmi:mixin_0 collection,
         // only soft forks i.e. inheriting overrides couldn't be handled this way)
         // no need to check, entity will anyway be written in its own project
         return modelService.getProject(dataEntity.getProjectName()); // entity project
      }
      return modelService.getProject(model.getProjectName());
   }
   
   /**
    * (instanciable model)
    * Helper using entity cached transient model (maintained over the course
    * of a request only) if available, else retrieving & setting it
    * (not sync'd nor threaded because within single request thread).
    * Might not be there (or not instanciable) if obsolete dataEntity (i.e. its model has changed, only in test),
    * so check it or use a method that does it ex. getCollectionName()
    * @param dataEntity
    * @return
    */
   public DCModelBase getModel(DCEntityBase dataEntity) {
      checkAndFillDataEntityCaches(dataEntity);
      return dataEntity.getCachedModel();
   }
   /** Might not be there (or not instanciable) if obsolete dataEntity (i.e. its model has changed, only in test),
    * so check it or use a method that does it ex. getCollectionName() */
   public DCModelBase getInstanciableModel(DCEntity dataEntity) {
      return getModel(dataEntity);
   }

   /** Might not be there if obsolete dataEntity (i.e. its model has changed), so check it
    * or use a method that does it ex. getCollectionName() */
   public DCModelBase getStorageModel(DCEntityBase dataEntity) {
      checkAndFillDataEntityCaches(dataEntity);
      return dataEntity.getCachedStorageModel();
   }

   /** Might not be there if obsolete dataEntity (i.e. its model has changed), so check it */
   public DCModelBase getDefinitionModel(DCEntity dataEntity) {
      checkAndFillDataEntityCaches(dataEntity);
      return dataEntity.getCachedDefinitionModel();
   }
   
   /**
    * Fills Models cache in given Entity and checks them. 
    * Indeed, if they are not there (or model type not instanciable) ex. if obsolete dataEntity
    * (i.e. its model has changed, only in test), it would anyway explode
    * later in ResourceEntityMapperService.entityToResource().
    * TODO LATER better : put such cases in data health / governance inbox, through event
    * @param dataEntity
    */
   public void checkAndFillDataEntityCaches(DCEntityBase dataEntity) {
      if (dataEntity.getCachedModel() != null) {
         return;
      }
      DCModelBase cachedModel = modelService.getModelBase(this.getModelName(dataEntity));
      if (cachedModel == null) { // = cachedInstanciableModel
         throw new IllegalArgumentException("Can't find model for DCEntity, "
               + "it's probably obsolete i.e. its model has changed since (only in test) : "
               + dataEntity.getUri()); // NB. if no exception will NullPointerException anyway
         // later in ResourceEntityMapperService.entityToResource() (so must rather be cleaned
         // by init or backgound job, and models even prevented to be deleted, and possibly
         // resources as well until is is sure they are not linked to anymore)
      }
      /*if (!cachedModel.isInstanciable()) { // = cachedInstanciableModel
         throw new IllegalArgumentException("DCEntity model type is not instanciable, "
               + "it's probably obsolete i.e. its model has changed since (only in test) : "
               + dataEntity.getUri()); // TODO custom ex ?
      }*/
      DCModelBase cachedStorageModel = projectService.getStorageModel(cachedModel);
      DCModelBase cachedDefinitionModel = projectService.getDefinitionModel(cachedModel);
      /*if (cachedStorageModel == null) {
         throw new IllegalArgumentException("Can't find storage (model) for DCEntity, "
               + "it's probably obsolete i.e. its model has changed since (only in test) : "
               + dataEntity.getUri()); // TODO custom ex ?
      }
      if (cachedDefinitionModel == null) {
         throw new IllegalArgumentException("Can't find definition (model) for DCEntity, "
               + "it's probably obsolete i.e. its model has changed since (only in test) : "
               + dataEntity.getUri()); // TODO custom ex ?
      }*/
      fillDataEntityCaches(dataEntity, cachedModel, cachedStorageModel, cachedDefinitionModel);
   }
   /** to be called on newly built / retrieved dataEntity */
   public void fillDataEntityCaches(DCEntityBase dataEntity,
         DCModelBase model, DCModelBase storageModel, DCModelBase definitionModel) {
      dataEntity.setCachedModel(model); // = cachedInstanciableModel
      dataEntity.setCachedStorageModel(storageModel);
      dataEntity.setCachedDefinitionModel(definitionModel);
   }

   public String getModelName(DCEntityBase dataEntity) {
      List<String> types = dataEntity.getTypes();
      if (types != null && !types.isEmpty()) {
         return types.get(0);
      }
      return null;
   }

   /**
    * 
    * @param model
    * @return not null
    * @throws ModelNotFoundException if storage model not found
    * TODO LATER better : put it in data health / governance inbox, through event
    */
   public DCModelBase getStorageModel(DCModelBase model) throws ModelNotFoundException {
      DCModelBase storageModel = modelService.getStorageModel(model);
      if (storageModel == null) {
         // TODO LATER better : put it in data health / governance inbox, through event
         throw new ModelNotFoundException(model, modelService.getProject(),
               "Can't find storage model of model, meaning it's a true (definition) mixin. "
               + "Maybe it had one at some point and this model (and its inherited mixins) "
               + "has changed since (only in test, in which case the missing model "
               + "must first be created again before patching the entity).");
      }
      
      if (!storageModel.isMultiProjectStorage()) { // NOO rather done in project.getModel() (?)
         // check if not forked :
         LinkedHashSet<String> forkedUriInvisibleProjectNames =
               getForkedUriInvisibleProjectNames(SimpleUriService.buildModelUri(model.getName()));
         if (forkedUriInvisibleProjectNames != null) {
               ///&& !namesOfProjectsForkingUri.contains(storageModel.getProjectName())) { // else stored in forking project (most common case)
            // (unless uri is visible through another visible project than projects that fork it,
            // but then it would be an inconsistent configuration)
            if (forkedUriInvisibleProjectNames.contains(storageModel.getProjectName())) {
               // TODO TODO TODOOOO do we still get there since project.getModel() checks forked URI ?
               throw new ModelNotFoundException(model, modelService.getProject(),
                     "Can't find storage model of model in visible models of projects "
                     + "(though it is in visible projects but among those where "
                     + "its URI has been made invisible by forking it above : "
                     + forkedUriInvisibleProjectNames);
            }
         }
      } // else must be checked using entity criteria
      
      return storageModel;
   }
   
   /**
    * logs if model not instanciable.
    * TODO LATER better : put it in data health / governance inbox, through event
    * @param dataEntity
    * @return
    * @throws ModelNotFoundException if model or storage not found (same remark) or not instanciable
    */
   public String getCollectionName(DCEntity dataEntity) throws ModelNotFoundException {
      DCModelBase model = getModel(dataEntity);
      if (model == null) {
         // TODO LATER better : put it in data health / governance inbox, through event
         throw new ModelNotFoundException(this.getModelName(dataEntity), projectService.getProject(), // NB. project can't be null ; TODO add method with param
               "When getting storage, can't find (instanciable) model type for entity, "
               + "it's probably obsolete i.e. its model (and inherited mixins) "
               + "has changed since (only in test, in which case the missing model "
               + "must first be created again before patching the entity) : " + dataEntity.getUri());
      }
      if (!model.isInstanciable()) {
         String msg = "When getting storage, entity model type is not instanciable, "
               + "it's probably obsolete i.e. its model has changed since (only in test, in which case "
               + "the missing model must first be created again before patching the entity) : "
               + dataEntity.getUri();
         logger.debug("Error when getting entity storage",
               new ModelException(model.getName(), projectService.getProject(), msg)); // NB. project can't be null ; TODO add method with param
         // TODO LATER better : put it in data health / governance inbox, through event
         //throw new ModelException(dataEntity.getModelName(), project, msg);
      }
      return getCollectionName(model);
   }
   public String getCollectionName(DCModelBase model) throws ModelNotFoundException {
      if (!model.isInstanciable()) {
         String msg = "When getting storage, entity model type is not instanciable, "
               + "it's probably obsolete i.e. its model has changed since (only in test, in which case "
               + "the missing model must first be created again before patching the entity)";
         logger.debug("Error when getting entity storage",
               new ModelException(model.getName(), projectService.getProject(), msg)); // NB. project can't be null ; TODO add method with param
         // TODO LATER better : put it in data health / governance inbox, through event
         //throw new ModelException(dataEntity.getModelName(), project, msg);
      }
      return getStorageModel(model).getCollectionName();
   }

   public LinkedHashSet<String> getForkedUriInvisibleProjectNames(String forkedUri) {
      LinkedHashSet<String> forkedUriProjectNames = modelService.getForkedUriProjectNames(forkedUri);
      if (forkedUriProjectNames == null) {
            ///|| namesOfProjectsForkingUri.contains(storageModel.getProjectName())) { // else stored in forking project (most common case)
         return null; // none
      }
      
      LinkedHashSet<String> visibleProjectNames = modelService.toNames(
            modelService.getVisibleProjects(modelService.getProject()));
      // only retain leaf-most visible project forking this uri
      // (if any, else if none visible result is not impacted)
      // therefore starting from last forkedUriProject :
      String[] forkedUriProjectNameArray = forkedUriProjectNames.toArray(new String[forkedUriProjectNames.size()]);
      for (int i = forkedUriProjectNameArray.length - 1; i >= 0; i--) { // starting from last
         String forkedUriProjectName = forkedUriProjectNameArray[i];
         if (visibleProjectNames.contains(forkedUriProjectName)) {
            LinkedHashSet<String> forkedUriInvisibleProjectNames = new LinkedHashSet<String>(visibleProjectNames);
            forkedUriInvisibleProjectNames.remove(forkedUriProjectName); // removing the only visible one (current) FOR THIS URI
            // (unless uri is visible through another visible project than projects that fork it,
            // but then it would be an inconsistent configuration)
            return forkedUriInvisibleProjectNames;
         }
      }
      // no visible project forking this uri, therefore making no project invisible
      return null;
   }

   public LinkedHashSet<String> getVisibleProjectNames(String uri) {
      LinkedHashSet<String> visibleProjectNames = modelService.toNames(
            modelService.getVisibleProjects(modelService.getProject()));
      if (uri == null) { // LDP
         return visibleProjectNames;
      }
      
      LinkedHashSet<String> forkedUriInvisibleProjectNames = getForkedUriInvisibleProjectNames(uri);
      if (forkedUriInvisibleProjectNames != null) {
         return new LinkedHashSet<String>(visibleProjectNames.stream()
               .filter(n -> !forkedUriInvisibleProjectNames.contains(n))
               .collect(Collectors.toList()));
      }
      return visibleProjectNames;
   }
   
   /** shared between entity and LDP query services */
   public void addMultiProjectStorageCriteria(Criteria criteria, DCModelBase storageModel, String uri) {
      // TODO handle uri null i.e. LDP query case, using $first aggregation on _uri sorted by _b
      if (storageModel.isMultiProjectStorage() && !disableMultiProjectStorageCriteriaForTesting) {
         criteria.and(DCEntity.KEY_B).in(this.getVisibleProjectNames(uri));
      } // (else forkedUris would have been handled in project.get(Storage)Model())
   }

   public boolean isDisableMultiProjectStorageCriteriaForTesting() {
      return disableMultiProjectStorageCriteriaForTesting;
   }
   public void setDisableMultiProjectStorageCriteriaForTesting(boolean disableMultiProjectStorageCriteriaForTesting) {
      this.disableMultiProjectStorageCriteriaForTesting = disableMultiProjectStorageCriteriaForTesting;
   }
   
}
