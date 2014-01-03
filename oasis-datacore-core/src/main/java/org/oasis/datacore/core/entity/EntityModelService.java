package org.oasis.datacore.core.entity;

import java.util.List;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Provides helpers that have been taken out of EntityService
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

   /**
    * Helper using entity cached transient model (mainainted over the course
    * of a request only) if available, else retrieving & setting it
    * @throws IllegalArgumentException if unknown model type (first of types)
    * @param dataEntity
    * @return
    */
   public DCModel getModel(DCEntity dataEntity) {
      DCModel cachedModel = dataEntity.getCachedModel();
      if (cachedModel != null) {
         return cachedModel;
      }
      cachedModel = modelService.getModel(this.getModelName(dataEntity));
      if (cachedModel == null) {
         throw new IllegalArgumentException("DCEntity should have a valid model type"); // TODO custom ex ?
      }
      dataEntity.setCachedModel(cachedModel);
      return cachedModel;
   }
   
   public String getModelName(DCEntity dataEntity) {
      List<String> types = dataEntity.getTypes();
      if (types != null && !types.isEmpty()) {
         return types.get(0);
      }
      return null;
   }
   
}
