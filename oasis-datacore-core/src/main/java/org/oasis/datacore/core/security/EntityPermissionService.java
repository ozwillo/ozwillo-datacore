package org.oasis.datacore.core.security;

import java.util.HashSet;
import java.util.Set;

import org.oasis.datacore.core.entity.model.DCEntity;


/**
 * 
 * 
 * @author mdutoo
 *
 */
public class EntityPermissionService {

   public void setReaders(DCEntity entity, Set<String> readers) {
      if (readers == null || readers.size() == 0) {
         if (entity.getReaders() != null) {
            entity.setReaders(null); // (or empty list ???)
         }
         return;
      }
      entity.setReaders(readers);
   }

   public void setWriters(DCEntity entity, Set<String> writers) {
      if (writers == null || writers.size() == 0) {
         if (entity.getWriters() != null) {
            entity.setWriters(null); // (or empty list ???)
         }
         return;
      }
      entity.setWriters(writers);
      Set<String> readers = entity.getReaders();
      if (readers == null) {
         entity.setReaders(new HashSet<String>(writers));
      } else {
         readers.addAll(writers);
         entity.setReaders(readers);
      }
   }

   public void setOwners(DCEntity entity, Set<String> owners) {
      if (owners == null || owners.size() == 0) {
         if (entity.getOwners() != null) {
            entity.setOwners(null); // (or empty list ???)
         }
         return;
      }
      entity.setOwners(owners);
   }
   
}
