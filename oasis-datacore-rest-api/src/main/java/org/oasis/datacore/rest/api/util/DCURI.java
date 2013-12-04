package org.oasis.datacore.rest.api.util;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.oasis.datacore.rest.api.DatacoreApi;


/**
 * Helper to build URIs (with UriHelper), but does not belong to model.
 * NB. beware, there's another different DCURI in -core's entity package. TODO merge them ??
 * TODO (type-relative) id or iri (but does not contain type) ? 
 * 
 * Datacore Resource URI, for now also works as Social Graph Resource URI.
 * TODO LATER refactor SCURI out of it for Social Graph ?
 * 
 * WARNING not thread safe (save if rendered at init)
 * 
 * @author mdutoo
 *
 */
public class DCURI {

   /** Container base URL ex. http://data.oasis-eu.org/ . Protocol is assumed to be HTTP
    * (if HTTPS, there must be a redirection) */
   private String container;
   /** Base Model type ex. "city.sample.city".
    * Corresponds to a type of use and a data governance configuration.
    * For Social Graph ex. user (account), organization */
   // TODO alternate extending type SCURI with ex. "account" instead
   private String type;
   /** ID / IRI ex. Lyon, London, Torino. Can't change (save by data migration operations).
    * For Social Graph ex. email */
   private String id;

   private String cachedStringUri = null;
   
   
   public DCURI() {
      
   }
   /** TODO also without container & type ? or handled somewhere else ?? */
   public DCURI(String container, String type, String id) {
      this.container = container;
      this.type = type;
      this.id = id;
   }
   public static DCURI fromUri(String uri) throws MalformedURLException, URISyntaxException {
      return UriHelper.parseURI(uri);
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
      sb.append(container); // ex. http://data.oasis-eu.org/
      sb.append(DatacoreApi.DC_TYPE_PATH); // NB. no front slash because in baseUrl
      sb.append('/');
      sb.append(type); // ex. "city.sample.city"
      sb.append('/');
      sb.append(id); // ex. "London", "Lyon"...
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
