package org.oasis.datacore.rest.client;

public class QueryParameter {
   
   /** field path */
   private String name;
   /** operatorValueSort */
   private String value;
   
   public QueryParameter(String name, String value) {
      this.name = name;
      this.value = value;
   }
   
   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }
   public String getValue() {
      return value;
   }
   public void setValue(String value) {
      this.value = value;
   }

}
