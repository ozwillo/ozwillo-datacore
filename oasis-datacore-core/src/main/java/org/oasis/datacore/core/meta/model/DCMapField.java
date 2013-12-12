package org.oasis.datacore.core.meta.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Defines a map, i.e. a collection of named fields.
 * 
 * TODO make i18n easier than having to define each language all the time :
 * * specifically define DCI18nField inheriting DCMapField with custom parsing
 * * or allow to share Field definitions (and not only named Fields i.e. Mixins)
 * and instantiate them in DCModels BUT THAT'S THE SAME SOLUTION
 * * or do in DCMapField features required (by such DCI18nField) but also useful beyond :
 * 
 * TODO also allow any named value conforming to a defined mapField (as in DCListField) !
 * TODO LATER and with constraints on names (such as belong to a given / resource list) ??
 * TODO LATER and also allow such "value list constraint" on all values of all field types ??
 * 
 * TODO LATER also allow any named value of any (Javascript-only) type ??? NO WOULD DEFEAT THE PURPOSE
 * OF DCMODEL DEPICTING WHAT DATA IS AVAILABLE
 * 
 * @author mdutoo
 */
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
