package org.oasis.datacore.rest.client;

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.DatacoreApi;

import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;


/**
 * Client-side-only API to allow using all operations
 * using the proxy client paradigm (with the help of some interceptors)
 * (TODO LATER and ease up the client user's job)
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
    * @param queryParams finder criteria in the form firstname=John&lastname=Doe .
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
         @HeaderParam(QUERY_PARAMETERS) String queryParams, // XXX HACK TODO better OrderedMap ?!
         @DefaultValue("0") @QueryParam("start") Integer start,
         @DefaultValue("10") @QueryParam("limit") Integer limit);
   
   /**
    * See findData(UriInfo)
    * @param queryParams finder criteria in the form firstname=John&lastname=Doe .
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
         @HeaderParam(QUERY_PARAMETERS) String queryParams, // XXX HACK TODO better OrderedMap ?! 
         @DefaultValue("0") @QueryParam("start") Integer start,
         @DefaultValue("10") @QueryParam("limit") Integer limit);
   
}
