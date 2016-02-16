package org.oasis.datacore.core.entity;

import java.net.URISyntaxException;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;


/**
 * DCEntity (secured) CRUD service.
 * To be impl'd on top of MongoDB storage.
 * 
 * @author mdutoo
 *
 */
// TODO default cannot @PreAuthorize(“hasRole(‘ROLE_EXCLUDE_ALL’)”) if possible ??
// see http://www.disasterarea.co.uk/blog/protecting-service-methods-with-spring-security-annotations/
public interface EntityService {

   /**
    * 
    * @param version
    * @return true if null or < 0
    */
   boolean isCreation(Long version);
   
   /**
    * Creates a new entity.
    * Uses transient cached model if any.
    * TODO rights
    * @param dataEntity without version (< 0 not allowed, though it is in DCResource)
    * @throws DuplicateKeyException if already exists
    * @throws NonTransientDataAccessException other, unexpected storage error
    */
   @PreAuthorize("hasPermission(#dataEntity, 'create')")
   void create(DCEntity dataEntity) throws NonTransientDataAccessException;

   /**
    * TODO or cache model : in custom context implementing "get" reused in both hasPermission & here ??
    * as transient param of resource ?? in EHCache (but beware of Resource obsolescence) ??
    * This method does not change state (else @PostAuthorize would not work)
    * @param uri
    * @param dcModel
    * @return
    * @throws NonTransientDataAccessException other, unexpected storage error
    * @throws EntityException when implicit only fork (several entities with the same uri).
    * To convert to ResourceException / HTTP 400.
    */
   @PostAuthorize("hasPermission(returnObject, 'read')")
   DCEntity getByUri(String uri, DCModelBase dcModel) throws NonTransientDataAccessException, EntityException;

   /**
    * Unprotected read to be used only for update purpose (to get ex. right ACL lists) ;
    * BEWARE no rights check, so should be followed by update() to check them or only
    * used for checking existence of Resource reference.
    * @param uri
    * @param dcModel
    * @return
    * @throws NonTransientDataAccessException
    * @throws EntityException from getUri() when implicit only fork (several entities with the same uri).
    * To convert to ResourceException / HTTP 400.
    */
   DCEntity getByUriUnsecured(String uri, DCModelBase dcModel) throws NonTransientDataAccessException, EntityException;
   
   /**
    * Used for more efficient If-None-Match=versionETag conditional GET :
    * allows not to load entity (nor resource) if same version.
    * BEWARE no rights check, so should be followed by ex. getByUriId()
    * to check them.
    * @param uri
    * @param dcModel
    * @param version
    * @return null if given version matches
    * @throws NonTransientDataAccessException other, unexpected storage error
    * @throws EntityException from getUri() (not called if no entity with this uri and version
    * across visible projects) when implicit only fork (several entities with the same uri).
    * To convert to ResourceException / HTTP 400.
    */
   boolean isUpToDate(String uri, DCModelBase dcModel, Long version) throws NonTransientDataAccessException, EntityException;

   /**
    * Updates given entity.
    * Uses transient cached model if any.
    * @param dataEntity must exist with version >= 0 and its ACLs be in sync with saved ones
    * @param dcModel
    * @throws OptimisticLockingFailureException if not up to date
    * @throws NonTransientDataAccessException other, unexpected storage error
    */
   @PreAuthorize("hasPermission(#dataEntity, 'write')")
   // NB. authorize before works because calling resourceService has loaded existing ACLs in dataEntity
   //@PreAuthorize("hasRole('admin') or hasRole('t_' + #dcModel.getName() + '_admin') or hasPermission(#dataEntity, 'write')")
   // NB. (model type) admin role check is done in hasPermission so it's never forgotten
   // (but could be done explicitly if no call to hasPermission is done)
   // NB. methods available in security el are (Method)SecurityExpressionRoot's
   void update(DCEntity dataEntity) throws OptimisticLockingFailureException,
         NonTransientDataAccessException;

   /**
    * Deletes given entity (with same uri, version & project).
    * NB. deleting a non-existing resource does silently nothing.
    * TODO LATER dedicated rights permission ?
    * @param dataEntity MUST have been retrieved from same current project
    * @throws NonTransientDataAccessException other, unexpected storage error
    */
   @PreAuthorize("hasPermission(#dataEntity, 'write')")
   void deleteByUriId(DCEntity dataEntity) throws NonTransientDataAccessException;

   @PreAuthorize("hasPermission(#dataEntity, 'changeRights')")
   void changeRights(DCEntity dataEntity);
   
   /**
    * Only here to check if we are owner on the resource
    * If we are not an exception is thrown
    * @param dataEntity
    */
   @PreAuthorize("hasPermission(#dataEntity, 'getRights')")
   void getRights(DCEntity dataEntity);
   
   /** TODO (re)move ?
    * @obsolete
    * @throws URISyntaxException */
   DCEntity getSampleData() throws URISyntaxException;

   /**
    * Create an alias of 'target' with the provided URI
    * @param target the entity to alias to
    * @param uri the uri that should be an alias of 'target'
    * @throws NonTransientDataAccessException
    */
   @PreAuthorize("hasPermission(#target, 'write')")
   void aliasOf(DCEntity target, String uri) throws NonTransientDataAccessException;


   /**
    * Does the provided URI point to an alias?
    * @param dcModel
    * @param uri          provided URI
    * @param rawEntity    entity the URI refers to
    * @return
    */
   boolean isAlias(String uri, DCEntity rawEntity);

   /**
    * Does the provided URI point to an alias?
    * @param uri      provided URI
    * @param model    model for that resource
    * @return
    */
   boolean isAlias(String uri, DCModelBase model);


   /**
    * Return the "real" aliased URI
    * @param uri          provided URI
    * @param rawEntity    "raw" entity the URI refers to
    * @return
    */
   String getAliased(String uri, DCEntity rawEntity);

   /**
    * Return the "real" aliased URI
    * @param dcModel
    * @param uri     provided URI
    * @param model   the model for that resource
    * @return
    */
   String getAliased(String uri, DCModelBase model);


   /**
    * Recursively delete all aliases that point to the provided URI
    * @param uri
    * @param model
    */
   void deleteAliases(String uri, DCModelBase model);

   /**
    * If dataEntity.uri is used for an alias, and that alias points (possibly
    * by way of other aliases) to our entity's previous uri, then delete that
    * alias (and only that alias: we want all previous uris for the resource
    * to remain valid)
    * @param dataEntity
    * @param previousUri
    */
   void unwindAliases(DCEntity dataEntity, String previousUri);
}