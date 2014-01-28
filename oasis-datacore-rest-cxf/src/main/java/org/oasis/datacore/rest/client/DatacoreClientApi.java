package org.oasis.datacore.rest.client;

import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.DatacoreApi;

import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;


/**
 * WARNING rather use DatacoreCachedClient, because this is a utilitarian API
 * designed to achieve DatacoreCachedClient(Impl) features. For instance,
 * DatacoreClientApi's DELETE and GET operations will still use the client-side
 * cache (which is injected in CXF interceptors), and if cache is not
 * up-to-date these DELETE and GET operations will throw up RedirectException
 * (meaning HTTP 304 Not Modified).
 * 
 * This interface is the client-side point of view on Datacore API and has been
 * designed to allow using all operations using the proxy client paradigm
 * (with the help of some interceptors). It is used by DatacoreCachedClientImpl
 * to provide those in a cached manner (again with the help of some interceptors).
 * 
 * Its implementation is an (ex. CXF) (uncached) proxy client of the server REST API.
 * This means that HTTP 304 Not Modified returns must be handled explicitly, as RedirectException.
 *    
 * Methods are almost-copies from DatacoreApi's ones, comments and Swagger doc included.
 * 
 * @author mdutoo
 *
 */
public interface DatacoreClientApi extends DatacoreApi {
   
   /**
    * TODO NO conflicts with postAllDataInType, rather client helper only ? or in interceptors ???
    * 
    * or createOrUpdate ?
    * 
    * Creates a new DCData, or updates it if it exists.
    * 
    * TODO return only changed fields ? or even nothing at all if no changes ??
    * 
    * TODO exception handling :
    * inject @Context final HttpServletResponse response, and maybe throw new WebApplicationException(response); 
    * 
    * @param dcData
    * @return
    */
   @Path("/type/{type}") // TODO rather than :
   ///@Path("/post/type/{type}")
   @POST
   @ApiOperation(value = "Creates a new data Resource in the given type, or updates it if allowed.",
      notes = "resource URI must be provided but can be relative to type, "
            + "up-to-date version is required to update an existing resource "
            + "(which first requires that strict POST mode is not enabled)",
            response = DCResource.class)
   @ApiResponses(value = {
      @ApiResponse(code = 500, message = "Field parsing errors (format, consistency...)"),
      @ApiResponse(code = 409, message = "Conflict : while trying to update existing "
            + "resource (in non-strict POST mode only), optimistic locking error "
            + "(provided resource version is not up-to-date with the server's latest)"),
      @ApiResponse(code = 400, message = "Bad request : missing content, missing or invalid URI, "
            + "(if enabled) strict POST mode not respected (version provided or resource already exists)"),
      @ApiResponse(code = 404, message = "Type model not found") 
   })
   DCResource postDataInType(
         @ApiParam(value = "Data Resource to create", required = true) DCResource dcData,
         @ApiParam(value = "Model type to create (or update) it in", required = true) @PathParam("type") String modelType
         /*, @Context Request request*/);

   /**
    * See findDataInType(UriInfo)
    * @param type
    * @param queryParams finder criteria : field names, and for each a string containing its
    * operator, value and optional sort criteria. Simple unquoted string value is accepted,
    * but ideally and formally value should be JSON (i.e. int, quoted string, array, map / Object...).
    * QueryParameters will then format them to the encoded form firstname=John&lastname=Doe.
    * NB. it is annotated as a JAXRS @HeaderParam but it is a hack to make it
    * visible from CXF interceptors.
    * @param start
    * @param limit
    * @param sort
    * @return
    */
   @Path("/type/{type}")
   @GET
   List<DCResource> findDataInType(@PathParam("type") String type,
         @HeaderParam(QUERY_PARAMETERS) QueryParameters queryParams, // XXX HACK TODO better OrderedMap ?!
         @DefaultValue("0") @QueryParam("start") Integer start,
         @DefaultValue("10") @QueryParam("limit") Integer limit);
   
   /**
    * See findData(UriInfo)
    * @param queryParams finder criteria : field names, and for each a string containing its
    * operator, value and optional sort criteria. Simple unquoted string value is accepted,
    * but ideally and formally value should be JSON (i.e. int, quoted string, array, map / Object...).
    * QueryParameters will then format them to the encoded form firstname=John&lastname=Doe.
    * NB. it is annotated as a JAXRS @HeaderParam but it is a hack to make it
    * visible from CXF interceptors.
    * @param start
    * @param limit
    * @param sort
    * @return
    */
   @Path("/")
   @GET
   List<DCResource> findData(
         @HeaderParam(QUERY_PARAMETERS) QueryParameters queryParams, // XXX HACK TODO better OrderedMap ?! 
         @DefaultValue("0") @QueryParam("start") Integer start,
         @DefaultValue("10") @QueryParam("limit") Integer limit);


