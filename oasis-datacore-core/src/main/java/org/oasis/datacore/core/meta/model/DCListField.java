package org.oasis.datacore.core.meta.model;


/**
 * delegates queryLimit, notQueriable & indexed to list field, but NOT their setters
 * @author mdutoo
 *
 */
public class DCListField extends DCField {
   
   private DCField listElementField;

   public DCListField() {
      
   }
   
   public DCListField(String name, DCField listElementField) {
      super(name, "list", false, 0, true);
      this.listElementField = listElementField;
   }
   /** required means at least empty and not null or not provided */
   public DCListField(String name, DCField listElementField, boolean required) {
      super(name, "list", required, 0, true);
      this.listElementField = listElementField;
   }
   public DCListField(String name, DCField listElementField, boolean required, String aliasedStorageName) {
      super(name, "list", required, 0, true);
      this.listElementField = listElementField;
      this.setAliasedStorageName(aliasedStorageName);
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

   @Override
   public int getQueryLimit() {
      return listElementField.getQueryLimit();
   }
   @Override
   public boolean isNotQueriable() {
      return listElementField.isNotQueriable();
   }
   @Override
   public boolean isIndexed() {
      return listElementField.isIndexed();
   }
   
   ///////////////////////////////////////
   // update methods

   public void setListElementField(DCField listElementField) {
      this.listElementField = listElementField;
   }

}
