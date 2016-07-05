package org.oasis.datacore.core.meta.model;

import java.util.LinkedHashSet;


/**
 * TODO LATER better, more flexible type constraints : several types, mixins...
 * 
 * @author mdutoo
 *
 */
public class DCResourceField extends DCField {
   
   /** required type
    * TODO LATER better, more flexible type constraints : several types, mixins... */
   private String resourceType;
   /** subresource or stored in another collection ?
    * (storage-level overridable, like queryLimit defines indexes) */
   private Boolean isStorage = false;

   /** for unmarshalling only */
   public DCResourceField() {
      
   }

   public DCResourceField(String name, String resourceType, boolean required, int queryLimit) {
      super(name, DCFieldTypeEnum.RESOURCE.getType(), required, queryLimit, true);
      this.setResourceType(resourceType);
   }
   public DCResourceField(String name, String resourceType, boolean required, int queryLimit,
         LinkedHashSet<String> aliasedStorageNames) {
      this(name, resourceType, required, queryLimit);
      this.setAliasedStorageNames(aliasedStorageNames);
   }
   public DCResourceField(String name, String resourceType, boolean required, int queryLimit,
         String singleAliasedStorageName) {
      this(name, resourceType, required, queryLimit);
      this.setSingleAliasedStorageName(singleAliasedStorageName);
   }
   public DCResourceField(String name, String resourceType, boolean required, int queryLimit,
         String singleAliasedStorageName, boolean readonly) {
      this(name, resourceType, required, queryLimit, singleAliasedStorageName);
      this.setReadonly(readonly);
   }

   public DCResourceField(String name, String resourceType) {
      super(name, DCFieldTypeEnum.RESOURCE.getType(), true);
      this.setResourceType(resourceType);
   }

   public String getResourceType() {
      return resourceType;
   }

   public void setResourceType(String resourceType) {
      this.resourceType = resourceType;
   }

   public Boolean isStorage() {
      return isStorage;
   }

   public void setStorage(Boolean isStorage) {
      this.isStorage = isStorage;
   }

}
