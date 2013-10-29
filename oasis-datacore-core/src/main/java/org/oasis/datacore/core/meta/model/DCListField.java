package org.oasis.datacore.core.meta.model;

public class DCListField extends DCField {
   
   private DCField listElementField;

   public DCListField() {
      
   }
   public DCListField(String name, DCField listElementField) {
      super(name, "list", false, 0);
      this.listElementField = listElementField;
   }


   public DCField getListElementField() {
      return listElementField;
   }
   
   
   ///////////////////////////////////////
   // update methods

   public void setListElementField(DCField listElementField) {
      this.listElementField = listElementField;
   }

}
