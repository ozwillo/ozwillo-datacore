package org.oasis.datacore.core.meta.model;

import java.util.Set;

import com.google.common.collect.ImmutableSet;


/**
 * TODO readonly : setters only for tests, LATER admin using another (inheriting) model ?!?
 * don't use this object for List or Maps (use DCListField and DCMapField)
 * @author mdutoo
 *
 */
public class DCField {

   private String name; // TODO also longName / displayName (i18n'd ??), description (/ help), sampleValues list ?
   /** string, boolean, int, float, long, double, date, map, list, resource, i18n ? geoloc ???
    * default is string */
   private String type = "string"; // TODO enum ?!? "text" ?
   /** Is a non-null value required ? defaults to false */
   private boolean required = false;
   /** size of results allowed when querying on this field
    * if > 0, field will be indexed (TODO LATER2 BUT RESULTS MAY BE INCOMPLETE sparse if !required)
    * defaults to 0
    * TODO LATER replace by indexed & maxScan fields OR rename to resourceJoinQueryLimit
    * OR add business fields "hotterRatherThanColder", "smallSetOfValuesRatherThanBigOne" */
   private int queryLimit = 0;
   
   // TODO also :
   // * default rights for Model ?! (or even Mixin ? Field ???)
   // * storedName (or auto minified ?)
   // * alias, for which use ??
   // * defaultOrder (asc or descs)
   // * allowedValuesModel : name of Model whose (primitive !?) values are the only allowed one for the field ESPECIALLY FOR RESOURCE
   // * validation (restricted to allowedValuesModel, regexp or script)
   // * on change event / action ??
   // * backoffice / admin UI conf ???

   /** types that are supported by DCField i.e. all without map, list & resource ;
    * TODO using Enum see DCFieldTypeEnum ?! */
   public static Set<String> basicFieldTypes = new ImmutableSet
         .Builder<String>().add("string").add("boolean").
         add("int").add("float").add("long").add("double").add("date").add("i18n").build();
   
   /** for unmarshalling only */
   public DCField() {
      
   }
   public DCField(String name, String type, boolean required, int queryLimit) {
      this(name, type);
      this.required = required;
      this.queryLimit = queryLimit;
   }
   public DCField(String name, String type) {
      if (!basicFieldTypes.contains(type)) {
         throw new ClassCastException("DCField only supports basic fields and not " + type
               + " (name: " + name + ")");
      }
      this.name = name;
      this.type = type;
   }
   /** to be used in inheriting classes only, to skip constraint on type */
   protected DCField(String name, String type, boolean required, int queryLimit,
         boolean superConstructor) {
      this(name, type, superConstructor);
      this.required = required;
      this.queryLimit = queryLimit;
   }
   /** to be used in inheriting classes only, to skip constraint on type */
   protected DCField(String name, String type, boolean superConstructor) {
      this.name = name;
      this.type = type;
   }
   
   public String getType() {
      return type;
   }

   // TODO also path in model ?!?
   public String getName() {
      return name;
   }

   public boolean isRequired() {
      return required;
   }
   public int getQueryLimit() {
      return queryLimit;
   }
   
   /**
    * TODO ObjectMapper
    */
   public String toString() {
      return name;
   }

   
   ///////////////////////////////////////
   // update methods

   public void setName(String name) {
      this.name = name;
   }

   public void setType(String type) {
      this.type = type;
   }

   public void setRequired(boolean required) {
      this.required = required;
   }

   public void setQueryLimit(int queryLimit) {
      this.queryLimit = queryLimit;
   }

}
