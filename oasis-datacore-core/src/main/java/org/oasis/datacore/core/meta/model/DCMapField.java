package org.oasis.datacore.core.meta.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Defines a map, i.e. a collection of named fields.
 * In a map instance, each named value must comply to its named field definition.
 * 
 * NB. a "single field map" where all values comply to a single field definition
 * is actually merely a list of map with at least a "name" field.
 * 
 * @author mdutoo
 */
// TODO common abstract / inherit with DCModel, possibly inherit from ancestor map fields
public class DCMapField extends DCField {

   private Map<String,DCField> mapFields = new HashMap<String,DCField>();
   private List<String> mapFieldNames = new ArrayList<String>(); // to maintain order
   
   public DCMapField(String name) {
      super(name, "map", false, 0, true); // TODO required ?!?
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
   
   public DCMapField addField(DCField field) {
      mapFields.put(field.getName(), field);
      mapFieldNames.add(field.getName());
      return this;
   }

   public void setMapFieldNames(List<String> mapFieldNames) {
      this.mapFieldNames = mapFieldNames;
   }

   public void setMapFields(Map<String, DCField> mapFields) {
      this.mapFields = mapFields;
   }

}
