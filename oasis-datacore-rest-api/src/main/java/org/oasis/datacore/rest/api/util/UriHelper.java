package org.oasis.datacore.rest.api.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;
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

   /** used to split id in order to encode its path elements if it's not disabled */
   public static final String URL_PATH_SEPARATOR = "/";
   public static final String URL_SAFE_CHARACTERS_REGEX = "0-9a-zA-Z\\$\\-_\\.\\+!\\*'\\(\\)";
   /** IRI rule, other characters must be encoded. NB. : are required for ex. prefixed field names */
   public static final String NOT_URL_SAFE_PATH_SEGMENT_OR_SLASH_CHARACTERS_REGEX = "[^"
         + URL_SAFE_CHARACTERS_REGEX + ":@~&,;=/]";
   /** model name & type best practice rule, other characters are forbidden */
   public static final String NOT_URL_ALWAYS_SAFE_OR_COLON_CHARACTERS_REGEX =
         "[^0-9a-zA-Z\\$\\-_\\.\\(\\)\\:]"; // not reserved +!*, not '

   /** type index in dc/type */
   public static final int typeIndexInType = DatacoreApi.DC_TYPE_PATH.length() -1; // TODO use Pattern & DC_TYPE_PATH

   /** to detect whether relative (rather than absolute) uri
    groups are delimited by () see http://stackoverflow.com/questions/6865377/java-regex-capture-group
    URI scheme : see http://stackoverflow.com/questions/3641722/valid-characters-for-uri-schemes */
   private static final Pattern anyBaseUrlPattern = Pattern.compile("^([a-zA-Z][a-zA-Z0-9\\.\\-\\+]*)://[^/]+"); // TODO or "^http[s]?://data\\.oasis-eu\\.org/" ?
   private static final Pattern multiSlashPattern = Pattern.compile("/+");
   private static final Pattern frontSlashesPattern = Pattern.compile("^/*");
   private static final Pattern notUrlAlwaysSafeCharactersPattern = Pattern.compile(NOT_URL_ALWAYS_SAFE_OR_COLON_CHARACTERS_REGEX);
   private static final Pattern notIriSafeCharactersPattern = Pattern.compile(NOT_URL_SAFE_PATH_SEGMENT_OR_SLASH_CHARACTERS_REGEX);
   
   @SuppressWarnings("serial")
   private static Set<String> allowedProtocolSet = new HashSet<String>() {{
      add("http");
      add("https");
   }};

   /**
    * @deprecated rather pass a java.net.URI containerUrl
    * Only for client, not used by server. Does new DCURI(...).toString()
    * and also checks syntax & normalizes URL.
    * @param containerUrl ex. http://data.oasis-eu.org/dc/type
    * @param modelType must not contain URL_ALWAYS_SAFE_CHARACTERS (best practice)
    * @param id (NB. unencoded, characters outside URL_SAFE_PATH_SEGMENT_CHARACTERS will be encoded)
    * @return
    * @throws URISyntaxException 
    * @throws IllegalArgumentException if bad URI or bad practice modelType
    */
   public static String buildUri(String containerUrl, String modelType, String id) throws URISyntaxException {
      return UriHelper.buildUri(new URI(containerUrl), modelType, id, false);
   }
   /**
    * Only for client, not used by server. Does new DCURI(...).toString()
    * and also checks syntax & normalizes URL.
    * @param containerUrl ex. http://data.oasis-eu.org/dc/type
    * @param modelType must not contain URL_ALWAYS_SAFE_CHARACTERS (best practice)
    * @param id (NB. unencoded, characters outside URL_SAFE_PATH_SEGMENT_CHARACTERS will be encoded)
    * @return
    * @throws URISyntaxException 
    * @throws IllegalArgumentException if bad URI or bad practice modelType
    */
   public static String buildUri(URI containerUrl, String modelType, String id) {
      return UriHelper.buildUri(containerUrl, modelType, id, false);
   }
   /**
    * Only for client, not used by server. Does new DCURI(...).toString()
    * and also checks syntax & normalizes URL.
    * @param containerUrl ex. http://data.oasis-eu.org/dc/type
    * @param modelType must not contain URL_ALWAYS_SAFE_CHARACTERS (best practice)
    * @param id (NB. unencoded, characters outside URL_SAFE_PATH_SEGMENT_CHARACTERS will be encoded,
    * save if dontEncodePathElements
    * @param dontEncodePathElements
    * @return
    * @throws URISyntaxException 
    * @throws IllegalArgumentException if bad URI or bad practice modelType
    */
   public static String buildUri(URI containerUrl, String modelType, String id, boolean dontEncodePathElements) {
      // TODO checkContainerUrl(containerUrl);
      checkModelType(modelType);
      DCURI dcUri = new DCURI(containerUrl, modelType, id, false, false, false);
      if (dontEncodePathElements) {
         return dcUri.toUnencodedString();
      }
      /*if (!dontEncodePathElements) {
         try {
            id = slashPattern.splitAsStream(id).map(pathElt -> URLEncoder.encode(pathElt, "UTF-8"))
                  .collect(Collectors.joining(URL_PATH_SEPARATOR));
         } c   atch (UnsupportedEncodingException e) {
            // never happens for UTF-8
         }
      }*/
      /*try {
         // encode & cannonicalize
         // TODO reuse getUriNormalizedContainerAndPathWithoutSlash's code...
         ///return new URL(uri).toURI().normalize().toString();
         return new URI(containerUrl.getScheme(), null, containerUrl.getHost(), containerUrl.getPort(),
               dcUri.getPath(), null, null).normalize().toString(); // NB. toURL() would only check if absolute
      } catch (URISyntaxException e) {
         throw new IllegalArgumentException("Bad URL : " + dcUri.toString(), e);
      }*/
      return dcUri.toString();
   }
   
   /**
    * No check for now.
    * NB. checking for URL_ALWAYS_SAFE_CHARACTERS only when not
    * country / language specific is done rather in ModelResourceMappingService
    * @param modelType must not contain URL_ALWAYS_SAFE_CHARACTERS (best practice)
    * @throws IllegalArgumentException otherwise
    */
   public static void checkModelType(String modelType) {
      /*
      if (notUrlAlwaysSafeCharactersPattern.matcher(modelType).find()) {
         throw new IllegalArgumentException("Model type does not follow best practice rule "
               + " of not containing URL always safe or colon characters i.e. "
               + NOT_URL_ALWAYS_SAFE_OR_COLON_CHARACTERS_REGEX);
      }*/
   }
   /**
    * Used by ModelResourceMappingService to check model type when not country / language specific
    * @param modelType
    * @return
    */
   public static boolean hasUrlAlwaysSafeCharacters(String modelType) {
      return !notUrlAlwaysSafeCharactersPattern.matcher(modelType).find();
   }
   
   /**
    * @param iri
    * @return whether has any characters outside URL_SAFE_PATH_SEGMENT_AND_SLASH_CHARACTERS
    * (not forbidden, but have to be encoded)
    */
   public static boolean hasNoIriForbiddenCharacters(String iri) {
      return notIriSafeCharactersPattern.matcher(iri).find();
   }

   /**
    * TODO replace by merely using URI to parse !
    * LATER optimize with containerUrl cache...
    * Adapts the given URI (URL) to the given container.
    * In given (endpoint URL) URI, replaces container URL part by the given one.
    * @param endpointUri encoded ex. http://localhost:8080/dc/type/city.sample.country/France
    * @param containerUrl must NOT have ending slash ; encoded
    * @return ex.http://data.oasis-eu.org/dc/type/city.sample.country/France
    * @throws URISyntaxException 
    * @throws MalformedURLException 
    */
   public static String toContainerUrl(String endpointUri, String containerUrl)
         throws MalformedURLException, URISyntaxException {
      String[] containerAndPathWithoutSlash = getUriNormalizedContainerAndPathWithoutSlash(
            endpointUri, containerUrl, true, true);
      String urlPathWithoutSlash = containerAndPathWithoutSlash[1];
      return containerUrl + URL_PATH_SEPARATOR + urlPathWithoutSlash;
   }
   
   /**
    * TODO replace by merely using URI to parse !
    * LATER optimize with containerUrl cache...
    * Parses any URI, even external
    * @param uri encoded
    * @param containerUrl used only for relative URIs, which require it ; encoded
    * @return
    * @throws MalformedURLException
    * @throws URISyntaxException
    */
   public static DCURI parseUri(String uri, String containerUrl) throws MalformedURLException, URISyntaxException {
      String[] containerAndPathWithoutSlash = getUriNormalizedContainerAndPathWithoutSlash(
            uri, null, true, false);
      String urlContainer = containerAndPathWithoutSlash[0];

      boolean isRelativeUri = urlContainer == null || urlContainer.isEmpty();
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
         String externalId = containerAndPathWithoutSlash[1];
         try {
            // decoding path remainder :
            externalId = URLDecoder.decode(externalId, "UTF-8");
         } catch (UnsupportedEncodingException e) {
            // never happens for UTF-8
         }
         return new DCURI(urlContainer, null, externalId, false, false, true);
      }
      
      String[] iri = parseIri(containerAndPathWithoutSlash[1]);
      return new DCURI(urlContainer, iri[0], iri[1], isRelativeUri, false, false);
   }
   /**
    * Parses any URI, even external ; shortcut to parseUri(uri, null).
    * @param uri encoded
    * @return
    * @throws MalformedURLException
    * @throws URISyntaxException
    */
   public static DCURI parseUri(String uri) throws MalformedURLException, URISyntaxException {
      return parseUri(uri, null);
   }
   
   /**
    * To be called after a previous getUriNormalizedContainerAndPathWithoutSlash
    * to complete URI parsing. Does URL decode.
    * @param urlPathWithoutSlash (encoded) iri to parse (NB. characters outside
    * URL_SAFE_PATH_SEGMENT_CHARACTERS will be decoded)
    * @return an array with modelType and (decoded) id
    * @throws IllegalArgumentException if bad practice modelType (contains characters
    * other than URL_ALWAYS_SAFE_CHARACTERS)
    */
   public static String[] parseIri(String urlPathWithoutSlash) {
      int idSlashIndex = urlPathWithoutSlash.indexOf('/', typeIndexInType); // TODO use Pattern & DC_TYPE_PATH
      if (idSlashIndex == -1) {
         return new String[] { null, urlPathWithoutSlash };
      }
      String modelType = urlPathWithoutSlash.substring(typeIndexInType, idSlashIndex);
      try {
         // decoding path element :
         modelType = URLDecoder.decode(modelType, "UTF-8");
      } catch (UnsupportedEncodingException e) {
         // never happens for UTF-8
      }
      checkModelType(modelType);
      String id = urlPathWithoutSlash.substring(idSlashIndex + 1); // TODO useful ??
      try {
         // decoding path remainder :
         id = URLDecoder.decode(id, "UTF-8");
      } catch (UnsupportedEncodingException e) {
         // never happens for UTF-8
      }
      return new String[] { modelType, id };
   }
   
   /**
    * TODO replace by merely using URI to parse !
    * LATER optimize with containerUrl cache...
    * If normalizeUrlMode, checks it is an URI and if absolute checks it is an URL
    * with http or https protocol
    * Else if matchBaseUrlMode, uses pattern matching to split and normalize if
    * absolute checks it has an http or https protocol
    * Else merely splits at the given containerUrl's length.
    * Does not URL decode.
    * @param stringUriValue (encoded) absolute ex. http://data.oasis-eu.org/dc//type/sample.marka.field//1
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
         URI uriValue = new URI(stringUriValue).normalize(); // unencodes ; from ex. http://localhost:8180//dc/type//country/UK
         if (uriValue.isAbsolute()) {
            /*URL urlValue = uriValue.toURL(); // also checks protocol
            if (!isHttpOrS(urlValue.getProtocol())) {
               throw new MalformedURLException("Datacore URIs should be HTTP(S)");
            }*/
            if (!allowedProtocolSet.contains(uriValue.getScheme())) {
               throw new MalformedURLException("Datacore URIs should be HTTP(S) but is " + uriValue.getScheme());
            }
         }
         uriBaseUrl = new URI(uriValue.getScheme(), null, uriValue.getHost(), uriValue.getPort(),
               null, null, null).toString(); // rather than substring, because stringUriValue
         urlPathWithoutSlash = uriValue.toString().substring(uriBaseUrl.length());
         // and not as follows else would already be decoded :
         //urlPathWithoutSlash = uriValue.getPath(); // NB. unencoded !!
         if (urlPathWithoutSlash.length() != 0 && urlPathWithoutSlash.charAt(0) == '/') {
            urlPathWithoutSlash = urlPathWithoutSlash.substring(1); // ex. dc/type/country/UK
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
            urlPathWithoutSlash = frontSlashesPattern.matcher(urlPathWithoutSlash).replaceAll("");
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
            urlPathWithoutSlash = frontSlashesPattern.matcher(urlPathWithoutSlash).replaceAll("");
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
