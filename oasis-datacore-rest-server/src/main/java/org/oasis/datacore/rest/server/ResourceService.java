package org.oasis.datacore.rest.server;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Map;

import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * TODO LATER2 once ModelService & Models are exposed as REST and available on client side,
 * make this code also available on client side
 * 
 * @author mdutoo
 *
 */
@Component // TODO @Service ??
public class ResourceService {

   /** Base URL of this endpoint. If broker mode enabled, used to detect when to use it.. */
   @Value("${datacoreApiServer.baseUrl}") 
   private String baseUrl; // "http://" + "data-lyon-1.oasis-eu.org" + "/"
   /** Unique per container, defines it. To be able to build a full uri
    * (for GET, DELETE, possibly to build missing or custom / relative URI...) */
   @Value("${datacoreApiServer.containerUrl}") 
   private String containerUrl; // "http://" + "data.oasis-eu.org" + "/"

   @Autowired
   private DCModelService modelService;

   public String getBaseUrl() {
      return baseUrl;
   }

   public String getContainerUrl() {
      return containerUrl;
   }
   
   /** helper method to create (fluently) new Resources FOR TESTING */
   public DCResource create(String modelType, String id) {
      DCResource r = DCResource.create(this.containerUrl, modelType, id);
      // init iri field :
      // TODO NO rather using creation (or builder) event hooked behaviours
      /*DCModel model = modelService.getModel(modelType);
      String iriFieldName = model.getIriFieldName();
      if (iriFieldName != null) {
         r.set(iriFieldName, iri);
      }*/
      return r;
   }

   /** helper method to create (build) new Resources FOR TESTING */
   public DCResource build(String type, String id, Map<String,Object> fieldValues) {
      DCResource resource = new DCResource();
      resource.setUri(UriHelper.buildUri(containerUrl, type, id));
      //resource.setVersion(-1l);
      resource.setProperty("type", type);
      for (Map.Entry<String, Object> fieldValueEntry : fieldValues.entrySet()) {
         resource.setProperty(fieldValueEntry.getKey(), fieldValueEntry.getValue());
      }
      return resource;
   }

   public String buildUri(String modelType, String id) {
      return new DCURI(this.containerUrl, modelType, id).toString();
   }

   /**
    * 
    * @param uri
    * @return
    * @throws ResourceParsingException if unknown model
    */
   public DCURI parseUri(String uri) throws ResourceParsingException {
      return normalizeAdaptCheckTypeOfUri(uri, null, false, true);
   }

   /**
    * TODO extract to UriService
    * Used
    * * by internalPostDataInType on root posted URI (where type is known and that can be relative)
    * * to check referenced URIs and URIs post/put/patch where type is not known (and are never relative)
    * @param stringUriValue
    * @param modelType if any should already have been checked and should be same as uri's,
    * else uri's will be checked
    * @param normalizeUrlMode
    * @param replaceBaseUrlMode
    * @return
    * @throws ResourceParsingException
    */
   public DCURI normalizeAdaptCheckTypeOfUri(String stringUri, String modelType,
         boolean normalizeUrlMode, boolean matchBaseUrlMode)
               throws ResourceParsingException {
      if (stringUri == null || stringUri.length() == 0) {
         // TODO LATER2 accept empty uri and build it according to model type (governance)
         // for now, don't support it :
         throw new ResourceParsingException("Missing uri");
      }
      String[] containerAndPathWithoutSlash;
      try {
         containerAndPathWithoutSlash = UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
            stringUri, this.containerUrl, normalizeUrlMode, matchBaseUrlMode);
      } catch (URISyntaxException usex) {
         throw new ResourceParsingException("Bad URI syntax", usex);
      } catch (MalformedURLException muex) {
         throw new ResourceParsingException("Malformed URL", muex);
      }
      String urlContainer = containerAndPathWithoutSlash[0];
      if (urlContainer == null) {
         // accept local type-relative uri (type-less iri) :
         if (modelType == null) {
            throw new ResourceParsingException("URI can't be relative when no target model type is provided");
         }
         return new DCURI(this.containerUrl, modelType, stringUri);
         // TODO LATER accept also iri including type
      }
      
      // otherwise absolute uri :
      String urlPathWithoutSlash = containerAndPathWithoutSlash[1];
      ///stringUri = this.containerUrl + urlPathWithoutSlash; // useless
      ///return stringUri;

      // check URI model type : against provided one if any, otherwise against known ones
      DCURI dcUri = UriHelper.parseURI(
            urlContainer, urlPathWithoutSlash/*, refModel*/); // TODO LATER cached model ref ?! what for ?
      String uriType = dcUri.getType();
      if (modelType != null) {
         if (!modelType.equals(uriType)) {
            throw new ResourceParsingException("URI resource model type " + uriType
                  + " does not match provided target one " + modelType);
         }
      } else {
         DCModel uriModel = modelService.getModel(uriType);
         if (uriModel == null) {
            throw new ResourceParsingException("Can't find resource model type " + uriType);
         }
      }
      return dcUri;
   }
   
}
