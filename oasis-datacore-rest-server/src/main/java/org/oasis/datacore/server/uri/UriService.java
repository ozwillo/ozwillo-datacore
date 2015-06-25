package org.oasis.datacore.server.uri;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.oasis.datacore.core.meta.SimpleUriService;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

/**
 * On server side, use this instead of UriHelper.
 * 
 * @author mdutoo
 *
 */
//@Service("uriService") // defined in XML so that can be override by inheriting service
public class UriService extends SimpleUriService {
   
   private final Logger logger = LoggerFactory.getLogger(getClass());
         
   /** Base URL of this endpoint. If broker mode enabled, used to detect when to use it.. */
   @Value("${datacoreApiServer.baseUrl}") 
   private String baseUrl; // "http://" + "data-lyon-1.oasis-eu.org" + "/"
   /** Known (others or all) Datacore containers (comma-separated) */
   @Value("${datacoreApiServer.knownDatacoreContainerUrls}")
   private String knownDatacoreContainerUrls;
   /** Known (others or all) Datacore containers */
   //@Value("#{datacoreApiServer.knownDatacoreContainerUrlSet}") // NO looks up in datacoreApiServer object
   private Set<String> knownDatacoreContainerUrlStringSet = null;
   private Set<URI> knownDatacoreContainerUrlSet = null;


   @PostConstruct
   protected void init() throws URISyntaxException {
      knownDatacoreContainerUrlStringSet = StringUtils.commaDelimitedListToSet(knownDatacoreContainerUrls);
      knownDatacoreContainerUrlSet = (Set<URI>) knownDatacoreContainerUrlStringSet.stream()
            .map(stringUrl -> {
               try {
                  return new URI(stringUrl);
               } catch (URISyntaxException urisex) {
                  logger.error("a knownDatacoreContainerUrl has bad URI syntax, skipping", urisex);
                  return null;
               }
            }).filter(url -> url != null).collect(Collectors.toSet());
      
      try {
         containerUrl = new URI(containerUrlString);
      } catch (URISyntaxException urisex) {
         logger.error("FATAL ERROR containerUrl has bad URI syntax, can't work !", urisex);
         throw urisex;
      }
   }
   
   
   public String getBaseUrl() {
      return baseUrl;
   }
   
   /*
    * Builds a URI (for this Datacore) ; NB. to build an external URI use new DCURI(...) directly.
    * Shortcut to new DCURI(...).toString().
    * @param modelType
    * @param id
    * @return
    */
   /*public String buildUri(String modelType, String id) {
      return UriHelper.buildUri(containerUrl, modelType, id);
   }*/

   /**
    * Parses any URI (Datacore or external)
    * @param uri
    * @return
    * @throws BadUriException if missing (null), malformed, syntax,
    * relative without type, uri's and given modelType don't match
    */
   public DCURI parseUri(String uri) throws BadUriException {
      return normalizeAdaptCheckTypeOfUri(uri, null, false, true);
   }

