/**
 * OASIS Datacore
 * Copyright (c) 2013 Open Wide
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact : http://www.oasis-eu.org/ oasis-eu-datacore-dev@googlegroups.com
 */
package org.oasis.datacore.rest.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiImplicitParam;
import com.wordnik.swagger.annotations.ApiImplicitParams;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;


/**
 * REST (JSON) Datacore API.
 * TODO DataResourceApi ??
 * 
 * Allows to manage (CRUD) and find Data Resources,
 * in JSON-LD (like ??) format
 * and with LDP (W3C Linked Data Platform)-like operations (URIs,finders etc.) .
 * TODO links
 * 
 * Available RESTful operations are defined in JAXRS.
 * 
 * Using operations where type of data is provided in URL usually improves performances :
 * "*inType" methods are faster, finder queries with type even more.
 * 
 * On GET, providing the If-None-Match header with an up-to-date ETag (dcData.version) doesn't
 * return the content but an HTTP 304 Not Modified.
 * This allows for efficient web-like client-side caching & refresh.
 * See explanation at http://odino.org/don-t-rape-http-if-none-match-the-412-http-status-code/ .
 * However, the If-Match header with ETag is not checked on POST, PUT, PATCH or DELETE,
 * because it's done anyway at persistence (MongoDB) level at update
 * (and it would make clients - and servers - harder to develop).
 * 
 * NB. operation path must start with / even if relative to resource path
 * else swagger UI playground doesn't work (they're missing their middle slash
 * ex. /dctype/city...).  
 * 
 * TODO LATER catch persistence (MongoDB) precondition error and prettily return HTTP 412 (Precondition Failed)
 * 
 * TODO Examples
 * 
 * TODO DatacoreNativeApi with only "native" methods ? AT LEAST NOT FOR CLIENT CACHING
 * 
 * TODO non-native queries ? with limited auto @Queriable joins ??
 * 
 * TODO updateMatchingQuery ??
 * 
 * TODO LATER2 XML ??
 * 
 * TODO LATER3 map/reduce ???
 * 
 * TODO LATER3 HTTP OPTIONS returns apidoc ? not much used, rather swagger for now http://zacstewart.com/2012/04/14/http-options-method.html
 * 
 * 
 * TODOs :
 * * HTTP Link as HeaderParam for more JSON-LD / LDP-like ?? 
 * 
 * @author mdutoo
 *
 */
@Path("dc") // relative path among other OASIS services
@Api(value = "/dc", description = "Operations about Datacore Resources",
      authorizations="OASIS OAuth and required Datacore Resource authorizations")
@Consumes(MediaType.APPLICATION_JSON) // TODO finer media type ex. +oasis-datacore ??
@Produces(MediaType.APPLICATION_JSON)
public interface DatacoreApi {
   
   public static final String QUERY_PARAMETERS = "#queryParameters";
   public static final String DC_TYPE_PATH = "/dc/type";
   
   /*
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
   ///@Path("/type/{type}") // TODO rather than :
   /*@Path("/post/type/{type}")
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
         *//*, @Context Request request*//*);*/
   
   /**
    * "mass" update in type
    * TODO won't work
    * TODO return : see above
    * @param dcDatas with iri
    * @return
    */
   @Path("/type/{type}")
   @POST
   @ApiOperation(value = "Creates new data Resource(s) in the given type, or updates them if allowed.",
      notes = "Mere wrapper over atomic typed POST. Resource URI must be provided but can be "
            + "relative to type, up-to-date versions are required to update existing resources "
            + "(which first requires that strict POST mode is not enabled). POST of a single data "
            + "resource (instead of an array with a single item) is supported.",
            response = DCResource.class, responseContainer="List")
   @ApiResponses(value = {
      @ApiResponse(code = 500, message = "Field parsing errors (format, consistency...)"),
      @ApiResponse(code = 409, message = "Conflict : while trying to update existing "
            + "resource (in non-strict POST mode only), optimistic locking error "
            + "(provided resource version is not up-to-date with the server's latest)"),
      @ApiResponse(code = 400, message = "Bad request : missing content, missing or invalid URIs, "
            + "(if enabled) strict POST mode not respected (version provided or resource already exists)"),
      @ApiResponse(code = 404, message = "Type model not found") 
   })
   List<DCResource> postAllDataInType(
         @ApiParam(value = "Data Resources to create", required = true) List<DCResource> dcDatas,
         @ApiParam(value = "Model type to create them in", required = true) @PathParam("type") String modelType
         /*, @Context Request request*/);
   
