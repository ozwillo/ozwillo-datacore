package org.oasis.datacore.contribution.rest.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.oasis.datacore.rest.api.DCResource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;


/**
 * 
 * @author agiraudon
 *
 */
@Path("dc/c")
@Api(value = "/dc/c", 
	 description = "Contribution management (add/get/remove)",
    authorizations = {
       @Authorization(value = "OAuth2", scopes = {
             @AuthorizationScope(scope = "datacore", description
                   = "Scope required on a Ozwillo Kernel OAuth2 token for using Datacore API")
       }),
       @Authorization(value = "Basic")
 })
public interface ContributionApi {

	   @Path("/")
	   @POST
	   @Produces(value = { MediaType.APPLICATION_JSON })
	   @Consumes(value = { MediaType.APPLICATION_JSON })
	   @ApiOperation(
			   value = "Add a contribution",
			   notes = "You can add a contribution by using this method." +
			   		 " You must provide a DCContribution object. " +
			   		 " Comment is required.",
			   response = DCContribution.class
	   )
	   @ApiResponses(value = {
			   @ApiResponse(code = 500, message = "Internal server error"),
			   @ApiResponse(code = 404, message = "Model not found"),
			   @ApiResponse(code = 400, message = "Bad request"),
			   @ApiResponse(code = 200, message = "Contribution created")
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
	   public DCContribution createContribution(
			@ApiParam(value = "Contribution as a DCContribution") DCContribution contribution
	   );
	   
	   @Path("/{modelType}")
	   @GET
	   @Produces(value = { MediaType.APPLICATION_JSON })
	   @ApiOperation(
			   value = "Get a list of contribution on a defined type",
			   notes = "You must provide the model type. " +
			   		   " In order to see contributions you must be the owner of the model type, or " +
			   		   "you will only see the one you created.",
			   response = DCContribution.class,
			   responseContainer = "List"
	   )
	   @ApiResponses(value = {
			   @ApiResponse(code = 500, message = "Internal server error"),
			   @ApiResponse(code = 404, message = "Contribution not found"),
			   @ApiResponse(code = 400, message = "Bad request"),
			   @ApiResponse(code = 200, message = "Contributions have been correctly retrieved")
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
	   public List<DCContribution> getContribution(
			@PathParam("modelType") String modelType
	   );
	   
	   @Path("/{modelType}/{contributionId}")
	   @GET
	   @Produces(value = { MediaType.APPLICATION_JSON })
	   @ApiOperation(
			   value = "Get a list of resources attached to a contribution on a defined type",
			   notes = "You must provide the model type. " +
			   		   " In order to see contributions you must be the owner of the model type, or " +
			   		   "you will only see the one you created.",
			   response = DCContribution.class,
			   responseContainer = "List"
	   )
	   @ApiResponses(value = {
			   @ApiResponse(code = 500, message = "Internal server error"),
			   @ApiResponse(code = 404, message = "Contribution not found"),
			   @ApiResponse(code = 400, message = "Bad request"),
			   @ApiResponse(code = 200, message = "Contributions have been correctly retrieved")
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
	   public List<DCContribution> getContribution(
			@PathParam("modelType") String modelType,
			@PathParam("contributionId") String contributionId
	   );
	   
	   @Path("/r/{modelType}/{contributionId}")
	   @GET
	   @Produces(value = { MediaType.APPLICATION_JSON })
	   @ApiOperation(
			   value = "Get a defined contribution on a defined type",
			   notes = "You must provide the model type. " +
			   		   " In order to see contributions you must be the owner of the model type, or " +
			   		   "you will only see the one you created.",
			   response = DCResource.class,
			   responseContainer = "List"
	   )
	   @ApiResponses(value = {
			   @ApiResponse(code = 500, message = "Internal server error"),
			   @ApiResponse(code = 404, message = "Contribution not found"),
			   @ApiResponse(code = 400, message = "Bad request"),
			   @ApiResponse(code = 200, message = "Contribution resources have been correctly retrieved")
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
	   public List<DCContribution> getContributionResources(
			@PathParam("modelType") String modelType,
			@PathParam("contributionId") String contributionId
	   );
	   
	   @Path("/{modelType}/{contributionId}")
	   @DELETE
	   @ApiOperation(
			   value = "Delete a defined contribution on a defined type",
			   notes = "You must provide the model type and the contributionId. " +
			   		   " You must be the owner of the model or of the contribution to delete it."
	   )
	   @ApiResponses(value = {
			   @ApiResponse(code = 500, message = "Internal server error"),
			   @ApiResponse(code = 404, message = "Contribution not found or model not found"),
			   @ApiResponse(code = 400, message = "Bad request"),
			   @ApiResponse(code = 200, message = "Contribution has been correctly removed")
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
	   public void deleteContribution(
			@PathParam("modelType") String modelType,
			@PathParam("contributionId") String contributionId
	   );

	
}
