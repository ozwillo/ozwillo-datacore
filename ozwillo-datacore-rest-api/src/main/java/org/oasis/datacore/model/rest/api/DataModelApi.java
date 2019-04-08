package org.oasis.datacore.model.rest.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;


/**
 * REST (JSON) Datacore Data Model API.
 * 
 * FOR NOW TEMPORARY ONLY
 * 
 * See Swagger API root doc in (used in spring conf) in
 * https://github.com/ozwillo/ozwillo-datacore/blob/master/ozwillo-datacore-rest-api/src/main/resources/ozwillo-datacore-rest-api.properties
 * 
 * 
 * ========================
 * JAXRS definition dev doc :
 * 
 * Available RESTful operations are defined in JAXRS.
 * 
 * NB. operation path must start with / even if relative to resource path
 * else swagger UI playground doesn't work (they're missing their middle slash
 * ex. /dctype/city...).
 * 
 * @author mdutoo
 *
 */
@Path("dc/model") // relative path among other Ozwillo services
@Api(value = "/dc/model", description = "DRAFT Operations about Datacore Models ",
   authorizations = {
      @Authorization(value = "OAuth2", scopes = {
            @AuthorizationScope(scope = "datacore", description
                  = "Scope required on a Ozwillo Kernel OAuth2 token for using Datacore API")
      }),
      @Authorization(value = "Basic")
})
@Consumes(MediaType.APPLICATION_JSON) // TODO finer media type ex. +ozwillo-datacore ??
@Produces(MediaType.APPLICATION_JSON)
public interface DataModelApi /*extends DataModelService*/ {


   /**
    * Returns all Datacore model resources.
    * @return
    */
   @Path("/")
   @GET
   @ApiOperation(value = "Returns all or matching models.",
      notes = "Returns all Datacore model resources.",
            response = String.class, position = 0) // fix jdk7 random order in UI
   @ApiImplicitParams({
      @ApiImplicitParam(name=HttpHeaders.AUTHORIZATION, paramType="header", dataType="string",
            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth", defaultValue="Basic YWRtaW46YWRtaW4=")
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
   @ApiResponses(value = {
      @ApiResponse(code = 500, message = "JSON (un)marshalling error."),
      @ApiResponse(code = 200, message = "OK : the resource has been updated")
   })
   String findModel();

   /*
   @Path("/names")
   @GET
   @ApiOperation(value = "Returns all model names.",
      notes = "Returns all Datacore model resources.",
            response = String.class, position = 0) // fix jdk7 random order in UI
   @ApiImplicitParams({
      @ApiImplicitParam(name=HttpHeaders.AUTHORIZATION, paramType="header", dataType="string",
            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth", defaultValue="Basic YWRtaW46YWRtaW4=")
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
   @ApiResponses(value = {
      @ApiResponse(code = 500, message = "JSON (un)marshalling error."),
      @ApiResponse(code = 200, message = "OK : the resource has been updated")
   })
   List<String> getModelNames();

   @Path("/model")
   @GET
   @ApiOperation(value = "Returns all or matching models.",
      notes = "Returns all Datacore model resources.",
            response = String.class, position = 0) // fix jdk7 random order in UI
   @ApiImplicitParams({
      @ApiImplicitParam(name=HttpHeaders.AUTHORIZATION, paramType="header", dataType="string",
            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth", defaultValue="Basic YWRtaW46YWRtaW4=")
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
   @ApiResponses(value = {
      @ApiResponse(code = 500, message = "JSON (un)marshalling error."),
      @ApiResponse(code = 200, message = "OK : the resource has been updated")
   })
   List<String> getModel(String modelName);
*/
}
