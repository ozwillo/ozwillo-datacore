package org.oasis.datacore.core.entity;

import java.util.List;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.ModelException;
import org.oasis.datacore.core.meta.ModelNotFoundException;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
   public DCModelBase getModel(DCEntity dataEntity) {
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
   public DCModelBase getStorageModel(DCEntity dataEntity) {
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
   public void checkAndFillDataEntityCaches(DCEntity dataEntity) {
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
      dataEntity.setCachedModel(cachedModel); // = cachedInstanciableModel
      dataEntity.setCachedStorageModel(cachedStorageModel);
      dataEntity.setCachedDefinitionModel(cachedDefinitionModel);
   }

   public String getModelName(DCEntity dataEntity) {
      List<String> types = dataEntity.getTypes();
      if (types != null && !types.isEmpty()) {
         return types.get(0);
      }
      return null;
   }

   /**
    * 
    * @param model
    * @return
    * @throws ModelNotFoundException if storage model not found
    * TODO LATER better : put it in data health / governance inbox, through event
    */
   public String getCollectionName(DCModelBase model) throws ModelNotFoundException {
      DCModelBase storageModel = modelService.getStorageModel(model);
      if (storageModel == null) {
         // TODO LATER better : put it in data health / governance inbox, through event
         throw new ModelNotFoundException(model, modelService.getProject(),
               "Can't find storage model of model, meaning it's a true (definition) mixin. "
               + "Maybe it had one at some point and this model (and its inherited mixins) "
               + "has changed since (only in test, in which case the missing model "
               + "must first be created again before patching the entity).");
      }
      return storageModel.getCollectionName();
   }
   
   /**
    * logs if model not instanciable.
    * TODO LATER better : put it in data health / governance inbox, through event
    * @param dataEntity
    * @return
    * @throws ModelNotFoundException if model or storage not found (same remark)
    */
   public String getCollectionName(DCEntity dataEntity) throws ModelNotFoundException {
      DCProject project = projectService.getProject(); // NB. can't be null ; TODO add method with param
      DCModelBase model = getModel(dataEntity);
      if (model == null) {
         // TODO LATER better : put it in data health / governance inbox, through event
         throw new ModelNotFoundException(this.getModelName(dataEntity), project,
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
               new ModelException(this.getModelName(dataEntity), project, msg));
         // TODO LATER better : put it in data health / governance inbox, through event
         //throw new ModelException(dataEntity.getModelName(), project, msg);
      }
      DCModelBase storageModel = modelService.getStorageModel(model);
      if (storageModel == null) {
         // TODO LATER better : put it in data health / governance inbox, through event
         throw new ModelNotFoundException(model, modelService.getProject(),
               "Can't find storage (model) for DCEntity, it's probably obsolete "
               + "i.e. its model (and inherited mixins) has changed since (only in test, in which case "
               + "the missing model must first be created again before patching the entity.) : "
               + dataEntity.getUri());
      }
      return storageModel.getCollectionName();
   }
   
}
