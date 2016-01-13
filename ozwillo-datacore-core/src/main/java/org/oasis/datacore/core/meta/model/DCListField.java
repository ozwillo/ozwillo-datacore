package org.oasis.datacore.core.meta.model;

import java.util.LinkedHashSet;


/**
 * delegates queryLimit, notQueriable & indexed to list field, but NOT their setters
 * @author mdutoo
 *
 */
public class DCListField extends DCField {
   
   private DCField listElementField;
   private boolean isSet = false;
   private String keyFieldName = null;

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
   public DCListField(String name, DCField listElementField, boolean required, LinkedHashSet<String> aliasedStorageNames) {
      super(name, "list", required, 0, true);
      this.listElementField = listElementField;
      this.setAliasedStorageNames(aliasedStorageNames);
   }
   public DCListField(String name, DCField listElementField, boolean required, String singleAliasedStorageName) {
      super(name, "list", required, 0, true);
      this.listElementField = listElementField;
      this.setSingleAliasedStorageName(singleAliasedStorageName);
   }
   public DCListField(String name, DCField listElementField, boolean required,
         String singleAliasedStorageName, boolean readonly) {
      this(name, listElementField, required, singleAliasedStorageName);
      this.setReadonly(readonly);
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

   public boolean isSet() {
      return isSet;
   }

   public void setIsSet(boolean isSet) {
      this.isSet = isSet;
   }

   public String getKeyFieldName() {
      return keyFieldName;
   }

   public void setKeyFieldName(String keyFieldName) {
      this.keyFieldName = keyFieldName;
   }
   
   ///////////////////////////////////////
   // update methods

   public void setListElementField(DCField listElementField) {
      this.listElementField = listElementField;
   }

}
