package org.oasis.datacore.rest.server.parsing.model;

import org.oasis.datacore.core.meta.model.DCField;

public class DCResourceValue {

   private String fullValuedPath;
   private DCField field;
   private Object value;
   
   public DCResourceValue(String fullValuedPath, DCField field, Object value) {
      this.fullValuedPath = fullValuedPath;
      this.field = field;
      this.value = value;
   }

   public String getFullValuedPath() {
      return fullValuedPath;
   }

   public void setFullValuedPath(String fullValuedPath) {
      this.fullValuedPath = fullValuedPath;
   }

   public DCField getField() {
      return field;
   }

   public void setField(DCField field) {
      this.field = field;
   }

   public Object getValue() {
      return value;
   }

   public void setValue(Object value) {
      this.value = value;
   }
   
}