   /**
    * "mass" update, types must be provided
    * TODO won't work
    * TODO return : see above
    * @param dcDatas with type & iri
    * @return
    */
   @Path("/")
   @POST
   @ApiOperation(value = "Creates new data Resources, or updates them if allowed.",
      notes = "Mere wrapper over atomic typed POST. Resource URI must be provided, "
            + "up-to-date versions are required to update existing resources "
            + "(which first requires that strict POST mode is not enabled)",
            response = DCResource.class, responseContainer="List")
   @ApiResponses(value = {
      @ApiResponse(code = 500, message = "Field parsing errors (format, consistency...)"),
      @ApiResponse(code = 409, message = "Conflict : while trying to update existing "
            + "resource (in non-strict POST mode only), optimistic locking error "
            + "(provided resource version is not up-to-date with the server's latest)"),
      @ApiResponse(code = 400, message = "Bad request : missing content, missing or invalid URIs, "
            + "(if enabled) strict POST mode not respected (version provided or resource already exists)"),
      @ApiResponse(code = 404, message = "Type model not found") 
   })
   List<DCResource> postAllData(
         @ApiParam(value = "Data Resources to create", required = true) List<DCResource> dcDatas
         /*, @Context Request request*/);
   
   /**
    * Not required, only for REST compliance ; PATCH version allows to only provide diff
    * @param dcData
    * @param type
    * @param iri
    * @return
    */
   @Path("/type/{type}/{iri:.+}") // :.+ to accept even /
   @PUT
   @PATCH
   @ApiOperation(value = "Updates an existing data Resource in the given type.",
      notes = "(For now) mere wrapper over atomic typed non-strict mode POST. "
            + "Resource URI (Model type and IRI i.e. Internal Resource Identifier) are required "
            + "but also up-to-date version sent as an ETag in an If-Match=version precondition, "
            + "TODO all fields must be provided OR PATCH behaviour differ from PUT's",
            response = DCResource.class)
   @ApiResponses(value = {
      @ApiResponse(code = 500, message = "Field parsing errors (format, consistency...)"),
      @ApiResponse(code = 409, message = "Conflict : while trying to update existing resource, "
            + "optimistic locking error "
            + "(provided resource version is not up-to-date with the server's latest)"),
      @ApiResponse(code = 400, message = "Bad request : missing content or version, "
            + "missing or invalid URI, non existing resource"),
      @ApiResponse(code = 404, message = "Type model not found") 
   })
   DCResource putDataInType(
         @ApiParam(value = "Data Resource to update", required = true) DCResource dcData,
         @ApiParam(value = "Model type to update it in", required = true) @PathParam("type") String modelType,
         @ApiParam(value = "Type-relative resource id", required = true) @PathParam("iri") String iri
         /*, @Context Request request*/);
   
   /**
    * Not required, only for REST compliance ; PATCH version allows to only provide diff
    * @param dcData with iri
    * @param type
    * @return
    */
   @Path("/type/{type}")
   @PUT
   @PATCH
   @ApiOperation(value = "Updates existing data Resources in the given type.",
      notes = "Mere wrapper over atomic typed PUT. "
            + "Resource URI (Model type and IRI i.e. Internal Resource Identifier) are required "
            + "TODO but also up-to-date version sent as an ETag in an If-Match=version precondition, "
            + "TODO all fields must be provided OR PATCH behaviour differ from PUT's",
            response = DCResource.class, responseContainer="List")
   @ApiResponses(value = {
      @ApiResponse(code = 500, message = "Field parsing errors (format, consistency...)"),
      @ApiResponse(code = 409, message = "Conflict : while trying to update existing resource, "
            + "optimistic locking error "
            + "(provided resource version is not up-to-date with the server's latest)"),
      @ApiResponse(code = 400, message = "Bad request : missing content or version, "
            + "missing or invalid URI, non existing resource"),
      @ApiResponse(code = 404, message = "Type model not found") 
   })
   List<DCResource> putAllDataInType(
         @ApiParam(value = "Data Resources to update", required = true) List<DCResource> dcDatas,
         @ApiParam(value = "Model type to update them in", required = true) @PathParam("type") String modelType
         /*, @Context Request request*/);
   
