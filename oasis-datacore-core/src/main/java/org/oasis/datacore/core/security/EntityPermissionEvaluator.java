package org.oasis.datacore.core.security;

import java.io.Serializable;
import java.util.Set;

import org.oasis.datacore.core.entity.EntityModelService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.ModelNotFoundException;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.security.service.DatacoreSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;


/**
 * Doc about how impl :
 * http://www.disasterarea.co.uk/blog/protecting-service-methods-with-spring-security-annotations/
 * 
 * Secures (only) DCEntity objects
 * 
 * Must NOT depend on EntityService, else circular dependency / cycle / loop at Spring init,
 * see details on EntityModelService.
 * 
 * @author mdutoo
 *
 */
public class EntityPermissionEvaluator implements PermissionEvaluator {

   @Autowired
   @Qualifier("datacoreSecurityServiceImpl")
   private DatacoreSecurityService datacoreSecurityService;

   @Autowired
   private DataModelServiceImpl projectService;
   
   @Autowired
   private EntityModelService entityModelService;

   
   /**
    * targetDomainObject must be DCEntity (or null if not found)
    * permission ca be : read, write, create, changeRights TODO N?
    */
   @Override
   public boolean hasPermission(Authentication authentication,
         Object targetDomainObject, Object permission) {
      if (targetDomainObject == null) {
         // happens when not found
         return true;
      }
      if (!(targetDomainObject instanceof DCEntity)) {
         throw new RuntimeException("Should only be called on DCEntity objects "
               + "but is " + targetDomainObject); //TODO better
      }
      DCEntity dataEntity = (DCEntity) targetDomainObject; // 
      
      if (!(permission instanceof String)) {
         throw new RuntimeException("Permission should be a String but is "
               + permission); //TODO better
      }
      String permissionName = (String) permission; // TODO or toString() ??
      
      
      // check (model type) admin :
      // (and check it only in @PreAuthorize if does not call hasPermission,
      // because this way it's not copied and never forgotten)
      
      //if (hasRole("admin") || hasRole("t_" + model.getName() + "_admin")) {
      DCUserImpl user = datacoreSecurityService.getCurrentUser();
      if (user.isAdmin()) {
         return true;
      }
      
      DCModelBase model = entityModelService.getModel(dataEntity);
      if (model == null) {
         // TODO LATER better : put it in data health / governance inbox, through event
         throw new RuntimeException("Error when checking rights of entity " + dataEntity.getUri(),
               new ModelNotFoundException(dataEntity.getModelName(), projectService.getProject(),
                     "When getting storage, can't find (instanciable) model type for entity, "
                     + "it's probably obsolete i.e. its model (and inherited mixins) "
                     + "has changed since (only in test) : " + dataEntity.getUri()));
      }

      if (model.getSecurity().isResourceAdmin(user)) {
         return true;
      }
      
      
      // check detailed permission :
      switch (permissionName) {
      case "read" :
         // to be used in @PostAuthorize on GET
         // NB. allReaders have to contain ALSO writers & owners (i.e. be precomputed)
         // to allow efficient query filtering by rights criteria
         // => TODO that in PermissionAdminApi/Service
         return model.getSecurity().isGuestReadable()
               || model.getSecurity().isAuthentifiedReadable() && !user.isGuest()
               || model.getSecurity().isResourceReader(user)
               || hasAnyEntityAclGroup(user, dataEntity.getAllReaders());
      case "write" :
         // to be used in @PreAuthorize on update ??
         // NB. writers have NOT to contain also writers & owners (i.e. be precomputed)
         // because no mass write operations (yet)
         return model.getSecurity().isAuthentifiedWritable() && !user.isGuest()
               || model.getSecurity().isResourceWriter(user)
               || hasAnyEntityAclGroup(user, dataEntity.getWriters())
               || hasAnyEntityAclGroup(user, dataEntity.getOwners());
         
      case "create" :
         // to be used in @PreAuthorize on update ????
         ///return entityService.getModel(dataEntity).getSecurity().hasCreator(user); /// TODO or this ?
         return model.getSecurity().isAuthentifiedCreatable() && !user.isGuest()
               || model.getSecurity().isResourceCreator(user);
         
      case "changeRights" :
         // to be used in @PostAuthorize on GET
         // NB. readers have to contain ALSO writers & owners (i.e. be precomputed)
         // to allow efficient query filtering by rights criteria
         // => TODO that in PermissionAdminApi/Service
         return hasAnyEntityAclGroup(user, dataEntity.getOwners()); // NB. no public owners !!
      
      case "getRights" :
    	 return hasAnyEntityAclGroup(user, dataEntity.getOwners()); 
    	 
      // NB. (model type) admin has already been done (first thing even)
         
      default :
         throw new RuntimeException("Unknown permission " + permissionName); //TODO better
      }
   }

   /**
    * TODO reuse SecurityExpressionRoot.has(Any)Role code ??
    * @param user 
    * @param entityAclGroups
    * @return
    */
   private boolean hasAnyEntityAclGroup(DCUserImpl user, Set<String> entityAclGroups) {
      if (entityAclGroups == null || entityAclGroups.isEmpty()) { // TODO null should not happen ?!?
         return false;
      }
      
      Set<String> currentUserEntityGroups = user.getEntityGroups();
      // TODO replace by GrantedAuth + GUEST
      // TODO BEWARE currentUserEntityGroups should only contain "actual" groups, NOT groups controlling
      // access to features ex. (model type) admin, kernel api ex. log, app feature-specific group

      for (String entityAclGroup : entityAclGroups) {
         // TODO to be more efficient (or secure !), avoid non-entity ((model) admin...) groups ?
         if (currentUserEntityGroups.contains(entityAclGroup)) { // new SimpleGrantedAuthority(readerGroup)
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean hasPermission(Authentication authentication,
         Serializable targetId, String targetType, Object permission) {
      throw new RuntimeException("Id and Class permissions are not supperted by this application "
            + "because DCEntity data entity object is anyway required to get ACLs");
      // TODO save for create & admin ??
   }

}
