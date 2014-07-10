package org.oasis.datacore.core.meta.model;

public class DCListField extends DCField {
   
   private DCField listElementField;

   public DCListField() {
      
   }
   public DCListField(String name, DCField listElementField) {
      super(name, "list", false, 0, true);
      this.listElementField = listElementField;
   }
   /**
    * To be used only to define subtypes of "list" ex. "i18n"
    * TODO LATER define how "required" applies to i18n (ex. required only for model/resource "default" language)
    * @param name
    * @param type
    * @param listElementField
    */
   protected DCListField(String name, String type, DCField listElementField) {
      super(name, type, false, 0);
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
