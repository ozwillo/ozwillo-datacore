package org.oasis.datacore.core.meta.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO TODO rather no field in root DCModel and rather all in mixinTypes !!!!!!!!!!!
 * 
 * TODO readonly : setters only for tests, LATER admin using another (inheriting) model ?!?
 * TODO common abstract / inherit with DCMapField
 * @author mdutoo
 *
 */
public class DCModel {
   
   private String name;
   private Map<String,DCField> fieldMap = new HashMap<String,DCField>();
   private List<String> fieldNames = new ArrayList<String>(); // to maintain order ; easiest to persist (json / mongo) than ordered map
   private List<DCModel> mixinTypes = new ArrayList<DCModel>();
   // TODO allFieldNames, allFieldMap cached
   
   public DCModel() {
      
   }
   public DCModel(String name) {
      this.name = name;
   }
   
   public DCField getField(String name) {
      return fieldMap.get(name);
   }
   
   public DCField getGlobalField(String name) {
      return fieldMap.get(name);
   }

   public String getCollectionName() {
      return name;
   }

   public String getName() {
      return name;
   }

   public Map<String, DCField> getFieldMap() {
      return fieldMap;
   }

   public List<String> getFieldNames() {
      return fieldNames;
   }
   
   
   ///////////////////////////////////////
   // update methods
   
   public void addField(DCField field) {
      fieldMap.put(field.getName(), field);
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setFieldMap(Map<String, DCField> fieldMap) {
      this.fieldMap = fieldMap;
   }

   public void setFieldNames(List<String> fieldNames) {
      this.fieldNames = fieldNames;
   }

}
