package org.oasis.datacore.rest.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.oasis.datacore.core.context.DatacoreRequestContextService;
import org.oasis.datacore.core.entity.EntityQueryService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.query.QueryException;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryService;
import org.oasis.datacore.core.meta.ModelException;
import org.oasis.datacore.core.meta.ModelNotFoundException;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.historization.exception.HistorizationException;
import org.oasis.datacore.historization.service.HistorizationService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.api.util.JaxrsExceptionHelper;
import org.oasis.datacore.rest.server.cxf.CxfJaxrsApiProvider;
import org.oasis.datacore.rest.server.cxf.JaxrsServerBase;
import org.oasis.datacore.rest.server.resource.ExternalResourceException;
import org.oasis.datacore.rest.server.resource.ResourceEntityMapperService;
import org.oasis.datacore.rest.server.resource.ResourceException;
import org.oasis.datacore.rest.server.resource.ResourceNotFoundException;
import org.oasis.datacore.rest.server.resource.ResourceObsoleteException;
import org.oasis.datacore.rest.server.resource.ResourceService;
import org.oasis.datacore.rest.server.resource.ResourceTypeNotFoundException;
import org.oasis.datacore.server.uri.BadUriException;
import org.oasis.datacore.server.uri.UriService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;


/**
 * Returned HTTP status : follows recommended standards, see
 * http://stackoverflow.com/questions/2342579/http-status-code-for-update-and-delete/18981344#18981344
 * 
 * Specific HTTP Status (and custom response content) are returned by throwing
 * WebApplicationException subclasses, rather than having ugly Response-typed returns in
 * all operations. Only drawback is more logs, which is mitigated by custom CXF
 * Datacore FaultListener. An alternative would have been to inject HttpServletResponse
 * and set its status : response.setStatus(Response.Status.CREATED.getStatusCode()).
 * 
 * TODOs :
 * * error messages as constants ? or i18n'd ???
 * 
 * @author mdutoo
 *
 */
@Path("dc") // relative path among other OASIS services
/*@Consumes(MediaType.APPLICATION_JSON) // TODO finer media type ex. +oasis-datacore ??
@Produces(MediaType.APPLICATION_JSON)*/
@Component("datacoreApiImpl") // else can't autowire Qualified ; NOT @Service (permissions rather around EntityService)
public class DatacoreApiImpl extends JaxrsServerBase implements DatacoreApi {

   private static final Logger logger = LoggerFactory.getLogger(DatacoreApiImpl.class);

   private boolean strictPostMode = false;

   
   @Autowired
   private ResourceService resourceService;
   @Autowired
   private UriService uriService;
   @Autowired
   private ResourceEntityMapperService resourceEntityMapperService;
   
   @Autowired
   private LdpEntityQueryService ldpEntityQueryService;
   @Autowired
   @Qualifier("datacore.entityQueryService")
   private EntityQueryService entityQueryService;
   
   @Autowired
   private DCModelService modelService;
   
   @Autowired
   private HistorizationService historizationService;

   @Autowired
   private CxfJaxrsApiProvider cxfJaxrsApiProvider;
   /** to access debug switch & put its explained results */
   @Autowired
   protected DatacoreRequestContextService serverRequestContext;

   
   public DCResource postDataInType(DCResource resource, String modelType) {
      DCResource postedResource = this.internalPostDataInType(resource, modelType,
            true, !this.strictPostMode, false);
      
      //return dcData; // rather 201 Created with ETag header :
      String httpEntity = postedResource.getVersion().toString(); // no need of additional uri because only for THIS resource
      EntityTag eTag = new EntityTag(httpEntity);
      Status status = (postedResource.getVersion() == 0) ? Response.Status.CREATED : Response.Status.OK;
      throw new WebApplicationException(Response.status(status)
            .tag(eTag) // .lastModified(dataEntity.getLastModified().toDate())
            .entity(postedResource)
            .type(cxfJaxrsApiProvider.getNegotiatedResponseMediaType()).build());
   }

