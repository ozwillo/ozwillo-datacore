package org.oasis.datacore.core.meta.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

   // NB. maintains order
   private LinkedHashMap<String,DCField> mapFields = new LinkedHashMap<String,DCField>();
   
   public DCMapField(String name) {
      super(name, "map", false, 0, true); // TODO required ?!?
      this.setName(name);
   }
   public DCMapField(String name, LinkedHashSet<String> aliasedStorageNames) {
      this(name);
      this.setAliasedStorageNames(aliasedStorageNames);
   }
   public DCMapField(String name, String singleAliasedStorageName) {
      this(name);
      this.setSingleAliasedStorageName(singleAliasedStorageName);
   }
   
   public DCMapField(String name, String singleAliasedStorageName, boolean readonly) {
      this(name, singleAliasedStorageName);
      this.setReadonly(readonly);
   }

   public Map<String,DCField> getMapFields() {
      return mapFields;
   }

   public Collection<String> getMapFieldNames() {
      return mapFields.keySet();
   }
   
   
   ///////////////////////////////////////
   // update methods
   
   public DCMapField addField(DCField field) {
      mapFields.put(field.getName(), field);
      return this;
   }

   public void setMapFields(Map<String, DCField> mapFields) {
      this.mapFields = new LinkedHashMap<String,DCField>(mapFields);
   }

}