   /**
    * Not required, only for REST compliance ; PATCH version allows to only provide diff
    * @param dcData with type & iri
    * @return
    */
   @Path("/")
   @PUT
   @PATCH
   @ApiOperation(value = "Updates existing data Resources.",
      notes = "Mere wrapper over atomic typed PUT. "
            + "Resource URI (Model type and IRI i.e. Internal Resource Identifier) are required "
            + "TODO but also up-to-date version sent as an ETag in an If-Match=version precondition, "
            + "TODO all fields must be provided OR PATCH behaviour differ from PUT's",
            response = DCResource.class, responseContainer="List")
   @ApiResponses(value = {
      @ApiResponse(code = 500, message = "Field parsing errors (format, consistency...)"),
      @ApiResponse(code = 409, message = "Conflict : while trying to update existing resource, "
            + "optimistic locking error "
            + "(provided resource version is not up-to-date with the server's latest)"),
      @ApiResponse(code = 400, message = "Bad request : missing content or version, "
            + "missing or invalid URI, non existing resource"),
      @ApiResponse(code = 404, message = "Type model not found") 
   })
   List<DCResource> putAllData(
         @ApiParam(value = "Data Resources to update", required = true) List<DCResource> dcDatas
         /*, @Context Request request*/);
   
   /**
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
            + "Allows web-style client-side caching : if client sends its current cached "
            + "resource's version as an ETag in an If-None-Match=version precondition, "
            + "the Resource is returned only if this precondition is matched on the server, "
            + "otherwise (if the client's version is not up-to-date with the server's), "
            + "it returns 304 Not Modified, allowing the client to get the Resource from its cache.",
            response = DCResource.class)
   @ApiResponses(value = {
      @ApiResponse(code = 404, message = "Type model not found"),
      @ApiResponse(code = 304, message = "Resource not modified (client can reuse its cache's)"),
      @ApiResponse(code = 204, message = "Resource doesn't exist")
   })
   DCResource getData(
         @ApiParam(value = "Model type to look up in", required = true) @PathParam("type") String modelType,
         @ApiParam(value = "Type-relative resource id", required = true) @PathParam("iri") String iri,
         @Context Request request);
   
   /**
    * Deletes the given Data.
    * If-Match header has to be be provided with the up-to-date version of the Data to delete.
    * @param type
    * @param iri
    * @param httpHeaders to look for If-Match header
    */
   @Path("/type/{type}/{iri:.+}") // :.+ to accept even /
   @DELETE
   @ApiOperation(value = "Deletes an existing data Resource.",
      notes = "Resource Model type and IRI (Internal Resource Identifier) are required, "
            + "but also up-to-date version sent as an ETag in an If-Match=version precondition.",
            response = DCResource.class)
   @ApiResponses(value = {
      @ApiResponse(code = 409, message = "Conflict : while trying to delete existing resource, "
            + "optimistic locking error "
            + "(provided resource version is not up-to-date with the server's latest)"),
      @ApiResponse(code = 404, message = "Type model not found"),
      @ApiResponse(code = 400, message = "Bad request : version ETag is missing or not a long integer"),
      @ApiResponse(code = 304, message = "Resource not modified (client can reuse its cache's)"),
      @ApiResponse(code = 204, message = "Delete succeeded")
   })
   void deleteData(
         @ApiParam(value = "Model type to look up in", required = true) @PathParam("type") String modelType,
         @ApiParam(value = "Type-relative resource id", required = true) @PathParam("iri") String iri,
         @Context HttpHeaders httpHeaders);

   
   
   ///////////////////////////////////////////////////////////////////
   // MULTI OPERATIONS
   
