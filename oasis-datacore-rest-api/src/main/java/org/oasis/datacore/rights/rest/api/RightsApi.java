package org.oasis.datacore.rights.rest.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiImplicitParam;
import com.wordnik.swagger.annotations.ApiImplicitParams;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import com.wordnik.swagger.annotations.Authorization;
import com.wordnik.swagger.annotations.AuthorizationScope;

/**
 * 
 * @author agiraudon
 *
 */

@Path("dc/r")
@Api(value = "/dc/r", 
	 description = "Rights management (add/remove/flush)",
	 authorizations = {
      @Authorization(value = "OAuth2", scopes = {
             @AuthorizationScope(scope = "datacore", description
                   = "Scope required on a Ozwillo Kernel OAuth2 token for using Datacore API")
       }),
       @Authorization(value = "Basic")
    })
public interface RightsApi {

	 	
	   @Path("/{type}/{iri:.+}/{version}")
	   @POST
	   @Consumes(value = { MediaType.APPLICATION_JSON })
	   @ApiOperation(
			   value = "Add rights on a resource",
			   notes = "You must provide the type, id and version of the resource " +
			   		   "you need to add rights on.<br/>" +
					   "At least one of writers, readers, owners must be defined. Those ACLs (Access Control "
					   + "List) may contain u_[username] or groups defined in Kernel using User Directory "
					   + "or Social API or (DEV MODE ONLY) mock auth conf.<br/>" +
			   		   "The DCRights object will add rights on your resource only if they are not already on it. " +
					   "Only owners are allowed to add rights on a resource. " +
					   "(The scope implementation will be added in the future)"
	   )
	   @ApiResponses(value = {
			   @ApiResponse(code = 500, message = "Internal server error"),
			   @ApiResponse(code = 404, message = "Resource not found"),
			   @ApiResponse(code = 400, message = "Bad request"),
			   @ApiResponse(code = 200, message = "Rights have been added on the resource")
	   })
	   @ApiImplicitParams({
		    @ApiImplicitParam(
		    		name=HttpHeaders.AUTHORIZATION,
		    		paramType="header",
		    		dataType="string",
		            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth",
		            defaultValue="Basic YWRtaW46YWRtaW4="
		    )
	   })
	   void addRightsOnResource(
			@PathParam("type") String modelType,
			@PathParam("iri") String iri,
			@PathParam("version") long version,
			@ApiParam(value = "readers/writers/owners collections to add to existents collections (union)") DCRights dcRights
	   );
	   
	   @Path("/{type}/{iri:.+}/{version}")
	   @DELETE
	   @Consumes(value = { MediaType.APPLICATION_JSON })
	   @ApiOperation(
			   value = "Remove rights on a resource",
			   notes = "You must provide the type, id and version of the resource " +
			   		   "you need to remove rights on<br/>" +
                  "At least one of writers, readers, owners must be defined. Those ACLs (Access Control "
                  + "List) may contain u_[username] or groups defined in Kernel using User Directory "
                  + "or Social API or (DEV MODE ONLY) mock auth conf.<br/>" +
					   "Only owners are allowed to remove rights on a resource" +
					   "(The scope implementation will be added in the future)"
	   )
	   @ApiResponses(value = {
			   @ApiResponse(code = 500, message = "Internal server error"),
			   @ApiResponse(code = 404, message = "Resource not found"),
			   @ApiResponse(code = 400, message = "Bad request"),
			   @ApiResponse(code = 200, message = "Rights have been correctly remove from the resource")
	   })
	   @ApiImplicitParams({
		    @ApiImplicitParam(
		    		name=HttpHeaders.AUTHORIZATION,
		    		paramType="header",
		    		dataType="string",
		            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth",
		            defaultValue="Basic YWRtaW46YWRtaW4="
		    )
	   })
	   void removeRightsOnResource(
			@PathParam("type") String modelType,
			@PathParam("iri") String iri,
			@PathParam("version") long version,
			@ApiParam(value = "readers/writers/owners collections to remove from existents collections (difference)") DCRights dcRights
	   );
	   
	   @Path("/f/{type}/{iri:.+}/{version}")
	   @PUT
	   @ApiOperation(
			   value = "Remove all rights defined on resource except owners",
			   notes = "You must provide the type, id and version of the resource " +
			   		   "you need to remove writer and reader rights on."
	   )
	   @ApiResponses(value = {
			   @ApiResponse(code = 500, message = "Internal server error"),
			   @ApiResponse(code = 404, message = "Resource not found"),
			   @ApiResponse(code = 400, message = "Bad request"),
			   @ApiResponse(code = 200, message = "Readers and writers have been correctly removed from the resource")
	   })
	   @ApiImplicitParams({
		    @ApiImplicitParam(
		    		name=HttpHeaders.AUTHORIZATION,
		    		paramType="header",
		    		dataType="string",
		            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth",
		            defaultValue="Basic YWRtaW46YWRtaW4="
		    )
	   })
	   void flushRightsOnResource(
			@PathParam("type") String modelType,
			@PathParam("iri") String iri,
			@PathParam("version") long version
	   );
	   
	   @Path("/{type}/{iri:.+}/{version}")
	   @PUT
	   @ApiOperation(
			   value = "Replace all rights on the selected resource by the DCRights in payload",
			   notes = "You must provide the type, id and version of the resource.<br/>" +
                  "At least one of writers, readers, owners must be defined. Those ACLs (Access Control "
                  + "List) may contain u_[username] or groups defined in Kernel using User Directory "
                  + "or Social API or (DEV MODE ONLY) mock auth conf.<br/>" +
			   		   "Don't forget that the DCRights you give in the body will replace entirely the one on the resource (EVEN THE OWNERS !)."
	   )
	   @ApiResponses(value = {
			   @ApiResponse(code = 500, message = "Internal server error"),
			   @ApiResponse(code = 404, message = "Resource not found"),
			   @ApiResponse(code = 400, message = "Bad request"),
			   @ApiResponse(code = 200, message = "Rights have been replace successfully")
	   })
	   @ApiImplicitParams({
		    @ApiImplicitParam(
		    		name=HttpHeaders.AUTHORIZATION,
		    		paramType="header",
		    		dataType="string",
		            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth",
		            defaultValue="Basic YWRtaW46YWRtaW4="
		    )
	   })
	   void replaceRightsOnResource(
			@PathParam("type") String modelType,
			@PathParam("iri") String iri,
			@PathParam("version") long version,
			@ApiParam(value = "New rights") DCRights dcRights
	   );
	   
	   
	   @Path("/{type}/{iri:.+}/{version}")
	   @GET
	   @ApiOperation(
			   value = "Get rights list of a resource",
			   notes = "You must provide the type, id and version of the resource " +
			   		   "to get the rights associated to it.",
			   response = DCRights.class
	   )
	   @ApiResponses(value = {
			   @ApiResponse(code = 500, message = "Internal server error"),
			   @ApiResponse(code = 404, message = "Resource not found"),
			   @ApiResponse(code = 400, message = "Bad request"),
			   @ApiResponse(code = 200, message = "Rights have been correctly retrieved")
	   })
	   @ApiImplicitParams({
		    @ApiImplicitParam(
		    		name=HttpHeaders.AUTHORIZATION,
		    		paramType="header",
		    		dataType="string",
		            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth",
		            defaultValue="Basic YWRtaW46YWRtaW4="
		    )
	   })
	   DCRights getRightsOnResource(
			@PathParam("type") String modelType,
			@PathParam("iri") String iri,
			@PathParam("version") long version
	   );
	   	
}
