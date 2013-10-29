package org.oasis.datacore.core.meta.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO common abstract / inherit with DCModel
public class DCMapField extends DCField {

   private Map<String,DCField> mapFields = new HashMap<String,DCField>();
   private List<String> mapFieldNames = new ArrayList<String>(); // to maintain order
   
   public DCMapField(String name) {
      super(name, "map", false, 0);
      this.setName(name);
   }

   public Map<String,DCField> getMapFields() {
      return mapFields;
   }

   public List<String> getMapFieldNames() {
      return mapFieldNames;
   }
   
   
   ///////////////////////////////////////
   // update methods
   
   public void addField(DCField field) {
      mapFields.put(field.getName(), field);
      mapFieldNames.add(field.getName());
   }

   public void setMapFieldNames(List<String> mapFieldNames) {
      this.mapFieldNames = mapFieldNames;
   }

   public void setMapFields(Map<String, DCField> mapFields) {
      this.mapFields = mapFields;
   }

}