   /**
    * TODO CAN'T PROVIDE VERSION SO STRONGLY DISCOURAGED BUT IS IT USEFUL ?? not implemented yet
    * on POST because not REST
    * @param dcDataDiff (without version
    * @param type
    * @return
    */
   /*@Path("/where/type/{type}")
   @PUT
   @PATCH
   @ApiOperation(value = "Multiple update, WON'T BE IMPLEMENTED (at least until type-level transactions).",
         response = DCResource.class, responseContainer="List")*/
   List<DCResource> updateDataInTypeWhere(DCResource dcDataDiff, @PathParam("type") String modelType,
         @Context UriInfo uriInfo);
   
   /**
    * TODO CAN'T PROVIDE VERSION SO STRONGLY DISCOURAGED BUT IS IT USEFUL ?? not implemented yet
    * on POST because not REST
    * @param type
    * @param uriInfo
    * @return
    */
   /*@Path("where/type/{type}")
   @DELETE
   @ApiOperation(value = "Multiple delete, WON'T BE IMPLEMENTED (at least until type-level transactions).",
      response = DCResource.class, responseContainer="List")*/
   void deleteDataInTypeWhere(@PathParam("type") String modelType,
         @Context UriInfo uriInfo);
   

   
   ///////////////////////////////////////////////////////////////////
   // OPERATIONS TUNNELED ON GET
   
