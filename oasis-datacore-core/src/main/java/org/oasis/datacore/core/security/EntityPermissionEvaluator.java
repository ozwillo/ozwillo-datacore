package org.oasis.datacore.core.security;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.oasis.datacore.core.context.DatacoreRequestContextService;
import org.oasis.datacore.core.entity.EntityModelService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.model.DCEntityBase;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.ModelNotFoundException;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCSecurity;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.core.security.service.DatacoreSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
 * NB. no guest mode (but apps can create their own system users for
 * various purposes).
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

   /** to use compute guest readable NOO */
   @Value("${datacore.localauthdevmode}")
   private boolean localauthdevmode;
   
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
   private DatacoreRequestContextService serverRequestContext = null;
   
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
               || model.getSecurity().isResourceOwner(user) // TODO or inherited
               || hasAnyEntityAclGroup(user, dataEntity.getOwners()); // NB. no public owners !!
      
      case GET_RIGHTS :
         return user.isAdmin()
               || model.getSecurity().isResourceOwner(user) // TODO or inherited
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
    * Also applies mixins view filtering
    * @param dataEntity
    * @param existingDataEntity if non null, is filled and if putRatherThanPatchMode cleaned
    * @param model
    * @param user
    * @param permission "read", write", "create"
    * @param putRatherThanPatchMode requires existingDataEntity non null
    * @return
    */
   private boolean filterMixinsAndCheckPermission(DCEntityBase dataEntity, DCEntityBase existingDataEntity,
         DCModelBase model, DCUserImpl user, String permission, boolean putRatherThanPatchMode) {
      int propNb = dataEntity.getProperties().size();
      LinkedHashSet<String> seenPropNameSet = new LinkedHashSet<String>(propNb);
      return filterMixinsAndCheckPermission(dataEntity, existingDataEntity,
            model, user, permission, putRatherThanPatchMode,
            seenPropNameSet, propNb, null);
   }
   private boolean filterMixinsAndCheckPermission(DCEntityBase dataEntity, DCEntityBase existingDataEntity,
         DCModelBase model, DCUserImpl user, String permission, boolean putRatherThanPatchMode,
         LinkedHashSet<String> seenPropNameSet, int propNb, Boolean isInheritingMixinAllowed) {
      boolean isThisModelAllowed = isThisModelAllowed(dataEntity, model,
            user, permission, isInheritingMixinAllowed);
      Map<String, Object> props = dataEntity.getProperties();
      Map<String, Object> existingProps = (existingDataEntity == null) ? null
            : existingDataEntity.getProperties();
      
      // #71 applying mixins view filtering :
      // TODO LATER (mixin) views also / rather as mongo field projection (and possibly even
      // to enforce "read" rights, though if resource-level rights are enabled entity is required
      // to compute rights, and anyway post filtering in EntityPermissionEvaluator & entityToResource
      // will still be required to apply model/mixin-level rights)
      // http://docs.mongodb.org/manual/tutorial/project-fields-from-query-results/
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
               seenPropNameSet, propNb, isThisModelAllowed) || isAnyMixinAllowed;
         // NB. ex. viewMixinNames=geo will show geo:name and not its override in geoci
         if (propNb == seenPropNameSet.size()) {
            return isAnyMixinAllowed; // everything has been filtered, filtering can be ended up right away
         }
      }
      return isAnyMixinAllowed;
   }

   /** shortcut to check only at model level */
   public boolean isThisModelAllowed(DCModelBase model, DCUserImpl user, String permission) {
      return isThisModelAllowed(null, model, user, permission, null);
   }

   /**
    * checks on model's (possibly inherited) security : guest, auth'd,
    * resource reader, resource admin, admin rights
    * PLUS (if needed) user ACL check
    * @param dataEntity if null, checks only at model-level
    * @param model
    * @param user
    * @param permission
    * @param isInheritingMixinIn if null, not yet found any security in inheritance tree
    * meaning get the next (primary inherited) one
    * @return
    */
   public boolean isThisModelAllowed(DCEntityBase dataEntity, DCModelBase model,
         DCUserImpl user, String permission, Boolean isInheritingMixinAllowed) {
      DCProject currentProject = modelService.getProject();
      DCProject project = modelService.getProject(model.getProjectName()); // model project
      
      // constraints in case of visible project :
      // (is it allowed to read / write in another project ?)
      boolean shouldCheckVisibleProjectConstraints = true;
      if (!model.getProjectName().equals(currentProject.getName())
            // TODO TODO HACK avoid visible constraints in case of facade projects :
            && !isFacadeProject(model.getProjectName())) {
         DCModelBase storageModel = (dataEntity != null) ? // else none yet (been queried by LDP)
               entityModelService.getStorageModel(dataEntity) : modelService.getStorageModel(model);
         if (storageModel != null && storageModel.isMultiProjectStorage()) { // especially oasis.meta.dcmi:mixin_0 in case of model resources !
            // (even oasis.sandbox models are stored in oasis.meta.dcmi:mixin_0 collection,
            // only soft forks i.e. inheriting overrides couldn't be handled this way)
            // no need to check, entity will anyway be written in its own project
            if (dataEntity == null // none yet (been queried by LDP ???), should be checked later
                  || dataEntity.getProjectName() == null) { // new (or not yet migrated), will be in the current project
               shouldCheckVisibleProjectConstraints = false;
            } else {
               shouldCheckVisibleProjectConstraints = !dataEntity.getProjectName().equals(currentProject.getName());
               project = modelService.getProject(dataEntity.getProjectName()); // entity project
            }
         }
         
         if (shouldCheckVisibleProjectConstraints) {
            DCSecurity modelVisibleSecurityConstraints = project.getVisibleSecurityConstraints();
            if (modelVisibleSecurityConstraints != null
                  && !isThisSecurityAllowed(null, modelVisibleSecurityConstraints, user, permission)) {
               return false; // ex. no (auth'd) "write" for geo outside itself i.e. from another project ex. org
            }
         }
      }
      // project-level constraints :
      DCSecurity projectSecurityConstraints = project.getSecurityConstraints();
      if (projectSecurityConstraints != null
            && !isThisSecurityAllowed(null, // WHATEVER THE RESOURCE
                  projectSecurityConstraints, user, permission)) {
         return false; // ex. no "read" outside 
      }
      
      if (user.isAdmin()) {
         return true; // (after constraints else admin won't have them)
      }
      
      // case of project-level only security :
      if (!project.isModelLevelSecurityEnabled()) {
         DCSecurity projectSecurityDefaults = project.getSecurityDefaults();
         if (projectSecurityDefaults == null) {
            return isDefaultSecurityAllowed(user, permission);
         }
         return isThisSecurityAllowed(null, // WHATEVER THE RESOURCE
               projectSecurityDefaults, user, permission);
      }
      
      DCSecurity security = model.getSecurity();
      if (security == null) {
         if (isInheritingMixinAllowed != null) {
            return isInheritingMixinAllowed;
         }
         // else first security from top, get primary-inherited one :
         security = modelService.getSecurity(model); // TODO cache !!!
         if (security == null) {
            security = project.getSecurityDefaults();
            if (security == null) {
               return isDefaultSecurityAllowed(user, permission);
            }
         }
      }
      
      return isThisSecurityAllowed(dataEntity, security, user, permission);
   }

   /**
    * TODO TODO HACK avoid visible constraints in case of facade projects
    * @param projectName
    * @return
    */
   private boolean isFacadeProject(String projectName) {
      return modelService.getProject(projectName).getUnversionedName().equals(modelService.getProject().getName());
   }

   public boolean isDefaultSecurityAllowed(DCUserImpl user, String permission) {
      if (localauthdevmode/* || project.defaultSecurity == phase1*/) {
         return true; // default Phase 1 (model design step) security 
      }
      // no (primary-inherited) security at all ex. Phase 1, TODO return project default
      return false;
   }

   /**
    * 
    * @param dataEntity works also if null
    * @param security not null
    * @param user
    * @param permission
    * @return
    */
   public boolean isThisSecurityAllowed(DCEntityBase dataEntity, DCSecurity security, DCUserImpl user, String permission) {
      // else has its own security :
      if (security.isResourceOwner(user)) { // TODO cache this one !
         return true;
      }

      switch (permission) {
      case READ :
         // to be used in @PostAuthorize on GET
         // NB. allReaders have to contain ALSO writers & owners (i.e. be precomputed)
         // to allow efficient query filtering by rights criteria
         // => TODO that in PermissionAdminApi/Service
         return security.isAuthentifiedReadable()
               || security.isResourceReader(user) // TODO cache this one
               || dataEntity != null && hasAnyEntityAclGroup(user, dataEntity.getAllReaders());
         
      case WRITE :
         // NB. writers have NOT to contain also writers & owners (i.e. be precomputed)
         // because no mass write operations (yet)
         return security.isAuthentifiedWritable()
               || security.isResourceWriter(user)
               || dataEntity != null
               && (hasAnyEntityAclGroup(user, dataEntity.getWriters()) // TODO cache this last one
               || hasAnyEntityAclGroup(user, dataEntity.getOwners())); // TODO remove BEFORE merging existing
         
      case CREATE :
         // to be used in @PreAuthorize on update ????
         ///return entityService.getModel(dataEntity).getSecurity().hasCreator(user); /// TODO or this ?
         return security.isAuthentifiedCreatable()
               || security.isResourceCreator(user); // TODO cache this last one
         
      case CHANGE_RIGHTS :
         // to be used in @PostAuthorize on GET
         // NB. readers have to contain ALSO writers & owners (i.e. be precomputed)
         // to allow efficient query filtering by rights criteria
         // => TODO that in PermissionAdminApi/Service
         return dataEntity != null && hasAnyEntityAclGroup(user, dataEntity.getOwners()); // NB. no public owners !!
      
      case GET_RIGHTS :
       return dataEntity != null && hasAnyEntityAclGroup(user, dataEntity.getOwners()); 
       
      // NB. (model type) admin has already been done (first thing even)
         
      default :
         throw new RuntimeException("Unknown permission " + permission); //TODO better
      }
   }

   /**
    * Used to apply mixins view filtering
    * @param mixinName
    * @return
    */
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
      // TODO replace by GrantedAuth
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
