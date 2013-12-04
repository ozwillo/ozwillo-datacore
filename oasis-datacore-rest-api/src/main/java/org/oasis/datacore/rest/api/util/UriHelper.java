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

   public static int typeIndexInType = DatacoreApi.DC_TYPE_PATH.length() + 1; // TODO use Pattern & DC_TYPE_PATH
   
   /** to detect whether relative (rather than absolute) uri
    groups are delimited by () see http://stackoverflow.com/questions/6865377/java-regex-capture-group
    URI scheme : see http://stackoverflow.com/questions/3641722/valid-characters-for-uri-schemes */
   private static Pattern anyBaseUrlPattern = Pattern.compile("^([a-zA-Z][a-zA-Z0-9\\.\\-\\+]*)://[^/]+/"); // TODO or "^http[s]?://data\\.oasis-eu\\.org/" ?
   private static Pattern multiSlashPattern = Pattern.compile("/+");
   

   /**
    * Only for client, not used by server ; does new DCURI().toString()
    * and also checks syntax & normalizes URL.
    * @param containerUrl
    * @param modelType
    * @param iri
    * @return
    */
   public static String buildUri(String containerUrl, String modelType, String iri) {
      String uri = new DCURI(containerUrl, modelType, iri).toString();
      try {
         // cannonicalize
         // TODO reuse getUriNormalizedContainerAndPathWithoutSlash's code...
         return new URL(uri).toURI().normalize().toString();
      } catch (MalformedURLException | URISyntaxException e) {
         throw new RuntimeException("Bad URL : " + uri);
      }
   }
   
   /**
    * Adapts the given URI (URL) to the given container.
    * In given (endpoint URL) URI, replaces container URL part by the given one.
    * @param endpointUri ex. http://localhost:8080/dc/type/city.sample.country/France
    * @param containerUrl must have ending slash
    * @return ex.http://data.oasis-eu.org/dc/type/city.sample.country/France
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
   
   public static DCURI parseURI(String uri) throws MalformedURLException, URISyntaxException {
      String[] containerAndPathWithoutSlash = getUriNormalizedContainerAndPathWithoutSlash(
            uri, null, true, false);
      String urlContainer = containerAndPathWithoutSlash[0];
      if (urlContainer == null) {
         // happens when URI is relative, which it should not be
         throw new MalformedURLException("Can't parse URI because is relative " + uri);
      }
      
      return parseURI(containerAndPathWithoutSlash[0], containerAndPathWithoutSlash[1]);
   }
   
   public static DCURI parseURI(String containerUrl, String urlPathWithoutSlash) {
      int iriSlashIndex = urlPathWithoutSlash .indexOf('/', typeIndexInType); // TODO use Pattern & DC_TYPE_PATH
      String modelType = urlPathWithoutSlash.substring(typeIndexInType, iriSlashIndex);
      String iri = urlPathWithoutSlash.substring(iriSlashIndex + 1); // TODO useful ??
      return new DCURI(containerUrl, modelType, iri);
   }
   
   /**
    * If normalizeUrlMode, checks it is an URI and if absolute checks it is an URL
    * with http or https protocol
    * Else if matchBaseUrlMode, uses pattern matching to split and normalize if
    * absolute checks it has an http or https protocol
    * Else merely splits at the given containerUrl's length
    * @param stringUriValue ex.http://data.oasis-eu.org/dc//type/sample.marka.field//1
    * @param containerUrl used only for default mode or if given URI is relative
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
         } else {
            // no parsed uriBaseUrl (so possibly no leading slash), defaults to given one
            uriBaseUrl = containerUrl;
         }
         
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
            // no parsed uriBaseUrl, defaults to given one
            uriBaseUrl = containerUrl;
            
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
