package org.oasis.datacore.core.security;

import java.util.HashSet;
import java.util.Set;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.springframework.stereotype.Component;


/**
 * 
 * 
 * @author mdutoo
 *
 */
@Component
public class EntityPermissionService {

   public void setReaders(DCEntity entity, Set<String> readers) {
      if (readers == null || readers.size() == 0) {
         if (entity.getReaders() != null) {
            entity.setReaders(null); // (or empty list ???)
         }
         return;
      }
      entity.setReaders(readers);
      recomputeAllReaders(entity);
   }

   public void setWriters(DCEntity entity, Set<String> writers) {
      if (writers == null || writers.size() == 0) {
         if (entity.getWriters() != null) {
            entity.setWriters(null); // (or empty list ???)
         }
         return;
      }
      entity.setWriters(writers);
      recomputeAllReaders(entity);
   }

   public void setOwners(DCEntity entity, Set<String> owners) {
      if (owners == null || owners.size() == 0) {
         if (entity.getOwners() != null) {
            entity.setOwners(null); // (or empty list ???)
         }
         return;
      }
      entity.setOwners(owners);
      recomputeAllReaders(entity);
   }

   private void recomputeAllReaders(DCEntity entity) {
      entity.setAllReaders(addAllIfNotNullOrEmpty(entity.getReaders(),
            addAllIfNotNullOrEmpty(entity.getWriters(),
                  addAllIfNotNullOrEmpty(entity.getOwners(), null))));
   }

   private Set<String> addAllIfNotNullOrEmpty(Set<String> acl, Set<String> setToFill) {
      if (acl == null || acl.isEmpty()) {
         return setToFill;
      }
      if (setToFill == null) {
         setToFill = new HashSet<String>(acl);
      } else {
         setToFill.addAll(acl);
      }
      return setToFill;
   }
   
}
