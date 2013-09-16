package org.oasis.datacore.api;

import java.util.HashMap;


/**
 * TODO DCObject<T> with binding to the local type T ???
 * 
 * TODO default / common props name constants :
 * uri, type, 
 * 
 */
public class DCObject extends HashMap<String,Object> {

   /** TODO requires (session) cache & its management */
   private Object businessObject;

   public DCObject() {
      
   }
   
   /** clone constructor */
   public DCObject(DCObject dcObject) {
      if (dcObject != null) {
         putAll(dcObject);
      }
   }

   /*public Object get(String key) {
      return null;
   }*/
   /** useful ? TODO others ?? */
   public String getString(String key) {
      // TODO also check if consistent with metamodel ??
      return String.class.cast(get(key));
   }

   /** TODO requires cache & its management */
   public void setBusinessObject(Object businessObject) {
      this.businessObject = businessObject;
   }
   public Object getBusinessObject() {
      return businessObject;
   }

   /** TODO rather (rdf:)_uri ?? */
   public String getUri() {
      return getString("uri");
   }
   /** TODO rather (rdf:)_type ?? */
   public String getType() {
      return getString("type");
   }

}
