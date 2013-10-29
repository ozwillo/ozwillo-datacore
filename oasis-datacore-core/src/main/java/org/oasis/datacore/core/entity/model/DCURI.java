package org.oasis.datacore.core.entity.model;

/**
 * Datacore Resource URI, for now also works as Social Graph Resource URI.
 * TODO LATER refactor SCURI out of it for Social Graph.
 * 
 * TODO direct persistence in DCEntity props (using Converter) ??? as string OR { uri, type } like DBRef ???
 * 
 * TODO not thread safe (save if rendered at init)
 * 
 * @author mdutoo
 *
 */
public class DCURI {
   
   /** NB. no front slash because in baseUrl */
   public static final String API_ROOT_PATH = "dc/type/";

   /** Container base URL ex. http://data.oasis-eu.org/ . Protocol is assumed to be HTTP
    * (if HTTPS, there must be a redirection) */
   private String container;
   /** Base type ex. city. Corresponds to a type of use and a data governance configuration.
    * For Social Graph ex. user (account), organization */
   // TODO alternate extending type SCURI with ex. "account" instead
   private String type;
   /** IRI ex. Lyon, London, Torino. Can't change (save by data migration operations).
    * For Social Graph ex. email */
   private String id;
   // TODO model & entity cache ?!?

   private String cachedStringUri = null;
   
   public DCURI() {
      
   }
   /** TODO also without container & type ? or handled somewhere else ?? */
   public DCURI(String container, String type, String id) {
      this.container = container;
      this.type = type;
      this.id = id;
   }
   
   /**
    * @return [container]dc/type/[type]/id
    */
   @Override
   public String toString() {
      if (cachedStringUri != null) {
         return cachedStringUri;
      }
      StringBuilder sb = new StringBuilder(/*"http://"*/);
      sb.append(container);
      sb.append(API_ROOT_PATH);
      sb.append(type);
      sb.append('/');
      sb.append(id);
      cachedStringUri = sb.toString();
      return cachedStringUri;
   }
   
   public String getContainer() {
      return container;
   }
   public String getType() {
      return type;
   }
   public String getId() {
      return id;
   }
   /*public void setContainer(String container) {
      this.container = container;
      this.cachedStringUri = null;
   }
   public void setType(String type) {
      this.type = type;
      this.cachedStringUri = null;
   }
   public void setId(String id) {
      this.id = id;
      this.cachedStringUri = null;
   }*/
   
}
