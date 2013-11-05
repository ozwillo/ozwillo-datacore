package org.oasis.datacore.rest.api.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * TODO or in UriService ? BUT also required on client side !
 * 
 * @author mdutoo
 *
 */
public class UriHelper {

   public static String DC_TYPE_PREFIX = "dc/type/"; // TODO better from JAXRS annotations using reflection ?
   
   /** to detect whether relative (rather than absolute) uri */
   ///private static Pattern absoluteUriStartProtocolPattern = Pattern.compile("^http[s]?");
   // or absoluteUrlProtocolHostPattern ?
   private static Pattern anyBaseUrlPattern = Pattern.compile("^http[s]?://[^/]+/"); // TODO or "^http[s]?://data\\.oasis-eu\\.org/" ?
   private static Pattern multiSlashPattern = Pattern.compile("/+");
   

   /**
    * Only for client, not used by server
    * @param containerUrl
    * @param modelType
    * @param iri
    * @return
    */
   public static String buildUri(String containerUrl, String modelType, String iri) {
      String uri = containerUrl + DC_TYPE_PREFIX + modelType + "/" + iri;
      try {
         // cannonicalize
         // TODO reuse getUriNormalizedContainerAndPathWithoutSlash's code
         return new URL(uri).toURI().normalize().toString();
      } catch (MalformedURLException | URISyntaxException e) {
         throw new RuntimeException("Bad URL : " + uri);
      }
   }
   
   /**
    * 
    * @param endpointUri
    * @param containerUrl must have ending slash
    * @return
    * @throws URISyntaxException 
    * @throws MalformedURLException 
    */
   public static String toContainerUrl(String endpointUri, String containerUrl)
         throws MalformedURLException, URISyntaxException {
      String[] containerAndPathWithoutSlash = getUriNormalizedContainerAndPathWithoutSlash(
            endpointUri, containerUrl, true, true);
      String urlPathWithoutSlash = containerAndPathWithoutSlash[1];
      return containerUrl + urlPathWithoutSlash;
   }
   
   /**
    * @param stringUriValue
    * @param normalizeUrlMode
    * @param matchBaseUrlMode
    * @return the given Datacore URI's base URL and URL Path without slash  
    * @throws ResourceParsingException
    */
   public static String[] getUriNormalizedContainerAndPathWithoutSlash(String stringUriValue,
         String containerUrl, boolean normalizeUrlMode, boolean matchBaseUrlMode)
               throws URISyntaxException, MalformedURLException {
      String uriBaseUrl = null;
      String urlPathWithoutSlash = null;

      if (matchBaseUrlMode) {
         Matcher replaceBaseUrlMatcher = anyBaseUrlPattern.matcher(stringUriValue);
         urlPathWithoutSlash = replaceBaseUrlMatcher.replaceFirst("");
         urlPathWithoutSlash = multiSlashPattern.matcher(urlPathWithoutSlash).replaceAll("/");
         if (!replaceBaseUrlMatcher.hitEnd()) {
            int uriBaseUrlWithSlashLength = replaceBaseUrlMatcher.end();
            uriBaseUrl = stringUriValue.substring(0, uriBaseUrlWithSlashLength); // includes end slash
         } // else no (null) uriBaseUrl
         
      } else if (normalizeUrlMode) {
         // NB. Datacore URIs should ALSO be URLs
         URI uriValue = new URI(stringUriValue).normalize(); // from ex. http://localhost:8180//dc/type//country/UK
         urlPathWithoutSlash = uriValue.getPath().substring(1); // ex. dc/type/country/UK
         if (uriValue.isAbsolute()) {
            URL urlValue = uriValue.toURL(); // also checks protocol
            stringUriValue = urlValue.toString(); // ex. http://localhost:8180/dc/type/country/UK
            uriBaseUrl = stringUriValue.substring(0, stringUriValue.length()
                  - urlPathWithoutSlash.length()); // includes end slash
         } // else no (null) uriBaseUrl
         
      } else {
         // default mode : fastest, assume that URI starts exactly with containerUrl
         // (no https vs http or additional slash)
         urlPathWithoutSlash = stringUriValue.substring(containerUrl.length());
         uriBaseUrl = containerUrl;
      }
      return new String[] {uriBaseUrl, urlPathWithoutSlash };
   }
   
}
