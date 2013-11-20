package org.oasis.datacore.core.meta.model;


/**
 * TODO readonly : setters only for tests, LATER admin using another (inheriting) model ?!?
 * don't use this object for List or Maps (use DCListField and DCMapField)
 * @author mdutoo
 *
 */
public class DCField {

   private String name;
   /** string, boolean, int, float, long, double, date, map, list, resource, i18n ? geoloc ???
    * default is string */
   private String type = "string"; // TODO enum ?!?
   /** Is a non-null value required ? defaults to false */
   private boolean required = false;
   /** size of results allowed when querying on this field
    * if > 0, field will be indexed (sparse if !required)
    * defaults to 0 */
   private int queryLimit = 0;

   public DCField() {
      
   }
   public DCField(String name, String type, boolean required, int queryLimit) {
      this.name = name;
      this.type = type;
      this.required = required;
      this.queryLimit = queryLimit;
   }
   public DCField(String name, String type) {
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
