package org.oasis.datacore.core.entity;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.model.DCModel;
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
// TODO default annot @PreAuthorize(“hasRole(‘ROLE_EXCLUDE_ALL’)”) if possible ??
// see http://www.disasterarea.co.uk/blog/protecting-service-methods-with-spring-security-annotations/
public interface EntityService {

   /**
    * Creates a new entity.
    * Uses transient cached model if any.
    * TODO rights
    * @param dataEntity
    * @param dcModel
    * @throws DuplicateKeyException if already exists
    * @throws NonTransientDataAccessException other, unexpected storage error
    */
   @PreAuthorize("hasPermission(#dataEntity, 'create')")
   void create(DCEntity dataEntity) throws DuplicateKeyException, NonTransientDataAccessException;

   /**
    * TODO or model cached : in custom context implementing "get" reused in both hasPermission & here ??
    * as transient param of resource ?? in EHCache (but beware of Resource obsolescence) ??
    * This method does not change state (else @PostAuthorize would not work)
    * @param uri
    * @param dcModel
    * @return
    * @throws NonTransientDataAccessException other, unexpected storage error
    */
   @PostAuthorize("hasPermission(returnObject, 'read')")
   DCEntity getByUri(String uri, DCModel dcModel) throws NonTransientDataAccessException;

   /**
    * Used for more efficient If-None-Match=versionETag conditional GET :
    * allows not to load entity (nor resource) if same version.
    * BEWARE no rights check, so should be followed by ex. getByUriId
    * to check them.
    * @param uri
    * @param dcModel
    * @param version
    * @return null if given version matches
    * @throws NonTransientDataAccessException other, unexpected storage error
    */
   boolean isUpToDate(String uri, DCModel dcModel, Long version) throws NonTransientDataAccessException;

   /**
    * Updates given entity.
    * Uses transient cached model if any.
    * @param dataEntity must exist and its ACLs be in sync with saved ones
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
    * Deletes given entity having given version.
    * NB. deleting a non-existing resource does silently nothing.
    * TODO rights
    * @param uri
    * @param version
    * @param dcModel
    * @throws NonTransientDataAccessException other, unexpected storage error
    */
   //@PreAuthorize("hasPermission(#dataEntity, 'write')")
   public abstract void deleteByUriId(String uri, long version, DCModel dcModel)
         throws NonTransientDataAccessException;

   /** TODO (re)move ?*/
   public abstract DCEntity getSampleData();

}