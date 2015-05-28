package org.oasis.datacore.core.security;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.oasis.datacore.core.context.DatacoreRequestContextService;
import org.oasis.datacore.core.entity.EntityModelService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.ModelNotFoundException;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCSecurity;
import org.oasis.datacore.core.security.service.DatacoreSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;




/**
 * Doc about how impl :
 * http://www.disasterarea.co.uk/blog/protecting-service-methods-with-spring-security-annotations/
 * 
 * Secures (only) DCEntity objects, also filters out on GET / query & in on POST/PUT
 * 
 * Must NOT depend on EntityService, else circular dependency / cycle / loop at Spring init,
 * see details on EntityModelService.
 * 
 * @author mdutoo
 *
 */
public class EntityPermissionEvaluator implements PermissionEvaluator {

   public static final String READ = "read";
   public static final String WRITE = "write";
   public static final String CREATE = "create";
   public static final String CHANGE_RIGHTS = "changeRights";
   public static final String GET_RIGHTS = "getRights";

   @Autowired
   @Qualifier("datacoreSecurityServiceImpl")
   private DatacoreSecurityService datacoreSecurityService;

   /** to get project & computed (inherited) DCSecurity */
   @Autowired
   private DataModelServiceImpl modelService;
   
   @Autowired
   private EntityModelService entityModelService;

   /** to know mixins view, putRatherThanPatchMode */
   @Autowired(required=false)
   protected DatacoreRequestContextService serverRequestContext = null;

   
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
      
      DCModelBase model = entityModelService.getModel(dataEntity);
      if (model == null) {
         // TODO LATER better : put it in data health / governance inbox, through event
         throw new RuntimeException("Error when checking rights of entity " + dataEntity.getUri(),
               new ModelNotFoundException(entityModelService.getModelName(dataEntity), modelService.getProject(),
                     "When getting storage, can't find (instanciable) model type for entity, "
                     + "it's probably obsolete i.e. its model (and inherited mixins) "
                     + "has changed since (only in test) : " + dataEntity.getUri()));
      }
      
