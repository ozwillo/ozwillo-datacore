package org.oasis.datacore.core.meta;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
   protected String containerUrlString; // "http://" + "data.ozwillo.com"
   protected static String staticContainerUrlString; // "http://" + "data.ozwillo.com"
   /** cache, built in init() */
   protected static URI containerUrl; // "http://" + "data.ozwillo.com"


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
    * SAME AS DCURI
    * TODO don't normalize ?
    * @param containerUrl if null, conf'd default
    * @param modelType
    * @param id
    * @return escaped ex. http://data.ozwillo.com/dc/type/geo:CityGroup_0/FR/CC%20les%20Ch%C3%A2teaux
    * SAVE IF "//" in id (ex. itself an URI) in which case id is fully URL encoded
    * ex. http://data.ozwillo.com/dc/type/photo:Library_0/https%3A%2F%2Fwww.10thingstosee.com%2Fmedia%2Fphotos%2Ffrance-778943_HjRL4GM.jpg
    * and to get an URI, new URI(returned)
    */
   public static String buildUri(URI containerUrl, String modelType, String id) {
      if (containerUrl == null) {
         containerUrl = SimpleUriService.containerUrl;
      }
      
      String path = "/dc/type/" + modelType;
      if (id.contains("//")) {
         try {
            URI escapedModelTypeUri = new URI(containerUrl.getScheme(), null,
                  containerUrl.getHost(), containerUrl.getPort(), path, null, null).normalize();
            // ex. escapedUri = http://data.ozwillo.com/dc/type/geo:CityGroup_0/FR/CC%20les%20Châteaux
            String escapedModelTypeUriString = escapedModelTypeUri.toASCIIString();
            // and to get an URI, new URI(escapedUri.toASCIIString()), else UTF-8 ex. â not encoded
            // ex. http://data.ozwillo.com/dc/type/geo:CityGroup_0/FR/CC%20les%20Ch%C3%A2teaux
            return escapedModelTypeUriString + '/' + URLEncoder.encode(id, StandardCharsets.UTF_8.name());
         } catch (URISyntaxException usex) {
            // can't happen since containerUrl & this.containerUrl are nice URIs
            logger.error("Error building URI", usex);
            return "bad uri";
         } catch (UnsupportedEncodingException ueex) {
            // can't happen
            logger.error("Error building URI", ueex);
            return "bad uri";
         }
      }
      
      path += '/' + id;
      // ex. (decoded) path = "/dc/type/geo:CityGroup_0/FR/CC les Châteaux"
      try {
         URI escapedUri = new URI(containerUrl.getScheme(), null,
               containerUrl.getHost(), containerUrl.getPort(), path, null, null).normalize();
         // ex. escapedUri = http://data.ozwillo.com/dc/type/geo:CityGroup_0/FR/CC%20les%20Châteaux
         return escapedUri.toASCIIString();
         // and to get an URI, new URI(escapedUri.toASCIIString()), else UTF-8 ex. â not encoded
         // ex. http://data.ozwillo.com/dc/type/geo:CityGroup_0/FR/CC%20les%20Ch%C3%A2teaux
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
