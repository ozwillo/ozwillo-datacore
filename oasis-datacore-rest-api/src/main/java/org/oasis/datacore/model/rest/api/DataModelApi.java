package org.oasis.datacore.model.rest.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiImplicitParam;
import com.wordnik.swagger.annotations.ApiImplicitParams;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;


/**
 * REST (JSON) Datacore Data Model API.
 * 
 * FOR NOW TEMPORARY ONLY
 * 
 * See Swagger API root doc in (used in spring conf) in
 * https://github.com/pole-numerique/oasis-datacore/blob/master/oasis-datacore-rest-api/src/main/resources/oasis-datacore-rest-api.properties
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
@Path("dc/model") // relative path among other OASIS services
@Api(value = "/dc/model", description = "DRAFT Operations about Datacore Models ",
      authorizations="OASIS OAuth and required Datacore Resource authorizations")
@Consumes(MediaType.APPLICATION_JSON) // TODO finer media type ex. +oasis-datacore ??
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