   /**
    * Parses container, if known (Datacore) parses model type and checks it and parses id.
    * Used
    * * by internalPostDataInType on root posted URI (where type is known and that can be relative)
    * * to check referenced URIs and URIs post/put/patch where type is not known (and are never relative)
    * @param stringUriValue
    * @param expectedModelType if any should already have been checked,
    * if relative is required and used to build URI
    * @param normalizeUrlMode
    * @param replaceBaseUrlMode
    * @return
    * @throws BadUriException if missing (null), malformed, syntax,
    * Datacore URI's and given modelType don't match
    */
   public DCURI normalizeAdaptCheckTypeOfUri(String stringUri, String expectedModelType,
         boolean normalizeUrlMode, boolean matchBaseUrlMode) throws BadUriException {
      return normalizeAdaptCheckTypeOfUri(stringUri, expectedModelType,
            normalizeUrlMode, matchBaseUrlMode, false);
   }
   /**
    * DON'T USE IT allows to ensureExpectedModelType which was historically the case
    * but is incompatible with openly typed links, ex. a (Geo)Ancestor-typed link
    * should allow City as well as Country model types.
    * 
    * Parses container, if known (Datacore) parses model type and checks it and parses id.
    * Used
    * * by internalPostDataInType on root posted URI (where type is known and that can be relative)
    * * to check referenced URIs and URIs post/put/patch where type is not known (and are never relative)
    * @param stringUriValue
    * @param expectedModelType if any should already have been checked and should be same as uri's
    * if matchExpectedModelType and not relative, if relative is required and used to build URI
    * @param normalizeUrlMode
    * @param replaceBaseUrlMode
    * @param matchExpectedModelType 
    * @return
    * @throws BadUriException if missing (null), malformed, syntax,
    * Datacore URI's and given modelType don't match
    */
   public DCURI normalizeAdaptCheckTypeOfUri(String stringUri, String expectedModelType,
         boolean normalizeUrlMode, boolean matchBaseUrlMode, boolean ensureExpectedModelType) throws BadUriException {
      if (stringUri == null || stringUri.length() == 0) {
         // TODO LATER2 accept empty uri and build it according to model type (governance)
         // for now, don't support it :
         throw new BadUriException();
      }
      String[] containerAndPathWithoutSlash;
      String urlContainerString;
      URI urlContainer = null;
      try {
         containerAndPathWithoutSlash = UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
            stringUri, this.containerUrlString, normalizeUrlMode, matchBaseUrlMode);
         urlContainerString = containerAndPathWithoutSlash[0];
         urlContainer = new URI((urlContainerString != null) ? urlContainerString : "");
      } catch (URISyntaxException usex) {
         throw new BadUriException("Bad URI syntax: " + usex.getMessage(), stringUri, usex);
      } catch (MalformedURLException muex) {
         throw new BadUriException("Malformed URL: " + muex.getMessage(), stringUri, muex);
      }
      
      boolean isRelativeUri = !urlContainer.isAbsolute();
      boolean isExternalDatacoreUri = false;
      ///boolean isExternalWebUri = false;
      if (isRelativeUri) {
         urlContainer = SimpleUriService.containerUrl;
         // TODO LATER OPT also accept local type-relative uri (type-less iri) ???
      } else if (!SimpleUriService.containerUrl.equals(urlContainer)) {
         // external resource (another Datacore or any Web resource) :
         // TODO or maybe also allow this endpoint's baseUrl ??
         if (this.knownDatacoreContainerUrlSet.contains(urlContainer)) {
            isExternalDatacoreUri = true;
         } else {
            ///isExternalWebUri = true;
            // doesn't know how to parse modelType out of containerAndPathWithoutSlash
            // TODO LATER knownWebContainerUrlSet allowing such parsing & auth schemes
            // therefore return :
            return new DCURI(urlContainer, null, containerAndPathWithoutSlash[1], false, false, true);
         }
      }
      
      // otherwise absolute uri :
      String urlPathWithoutSlash = containerAndPathWithoutSlash[1];
      ///stringUri = this.containerUrl + urlPathWithoutSlash; // useless
      ///return stringUri;

      // check URI model type : against provided one if any, otherwise against known ones
      String[] iri = UriHelper.parseIri(urlPathWithoutSlash/*, refModel*/); // TODO LATER cached model ref ?! what for ?
      String uriType = iri[0];
      if (expectedModelType != null) {
         if (uriType == null) {
            uriType = expectedModelType;
         } else if (ensureExpectedModelType && !expectedModelType.equals(uriType)) {
            //if (!modelService.hasType(refEntity, ((DCResourceField) dcField).getTypeConstraints())) {
            throw new BadUriException("URI resource model type " + uriType
                  + " does not match provided expected one " + expectedModelType, stringUri);
         }
      } else if (uriType == null) {
         throw new BadUriException("At least one of expected or provided URI "
               + "resource model type must be provided", stringUri);
      }
      return new DCURI(urlContainer, uriType, iri[1], isRelativeUri, isExternalDatacoreUri, false);
   }
   
}
