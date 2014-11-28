package org.oasis.datacore.core.entity;

import java.util.List;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.ModelNotFoundException;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Provides helpers on Entities
 * - to manage their models, by managing their request-scoped caching on each entity
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
   
   @Autowired
   private DCModelService modelService;

   @Autowired
   private DataModelServiceImpl projectService;
   
   /**
    * (instanciable model)
    * Helper using entity cached transient model (mainainted over the course
    * of a request only) if available, else retrieving & setting it
    * (not sync'd nor threaded because within single request thread)
    * @throws IllegalArgumentException if unknown model type (first of types)
    * @param dataEntity
    * @return
    */
   public DCModelBase getModel(DCEntity dataEntity) {
      checkDataEntityCaches(dataEntity);
      return dataEntity.getCachedModel();
   }
   public DCModelBase getInstanciableModel(DCEntity dataEntity) {
      return getModel(dataEntity);
   }

   public DCModelBase getStorageModel(DCEntity dataEntity) {
      checkDataEntityCaches(dataEntity);
      return dataEntity.getCachedStorageModel();
   }

   public DCModelBase getDefinitionModel(DCEntity dataEntity) {
      checkDataEntityCaches(dataEntity);
      return dataEntity.getCachedDefinitionModel();
   }
   
   /**
    * Checks and fills Models cache in given Entity
    * @param dataEntity
    */
   private void checkDataEntityCaches(DCEntity dataEntity) {
      if (dataEntity.getCachedModel() != null) {
         return;
      }
      DCModelBase cachedModel = modelService.getModelBase(this.getModelName(dataEntity));
      if (cachedModel == null) { // = cachedInstanciableModel
         throw new IllegalArgumentException("DCEntity should have a valid (instance) model type"); // TODO custom ex ?
      }
      if (!cachedModel.isInstanciable()) { // = cachedInstanciableModel
         throw new IllegalArgumentException("DCEntity model type should be instanciable"); // TODO custom ex ?
      }
      DCModelBase cachedStorageModel = projectService.getStorageModel(cachedModel.getName());
      if (cachedStorageModel == null) {
         throw new IllegalArgumentException("DCEntity should have a valid storage model type"); // TODO custom ex ?
      }
      DCModelBase cachedDefinitionModel = projectService.getDefinitionModel(cachedModel.getName());
      if (cachedDefinitionModel == null) {
         throw new IllegalArgumentException("DCEntity should have a valid definition model type"); // TODO custom ex ?
      }
      dataEntity.setCachedModel(cachedModel);
      dataEntity.setCachedStorageModel(cachedModel);
      dataEntity.setCachedDefinitionModel(cachedModel);
   }

   public String getModelName(DCEntity dataEntity) {
      List<String> types = dataEntity.getTypes();
      if (types != null && !types.isEmpty()) {
         return types.get(0);
      }
      return null;
   }

   public String getCollectionName(DCModelBase model) throws ModelNotFoundException {
      DCModelBase storageModel = modelService.getStorageModel(model.getName());
      if (storageModel == null) {
         throw new ModelNotFoundException(model, modelService.getProject(model.getName()),
               "can't find storage model of model type");
      }
      return storageModel.getCollectionName();
   }
   
   public String getCollectionName(DCEntity dataEntity) throws ModelNotFoundException {
      DCProject project = projectService.getProject(); // NB. can't be null ; TODO add method with param
      DCModelBase model = getModel(dataEntity);
      if (model == null) {
         throw new ModelNotFoundException(dataEntity.getModelName(), project,
               "for entity with uri " + dataEntity.getUri());
      }
      return getCollectionName(model);
   }
   
}
