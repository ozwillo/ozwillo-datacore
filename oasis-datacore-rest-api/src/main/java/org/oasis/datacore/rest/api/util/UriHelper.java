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
    * Only for client, not used by server. Does new DCURI(...).toString()
    * and also checks syntax & normalizes URL.
    * @param containerUrl
    * @param modelType
    * @param iri
    * @return
    */
   public static String buildUri(String containerUrl, String modelType, String iri) {
      String uri = new DCURI(containerUrl, modelType, iri, false, false, false).toString();
      try {
         // cannonicalize
         // TODO reuse getUriNormalizedContainerAndPathWithoutSlash's code...
         return new URL(uri).toURI().normalize().toString();
      } catch (MalformedURLException | URISyntaxException e) {
         throw new IllegalArgumentException("Bad URL : " + uri, e);
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
   
   /**
    * Parses any URI, even external
    * @param uri
    * @param containerUrl used only for relative URIs, which require it
    * @return
    * @throws MalformedURLException
    * @throws URISyntaxException
    */
   public static DCURI parseUri(String uri, String containerUrl) throws MalformedURLException, URISyntaxException {
      String[] containerAndPathWithoutSlash = getUriNormalizedContainerAndPathWithoutSlash(
            uri, null, true, false);
      String urlContainer = containerAndPathWithoutSlash[0];

      boolean isRelativeUri = urlContainer == null;
      ///boolean isExternalDatacoreUri = false;
      ///boolean isExternalWebUri = false;
      if (isRelativeUri) {
         if (containerUrl == null) {
            throw new MalformedURLException("Can't parse URI because is relative "
                  + "and no provided container URL " + uri);
         }
         urlContainer = containerUrl;
         // TODO LATER OPT also accept local type-relative uri (type-less iri) ???
      } else if (containerUrl != null && !containerUrl.equals(urlContainer)) {
         // external resource (another Datacore or any Web resource) :
         // NB. can't know if it's another Datacore
         // so Web resource :
         ///isExternalWebUri = true;
         // doesn't know how to parse modelType out of containerAndPathWithoutSlash
         // TODO LATER knownWebContainerUrlSet allowing such parsing & auth schemes
         // therefore return :
         return new DCURI(urlContainer, null, containerAndPathWithoutSlash[1], false, false, true);
      }
      
      String[] iri = parseIri(containerAndPathWithoutSlash[1]);
      return new DCURI(urlContainer, iri[0], iri[1], isRelativeUri, false, false);
   }
   /**
    * Parses any URI, even external ; shortcut to parseUri(uri, null)
    * @param uri
    * @return
    * @throws MalformedURLException
    * @throws URISyntaxException
    */
   public static DCURI parseUri(String uri) throws MalformedURLException, URISyntaxException {
      return parseUri(uri, null);
   }
   
   /**
    * To be called after a previous getUriNormalizedContainerAndPathWithoutSlash
    * to complete URI parsing.
    * @param containerUrl used to build parsed DCURI
    * @param urlPathWithoutSlash
    * @return
    */
   public static String[] parseIri(String urlPathWithoutSlash) {
      int idSlashIndex = urlPathWithoutSlash.indexOf('/', typeIndexInType); // TODO use Pattern & DC_TYPE_PATH
      if (idSlashIndex == -1) {
         return new String[] { null, urlPathWithoutSlash };
      }
      String modelType = urlPathWithoutSlash.substring(typeIndexInType, idSlashIndex);
      String id = urlPathWithoutSlash.substring(idSlashIndex + 1); // TODO useful ??
      return new String[] { modelType, id };
   }
   
   /**
    * If normalizeUrlMode, checks it is an URI and if absolute checks it is an URL
    * with http or https protocol
    * Else if matchBaseUrlMode, uses pattern matching to split and normalize if
    * absolute checks it has an http or https protocol
    * Else merely splits at the given containerUrl's length
    * @param stringUriValue absolute ex. http://data.oasis-eu.org/dc//type/sample.marka.field//1
    * or relative (then uses provided containerUrl) ex. type//sample.marka.field///1
    * @param containerUrl used only for default mode or if given URI is relative,
    * not used to check absolute URL
    * @param normalizeUrlMode
    * @param matchBaseUrlMode
    * @return the given Datacore URI's container URL (i.e. base URL, or null if URI is relative)
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
         if (urlPathWithoutSlash == null) {
            urlPathWithoutSlash = ""; // TODO constant
         }
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
         }
         
         // else no parsed uriBaseUrl (so possibly no leading slash)
         //uriBaseUrl = containerUrl; // DON'T set default, let caller decide (ex. rather baseUrl)
         
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
            // no parsed uriBaseUrl
            //uriBaseUrl = containerUrl; // DON'T set default, let caller decide (ex. rather baseUrl)
            
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
         // default, "exact container" mode :
         // fastest, assumes that URI starts exactly with containerUrl ending by single slash
         // (no relative URL or https vs http or additional slash)
         urlPathWithoutSlash = stringUriValue.substring(containerUrl.length());
         uriBaseUrl = containerUrl; // stringUriValue.substring(0, containerUrlLength);
      }
      return new String[] {uriBaseUrl, urlPathWithoutSlash };
   }
   
   private static boolean isHttpOrS(String protocol) {
      protocol = protocol.toLowerCase();
      return protocol != null && protocol.startsWith("http")
            && (protocol.length() == 4 || protocol.length() == 5 && protocol.charAt(4) == 's');
   }
   
}