   /**
    * Shortcut to getData(modelType, iri, null, null) i.e. without version
    * which uses client local cache to get version if there is any cached value.
    * 
    * Returns data resource at http://[this container]/dc/[type]/[iri].
    * Supports HTTP ETag (If-Match ; but no If-Modified-Since).
    * @param type
    * @param iri
    * @return
    */
   @Path("/type/{type}/{iri:.+}") // :.+ to accept even /
   @GET
   @ApiOperation(value = "Get an existing data Resource.",
      notes = "Resource Model type and IRI (Internal Resource Identifier) are required. "
            + "\n<br/><br/>\n"
            + "Allows web-style client-side caching : if client sends its current cached "
            + "resource's version as an ETag in an If-None-Match=version precondition, "
            + "the Resource is returned only if this precondition is matched on the server, "
            + "otherwise (if the client's version is not up-to-date with the server's), "
            + "it returns 304 Not Modified, allowing the client to get the Resource from its cache.",
            response = DCResource.class, position = 5)
   @ApiResponses(value = {
      @ApiResponse(code = 404, message = "Resource does not exist, or type model not found"),
      @ApiResponse(code = 304, message = "Resource not modified (client can reuse its cache's)"),
      @ApiResponse(code = 200, message = "OK : resource found and returned")
   })
   DCResource getData(
         @ApiParam(value = "Model type to look up in", required = true) @PathParam("type") String modelType,
         @ApiParam(value = "Type-relative resource id", required = true) @PathParam("iri") String iri)
         throws NotFoundException;

   /**
    * Shortcut to deleteData(modelType, iri, null)
    * which uses client local cache to get version if there is any cached value
    * 
    * Deletes the given Data.
    * If-Match header has to be be provided with the up-to-date version of the Data to delete.
    * TODO LATER also add a "deleted" flag that can be set in POST ?!?
    * @param type
    * @param iri
    */
   @Path("/type/{type}/{iri:.+}") // :.+ to accept even /
   @DELETE
   @ApiOperation(value = "Deletes an existing data Resource.",
      notes = "Resource Model type and IRI (Internal Resource Identifier) are required, "
            + "but also up-to-date version sent as an ETag in an If-Match=version precondition. "
            + "Doesn't check whether said data Resource exists beforehands.",
            response = DCResource.class, position = 6)
   @ApiResponses(value = {
      @ApiResponse(code = 409, message = "Conflict : while trying to delete existing resource, "
            + "optimistic locking error "
            + "(provided resource version is not up-to-date with the server's latest)"),
      @ApiResponse(code = 404, message = "Resource does not exist, or type model not found"),
      @ApiResponse(code = 400, message = "Bad request : version ETag is missing or not a long integer"),
      @ApiResponse(code = 204, message = "Delete succeeded")
   })
   void deleteData(
         @ApiParam(value = "Model type to look up in", required = true) @PathParam("type") String modelType,
         @ApiParam(value = "Type-relative resource id", required = true) @PathParam("iri") String iri)
         throws BadRequestException, NotFoundException, ClientErrorException;

   /**
    * Shortcut to putPatchDeleteDataOnGet(modelType, iri, method, null, null)
    * which in case of DELETE uses client local cache to get version if there is any cached value
    * 
    * WARNING commented for now because conflicts with getData(),
    * TODO write interceptor doing the routing making it work  (or client only helper ??)
    * 
    * Provides POST, PUT, PATCH, DELETE methods on GET for easier testing.
    * @param type
    * @param iri
    * @param method PUT, PATCH, DELETE
    * @param uriInfo to take DCData fields from HTTP request parameters
    * @return
    */
   //@Path("/type/{type}/{iri:.+}") // NB. :.+ to accept even / // TODO rather than :
   @Path("/change/type/{type}/{iri:.+}")
   @GET
   @ApiOperation(value = "NOT IMPLEMENTED YET, (Testing only) udpates or deletes "
            + "an existing data Resource in the given type.",
         notes = "This is a tunneled version of the PUT / PATCH and DELETE operations over GET "
               + "to ease up testing (e.g. on web browsers), prefer PUT / PATCH or DELETE if possible. "
               + "\n<br/><br/>\n"
               + "Resource URI (Model type and IRI i.e. Internal Resource Identifier) are required "
               + "but also up-to-date version sent as an ETag in an If-Match=version precondition, "
               + "TODO all fields must be provided OR PATCH behaviour differ from PUT's. "
               + "Delete doesn't check whether said data Resource exists beforehands.",
               response = DCResource.class, position = 8)
      @ApiResponses(value = {
         @ApiResponse(code = 500, message = "Internal server error"),
         @ApiResponse(code = 409, message = "Conflict : while trying to update existing "
               + "resource (in non-strict POST mode only), optimistic locking error "
               + "(provided resource version is not up-to-date with the server's latest)"),
         @ApiResponse(code = 404, message = "Resource does not exist, or type model not found"),
         @ApiResponse(code = 400, message = "Bad request : missing content, missing or invalid URI, "
               + "(if enabled) strict POST mode not respected (version provided or resource already exists), "
               + "field parsing errors (format, consistency with Model & Mixin types...)"),
         @ApiResponse(code = 204, message = "Delete succeeded"),
         @ApiResponse(code = 200, message = "OK : the resource has been updated")
      })
   DCResource putPatchDeleteDataOnGet(
         @ApiParam(value = "Model type to update it in", required = true) @PathParam("type") String modelType,
         @ApiParam(value = "Type-relative resource id", required = true) @PathParam("iri") String iri,
         @ApiParam(value = "HTTP method to tunnel over", required = true,
         allowableValues="POST") @QueryParam("method") String method,
         @Context UriInfo uriInfo)
               throws BadRequestException, NotFoundException;
   
}