   /**
    * Does the actual work of postDataInType except returning status & etag,
    * so it can also be used in post/putAllData(InType).
    * TODO LATER on ResourceObsoleteException rather 412 Precondition Failed (requires checking client-sent If-Match header)
    * @param resource
    * @param modelType
    * @param canCreate
    * @param canUpdate
    * @return
    */
   public DCResource internalPostDataInType(DCResource resource, String modelType,
         boolean canCreate, boolean canUpdate, boolean putRatherThanPatchMode) {
      try {
         return resourceService.createOrUpdate(resource, modelType,
               canCreate, canUpdate, putRatherThanPatchMode);
         
      } catch (ResourceTypeNotFoundException rtnfex) {
         throw new NotFoundException(toNotFoundJsonResponse(rtnfex));
         
      } catch (ExternalResourceException erex) {
         String msg;
         if (erex.getUri().isExternalDatacoreUri()) {
            // TODO LATER OPT true broker / proxy mode ?
            msg = "Resource URI has known external Datacore container, "
                  + "this Datacore can't handle it, but doing a POST/PUT rather there should work.";
         } else { // ere.getUri().isExternalWebUri())
            msg = "Resource has external unkown container, this Datacore can't handle it, "
                  + "see rather there how to do a POST/PUT (if it can).";
         } // TODO LATER OPT known external Web container ?
         throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
               .entity(msg + "\n   Cause :\n" + JaxrsExceptionHelper.toString(erex))
               .type(MediaType.TEXT_PLAIN).build());
         
      } catch (ResourceNotFoundException rnfex) {
         throw new NotFoundException(toNotFoundJsonResponse(rnfex));
         
      } catch (ResourceObsoleteException roex) {
         throw new ClientErrorException(toConflictJsonResponse(roex));
         // TODO LATER rather 412 Precondition Failed (requires checking client-sent If-Match header)
         
      } catch (BadUriException buex) {
         throw new BadRequestException(toBadRequestJsonResponse(
               new ResourceException(buex.getMessage(), resource, modelService.getProject())));
         
      } catch (ResourceException rex) {
         throw new BadRequestException(toBadRequestJsonResponse(rex));
      } catch (DuplicateKeyException dkex) {
         throw new BadRequestException(toBadRequestJsonResponse(
               new ResourceException(dkex.getMessage(), resource, modelService.getProject())));
      }
   }
   
   public List<DCResource> postAllDataInType(List<DCResource> resources, String modelType) {
      List<DCResource> res = this.internalPostAllDataInType(resources, modelType,
            true, !this.strictPostMode, false);
      throw new WebApplicationException(Response.status(Response.Status.CREATED)
            ///.tag(new EntityTag(httpEntity)) // .lastModified(dataEntity.getLastModified().toDate())
            .entity(res).type(cxfJaxrsApiProvider.getNegotiatedResponseMediaType()).build());
   }
   public List<DCResource> internalPostAllDataInType(List<DCResource> resources, String modelType,
         boolean canCreate, boolean canUpdate, boolean putRatherThanPatchMode) {
      if (resources == null || resources.isEmpty()) {
         throw new WebApplicationException(Response.status(Response.Status.NO_CONTENT)
               .type(cxfJaxrsApiProvider.getNegotiatedResponseMediaType()).build());
      }
      ArrayList<DCResource> res = new ArrayList<DCResource>(resources.size());
      for (DCResource resource : resources) {
         // TODO LATER rather put exceptions in context to allow always checking multiple POSTed resources
         res.add(internalPostDataInType(resource, modelType, canCreate, canUpdate, putRatherThanPatchMode));
         // NB. ETag validation is not supported because will be checked at db save time anyway
         // (and to support multiple POSTs, would have to be complex or use
         // the given dataResource.version instead)
         // Complex means validate ETag as hash of concatenation of all ordered versions :
         ///httpEntity += resource.getVersion().toString() + ',';
         // rather than pass request, here or in a CXF ResponseHandler (or interceptor but closer to JAXRS 2)
         // (see http://cxf.apache.org/docs/jax-rs-filters.html) by getting result with
         // outMessage.getContent(List.class), see ServiceInvokerInterceptor.handleMessage() l.78
         // and also put complex code to handle concatenated ETag on client side))
      }
      return res;
   }
   public List<DCResource> postAllData(List<DCResource> resources) {
      List<DCResource> res = this.internalPostAllData(resources, true, !this.strictPostMode, false);
      throw new WebApplicationException(Response.status(Response.Status.CREATED)
            ///.tag(new EntityTag(httpEntity)) // .lastModified(dataEntity.getLastModified().toDate())
            .entity(res)
            .type(cxfJaxrsApiProvider.getNegotiatedResponseMediaType()).build());
   }
   public List<DCResource> internalPostAllData(List<DCResource> resources,
         boolean canCreate, boolean canUpdate, boolean putRatherThanPatchMode) {
      if (resources == null || resources.isEmpty()) {
         throw new WebApplicationException(Response.status(Response.Status.NO_CONTENT)
               .type(cxfJaxrsApiProvider.getNegotiatedResponseMediaType()).build());
      }
      
      boolean replaceBaseUrlMode = true;
      boolean normalizeUrlMode = true;
      
      ArrayList<DCResource> res = new ArrayList<DCResource>(resources.size());
      for (DCResource resource : resources) {
         DCURI uri;
         try {
            uri = uriService.normalizeAdaptCheckTypeOfUri(resource.getUri(), null, 
                  normalizeUrlMode, replaceBaseUrlMode);
         } catch (BadUriException buex) {
            // TODO LATER rather context & for multi post
            throw new BadRequestException(toBadRequestJsonResponse(
                  new ResourceException(buex.getMessage(), resource, modelService.getProject())));
         }
         res.add(internalPostDataInType(resource, uri.getType(), canCreate, canUpdate, putRatherThanPatchMode));
         // NB. no ETag validation support, see discussion in internalPostAllDataInType()
      }
      return res;
   }
   
   public DCResource putDataInType(DCResource resource, String modelType, String iri) {
      // NB. no ETag validation support (same as POST), see discussion in internalPostAllDataInType()
      return internalPostDataInType(resource, modelType, false, true, true);
   }
   public List<DCResource> putAllDataInType(List<DCResource> resources, String modelType) {
      // NB. no ETag validation support (same as POST), see discussion in internalPostAllDataInType()
      return internalPostAllDataInType(resources, modelType, false, true, true);
   }
   public List<DCResource> putAllData(List<DCResource> resources) {
      // NB. no ETag validation support (same as POST), see discussion in internalPostAllDataInType()
      return internalPostAllData(resources, false, true, true);
   }
   
   public DCResource getData(String modelType, String iri, Long version) {
      String uri = uriService.buildUri(modelType, iri);

      ///Long version = getVersionFromHeader(HttpHeaders.IF_NONE_MATCH, uri);

      DCResource resource;
      boolean versionIsUpToDate;
      String httpEntityTag;
      try {
         resource = resourceService.getIfVersionDiffers(uri, modelType, version);
         versionIsUpToDate = resource == null;
         httpEntityTag = versionIsUpToDate ? String.valueOf(version) :
            resource.getVersion().toString(); // no need of additional uri because only for THIS resource
         
      } catch (ResourceTypeNotFoundException rtnfex) {
         throw new NotFoundException(toNotFoundJsonResponse(rtnfex));
         
      // NB. no ExternalResourceException because URI is HTTP URL which can't be external !
      // therefore no RedirectException to throw because of it
         
      } catch (ResourceNotFoundException rnfex) {
         throw new NotFoundException(toNotFoundJsonResponse(rnfex));
         // rather than NO_CONTENT ; like Atol ex. deleteApplication in
         // https://github.com/pole-numerique/oasis/blob/master/oasis-webapp/src/main/java/oasis/web/apps/ApplicationDirectoryResource.java
      } catch (ResourceException rex) { // asked to abort from within triggered event
         throw new BadRequestException(toBadRequestJsonResponse(rex));
      }

      // ETag caching :
      // on GET, If-None-Match precondition else return 304 Not Modified http://stackoverflow.com/questions/2021882/is-my-implementation-of-http-conditional-get-answers-in-php-is-ok/2049723#2049723
      // see for jaxrs https://devcenter.heroku.com/articles/jax-rs-http-caching#conditional-cache-headers
      // and for http http://odino.org/don-t-rape-http-if-none-match-the-412-http-status-code/
      // also http://stackoverflow.com/questions/2085411/how-to-use-cxf-jax-rs-and-http-caching
      
      // Doing it manually :
      ResponseBuilder builder;
      if (versionIsUpToDate) {
         builder = Response.notModified(httpEntityTag);
      } else {
         // (if provided) If-None-Match precondition OK (resource did change), so serve updated content
         builder = Response.ok(resource).tag(httpEntityTag); // .lastModified(dataEntity.getLastModified().toDate())
      }
      
      /*
      // (alternative) Doing it using JAXRS helper :
      EntityTag eTag = new EntityTag(httpEntityTag);
      //ResponseBuilder builder = request.evaluatePreconditions(dataEntity.getUpdated(), eTag);
      builder = (request != null) ?
            request.evaluatePreconditions(eTag) : null; // else not deployed as jaxrs
      versionIsUpToDate = (builder != null);
      
      if (!versionIsUpToDate) {
         // (if provided) If-None-Match precondition OK (resource did change), so serve updated content
         ////DCResource resource = entityToResource(entity);
         builder = Response.ok(resource).tag(eTag); // .lastModified(dataEntity.getLastModified().toDate())
      } // else provided If-None-Match precondition KO (resource didn't change),
      // so return 304 Not Modified, prepared by JAXRS Request helper (and don't send the dcData back)
      
      // NB. no cache control max age, else HTTP clients won't even send new requests until period ends !
      //CacheControl cc = new CacheControl();
      //cc.setMaxAge(600);
      //builder.cacheControl(cc);
      
      // NB. lastModified would be pretty but not used at HTTP level
      //builder.lastModified(person.getUpdated());
       */
      
      throw new WebApplicationException(builder.build());
   }
   
   public void deleteData(String modelType, String iri, Long version) {
      // TODO LATER also add a "deleted" flag that can be set in POST ?!?
      String uri = uriService.buildUri(modelType, iri);

      ///Long version = getVersionFromHeader(HttpHeaders.IF_MATCH, uri);
      if (version == null) {
         throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
               .entity("Missing " + HttpHeaders.IF_MATCH + "=version header to delete "
         + uri).type(MediaType.TEXT_PLAIN).build());
      }
      
      try {
         resourceService.delete(uri, modelType, version);
         
      } catch (ResourceTypeNotFoundException rtnfex) {
         throw new NotFoundException(toNotFoundJsonResponse(rtnfex));
         
      // NB. no ExternalResourceException because URI is HTTP URL which can't be external !
         
      } catch (ResourceNotFoundException rnfex) {
         throw new NotFoundException(toNotFoundJsonResponse(rnfex));
      } catch (ResourceException rex) { // asked to abort from within triggered event
         throw new BadRequestException(toBadRequestJsonResponse(rex));
      }

      throw new WebApplicationException(Response.status(Response.Status.NO_CONTENT).build());
   }


   /**
    * NOW USELESS, AT LEAST TODO MOVE
    * @param httpHeaders
    * @param etagHeader ex. If-Match or If-None-Match
    * @param uri for logging purpose
    * @return null if none
    * @throws BadRequestException if not a Long
    */
   private Long getVersionFromHeader(String etagHeader, String uri) {
      String versionEtag = httpHeaders.getHeaderString(etagHeader);
      if (versionEtag == null) {
         return null;
      }
      
      try {
         return Long.parseLong(versionEtag);
      } catch (NumberFormatException e) {
         throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
               .entity(etagHeader + " header should be a long to delete " + uri
                     + " but is " + versionEtag).type(MediaType.TEXT_PLAIN).build());
      }
   }

   @Override
   public List<DCResource> updateDataInTypeWhere(DCResource dcDataDiff,
         String modelType, UriInfo uriInfo) {
      throw new InternalServerErrorException(JaxrsExceptionHelper.toNotImplementedResponse());
   }

   @Override
   public void deleteDataInTypeWhere(String modelType, UriInfo uriInfo) {
      throw new InternalServerErrorException(JaxrsExceptionHelper.toNotImplementedResponse());
   }
   
   
   public DCResource postDataInTypeOnGet(String modelType, String method, UriInfo uriInfo) {
      if ("POST".equalsIgnoreCase(method)) {
         DCResource resource = null;
         // TODO impl like find
         return this.postDataInType(resource, modelType);
      } else {
         throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
               .entity("method query parameter should be POST but is "
                     + method).type(MediaType.TEXT_PLAIN).build());
      }
   }
   
   public DCResource putPatchDeleteDataOnGet(String modelType, String iri,
         String method, Long version, UriInfo uriInfo) {
      if ("PUT".equalsIgnoreCase(method)
            || "PATCH".equalsIgnoreCase(method)) {
         DCResource resource = new DCResource();
         // TODO impl like find
         return this.putDataInType(resource, modelType, iri);
      } else if ("DELETE".equalsIgnoreCase(method)) {
         this.deleteData(modelType, iri, version); // will throw an exception to return the result code
         return null; // so will never go there
      } else {
         throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
               .entity("method query parameter should be PUT, PATCH, DELETE but is "
                     + method).type(MediaType.TEXT_PLAIN).build());
      }
   }

   // TODO "native" query (W3C LDP-like) also refactored within a dedicated query engine ??
   public List<DCResource> findDataInType(String modelType, UriInfo uriInfo, Integer start, Integer limit, boolean debug) {
      MultivaluedMap<String, String> params = cxfJaxrsApiProvider.getQueryParameters(); // decodes
      // NB. rather than uriInfo.getQueryParameters(true) because also stores it in context
      // NB. to be able to refactor in LdpNativeQueryServiceImpl, could rather do :
      /// params = JAXRSUtils.getStructuredParams((String)message.get(Message.QUERY_STRING), "&", decode, decode);
      // (as getQueryParameters itself does, or as is done in LdpEntityQueryServiceImpl)
      // by getting query string from either @Context-injected CXF Message or uriInfo.getRequestUri()
      // BUT this would be costlier and the ideal, unified solution is rather having a true object model
      // of parsed queries
      List<DCEntity> foundEntities;
      try {
         foundEntities = ldpEntityQueryService.findDataInType(modelType, params, start, limit);
      } catch (ModelNotFoundException mnfex) {
         throw new NotFoundException(toNotFoundJsonResponse(mnfex));
      } catch (QueryException qex) {
         throw new BadRequestException(JaxrsExceptionHelper.toBadRequestJsonResponse(qex));
         // NB. if warnings returned only if debug / explain mode
      }
      
      List<DCResource> foundDatas = resourceEntityMapperService.entitiesToResources(foundEntities, true); // apply view
      
      if (serverRequestContext.isDebug()) {
         // TODO move that to LdpEntityQueryServiceImpl !
         // NB. supports only json
         throw new WebApplicationException(Response.status(Response.Status.OK)
               .entity(new ImmutableMap.Builder<String, Object>()
                     .put(DEBUG_RESULT, serverRequestContext.getDebug())
                     .put(RESOURCES_RESULT, foundDatas).build())
               .type(MediaType.APPLICATION_JSON).build());
      }
      
      return foundDatas;
   }


   public List<DCResource> findData(UriInfo uriInfo, Integer start, Integer limit) {
      // TODO same as findData, but parse model names in front of field names,
      // get collection names from their type field,
      // and apply limited sort join algo
      return new ArrayList<DCResource>();
   }

   @Override
   public List<DCResource> queryDataInType(String modelType, String query, String language) {
      List<DCEntity> entities;
      try {
         entities = this.entityQueryService.queryInType(modelType, query, language);
      } catch (ModelNotFoundException mnfex) {
         throw new NotFoundException(toNotFoundJsonResponse(mnfex));
      } catch (QueryException qex) {
         throw new BadRequestException(JaxrsExceptionHelper.toBadRequestJsonResponse(qex));
         // TODO if warnings return them as response header ?? or only if failIfWarningsMode ??
         // TODO better support for query parsing errors / warnings / detailedMode & additional
         // non-error behaviours of query engines like "explain" switch
      }
      return resourceEntityMapperService.entitiesToResources(entities, true); // apply view
   }
   @Override
   public List<DCResource> queryData(String query, String language) {
      List<DCEntity> entities;
      try {
         entities = this.entityQueryService.query(query, language);
      } catch (QueryException qex) {
         throw new BadRequestException(JaxrsExceptionHelper.toBadRequestJsonResponse(qex));
         // TODO if warnings return them as response header ?? or only if failIfWarningsMode ??
         // TODO better support for query parsing errors / warnings / detailedMode & additional
         // non-error behaviours of query engines like "explain" switch
      }
      return resourceEntityMapperService.entitiesToResources(entities, true); // apply view
   }

   
   public boolean isStrictPostMode() {
      return strictPostMode;
   }

   public void setStrictPostMode(boolean strictPostMode) {
      this.strictPostMode = strictPostMode;
   }

   	@Override
	public DCResource findHistorizedResource(String modelType, String iri, Integer version) throws BadRequestException, NotFoundException {

		String uri = uriService.buildUri(modelType, iri);
		
		DCModelBase dcModel = modelService.getModelBase(modelType);
		if (dcModel == null) {
         // TODO LATER OPT client side might deem it a data health / governance problem,
         // and put it in the corresponding inbox
         throw new BadRequestException(toBadRequestJsonResponse(
               new ResourceTypeNotFoundException(modelType, "Can't find model type for " + uri
               + ". Maybe it is badly spelled, or it has been deleted or renamed since (only in test). "
               + "In this case, the missing model must first be created again, "
               + "before patching the entity.", null, null, modelService.getProject())));
		}
		modelType = dcModel.getName();
		DCEntity historizedEntity = null;
		try {
			historizedEntity = historizationService.getHistorizedEntity(uri, version, dcModel);
		} catch (HistorizationException hex) {
			throw new BadRequestException(JaxrsExceptionHelper.toBadRequestJsonResponse(hex));
		}
		if (historizedEntity == null) {
			throw new NotFoundException(toNotFoundJsonResponse(
			      new ResourceNotFoundException("Entity with URI : " + uri + " and version : "
			            + version + "was not found", uri, null, modelService.getProject())));
		}

		DCResource resource = resourceEntityMapperService.entityToResource(historizedEntity, null, true); // apply view TODO NO view already applied by Entity PermissionEvaluator
		ResponseBuilder responseBuilder = Response.ok(resource);

		throw new WebApplicationException(responseBuilder.build());

	}
   
   /**
    * To be used inside ClientErrorException, like this :
    * throw new ClientErrorException(toConflictJsonResponse(e)).
    * Inspired by JaxrsExceptionHelper
    * @param t
    * @return
    */
   public static Response toConflictJsonResponse(ResourceException rex) {
      return Response.status(Response.Status.CONFLICT)
            .entity(toJson(rex)).type(MediaType.APPLICATION_JSON).build();
   }
   /**
    * To be used inside BadRequestException, like this :
    * throw new BadRequestException(toBadRequestJsonResponse(e)).
    * Inspired by JaxrsExceptionHelper
    * @param t
    * @return
    */
   public static Response toBadRequestJsonResponse(ResourceException rex) {
      return Response.status(Response.Status.BAD_REQUEST)
            .entity(toJson(rex)).type(MediaType.APPLICATION_JSON).build();
   }
   /**
    * To be used inside NotFoundException, like this :
    * throw new NotFoundException(toNotFoundJsonResponse(e))
    * Inspired by JaxrsExceptionHelper
    * @param t
    * @return
    */
   public static Response toNotFoundJsonResponse(ResourceException rex) {
      return Response.status(Response.Status.NOT_FOUND)
            .entity(toJson(rex)).type(MediaType.APPLICATION_JSON).build();
   }
   private static Object toJson(ResourceException rex) {
      Map<String, Object> exMap = JaxrsExceptionHelper.toJson(rex);
      if (rex.getResource() != null) {
         exMap.put("resource", rex.getResource());
      }
      exMap.put("projectName", rex.getProject().getName());
      return exMap;
   }
   
   /**
    * To be used inside BadRequestException, like this :
    * throw new BadRequestException(toConflictJsonResponse(e)).
    * Inspired by JaxrsExceptionHelper
    * @param t
    * @return
    */
   public static Response toConflictJsonResponse(ModelException mex) {
      return Response.status(Response.Status.CONFLICT)
            .entity(toJson(mex)).type(MediaType.APPLICATION_JSON).build();
   }
   /**
    * To be used inside BadRequestException, like this :
    * throw new BadRequestException(toBadRequestJsonResponse(e)).
    * Inspired by JaxrsExceptionHelper
    * @param t
    * @return
    */
   public static Response toBadRequestJsonResponse(ModelException mex) {
      return Response.status(Response.Status.BAD_REQUEST)
            .entity(toJson(mex)).type(MediaType.APPLICATION_JSON).build();
   }
   /**
    * To be used inside NotFoundException, like this :
    * throw new NotFoundException(toNotFoundJsonResponse(e))
    * Inspired by JaxrsExceptionHelper
    * @param t
    * @return
    */
   public static Response toNotFoundJsonResponse(ModelException mex) {
      return Response.status(Response.Status.NOT_FOUND)
            .entity(toJson(mex)).type(MediaType.APPLICATION_JSON).build();
   }
   private static Object toJson(ModelException mex) {
      Map<String, Object> exMap = JaxrsExceptionHelper.toJson(mex);
      exMap.put("model", mex.getModel()); // OR modelType ; TODO rather as resource ?!
      exMap.put("projectName", mex.getProject().getName());
      return exMap;
   }
   
}
