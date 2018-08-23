package org.oasis.datacore.core.meta.model;

import java.util.LinkedHashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;


/**
 * TODO LATER inherits DCNamedBase .getName() common with DCModelBase for fieldOrMixins ?
 * 
 * TODO readonly : setters only for tests, LATER admin using another (inheriting) model ?!?
 * don't use this object for List or Maps (use DCListField and DCMapField)
 * @author mdutoo
 *
 */
public class DCField {

   /** Name of the single (sub) entity i18n field containing tokens for the fulltext search.
    * NB. even non-i18n string-typed fields are tokenized & fulltext searched in i18n'd tokens,
    * otherwise it would require separate indexes and therefore less efficient criteria
    * or recomputing all existing tokens when the first i18n fulltext field is added.
    * No resource field can have this name.
    * For now this field is hidden and must be accessed in queries using one of the fulltext
    * enabled field, but TODO LATER add it as a "@fulltext" field of a dynamic mixin to the
    * native model. */
   public static final String FULLTEXT_FIELD_NAME = "_ft";

   private String name; // TODO also longName / displayName (i18n'd ??), description (/ help), sampleValues list ?
   /** string, boolean, int, float, long, double, date, map, list, resource, i18n ; geoloc ???
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
   /** set on a POST/PUT Resource if not provided */
   private Object defaultValue = null;
   /** name of aliased field if any (or storage entity field if differs from name),
    * if null same as name, if empty means soft computed therefore not queriable */
   private LinkedHashSet<String> aliasedStorageNames = null;
   /** caches [name] */
   private LinkedHashSet<String> defaultAliasedStorageNames = null;
   /** means is filled by another field with aliasedStorageName, to avoid both being not in sync ;
    * meaning has a single aliasedStorageName (where it is read from), unless it is computed live */
   private boolean readonly = false;
   /** only valid for "string" fields (possibly within i18n fields) */
   private boolean fulltext = false;
   
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
   
   public DCField(String name, String type, boolean required, int queryLimit,
         String singleAliasedStorageName, boolean readonly) {
      this(name, type, required, queryLimit, singleAliasedStorageName);
      this.readonly = readonly;
   }
   
   public DCField(String name, String type, boolean required, int queryLimit, LinkedHashSet<String> aliasedStorageNames) {
      this(name, type, required, queryLimit);
      this.aliasedStorageNames = aliasedStorageNames;
   }
   
   public DCField(String name, String type, boolean required, int queryLimit, String singleAliasedStorageName) {
      this(name, type, required, queryLimit);
      this.setSingleAliasedStorageName(singleAliasedStorageName);
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
      this.setName(name); // inits defaultAliasedStorageNames
      this.type = type;
   }
   /** NB. if there can be defaultValue then the field is not required ! */
   public DCField(String name, String type, Object defaultValue, int queryLimit) {
      this(name, type);
      this.defaultValue = defaultValue;
      this.queryLimit = queryLimit;
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
      this.setName(name); // inits defaultAliasedStorageNames
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
   /** LATER could prevent from using it in a query criteria at all, for now queryLimit <= 0 */
   public boolean isNotQueriable() {
      return queryLimit <= 0;
   }
   /** whether to create an index, for now queryLimit > 0 */
   public boolean isIndexed() {
      return queryLimit > 0;
   }
   public Object getDefaultValue() {
      return defaultValue;
   }
   public LinkedHashSet<String> getAliasedStorageNames() {
      return aliasedStorageNames;
   }
   /**
    * @return aliasedStorageNames if not null (if empty means soft computed therefore not stored),
    * else name
    */
   public LinkedHashSet<String> getStorageNames() {
      return aliasedStorageNames == null ? defaultAliasedStorageNames : aliasedStorageNames;
   }
   /**
    * @return first of aliasedStorageNames if not null nor empty
    * (if empty means soft computed therefore not queriable), else name
    */
   public String getStorageReadName() {
      return aliasedStorageNames == null ? name
            : aliasedStorageNames.isEmpty() ? null // i.e. soft computed therefore not queriable
                  : aliasedStorageNames.iterator().next();
   }
   
   /**
    * TODO better (ObjectMapper ??)
    */
   @Override
   public String toString() {
      return name + "(" + type + ")";
   }

   
   ///////////////////////////////////////
   // update methods

   public void setName(String name) throws IllegalArgumentException {
      if (name.equals(FULLTEXT_FIELD_NAME)) {
         throw new IllegalArgumentException("Field names are forbidden to be " + FULLTEXT_FIELD_NAME);
      }
      this.name = name;
      this.defaultAliasedStorageNames = new LinkedHashSet<String>(2);
      this.defaultAliasedStorageNames.add(name);
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
   
   public void setDefaultValue(Object defaultValue) {
      this.defaultValue = defaultValue;
   }

   public void setAliasedStorageNames(LinkedHashSet<String> aliasedStorageNames) {
      this.aliasedStorageNames = aliasedStorageNames;
   }

   /**
    * 
    * @param singleAliasedStorageName null means empty i.e. not stored (soft computed)
    */
   public void setSingleAliasedStorageName(String singleAliasedStorageName) {
      this.aliasedStorageNames = new LinkedHashSet<String>(2);
      if (singleAliasedStorageName != null) {
         this.aliasedStorageNames.add(singleAliasedStorageName);
      } else {// else not stored (soft computed)
      }
   }

   public boolean isReadonly() {
      return readonly;
   }

   public void setReadonly(boolean readonly) {
      this.readonly = readonly;
   }

   public boolean isFulltext() {
      return fulltext;
   }

   public void setFulltext(boolean fulltext) {
      this.fulltext = fulltext;
   }
  
}