      // check detailed permission :
      switch (permissionName) {
      case READ :
         // to be used in @PostAuthorize on GET
         // NB. allReaders have to contain ALSO writers & owners (i.e. be precomputed)
         // to allow efficient query filtering by rights criteria
         // => TODO that in PermissionAdminApi/Service
         
         /*if (user.isAdmin()
            || model.getSecurity().isResourceAdmin(user)) { // TODO or inherited
            return true; NOO else no view filtering nor PUT
         }*/
         
         return filterMixinsAndCheckPermission(dataEntity, null, model, user, permissionName, false);
         /*return model.getSecurity().isGuestReadable()
               || model.getSecurity().isAuthentifiedReadable() && !user.isGuest()
               || model.getSecurity().isResourceReader(user)
               || hasAnyEntityAclGroup(user, dataEntity.getAllReaders());*/

         
      case CHANGE_RIGHTS :
         // to be used in @PostAuthorize on GET
         // NB. readers have to contain ALSO writers & owners (i.e. be precomputed)
         // to allow efficient query filtering by rights criteria
         // => TODO that in PermissionAdminApi/Service
         return user.isAdmin()
               || model.getSecurity().isResourceAdmin(user) // TODO or inherited
               || hasAnyEntityAclGroup(user, dataEntity.getOwners()); // NB. no public owners !!
      
      case GET_RIGHTS :
         return user.isAdmin()
               || model.getSecurity().isResourceAdmin(user) // TODO or inherited
               || hasAnyEntityAclGroup(user, dataEntity.getOwners());
         
      case WRITE :
         boolean putRatherThanPatchMode = serverRequestContext != null
               && serverRequestContext.getPutRatherThanPatchMode();
         // NB. writers have NOT to contain also writers & owners (i.e. be precomputed)
         // because no mass write operations (yet)
         return filterMixinsAndCheckPermission(dataEntity, dataEntity.getPreviousEntity(),
               model, user, permissionName, putRatherThanPatchMode);
         // keep other properties :
         ///existingDataEntity.getProperties().keySet().removeAll(dataEntity.getProperties().keySet());
         ///dataEntity.getProperties().putAll(existingDataEntity.getProperties());
         /*return model.getSecurity().isAuthentifiedWritable() && !user.isGuest()
               || model.getSecurity().isResourceWriter(user)
               || hasAnyEntityAclGroup(user, dataEntity.getWriters())
               || hasAnyEntityAclGroup(user, dataEntity.getOwners()); // TODO remove BEFORE merging existing*/
         
      case CREATE :
         // to be used in @PreAuthorize on update ????
         ///return entityService.getModel(dataEntity).getSecurity().hasCreator(user); /// TODO or this ?
         /*return model.getSecurity().isAuthentifiedCreatable() && !user.isGuest()
               || model.getSecurity().isResourceCreator(user);*/
         return filterMixinsAndCheckPermission(dataEntity, null,
               model, user, permissionName, false);
    	 
      // NB. (model type) admin has already been done (first thing even)
         
      default :
         throw new RuntimeException("Unknown permission " + permissionName); //TODO better
      }
   }

   /**
    * 
    * @param dataEntity
    * @param existingDataEntity if non null, is filled and if putRatherThanPatchMode cleaned
    * @param model
    * @param user
    * @param permission "read", write", "create"
    * @param putRatherThanPatchMode requires existingDataEntity non null
    * @return
    */
   private boolean filterMixinsAndCheckPermission(DCEntity dataEntity, DCEntity existingDataEntity,
         DCModelBase model, DCUserImpl user, String permission, boolean putRatherThanPatchMode) {
      int propNb = dataEntity.getProperties().size();
      LinkedHashSet<String> seenPropNameSet = new LinkedHashSet<String>(propNb);
      return filterMixinsAndCheckPermission(dataEntity, existingDataEntity,
            model, user, permission, putRatherThanPatchMode,
            seenPropNameSet, propNb);
   }
   private boolean filterMixinsAndCheckPermission(DCEntity dataEntity, DCEntity existingDataEntity,
         DCModelBase model, DCUserImpl user, String permission, boolean putRatherThanPatchMode,
         LinkedHashSet<String> seenPropNameSet, int propNb) {
      boolean isThisModelAllowed = isThisModelAllowed(dataEntity, model, user, permission);
      Map<String, Object> props = dataEntity.getProperties();
      Map<String, Object> existingProps = (existingDataEntity == null) ? null
            : existingDataEntity.getProperties();
      boolean modelAllowedAndInViewMixins = isThisModelAllowed && viewMixinNamesContain(model.getName());
      
      // remove (and remember seen), in the order of mixins :
      //dataEntity.getProperties().entrySet().removeAll(model.getFieldMap().entrySet());
      for (String fieldName : model.getFieldMap().keySet()) {
         boolean seenProp = false;
         if (!modelAllowedAndInViewMixins) {
            seenProp = props.remove(fieldName) != null; // remove forbidden or outside view
            if (existingProps != null) {
               Object existingValue = existingProps.get(fieldName);
               if (existingValue != null) {
                  props.put(fieldName, existingValue);
               }
            }
         } else {
            // allowed : 
            if (props.containsKey(fieldName)) {
               seenProp = true;
            } else {
               if (!putRatherThanPatchMode) {
                  if (existingProps != null) {
                     Object existingValue = existingProps.get(fieldName);
                     if (existingValue != null) {
                        props.put(fieldName, existingValue);
                     }
                  }
               }
            }
         }
         if (seenProp) {
            seenPropNameSet.add(fieldName); // remember seen !
         }
      }
      
      boolean isAnyMixinAllowed = isThisModelAllowed;
      if (propNb == seenPropNameSet.size()) {
         return isAnyMixinAllowed; // everything has been filtered, filtering can be ended up right away
      }
      
      for (DCModelBase mixin : model.getMixins()) {
         isAnyMixinAllowed = filterMixinsAndCheckPermission(dataEntity, existingDataEntity, mixin, user, permission,
               putRatherThanPatchMode,
               seenPropNameSet, propNb) || isAnyMixinAllowed;
         // NB. ex. viewMixinNames=geo will show geo:name and not its override in geoci
         if (propNb == seenPropNameSet.size()) {
            return isAnyMixinAllowed; // everything has been filtered, filtering can be ended up right away
         }
      }
      return isAnyMixinAllowed;
   }

   /** shortcut to check only at model level */
   public boolean isThisModelAllowed(DCModelBase model, DCUserImpl user, String permission) {
      return isThisModelAllowed(null, model, user, permission);
   }

   /**
    * checks on model's (possibly inherited) security : guest, auth'd,
    * resource reader, resource admin, admin rights
    * PLUS (if needed) user ACL check
    * @param dataEntity if null, checks only at model-level
    * @param model
    * @param user
    * @param permission
    * @return
    */
   public boolean isThisModelAllowed(DCEntity dataEntity, DCModelBase model, DCUserImpl user, String permission) {
      if (user.isAdmin()) {
         return true;
      }
      DCSecurity security = modelService.getSecurity(model); // TODO cache !!!
      if (security == null) {
         return false; // can't find primary-inherited security, TODO error
      }
      // else has its own security :
      if (security.isResourceAdmin(user)) { // TODO cache this one !
         return true;
      }

      switch (permission) {
      case "read" :
         // to be used in @PostAuthorize on GET
         // NB. allReaders have to contain ALSO writers & owners (i.e. be precomputed)
         // to allow efficient query filtering by rights criteria
         // => TODO that in PermissionAdminApi/Service
         return security.isGuestReadable()
               || security.isAuthentifiedReadable() && !user.isGuest()
               || security.isResourceReader(user) // TODO cache this one
               || dataEntity != null && hasAnyEntityAclGroup(user, dataEntity.getAllReaders());
         
      case "write" :
         // NB. writers have NOT to contain also writers & owners (i.e. be precomputed)
         // because no mass write operations (yet)
         return security.isAuthentifiedWritable() && !user.isGuest()
               || security.isResourceWriter(user)
               || dataEntity != null
               && (hasAnyEntityAclGroup(user, dataEntity.getWriters()) // TODO cache this last one
               || hasAnyEntityAclGroup(user, dataEntity.getOwners())); // TODO remove BEFORE merging existing
         
      case "create" :
         // to be used in @PreAuthorize on update ????
         ///return entityService.getModel(dataEntity).getSecurity().hasCreator(user); /// TODO or this ?
         return security.isAuthentifiedCreatable() && !user.isGuest()
               || security.isResourceCreator(user); // TODO cache this last one
         
      case "changeRights" :
         // to be used in @PostAuthorize on GET
         // NB. readers have to contain ALSO writers & owners (i.e. be precomputed)
         // to allow efficient query filtering by rights criteria
         // => TODO that in PermissionAdminApi/Service
         return dataEntity != null && hasAnyEntityAclGroup(user, dataEntity.getOwners()); // NB. no public owners !!
      
      case "getRights" :
       return dataEntity != null && hasAnyEntityAclGroup(user, dataEntity.getOwners()); 
       
      // NB. (model type) admin has already been done (first thing even)
         
      default :
         throw new RuntimeException("Unknown permission " + permission); //TODO better
      }
   }

   private boolean viewMixinNamesContain(String mixinName) {
      LinkedHashSet<String> viewMixinNames;
      return serverRequestContext == null
            || (viewMixinNames = serverRequestContext.getViewMixinNames()) == null
                  || viewMixinNames.contains(mixinName);
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
