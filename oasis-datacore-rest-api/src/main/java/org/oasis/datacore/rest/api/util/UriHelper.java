package org.oasis.datacore.rest.api.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.oasis.datacore.rest.api.DatacoreApi;


/**
 * TODO or in UriService ? BUT also required on client side !
 * 
 * @author mdutoo
 *
 */
public class UriHelper {

   public static String DC_TYPE_PREFIX = DatacoreApi.DC_TYPE_PATH + "/"; // TODO better from JAXRS annotations using reflection ?
   
   /** to detect whether relative (rather than absolute) uri
    groups are delimited by () see http://stackoverflow.com/questions/6865377/java-regex-capture-group
    URI scheme : see http://stackoverflow.com/questions/3641722/valid-characters-for-uri-schemes */
   private static Pattern anyBaseUrlPattern = Pattern.compile("^([a-zA-Z][a-zA-Z0-9\\.\\-\\+]*)://[^/]+/"); // TODO or "^http[s]?://data\\.oasis-eu\\.org/" ?
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
    * If normalizeUrlMode, checks it is an URI and if absolute checks it is an URL
    * with http or https protocol
    * Else if matchBaseUrlMode, uses pattern matching to split and normalize and if
    * absolute checks it has an http or https protocol
    * @param stringUriValue ex.http://data.oasis-eu.org/dc//type/sample.marka.field//1
    * @param normalizeUrlMode
    * @param matchBaseUrlMode
    * @return the given Datacore URI's container URL (i.e. base URL, null if URI is relative)
    * and URL Path without slash
    * ex. [ "http://data.oasis-eu.org/", "dc/type/sample.marka.field/1" ]
    * @throws ResourceParsingException
    */
   public static String[] getUriNormalizedContainerAndPathWithoutSlash(String stringUriValue,
         String containerUrl, boolean normalizeUrlMode, boolean matchBaseUrlMode)
               throws URISyntaxException, MalformedURLException {
      String uriBaseUrl = null;
      String urlPathWithoutSlash = null;

      if (normalizeUrlMode) {
         // NB. Datacore URIs should ALSO be URLs
         URI uriValue = new URI(stringUriValue).normalize(); // from ex. http://localhost:8180//dc/type//country/UK
         urlPathWithoutSlash = uriValue.getPath();
         if (urlPathWithoutSlash.length() != 0 && urlPathWithoutSlash.charAt(0) == '/') {
            urlPathWithoutSlash = urlPathWithoutSlash.substring(1); // ex. dc/type/country/UK
         }
         if (uriValue.isAbsolute()) {
            URL urlValue = uriValue.toURL(); // also checks protocol
            if (!isHttpOrS(urlValue.getProtocol())) {
               throw new MalformedURLException("Datacore URIs should be HTTP(S)");
            }
            stringUriValue = urlValue.toString(); // ex. http://localhost:8180/dc/type/country/UK
            uriBaseUrl = stringUriValue.substring(0, stringUriValue.length()
                  - urlPathWithoutSlash.length()); // includes end slash
         } // else no (null) uriBaseUrl (so possibly no leading slash)
         
      } else if (matchBaseUrlMode) {
         // checking that URI is an HTTP(S) one
         Matcher replaceBaseUrlMatcher = anyBaseUrlPattern.matcher(stringUriValue); // ex. http://data.oasis-eu.org/dc//type/sample.marka.field//1
         if (!replaceBaseUrlMatcher.find()) {
            // maybe a relative URI
            /*if (normalizeUrlMode) {
               URI uriValue = new URI(stringUriValue).normalize(); // from ex. http://localhost:8180//dc/type//country/UK
               if (uriValue.isAbsolute()) {
                  throw new MalformedURLException("Datacore URIs should be HTTP(S) (i.e. respect pattern "
                        + anyBaseUrlPattern.pattern());
               }
            }*/
            urlPathWithoutSlash = multiSlashPattern.matcher(stringUriValue).replaceAll("/"); // ex. dc/type/sample.marka.field/1
            // no (null) uriBaseUrl
            
         } else {
            // building uriBaseUrl & checking protocol
            String protocol = replaceBaseUrlMatcher.group(1); // group is delimited by ()
            if (!isHttpOrS(protocol)) {
               throw new MalformedURLException("Datacore URIs should be HTTP(S)");
            }
            uriBaseUrl = replaceBaseUrlMatcher.group(0); // full match, includes end slash, ex. http://data.oasis-eu.org/
            
            // building urlPathWithoutSlash, see http://www.tutorialspoint.com/java/java_string_replacefirst.htm
            StringBuffer sbuf = new StringBuffer();
            replaceBaseUrlMatcher.appendReplacement(sbuf, "");
            replaceBaseUrlMatcher.appendTail(sbuf);
            urlPathWithoutSlash = sbuf.toString(); // ex. dc//type/sample.marka.field//1
            
            // replacing multi slash (i.e. normalizing)
            urlPathWithoutSlash = multiSlashPattern.matcher(urlPathWithoutSlash).replaceAll("/"); // ex. dc/type/sample.marka.field/1
         }
         
      } else {
         // default mode : fastest, assume that URI starts exactly with containerUrl
         // (no https vs http or additional slash)
         urlPathWithoutSlash = stringUriValue.substring(containerUrl.length());
         uriBaseUrl = containerUrl;
      }
      return new String[] {uriBaseUrl, urlPathWithoutSlash };
   }
   
   private static boolean isHttpOrS(String protocol) {
      protocol = protocol.toLowerCase();
      return protocol != null && protocol.startsWith("http")
            && (protocol.length() == 4 || protocol.length() == 5 && protocol.charAt(4) == 's');
   }
   
}