   /**
    * WARNING commented for now because conflicts with findDataInType(),
    * TODO write interceptor doing the routing making it work (or client only helper ??)
    * 
    * Provides POST on GET for easier testing.
    * @param type
    * @param iri
    * @param method POST (required not to mix it up with getData)
    * @param uriInfo to take DCData fields from HTTP request parameters
    * @return
    */
   //@Path("type/{type}") // TODO rather than :
   @Path("/post/type/{type}")
   @GET
   @ApiOperation(value = "NOT IMPLEMENTED YET, (Testing only) creates a new data Resource "
         + "in the given type, or updates it if allowed.",
      notes = "This is a tunneled version of the POST operation over GET "
            + "to ease up testing (e.g. on web browsers), prefer POST if possible. "
            + "Resource URI must be provided but can be relative to type, "
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
   DCResource postDataInTypeOnGet(
         @ApiParam(value = "Model type to create (or update) it in", required = true) @PathParam("type") String modelType,
         @ApiParam(value = "HTTP method to tunnel over", required = true,
         allowableValues="POST") @QueryParam("method") String method,
         @Context UriInfo uriInfo/*, @Context Request request*/);
   
   /**
    * WARNING commented for now because conflicts with getData(),
    * TODO write interceptor doing the routing making it work  (or client only helper ??)
    * 
    * Provides POST, PUT, PATCH, DELETE methods on GET for easier testing.
    * @param type
    * @param iri
    * @param method PUT, PATCH, DELETE
    * @param uriInfo to take DCData fields from HTTP request parameters
    * @param httpHeaders to look for If-Match header in case of DELETE (see deleteData())
    * @return
    */
   //@Path("/type/{type}/{iri:.+}") // NB. :.+ to accept even / // TODO rather than :
   @Path("/change/type/{type}/{iri:.+}")
   @GET
   @ApiOperation(value = "NOT IMPLEMENTED YET, (Testing only) udpates or deletes "
            + "an existing data Resource in the given type.",
         notes = "This is a tunneled version of the PUT / PATCH and DELETE operations over GET "
               + "to ease up testing (e.g. on web browsers), prefer PUT / PATCH or DELETE if possible. "
               + "Resource URI (Model type and IRI i.e. Internal Resource Identifier) are required "
               + "but also up-to-date version sent as an ETag in an If-Match=version precondition, "
               + "TODO all fields must be provided OR PATCH behaviour differ from PUT's",
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
   DCResource putPatchDeleteDataOnGet(
         @ApiParam(value = "Model type to update it in", required = true) @PathParam("type") String modelType,
         @PathParam("iri") String iri,
         @ApiParam(value = "HTTP method to tunnel over", required = true,
         allowableValues="POST") @QueryParam("method") String method,
         @Context UriInfo uriInfo, @Context HttpHeaders httpHeaders);


   
   ///////////////////////////////////////////////////////////////////
   // FINDER / QUERY OPERATIONS
   
   /**
    * Returns all Datacore data of the given type that match all (AND) criteria provided
    * in query parameters.
    * It is a "native" query in that it is implemented as a single MongoDB query
    * with pagination by default.
    * TODO "only Queriable fields" ?!?
    * 
    * Each HTTP query parameter is a Datacore query criteria in the form fieldPathInType=operatorValue
    * where operator (if any, else defaults to '=') can be in logical, XML (most), MongoDB (all) or
    * sometimes Java-like form :
    * TODO operators...
    * TODO $exist => whether has type / mixin
    * TODO $mod ? projection operators ?? $where javascript ???
    * TODO LATER2 geospatial ?
    *   
    * @param type to look in. Its implementations maps it to a single MongoDB collection
    * to look in.
    * @param uriInfo to take query criteria from HTTP request parameters
    * @param start pagination start, default is 0
    * @param limit pagination limit, default is 10 (?)
    * @param ex. +ascending.field-another_descendingField
    * @return
    */
   @Path("/type/{type}")
   @GET
   @ApiOperation(value = "Returns matching resources of the given Model type.",
      notes = "Returns all Datacore resources of the given Model type that match all (AND)"
            + "criteria provided in HTTP query parameters, sorted in the given order(s) if any, "
            + "with (limited) pagination."
            + "Each additional HTTP query parameter (in Swagger UI, enter them "
            + "as a single URL query string, ex. name=London&population=>100000) "
            + "is a Datacore query criteria in the form field=operatorValueSort "
            + "where 'field' is the field's dotted path in its model type(s), "
            + "'operator' (if any, else defaults to '=') can be in "
            + "logical, XML (most), MongoDB (all) or sometimes Java-like form, "
            + "and 'sort' is an optional + (resp. -) suffix for ascending (resp. descending) order. "
            + "Sorting according to a field is done by providing an HTTP query parameter in the form "
            + "field=sort where 'sort' is + (resp. -) for ascending (resp. descending) order. "
            + "Accepted operators, in their logical/XML/MongoDB/Java forms if any (else -), are : "
            + "=/-/-/==, >/&gt/$gt/-, </&lt;/$lt/-, >=/&gt;=/$gte/-, <=/&lt;=/$lte/-, "
            + "<>/&lt;&gt;/$ne/!=, -/-/$in/- (in JSON list), -/-/$nin/- (not in JSON list)"
            + ", -/-/$regex/-, -/-/$exists/- (field exists) ; list operators : "
            + "-/-/$all/-, -/-/$elemMatch/- (NOT IMPLEMENTED YET), -/-/$size/- . "
            + "Detailed documentation about MongoDB operators can be found at "
            + "http://docs.mongodb.org/manual/reference/operator/query/ .",
            response = DCResource.class, responseContainer="List")
   @ApiImplicitParams({
      @ApiImplicitParam(name=QUERY_PARAMETERS, paramType="query", dataType="string", allowMultiple=true,
            value="Each other HTTP query parameter (in Swagger UI, enter them "
                  + "as a single URL query string, ex. name=London&population=>100000) "
                  + "is a Datacore query criteria")
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
   @ApiResponses(value = {
      @ApiResponse(code = 500, message = "Field parsing errors (format, consistency...)"),
      @ApiResponse(code = 404, message = "Type model not found")
   })
   List<DCResource> findDataInType(@PathParam("type") String modelType, @Context UriInfo uriInfo,
         @ApiParam(value="Pagination start") @DefaultValue("0") @QueryParam("start") Integer start,
         @ApiParam(value="Pagination limit") @DefaultValue("10") @QueryParam("limit") Integer limit);


   /**
    * Returns all Datacore data of the given type that match all (AND) criteria provided
    * in query parameters.
    * It is a non-"native", limited query in that it is implemented by several MongoDB
    * queries whose criteria on non-data (URI) fields are limited as defined in their model.
    * Therefore it is advised to rather do several findDataInType queries.
    * TODO "only Queriable fields" ?!?
    * with pagination by default (limit of 10)
    * 
    * Each HTTP query parameter is a Datacore query criteria in the form type.fieldPathInType=operatorValue
    * See available operators and values in findDataInType() documentation.
    * 
    * @param uriInfo to take query criteria from HTTP request parameters
    * @param start pagination start
    * @param limit pagination limit
    * @param ex. +ascending.field-another_descendingField
    * @return found datas
    */
   @Path("/")
   @GET
   @ApiOperation(value = "Returns matching resources.",
      notes = "Returns all Datacore resources of the given type that match all (AND)"
            + "criteria provided in HTTP query parameters, sorted in the given order(s) if any, "
            + "with (limited) pagination."
            + "It is a 'native' query in that it is"
            + "implemented as a single MongoDB query with pagination by default. "
            + "Each additional HTTP query parameter (in Swagger UI, enter them "
            + "as a single URL query string, ex. name=London&population=>100000) "
            + "is a Datacore query criteria in the form field=operatorValueSort "
            + "where 'field' is the field's dotted path in its model type(s), "
            + "'operator' (if any, else defaults to '=') can be in "
            + "logical, XML (most), MongoDB (all) or sometimes Java-like form, "
            + "and 'sort' is an optional + (resp. -) suffix for ascending (resp. descending) order. "
            + "Sorting according to a field is done by providing an HTTP query parameter in the form "
            + "field=sort where 'sort' is + (resp. -) for ascending (resp. descending) order. "
            + "Accepted operators, in their logical/XML/MongoDB/Java forms if any (else -), are : "
            + "=/-/-/==, >/&gt/$gt/-, </&lt;/$lt/-, >=/&gt;=/$gte/-, <=/&lt;=/$lte/-, "
            + "<>/&lt;&gt;/$ne/!=, -/-/$in/- (in JSON list), -/-/$nin/- (not in JSON list)"
            + ", -/-/$regex/-, -/-/$exists/- (field exists) ; list operators : "
            + "-/-/$all/-, -/-/$elemMatch/- (NOT IMPLEMENTED YET), -/-/$size/- . "
            + "Detailed documentation about MongoDB operators can be found at "
            + "http://docs.mongodb.org/manual/reference/operator/query/ .",
            response = DCResource.class, responseContainer="List")
   @ApiImplicitParams({
      @ApiImplicitParam(name=QUERY_PARAMETERS, paramType="query", dataType="string", allowMultiple=true,
            value="Each other HTTP query parameter (in Swagger UI, enter them "
                  + "as a single URL query string, ex. name=London&population=>100000) "
                  + "is a Datacore query criteria.")
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
   @ApiResponses(value = {
      @ApiResponse(code = 500, message = "Field parsing errors (format, consistency...)"),
      @ApiResponse(code = 404, message = "Type model not found")
   })
   List<DCResource> findData(@Context UriInfo uriInfo,
         @ApiParam(value="Pagination start") @DefaultValue("0") @QueryParam("start") Integer start,
         @ApiParam(value="Pagination limit") @DefaultValue("10") @QueryParam("limit") Integer limit);

   /**
    * Returns all Datacore data of the given type that are found by the given query
    * written in the given language.
    * It is a "native" query in that it is implemented as a single MongoDB query
    * with pagination by default.
    * TODO "only Queriable fields" ?!?
    * 
    * Supported query languages are :
    * 
    * * SPARQL :
    * this is a limited form of SPARQL's SELECT statement. By default, the first
    * variable receives an rdf:type predicate to the given type.
    * Allowed :
    * PREFIX ?, SELECT * (more ??), FROM (type ?), WHERE
    * a / rdf:type, rdfs:subClassOf ? (for types & aspects) ; always AND / && (never ||), OPTIONAL ?!?
    * 1.0^^xsd:float ??
    * VALUES ??
    * FILTER : ! / NOT EXISTS ?, !=, NOT IN, <, >
    * and string functions : REGEX, more ???, langMatches ??, IRI ??
    * count on lists, else no aggregate function (TODO LATER2 using map / reduce) 
    * LIMIT, OFFSET, ORDER BY
    * 
    * * TODO LATER2 support JSONLD LDP-like query language (findDataInType's) to make it nicer ?
    * 
    * @param type to look in. Its implementations maps it to a single MongoDB collection
    * @return found datas
    */
   @Path("/query/type/{type}")
   @GET
   @ApiOperation(value = "NOT IMPLEMENTED YET, Executes the query in the given Model type and returns found resources.",
      notes = "Returns all Datacore data of the given type that are found "
            + "by the given query written in the given language. "
            + "It is a 'native' query in that it is implemented as a single MongoDB query with pagination by default. "
            + "Supported query languages could be : "
            + "1. SPARQL : "
            + "this is a limited form of SPARQL's SELECT statement. By default, the first variable "
            + "receives an rdf:type predicate to the given type. Fields are adressed in their "
            + "dotted form without root type."
            + "Allowed : "
            + "PREFIX ?, SELECT(more ??), FROM (type ?), WHERE "
            + "a / rdf:type, rdfs:subClassOf ? (for types & aspects) ; always AND / && (never ||), OPTIONAL ?!?"
            + " 1.0^^xsd:float ?? "
            + "VALUES ?? "
            + "FILTER : ! / NOT EXISTS ?, !=, NOT IN, <, > "
            + "and string functions : REGEX, more ???, langMatches ??, IRI ?? "
            + "count on lists, else no aggregate function (TODO LATER2 using map / reduce) "
            + "LIMIT, OFFSET, ORDER BY",
            response = DCResource.class, responseContainer="List")
   @ApiResponses(value = {
      @ApiResponse(code = 500, message = "Field parsing errors (format, consistency...)"),
      @ApiResponse(code = 404, message = "Type model not found")
   })
   List<DCResource> queryDataInType(@PathParam("type") String modelType,
         @QueryParam("query") String query,
         @DefaultValue("SPARQL") @QueryParam("language") String language);


   /**
    * Returns all Datacore data of the given type that match all (AND) criteria provided
    * in query parameters.
    * It is a non-"native", limited query in that it is implemented by several MongoDB
    * queries whose criteria on non-data (URI) fields are limited as defined in their model.
    * Therefore it is advised to rather do several findDataInType queries.
    * TODO "only Queriable fields" ?!?
    * 
    * See supported languages in queryDataInType() documentation, the only difference being
    * that type must be provided for all Datacore root resources (as "a" or "rdf:type" predicate).
    * 
    * with pagination by default (limit of 10)
    * @return found datas
    */
   @Path("/query")
   @GET
   @ApiOperation(value = "NOT IMPLEMENTED YET, Executes the query in the given Model type and returns found resources.",
      notes = "Returns all Datacore data of the given type that are found "
            + "by the given query written in the given language. "
            + "It is a non-'native', limited query in that it is implemented by several MongoDB queries "
            + "whose criteria on non-data (URI) fields are limited as defined in their model. Therefore "
            + "it is advised to rather do several findDataInType queries. "
            + "Supported query languages could be : "
            + "1. SPARQL : "
            + "this is a limited form of SPARQL's SELECT statement. Fields are adressed "
            + "in their dotted form including root type. "
            + "Allowed : "
            + "PREFIX ?, SELECT(more ??), FROM (type ?), WHERE "
            + "a / rdf:type, rdfs:subClassOf ? (for types & aspects) ; always AND / && (never ||), OPTIONAL ?!?"
            + " 1.0^^xsd:float ?? "
            + "VALUES ?? "
            + "FILTER : ! / NOT EXISTS ?, !=, NOT IN, <, > "
            + "and string functions : REGEX, more ???, langMatches ??, IRI ?? "
            + "count on lists, else no aggregate function (TODO LATER2 using map / reduce) "
            + "LIMIT, OFFSET, ORDER BY",
            response = DCResource.class, responseContainer="List")
   @ApiResponses(value = {
      @ApiResponse(code = 500, message = "Field parsing errors (format, consistency...)"),
      @ApiResponse(code = 404, message = "Type model not found")
   })
   List<DCResource> queryData(@QueryParam("query") String query,
         @DefaultValue("SPARQL") @QueryParam("language") String language);
   
}