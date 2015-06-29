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

   /** Container base URL ex. http://data.ozwillo.com/ . Protocol is assumed to be HTTP
    * (if HTTPS, there must be a redirection) */
   private URI containerUrl;
   /** String version of container base URL ex. http://data.ozwillo.com/ . Protocol is assumed to be HTTP
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

   /** encoded */
   private String cachedStringUri = null;
   private String cachedUnencodedStringUri = null;
   /** normalized */
   private URI cachedUri = null;
   /** unencoded ; i.e. /dc/type/[type]/[id] to help working with Java URIs */
   private String cachedPath = null;
   
   
   public DCURI() {
      
   }
   /**
    * Creates a new URI (Datacore or external)
    * @param container must not be null
    * @param type null for (unknown) external Web URI 
    * @param id unencoded
    * @param isRelativeUri
    * @param isExternalDatacoreUri
    * @param isExternalWebUri
    * @throws URISyntaxException 
    */
   public DCURI(String container, String type, String id,
         boolean isRelativeUri, boolean isExternalDatacoreUri, boolean isExternalWebUri) throws URISyntaxException {
      this(new URI(container), type, id, isRelativeUri, isExternalDatacoreUri, isExternalWebUri);
   }
   /**
    * Creates a new URI (Datacore or external)
    * @param container must not be null
    * @param type null for (unknown) external Web URI 
    * @param id unencoded
    * @param isRelativeUri
    * @param isExternalDatacoreUri
    * @param isExternalWebUri
    * @throws URISyntaxException 
    */
   public DCURI(URI containerUrl, String type, String id,
         boolean isRelativeUri, boolean isExternalDatacoreUri, boolean isExternalWebUri) {
      this.containerUrl = containerUrl;
      this.container = containerUrl.toString();
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
    * @param id unencoded
    * @param isRelativeUri
    * @param isExternalDatacoreUri
    * @param isExternalWebUri
    * @throws URISyntaxException 
    */
   public DCURI(String container, String type, String id) throws URISyntaxException {
      this(container, type, id, false, false, false);
   }
   /**
    * Creates a new Datacore URI
    * @param container must not be null
    * @param type null for (unknown) external Web URI 
    * @param id unencoded
    * @param isRelativeUri
    * @param isExternalDatacoreUri
    * @param isExternalWebUri
    * @throws URISyntaxException 
    */
   public DCURI(URI container, String type, String id) {
      this(container, type, id, false, false, false);
   }

   /**
    * 
    * @return unencoded
    */
   public String getPath() {
      if (cachedPath != null) {
         return cachedPath;
      }
      StringBuilder pathSb = new StringBuilder();
      if (type != null) {
         pathSb.append(DatacoreApi.DC_TYPE_PATH); // NB. front slash because none in baseUrl
         pathSb.append(type); // ex. "city.sample.city"
         pathSb.append('/');
      } else {
         // external URI : type not known because not parsable
         pathSb.append('/');
      }
      pathSb.append(id); // ex. "London", "Lyon"...
      cachedPath = pathSb.toString();
      return cachedPath;
   }
   /**
    * cached ; encodes path (including id) using java.net.URI
    * @return
    * @throws URISyntaxException
    */
   public URI toURI() throws URISyntaxException {
      if (cachedUri == null) {
         // ex. (decoded) path = "/dc/type/geo:CityGroup_0/FR/CC les Châteaux"
         URI escapedUri = new URI(containerUrl.getScheme(), null,
            containerUrl.getHost(), containerUrl.getPort(), getPath(), null, null).normalize();
         // ex. escapedUri = http://data.ozwillo.com/dc/type/geo:CityGroup_0/FR/CC%20les%20Châteaux
         cachedUri = new URI(escapedUri.toASCIIString()); // else UTF-8 ex. â not encoded
         // ex. cachedUri = http://data.ozwillo.com/dc/type/geo:CityGroup_0/FR/CC%20les%20Ch%C3%A2teaux
      }
      return cachedUri;
   }
   /**
    * cached ; built on toURI()
    * @return [container]dc/type/[type]/id
    * @throws IllegalArgumentException if bad URI syntax
    */
   @Override
   public String toString() {
      if (cachedStringUri != null) {
         return cachedStringUri;
      }
      try {
         cachedStringUri = toURI().toString();
      } catch (URISyntaxException urisex) {
         throw new IllegalArgumentException("Bad URI syntax ", urisex);
      }
      return cachedStringUri;
   }
   /**
    * cached
    * @return
    */
   public String toUnencodedString() {
      if (cachedUnencodedStringUri != null) {
         return cachedUnencodedStringUri;
      }
      cachedUnencodedStringUri = container + getPath();  // NB. container is ex. http://data.ozwillo.com
      return cachedUnencodedStringUri;
   }
   @Override
   public boolean equals(Object o) {
      if (o == null || !(o instanceof DCURI)) {
         return false;
      }
      return this.toString().equals(o.toString());
   }
   @Override
   public int hashCode() {
      return toString().hashCode();
   }

   public URI getContainerUrl() {
      return containerUrl;
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
