package org.oasis.datacore.rest.api.util;

import java.net.URI;
import java.net.URISyntaxException;

import org.oasis.datacore.rest.api.DatacoreApi;


/**
 * Helper to build URIs (along with UriHelper), but does not belong to model.
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
   private boolean isRelativeUri;
   private boolean isExternalDatacoreUri;
   private boolean isExternalWebUri;
   private boolean isExternalUri;

   private String cachedStringUri = null;
   private URI cachedUri = null;
   
   
   public DCURI() {
      
   }
   /**
    * Creates a new URI (Datacore or external)
    * @param container must not be null
    * @param type null for (unknown) external Web URI 
    * @param id
    * @param isRelativeUri
    * @param isExternalDatacoreUri
    * @param isExternalWebUri
    */
   public DCURI(String container, String type, String id,
         boolean isRelativeUri, boolean isExternalDatacoreUri, boolean isExternalWebUri) {
      this.container = container;
      this.type = type;
      this.id = id;
      this.isRelativeUri = isRelativeUri;
      this.isExternalDatacoreUri = isExternalDatacoreUri;
      this.isExternalWebUri = isExternalWebUri;
      this.isExternalUri = this.isExternalDatacoreUri || this.isExternalWebUri;
   }
   /**
    * Creates a new Datacore URI
    * @param container must not be null
    * @param type null for (unknown) external Web URI 
    * @param id
    * @param isRelativeUri
    * @param isExternalDatacoreUri
    * @param isExternalWebUri
    */
   public DCURI(String container, String type, String id) {
      this(container, type, id, false, false, false);
   }

   public URI toURI() throws URISyntaxException {
      if (cachedUri == null) {
         cachedUri = new URI(toString());
      }
      return cachedUri;
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
      if (type != null) {
         sb.append(DatacoreApi.DC_TYPE_PATH); // NB. no front slash because in baseUrl
         sb.append('/');
         sb.append(type); // ex. "city.sample.city"
         sb.append('/');
      } // else external URI : type not known because not parsable
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
   public boolean isRelativeUri() {
      return isRelativeUri;
   }
   public boolean isExternalDatacoreUri() {
      return isExternalDatacoreUri;
   }
   public boolean isExternalWebUri() {
      return isExternalWebUri;
   }
   public boolean isExternalUri() {
      return isExternalUri;
   }
   
}
