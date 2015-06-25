package org.oasis.datacore.core.meta;

import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

/**
 * To be able to build mainly model URIs in -core
 * TODO merge with -server UriService, -api UriHelper ?
 * Static methods (& public fields) to allow use in ex. DCProject
 * @author mdutoo
 *
 */
//@Service("uriService") // defined in XML so that can be override by inheriting service
public class SimpleUriService {
   
   protected static final Logger logger = LoggerFactory.getLogger(SimpleUriService.class);

   /**
    * Unique per container, defines it. To be able to build a full uri
    * (for GET, DELETE, possibly to build missing or custom / relative URI...) */
   @Value("${datacoreApiServer.containerUrl}")
   protected String containerUrlString; // "http://" + "data.oasis-eu.org"
   protected static String staticContainerUrlString; // "http://" + "data.oasis-eu.org"
   /** cache, built in init() */
   protected static URI containerUrl; // "http://" + "data.oasis-eu.org"


   /**
    * 
    * @throws URISyntaxException let it explode if bad containerUrl
    */
   @PostConstruct
   protected void init() throws URISyntaxException {
      SimpleUriService.containerUrl = new URI(containerUrlString);
      SimpleUriService.staticContainerUrlString = this.containerUrlString;
   }
   

   /**
    * TODO don't normalize ?
    * @param containerUrl if null, conf'd default
    * @param modelType
    * @param id
    * @return escaped ex. http://data.oasis-eu.org/dc/type/geo:CityGroup_0/FR/CC%20les%20Ch%C3%A2teaux
    * and to get an URI, new URI(returned)
    */
   public static String buildUri(URI containerUrl, String modelType, String id) {
      if (containerUrl == null) {
         containerUrl = SimpleUriService.containerUrl;
      }
      String path = "/dc/type/" + modelType + '/' + id;
      // ex. (decoded) path = "/dc/type/geo:CityGroup_0/FR/CC les Châteaux"
      try {
         URI escapedUri = new URI(containerUrl.getScheme(), null,
               containerUrl.getHost(), containerUrl.getPort(), path, null, null).normalize();
         // ex. escapedUri = http://data.oasis-eu.org/dc/type/geo:CityGroup_0/FR/CC%20les%20Châteaux
         return escapedUri.toASCIIString();
         // and to get an URI, new URI(escapedUri.toASCIIString()), else UTF-8 ex. â not encoded
         // ex. http://data.oasis-eu.org/dc/type/geo:CityGroup_0/FR/CC%20les%20Ch%C3%A2teaux
      } catch (URISyntaxException usex) {
         // can't happen since containerUrl & this.containerUrl are nice URIs
         logger.error("Error building URI", usex);
         return "bad uri";
      }
   }

   public static String buildUri(String modelType, String id) {
      return buildUri(SimpleUriService.containerUrl, modelType, id);
   }

   public static String buildModelUri(String modelName) {
      return buildUri(SimpleUriService.containerUrl, "dcmo:model_0", modelName);
   }
   
   public static URI getContainerUrl() {
      return SimpleUriService.containerUrl;
   }
   
   public static String getContainerUrlString() {
      return SimpleUriService.staticContainerUrlString;
   }

}
