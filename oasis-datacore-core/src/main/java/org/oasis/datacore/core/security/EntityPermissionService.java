package org.oasis.datacore.core.security;

import java.util.HashSet;
import java.util.List;
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

   public void recomputeAllReaders(DCEntity entity) {
      entity.setAllReaders(addAllIfNotNullOrEmpty(entity.getReaders(),
            addAllIfNotNullOrEmpty(entity.getWriters(),
                  addAllIfNotNullOrEmpty(entity.getOwners(), null))));
   }

   public Set<String> addAllIfNotNullOrEmpty(Set<String> acl, Set<String> setToFill) {
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
   
	public Set<String> addRights(Set<String> entitySet, List<String> toAddSet) {
		
		if(entitySet == null && toAddSet != null) {
			return new HashSet<>(toAddSet);
		} else if (entitySet != null && toAddSet == null) {
			return entitySet;
		} else if (entitySet == null && toAddSet == null) {
			return new HashSet<String>();
		} else {
			entitySet.addAll(toAddSet);
			return entitySet;
		}
		
	}
	
	public Set<String> removeRights(Set<String> entitySet, List<String> toRemoveSet) {
		
		if(entitySet == null && toRemoveSet != null) {
			return new HashSet<String>();
		} else if (entitySet != null && toRemoveSet == null) {
			return entitySet;
		} else if (entitySet == null && toRemoveSet == null) {
			return new HashSet<String>();
		} else {
			entitySet.removeAll(toRemoveSet);
			return entitySet;
		}
		
	}
   
}
