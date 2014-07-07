package org.oasis.datacore.core.meta.model;


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

   /** for unmarshalling only */
   public DCResourceField() {
      
   }

   public DCResourceField(String name, String resourceType, boolean required,
         int queryLimit) {
      super(name, DCFieldTypeEnum.RESOURCE.getType(), required, queryLimit, true);
      this.setResourceType(resourceType);
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

}
