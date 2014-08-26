package org.oasis.datacore.rest.server.resource;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.server.BadUriException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * On server side, use this instead of UriHelper.
 * 
 * @author mdutoo
 *
 */
@Component
public class UriService {
   
   /** Base URL of this endpoint. If broker mode enabled, used to detect when to use it.. */
   @Value("${datacoreApiServer.baseUrl}") 
   private String baseUrl; // "http://" + "data-lyon-1.oasis-eu.org" + "/"
   /** Unique per container, defines it. To be able to build a full uri
    * (for GET, DELETE, possibly to build missing or custom / relative URI...) */
   @Value("${datacoreApiServer.containerUrl}")
   private String containerUrl; // "http://" + "data.oasis-eu.org" + "/"
   /** Known (others or all) Datacore containers (comma-separated) */
   @Value("${datacoreApiServer.knownDatacoreContainerUrls}")
   private String knownDatacoreContainerUrls;
   /** Known (others or all) Datacore containers */
   //@Value("#{datacoreApiServer.knownDatacoreContainerUrlSet}") // NO looks up in datacoreApiServer object
   private Set<String> knownDatacoreContainerUrlSet = null;


   @PostConstruct
   public void init() {
      knownDatacoreContainerUrlSet = StringUtils.commaDelimitedListToSet(knownDatacoreContainerUrls);
   }
   
   
   public String getBaseUrl() {
      return baseUrl;
   }

   public String getContainerUrl() {
      return containerUrl;
   }

   /**
    * Builds a URI (for this Datacore) ; NB. to build an external URI use new DCURI(...) directly.
    * Shortcut to new DCURI(...).toString().
    * @param modelType
    * @param id
    * @return
    */
   public String buildUri(String modelType, String id) {
      return new DCURI(this.containerUrl, modelType, id).toString();
   }

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
    * @param modelType if any should already have been checked and should be same as uri's if not relative
    * @param normalizeUrlMode
    * @param replaceBaseUrlMode
    * @return
    * @throws BadUriException if missing (null), malformed, syntax,
    * Datacore URI's and given modelType don't match
    */
   public DCURI normalizeAdaptCheckTypeOfUri(String stringUri, String modelType,
         boolean normalizeUrlMode, boolean matchBaseUrlMode) throws BadUriException {
      if (stringUri == null || stringUri.length() == 0) {
         // TODO LATER2 accept empty uri and build it according to model type (governance)
         // for now, don't support it :
         throw new BadUriException();
      }
      String[] containerAndPathWithoutSlash;
      try {
         containerAndPathWithoutSlash = UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
            stringUri, this.containerUrl, normalizeUrlMode, matchBaseUrlMode);
      } catch (URISyntaxException usex) {
         throw new BadUriException("Bad URI syntax", stringUri, usex);
      } catch (MalformedURLException muex) {
         throw new BadUriException("Malformed URL", stringUri, muex);
      }
      String urlContainer = containerAndPathWithoutSlash[0];
      
      boolean isRelativeUri = urlContainer == null;
      boolean isExternalDatacoreUri = false;
      ///boolean isExternalWebUri = false;
      if (isRelativeUri) {
         urlContainer = this.containerUrl;
         // TODO LATER OPT also accept local type-relative uri (type-less iri) ???
      } else if (!this.containerUrl.equals(urlContainer)) {
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
      if (modelType != null) {
         if (uriType == null) {
            uriType = modelType;
         } else if (!modelType.equals(uriType)) {
            //if (!modelService.hasType(refEntity, ((DCResourceField) dcField).getTypeConstraints())) {
            throw new BadUriException("URI resource model type " + uriType
                  + " does not match provided expected one " + modelType, stringUri);
         }
      } else if (uriType == null) {
         throw new BadUriException("At least one of expected or provided URI "
               + "resource model type must be provided", stringUri);
      }
      return new DCURI(urlContainer, uriType, iri[1], isRelativeUri, isExternalDatacoreUri, false);
   }
   
}
