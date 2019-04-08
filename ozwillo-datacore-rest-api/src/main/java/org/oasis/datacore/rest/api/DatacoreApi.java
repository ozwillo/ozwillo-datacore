/**
 * Ozwillo Datacore
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
 * Contact : http://www.ozwillo.com/ ozwillo-talk@googlegroups.com
 */
package org.oasis.datacore.rest.api;

import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.oasis.datacore.rest.api.util.DatacoreMediaType;

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
 * REST (JSON) Datacore Data resource API.
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
 * About return codes :
 * * when a resource (or its Model type) doesn't exist, 404 Not Found,
 * see http://stackoverflow.com/questions/11746894/what-is-the-proper-rest-response-code-for-a-valid-request-but-an-empty-data
 * and http://benramsey.com/blog/2008/05/http-status-204-no-content-and-205-reset-content/
 * * when parsing errors, 400 Bad Request
 * * see also http://www.infoq.com/articles/designing-restful-http-apps-roth
 * 
 * TODO break it up : at least additional features (history...) vs core API,
 * or maybe one API per feature and CRUD API vs finders API
 * TODO rename ex. to DataResourceApi ??
 * TODO DatacoreNativeApi with only "native" methods ? AT LEAST NOT FOR CLIENT CACHING
 * 
 * more features :
 * * TODO v1 features
 * * TODO LATER non-native queries ? with limited auto @Queriable joins ??
 * * TODO LATER2 version system (copy model data, pull / push diffs up to pull requests) ?
 * * TODO LATER2 mass operations ex. updateMatchingQuery ??
 * * TODO LATER3 map/reduce ???
 * 
 * more bindings :
 * * TODO LATER2 XML ??
 * 
 * more HTTP-like :
 * * HTTP OPTIONS returns apidoc ? NO not much used, rather swagger for now http://zacstewart.com/2012/04/14/http-options-method.html
 * 
 * more RDF-like : 
 * * TODO LATER2 plug jsonld-java on top to provide other JSON-LD representations (compacted, expanded,
 * framed / embedded...)
 * * TODO LATER2 provide other RDF languages through content-negociation using json-ld-java,
 * especially turtle for finder results.
 * * LATER3 HTTP Link as HeaderParam for more JSON-LD / LDP-like ?? 
 * 
 * @author mdutoo
 *
 */
@Path("dc") // relative path among other OASIS services
@Api(value = "/dc",
   description = "Operations about Datacore Resources",
   authorizations = {
         @Authorization(value = "OAuth2", scopes = {
               @AuthorizationScope(scope = "datacore", description
                     = "Scope required on a Ozwillo Kernel OAuth2 token for using Datacore API")
         }),
         @Authorization(value = "Basic")
   })
///@Consumes(MediaType.MEDIA_TYPE_WILDCARD) // TODO finer media type ex. +ozwillo-datacore ??
///@Produces(MediaType.MEDIA_TYPE_WILDCARD)
@Consumes({MediaType.APPLICATION_JSON, DatacoreMediaType.APPLICATION_NQUADS,
   DatacoreMediaType.APPLICATION_TURTLE, DatacoreMediaType.APPLICATION_JSONLD_EXPAND,
   DatacoreMediaType.APPLICATION_JSONLD_FRAME, DatacoreMediaType.APPLICATION_JSONLD_FLATTEN,
   DatacoreMediaType.APPLICATION_JSONLD_COMPACT})
@Produces({MediaType.APPLICATION_JSON, DatacoreMediaType.APPLICATION_NQUADS,
   DatacoreMediaType.APPLICATION_TURTLE, DatacoreMediaType.APPLICATION_JSONLD_EXPAND,
   DatacoreMediaType.APPLICATION_JSONLD_FRAME, DatacoreMediaType.APPLICATION_JSONLD_FLATTEN,
   DatacoreMediaType.APPLICATION_JSONLD_COMPACT})
public interface DatacoreApi {
   
   public String QUERY_PARAMETERS = "#queryParameters";
   
   public String DC_TYPE_PATH = "/dc/type/"; // TODO better from JAXRS annotations using reflection ?
   
   public String AUTHORIZATION_DEFAULT = "Basic YWRtaW46YWRtaW4="; // admin/admin from mock conf
   
   public String PROJECT_HEADER = "X-Datacore-Project";
   public String PROJECT_DEFAULT = "oasis.main"; // TODO oasis.test
   
   public String DEBUG_HEADER = "X-Datacore-Debug";
   public String START_PARAM = "start";
   public String LIMIT_PARAM = "limit";
   public String DEBUG_PARAM = "debug";
   public String DEBUG_RESULT = "debug";
   public String RESOURCES_RESULT = "resources";

   public String VIEW_HEADER = "X-Datacore-View";
   /** NOT " " else eaten up by CXF 3.1.4 server, nor '@'
    * (forbidden http://stackoverflow.com/questions/19028068/illegal-characters-in-http-headers */
   public String VIEW_HEADER_MINIMAL = "-";

   public String[] CONTEXT_HEADERS = new String[] { PROJECT_HEADER, DEBUG_HEADER, VIEW_HEADER };

   public String NATIVE_QUERY_COMMON_DOC = "'operator' (if any, else defaults to '=') can be in "
         + "logical, XML (most), MongoDB (all) or sometimes Java-like form, "
         + "simple unquoted value is accepted but ideally and formally value should be JSON "
         + "(i.e. number, list / array, map / Object, or quoted string including for string "
         + "serialized types : resource, date, long) (meaning a quoted string should be doubly quoted), "
         + "and 'sort' is an optional + (resp. -) suffix for ascending (resp. descending) order "
         + "(that logically only applies to greater, lower, (not) in and not equals operators). "
         + "\n<br/>\n"
         + "They (operatorValueSort) must be rightly encoded as an HTTP query parameter "
         + "value (meaning in Java using URLEncoder.encode(\"UTF-8\") and Javascript using "
         + "encodeURIComponent(), <a href=\"http://xkr.us/articles/javascript/encode-compare/\">"
         + "see difference and test it</a>)."
         + "Sorting according to a field not already specified in a criteria is done by providing "
         + "an HTTP query parameter in the form field=sort where 'sort' is + (resp. -) "
         + "for ascending (resp. descending) order ; by default it sorts on modified date. "
         + "A given field can be specified twice (even if it's not perfect HTTP), for instance "
         + "to define \"between\" queries using &gt and < operators."
         + "\n<br/><br/>\n"
         + "<b>Available operators</b>, in their logical/XML/MongoDB/Java forms if any (else -), are : "
         + "<br/>&nbsp;&nbsp; - <b>logical</b> operators (apply to primitive fields) : "
         + "=/-/-/==, >/&gt/$gt/- (ex. >3), </&lt;/$lt/-, >=/&gt;=/$gte/-, <=/&lt;=/$lte/-, "
         + "<>/&lt;&gt;/$ne/!=, -/-/$in/- (in JSON list, ex. $in[0,1]), -/-/$nin/- (not in JSON list)"
         + "<br/>&nbsp;&nbsp; - <b>text</b> operators (apply to string and i18n fields) : "
         + ", -/-/$regex/- (Perl's ex. $regex/^Lond.*n/i or only $regex^Lond.*n ex. "
         + "GET <a href=\"/dc/type/pli:city_0?i18n:name=$regex^tori\" class=\"dclink\" onclick=\""
         + "javascript:return findDataByType($(this).attr('href'));"
         + "\">/dc/type/pli:city_0?i18n:name=$regex^tori</a> ; <b>beware</b>, should always start "
         + "by ^ i.e. be a prefix expression, otherwise will be slow or query may even be aborted), "
         + ", -/-/$fulltext/- (looks for token \"exact\" then \"starting with\" matches, ex. "
         + "GET <a href=\"/dc/type/sample.city.city?i18n:name=$fulltextsaint lo\" class=\"dclink\" onclick=\""
         + "javascript:return findDataByType($(this).attr('href'));"
         + "\">/dc/type/sample.city.city?i18n:name=$fulltextsaint lo</a>"
         + " or in a given language on an i18n field or even ordered, ex. "
         + "GET <a href=\"/dc/type/sample.city.city?i18n:name.fr=$fulltextsaint lo+\" class=\"dclink\" onclick=\""
         + "javascript:return findDataByType($(this).attr('href'));"
         + "\">/dc/type/sample.city.city?i18n:name.fr=$fulltextsaint lo+</a>, only on fields whose "
         + "\"fulltext\" have been set at true, such as odisp:name by default, and on resources "
         + "that have been saved since)"
         + "<br/>&nbsp;&nbsp; - <b>list</b> operators : -/-/$all/- (on JSON list whose elements must be all present in "
         + "list values found ex. $all[\"cat1\", \"cat2\"]), -/-/$elemMatch/- (on JSON object "
         + "of key/values to be ALL matched ex. on i18n field "
         + "$elemMatch{\"v\":\"Torino\",\"l\":\"it\"} ), -/-/$size/- (list field size ; no value). "
         + "<br/>&nbsp;&nbsp; - <b>meta</b> operators : "
         + "-/-/$exists/- (field exists ; boolean value, none means true ; can be used for data quality, "
         + "but besides that rather use Model introspection)"
         + "\n<br/><br/>\n"
         + "<b>More :</b>"
         + "\n<br/>\n"
         + "See <a href=\"http://docs.mongodb.org/manual/reference/operator/query/\">"
         + "detailed documentation about MongoDB operators</a>."
         + "\n<br/>\n"
         + "Multiple criteria on same field allow to achieve a \"between\" constraint, ex. "
         + "GET <a href=\"/dc/type/dcmo:model_0?dc:modified=>=1943-04-02T00:00:00.000Z&dc:modified=<=2043-04-02T00:00:00.000Z\" class=\"dclink\" onclick=\""
         + "javascript:return findDataByType($(this).attr('href'));"
         + "\">/dc/type/dcmo:model_0?dc:modified=>=1943-04-02T00:00:00.000Z&dc:modified=<=2043-04-02T00:00:00.000Z</a>."
         + "\n<br/>\n"
         + "Some generic fields can also be used as criteria : " + DCResource.KEY_URI
         + " , " + DCResource.KEY_DCMODIFIED + " ."
         + "\n<br/>\n"
         + "Rather than for each field, text operator language can also be specified once "
         + "for the whole query, in a " + DCResource.KEY_I18N_LANGUAGE + " or "
         + DCResource.KEY_I18N_LANGUAGE_JSONLD + " parameter."
         + "\n<br/><br/>\n"
         + "If <b>debug</b> is enabled, it rather returns a JSON object containing in \""
         + RESOURCES_RESULT + "\" regular results and in \"" + DEBUG_RESULT
         + "\" various debug details about the query and its execution, such as the result of a "
         + "<a href=\"http://docs.mongodb.org/manual/reference/method/cursor.explain\">"
         + "MongoDB query & sort explain commands</a>, in order to help users optimize queries. "
         + "It also returns in \"ownedEntities\" entities on which the user has owner rights, "
         + "that is underlaying bare data as stored i.e. without parsing it as Resources, which "
         + "can help understand results, rights or allow to salvage data whose model has become "
         + "incompatible with it."
         + "\n<br/><br/>\n"
         + "<b>Pagination and iterating on all data :</b></br>\n"
         + "Only the first results of big queries are returned, to avoid one badly "
         + "optimized query worsen performance for all other users. And similarly, "
         + "<b>ranged queries (see below) should be preferred to pagination</b>, because "
         + "pagination start and limit are capped (by default maxima defined in server "
         + "properties datacoreApiServer.query.maxStart and datacoreApiServer.query.maxLimit). "
         + "If limit goes beyond, it is reset to maxLimit. If start goes beyond, a 400 "
         + "error is raised, and a ranged query on an <b>unique indexed required</b> field should be used instead. "
         + "A ranged query is a query with a > or < comparison criteria on a sorted native (ex. "
         + "GET <a href=\"/dc/type/dcmo:model_0?@id=<%22http://data.ozwillo.com/dc/type/dcmo:model_0/dcmpv:PointOfView_0%22-\" class=\"dclink\" onclick=\""
         + "javascript:return findDataByType($(this).attr('href'));"
         + "\">/dc/type/dcmo:model_0?@id=<\"http://data.ozwillo.com/dc/type/dcmo:model_0/dcmpv:PointOfView_0\"-</a>"
         + " or "
         + "GET <a href=\"/dc/type/dcmo:model_0?dc:modified=<%222043-04-02T00:00:00.000Z%22-\" class=\"dclink\" onclick=\""
         + "javascript:return findDataByType($(this).attr('href'));"
         + "\">/dc/type/dcmo:model_0?dc:modified=<\"2043-04-02T00:00:00.000Z\"-</a>"
         + ") or Resource (ex. "
         + "GET <a href=\"/dc/type/dcmo:model_0?dcmo:name=>%22dcmo:model_0%22+\" class=\"dclink\" onclick=\""
         + "javascript:return findDataByType($(this).attr('href'));"
         + "\">/dc/type/dcmo:model_0?dcmo:name=>\"dcmo:model_0\"+</a>"
         + ") field, with the first query having been sort only ex. "
         + "GET <a href=\"/dc/type/dcmo:model_0?dc:modified=-\" class=\"dclink\" onclick=\""
         + "javascript:return findDataByType($(this).attr('href'));"
         + "\">/dc/type/dcmo:model_0?dc:modified=-</a>. "
         + "\n<br/>\n"
         + "<b>Beware</b> : if the field is not required, results that miss it will be lost ; "
         + "if the field is not unique, "
         + "some results that have the same value may be lost unless criteria is >= or <= instead "
         + "(clients may then keep track of already seen Resources to avoid handling them more than one time) "
         + "and no value occurs more times than the query limit (in which case limit must be "
         + "increased or another field comparison criteria should be added) ; "
         + "if the field is not indexed, performance will be unacceptable. "
         + "\n<br/><br/>\n"
         + "<b>Performance :</b>"
         + "\n<br/>\n"
         + "In order to prevent non-optimized queries to hamper overall performance, "
         + "they are constrained and can be aborted if they take too lon. MongoDB "
         + "<a href=\"http://docs.mongodb.org/v2.6/reference/operator/meta/maxScan/\">maxScan</a> "
         + "is set on the query to 3 times the maximal queried fields' "
         + "queryLimit, and at most to default maximum (defined in server property "
         + "datacoreApiServer.query.maxScan) ; beware, maxScan differs from limit "
         + "because sorts or multiple criteria can eat some scans."
         + "\n<br/>\n"
         + "Therefore, queries must be optimized, which means (as for any database) "
         + "that 1. indexes that are appropriate to common use cases have to be defined "
         + "(to create an index on a given field, set its queryLimit to a value > 0), "
         + "and 2. common queries have to be checked to be using those indexes "
         + "(do a query in debug mode and look for "
         + "...\"queryExplain\" : { \"cursor\" : BtreeCursor [entity field path]_1, "
         + "\"nscanned\" : [number of records that had still to be read]... "
         + "with BtreeCursor rather than BasicCursor, and an nscanned as close as "
         + "possible to the number of Resources returned).";
   public String NATIVE_QUERY_DOC_TYPE = "Returns all Datacore resources of the given Model type that match all (AND)"
         + "criteria provided in HTTP query parameters, sorted in the given order(s) if any, "
         + "with (limited) pagination."
         + "\n<br/><br/>\n"
         + "Each additional HTTP query parameter (in Swagger UI, enter them "
         + "as a single URL query string, ex. name=London&population=>100000) "
         + "is a Datacore query criteria in the form <b>field=operatorValueSort</b>. "
         + "There, 'field' is the field's dotted path within its model type(s), "
         + NATIVE_QUERY_COMMON_DOC;
   public String NATIVE_QUERY_DOC_TYPES = "Returns all Datacore resources of the given type that match all (AND)"
         + "criteria provided in HTTP query parameters, sorted in the given order(s) if any, "
         + "with (limited) pagination."
         + "It is a 'native' query in that it is"
         + "implemented as a single MongoDB query with pagination by default. "
         + "\n<br/><br/>\n"
         + "Each additional HTTP query parameter (in Swagger UI, enter them "
         + "as a single URL query string, ex. name=London&population=>100000) "
         + "is a Datacore query criteria in the form <b>field=operatorValueSort</b>. "
         + "There, 'field' is the field's dotted path starting by its model type(s), "
         + NATIVE_QUERY_COMMON_DOC;
   
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
      notes = "Resource URI(s) must be provided but can be relative to type. "
            + "In order to update an existing resource, " 
            + "up-to-date version is required (GET it first if not saved locally). "
            + "Update is done in merge mode (otherwise rather use PUT) i.e. existing fields are kept, "
            + "with set lists being merged, and other fields replaced by provided ones if any.",
            response = DCResource.class)
   @ApiImplicitParams({
      @ApiImplicitParam(name=HttpHeaders.AUTHORIZATION, paramType="header", dataType="string",
            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth", defaultValue=AUTHORIZATION_DEFAULT),
      @ApiImplicitParam(name=PROJECT_HEADER, paramType="header", dataType="string",
            value="Examples : oasis.test, oasis.main", defaultValue=PROJECT_DEFAULT)
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
   @ApiResponses(value = {
      @ApiResponse(code = 500, message = "Internal server error"),
      @ApiResponse(code = 409, message = "Conflict : while trying to update existing "
            + "resource (in non-strict POST mode only), optimistic locking error "
            + "(provided resource version is not up-to-date with the server's latest)"),
      @ApiResponse(code = 404, message = "Type model not found"),
      @ApiResponse(code = 400, message = "Bad request : missing content, missing or invalid URI, "
            + "(if enabled) strict POST mode not respected (version provided or resource already exists), "
            + "field parsing errors (format, consistency with Model & Mixin types...)"),
      @ApiResponse(code = 201, message = "Created : the resource has been created"),
      @ApiResponse(code = 200, message = "OK : the resource has been updated (if not strict POST mode)")
   })
   DCResource postDataInType(
         @ApiParam(value = "Data Resource to create", required = true) DCResource dcData,
         @ApiParam(value = "Model type to create (or update) it in", required = true) @PathParam("type") String modelType)
         throws BadRequestException, NotFoundException, ClientErrorException;*/
   
   /**
    * merge existing single data
    * @param dcData
    * @param type
    * @param iri
    * @return
    */
   @Path("/type/{type}/{__unencoded__iri:.+}") // :.+ to accept even /
   @POST
   @PATCH
   @ApiOperation(value = "Merges an existing data Resource in the given type.",
      notes = "Mere wrapper over atomic typed POST that merges Resources. "
            + "\n<br/><br/>\n"
            + "Resource URI (Model type and IRI i.e. Internal Resource Identifier) are required "
            + "but also up-to-date version (GET it first if not saved locally) "
            + "sent as an ETag in an If-Match=version precondition, "
            + "Existing fields are kept, with set lists being merged, and other fields replaced by provided ones if any.",
            response = DCResource.class, position = 2)
   @ApiImplicitParams({
      @ApiImplicitParam(name=HttpHeaders.AUTHORIZATION, paramType="header", dataType="string",
            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth", defaultValue=AUTHORIZATION_DEFAULT),
      @ApiImplicitParam(name=PROJECT_HEADER, paramType="header", dataType="string",
            value="Examples : oasis.test, oasis.main", defaultValue=PROJECT_DEFAULT)
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
   @ApiResponses(value = {
      @ApiResponse(code = 500, message = "Internal server error"),
      @ApiResponse(code = 409, message = "Conflict : while trying to update existing resource, "
            + "optimistic locking error "
            + "(provided resource version is not up-to-date with the server's latest)"),
      @ApiResponse(code = 404, message = "Resource does not exist, or type model not found"),
      @ApiResponse(code = 400, message = "Bad request : missing content or version, "
            + "missing or invalid URI, non existing resource, "
            + "field parsing errors (format, consistency with Model & Mixin types...)"),
      @ApiResponse(code = 200, message = "OK : the resource has been updated")
   })
   DCResource patchDataInType(
         @ApiParam(value = "Data Resource to merge", required = true) DCResource dcData,
         @ApiParam(value = "Model type to merge it in", required = true) @PathParam("type") String modelType,
         @ApiParam(value = "Type-relative resource id", required = true) @PathParam("__unencoded__iri") String iri)
         throws BadRequestException, NotFoundException, ClientErrorException;
   
   /**
    * "mass" create or merge in type
    * TODO won't work
    * TODO return : see above
    * @param dcDatas with iri
    * @return
    */
   @Path("/type/{type}")
   @POST
   @PATCH
   @ApiOperation(value = "Creates new data Resource(s) in the given type, or updates them (merge) if allowed.",
      notes = "A single data Resource or an array of several ones (in which case it is a mere wrapper "
            + "over the atomic case) are accepted. "
            + "\n<br/><br/>\n"
            + "Resource URI(s) must be provided but can be relative to type. "
            + "In order to update an existing resource, " 
            + "up-to-date version is required (GET it first if not saved locally). "
            + "Update is done in merge mode (otherwise rather use PUT) i.e. existing fields are kept, "
            + "with set lists being merged, and other fields replaced by provided ones if any.",
            response = DCResource.class, responseContainer="List", position = 0) // fix jdk7 random order in UI
   @ApiImplicitParams({
      @ApiImplicitParam(name=HttpHeaders.AUTHORIZATION, paramType="header", dataType="string",
            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth", defaultValue=AUTHORIZATION_DEFAULT),
      @ApiImplicitParam(name=PROJECT_HEADER, paramType="header", dataType="string",
            value="Examples : oasis.test, oasis.main", defaultValue=PROJECT_DEFAULT)
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
   @ApiResponses(value = {
      @ApiResponse(code = 500, message = "Internal server error"),
      @ApiResponse(code = 409, message = "Conflict : while trying to update existing "
            + "resource (in non-strict POST mode only), optimistic locking error "
            + "(provided resource version is not up-to-date with the server's latest)"),
      @ApiResponse(code = 404, message = "Type model not found"),
      @ApiResponse(code = 400, message = "Bad request : missing content, missing or invalid URIs, "
            + "(if enabled) strict POST mode not respected (version provided or resource already exists), "
            + "field parsing errors (format, consistency with Model & Mixin types...)"),
      @ApiResponse(code = 201, message = "Created : resources have been created")
   })
   List<DCResource> postAllDataInType(
         @ApiParam(value = "Data Resources to create or merge", required = true) List<DCResource> dcDatas,
         @ApiParam(value = "Model type to create them in", required = true) @PathParam("type") String modelType)
         throws BadRequestException, NotFoundException, ClientErrorException;
   
   /**
    * "mass" create or merge, types must be provided
    * TODO won't work
    * TODO return : see above
    * @param dcDatas with type & iri
    * @return
    */
   @Path("/")
   @POST
   @PATCH
   @ApiOperation(value = "Creates new data Resources, or updates them (merge) if allowed.",
      notes = "Mere wrapper over atomic typed POST. "
            + "\n<br/><br/>\n"
            + "Resource URI(s) must be provided. "
            + "In order to update an existing resource, " 
            + "up-to-date version is required (GET it first if not saved locally). "
            + "Update is done in merge mode (otherwise rather use PUT) i.e. existing fields are kept, "
            + "with set lists being merged, and other fields replaced by provided ones if any.",
            response = DCResource.class, responseContainer="List", position = 1)
   @ApiImplicitParams({
      @ApiImplicitParam(name=HttpHeaders.AUTHORIZATION, paramType="header", dataType="string",
            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth", defaultValue=AUTHORIZATION_DEFAULT),
      @ApiImplicitParam(name=PROJECT_HEADER, paramType="header", dataType="string",
            value="Examples : oasis.test, oasis.main", defaultValue=PROJECT_DEFAULT)
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
   @ApiResponses(value = {
      @ApiResponse(code = 500, message = "Internal server error"),
      @ApiResponse(code = 409, message = "Conflict : while trying to update existing "
            + "resource (in non-strict POST mode only), optimistic locking error "
            + "(provided resource version is not up-to-date with the server's latest)"),
      @ApiResponse(code = 404, message = "Type model not found"),
      @ApiResponse(code = 400, message = "Bad request : missing content, missing or invalid URIs, "
            + "(if enabled) strict POST mode not respected (version provided or resource already exists), "
            + "field parsing errors (format, consistency with Model & Mixin types...)"),
      @ApiResponse(code = 201, message = "Created : resources have been created")
   })
   List<DCResource> postAllData(
         @ApiParam(value = "Data Resources to create or merge", required = true) List<DCResource> dcDatas)
         throws BadRequestException, NotFoundException, ClientErrorException;
   
   /**
    * replace existing data
    * @param dcData
    * @param type
    * @param iri
    * @return
    */
   @Path("/type/{type}/{__unencoded__iri:.+}") // :.+ to accept even /
   @PUT
   @ApiOperation(value = "Replaces an existing data Resource in the given type.",
      notes = "Mere wrapper over atomic typed POST that replaces (PUT) or merges (POST, PATCH) Resources. "
            + "\n<br/><br/>\n"
            + "Resource URI (Model type and IRI i.e. Internal Resource Identifier) are required "
            + "but also up-to-date version (GET it first if not saved locally) "
            + "sent as an ETag in an If-Match=version precondition, "
            + "ALL fields must be provided.",
            response = DCResource.class, position = 2)
   @ApiImplicitParams({
      @ApiImplicitParam(name=HttpHeaders.AUTHORIZATION, paramType="header", dataType="string",
            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth", defaultValue=AUTHORIZATION_DEFAULT),
      @ApiImplicitParam(name=PROJECT_HEADER, paramType="header", dataType="string",
            value="Examples : oasis.test, oasis.main", defaultValue=PROJECT_DEFAULT)
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
   @ApiResponses(value = {
      @ApiResponse(code = 500, message = "Internal server error"),
      @ApiResponse(code = 409, message = "Conflict : while trying to update existing resource, "
            + "optimistic locking error "
            + "(provided resource version is not up-to-date with the server's latest)"),
      @ApiResponse(code = 404, message = "Resource does not exist, or type model not found"),
      @ApiResponse(code = 400, message = "Bad request : missing content or version, "
            + "missing or invalid URI, non existing resource, "
            + "field parsing errors (format, consistency with Model & Mixin types...)"),
      @ApiResponse(code = 200, message = "OK : the resource has been updated")
   })
   DCResource putDataInType(
         @ApiParam(value = "Data Resource to replace", required = true) DCResource dcData,
         @ApiParam(value = "Model type to replace it in", required = true) @PathParam("type") String modelType,
         @ApiParam(value = "Type-relative resource id", required = true) @PathParam("__unencoded__iri") String iri)
         throws BadRequestException, NotFoundException, ClientErrorException;
   
   /**
    * "mass" replace existing data in type
    * @param dcData with iri
    * @param type
    * @return
    */
   @Path("/type/{type}")
   @PUT
   @ApiOperation(value = "Replaces existing data Resources in the given type.",
      notes = "Mere wrapper over atomic typed PUT. "
            + "\n<br/><br/>\n"
            + "Resource URI (Model type and IRI i.e. Internal Resource Identifier) are required "
            + "but also up-to-date version (GET it first if not saved locally) "
            + "sent within each Resource as the o:version field "
            + "(TODO LATER also as an ETag in an If-Match=version precondition), "
            + "ALL fields must be provided.",
            response = DCResource.class, responseContainer="List", position = 3)
   @ApiImplicitParams({
      @ApiImplicitParam(name=HttpHeaders.AUTHORIZATION, paramType="header", dataType="string",
            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth", defaultValue=AUTHORIZATION_DEFAULT),
      @ApiImplicitParam(name=PROJECT_HEADER, paramType="header", dataType="string",
            value="Examples : oasis.test, oasis.main", defaultValue=PROJECT_DEFAULT)
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
   @ApiResponses(value = {
      @ApiResponse(code = 500, message = "Internal server error"),
      @ApiResponse(code = 409, message = "Conflict : while trying to update existing resource, "
            + "optimistic locking error "
            + "(provided resource version is not up-to-date with the server's latest)"),
      @ApiResponse(code = 404, message = "Resource does not exist, or type model not found"),
      @ApiResponse(code = 400, message = "Bad request : missing content or version, "
            + "missing or invalid URI, non existing resource, "
            + "field parsing errors (format, consistency with Model & Mixin types...)"),
      @ApiResponse(code = 200, message = "OK : resources have been updated")
   })
   List<DCResource> putAllDataInType(
         @ApiParam(value = "Data Resources to replace", required = true) List<DCResource> dcDatas,
         @ApiParam(value = "Model type to update them in", required = true) @PathParam("type") String modelType)
         throws BadRequestException, NotFoundException, ClientErrorException;
   
   /**
    * "mass" replace existing data
    * @param dcData with type & iri
    * @return
    */
   @Path("/")
   @PUT
   @ApiOperation(value = "Replaces existing data Resources.",
      notes = "Mere wrapper over atomic typed PUT. "
            + "\n<br/><br/>\n"
            + "Resource URI (Model type and IRI i.e. Internal Resource Identifier) are required "
            + "but also up-to-date version (GET it first if not saved locally) "
            + "sent within each Resource as the o:version field "
            + "(TODO LATER also as an ETag in an If-Match=version precondition), "
            + "ALL fields must be provided.",
            response = DCResource.class, responseContainer="List", position = 4)
   @ApiImplicitParams({
      @ApiImplicitParam(name=HttpHeaders.AUTHORIZATION, paramType="header", dataType="string",
            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth", defaultValue=AUTHORIZATION_DEFAULT),
      @ApiImplicitParam(name=PROJECT_HEADER, paramType="header", dataType="string",
            value="Examples : oasis.test, oasis.main", defaultValue=PROJECT_DEFAULT)
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
   @ApiResponses(value = {
      @ApiResponse(code = 500, message = "Internal server error"),
      @ApiResponse(code = 409, message = "Conflict : while trying to update existing resource, "
            + "optimistic locking error "
            + "(provided resource version is not up-to-date with the server's latest)"),
      @ApiResponse(code = 404, message = "Resource does not exist, or type model not found"),
      @ApiResponse(code = 400, message = "Bad request : missing content or version, "
            + "missing or invalid URI, non existing resource, "
            + "field parsing errors (format, consistency with Model & Mixin types...)"),
      @ApiResponse(code = 200, message = "OK : resources have been updated")
   })
   List<DCResource> putAllData(
         @ApiParam(value = "Data Resources to replace", required = true) List<DCResource> dcDatas)
         throws BadRequestException, NotFoundException, ClientErrorException;
   
   /**
    * Returns data resource at http://[this container]/dc/[type]/[iri].
    * Supports HTTP ETag (If-Match ; but no If-Modified-Since).
    * @param type
    * @param iri
    * @param versionETag resource version provided as If-None-Match header
    * @return
    */
   @Path("/type/{type}/{__unencoded__iri:.+}") // :.+ to accept even /
   @GET
   @ApiOperation(value = "Get an existing data Resource.",
      notes = "Resource Model type and IRI (Internal Resource Identifier) are required. "
            + "\n<br/><br/>\n"
            + "Allows web-style client-side caching : if client sends its current cached "
            + "resource's version (GET it first if not saved locally) "
            + "as an ETag in an If-None-Match=version precondition, "
            + "the Resource is returned only if this precondition is matched on the server, "
            + "otherwise (if the client's version is not up-to-date with the server's), "
            + "it returns 304 Not Modified, allowing the client to get the Resource from its cache.",
            response = DCResource.class, position = 5)
   @ApiImplicitParams({
      /*@ApiImplicitParam(name=HttpHeaders.IF_NONE_MATCH, paramType="header", dataType="string",
            value="version (if matched, returns 304 instead)")*/
      @ApiImplicitParam(name=HttpHeaders.AUTHORIZATION, paramType="header", dataType="string",
            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth", defaultValue=AUTHORIZATION_DEFAULT),
      @ApiImplicitParam(name=PROJECT_HEADER, paramType="header", dataType="string",
            value="Examples : oasis.test, oasis.main", defaultValue=PROJECT_DEFAULT),
      @ApiImplicitParam(name=VIEW_HEADER, paramType="header", dataType="string",
            value="Examples : nothing (all), '" + VIEW_HEADER_MINIMAL
                  + "' (@id, o:version), 'dc:DublinCore_0' (also "
                  + "@type, dc: fields), 'geo:Area_0,o:Ancestors_0' (with implied dc:DublinCore_0)")
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
   @ApiResponses(value = {
      @ApiResponse(code = 404, message = "Resource does not exist, or type model not found"),
      @ApiResponse(code = 304, message = "Resource not modified (client can reuse its cache's)"),
      @ApiResponse(code = 200, message = "OK : resource found and returned")
   })
   DCResource getData(
         @ApiParam(value = "Model type to look up in", required = true) @PathParam("type") String modelType,
         @ApiParam(value = "Type-relative resource id", required = true) @PathParam("__unencoded__iri") String iri,
         @ApiParam(value = "Resource version", required = false) @HeaderParam(HttpHeaders.IF_NONE_MATCH) Long version)
         throws NotFoundException;
   
   /**
    * Deletes the given Data.
    * If-Match header has to be be provided with the up-to-date version of the Data to delete.
    * TODO LATER also add a "deleted" flag that can be set in POST ?!?
    * @param type
    * @param iri
    * @param versionETag resource version provided as If-Match header
    */
   @Path("/type/{type}/{__unencoded__iri:.+}") // :.+ to accept even /
   @DELETE
   @ApiOperation(value = "Deletes an existing data Resource.",
      notes = "Resource Model type and IRI (Internal Resource Identifier) are required, "
            + "but also up-to-date version (GET it first if not saved locally) "
            + "sent within each Resource as the o:version field. "
            + "Doesn't check whether said data Resource exists beforehands.",
            response = DCResource.class, position = 6)
   @ApiImplicitParams({
      /*@ApiImplicitParam(name=HttpHeaders.IF_MATCH, paramType="header", dataType="string",
            required=true, value="version to match")*/
      @ApiImplicitParam(name=HttpHeaders.AUTHORIZATION, paramType="header", dataType="string",
            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth", defaultValue=AUTHORIZATION_DEFAULT),
      @ApiImplicitParam(name=PROJECT_HEADER, paramType="header", dataType="string",
            value="Examples : oasis.test, oasis.main", defaultValue=PROJECT_DEFAULT)
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
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
         @ApiParam(value = "Type-relative resource id", required = true) @PathParam("__unencoded__iri") String iri,
         @ApiParam(value = "Resource version", required = false) @HeaderParam(HttpHeaders.IF_MATCH) Long version)
         throws BadRequestException, NotFoundException, ClientErrorException;

   
   
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
            + "\n<br/><br/>\n"
            + "Resource URI must be provided but can be relative to type; "
            + "in order to update an existing resource " 
            + "(which first requires that strict POST mode is not enabled), "
            + "up-to-date version is required (GET it first if not saved locally).",
            response = DCResource.class, position = 7)
   @ApiImplicitParams({
      @ApiImplicitParam(name=HttpHeaders.AUTHORIZATION, paramType="header", dataType="string",
            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth", defaultValue=AUTHORIZATION_DEFAULT),
      @ApiImplicitParam(name=PROJECT_HEADER, paramType="header", dataType="string",
            value="Examples : oasis.test, oasis.main", defaultValue=PROJECT_DEFAULT)
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
   @ApiResponses(value = {
      @ApiResponse(code = 500, message = "Internal server error"),
      @ApiResponse(code = 409, message = "Conflict : while trying to update existing "
            + "resource (in non-strict POST mode only), optimistic locking error "
            + "(provided resource version is not up-to-date with the server's latest)"),
      @ApiResponse(code = 404, message = "Type model not found"),
      @ApiResponse(code = 400, message = "Bad request : missing content, missing or invalid URI, "
            + "(if enabled) strict POST mode not respected (version provided or resource already exists), "
            + "field parsing errors (format, consistency with Model & Mixin types...)"),
      @ApiResponse(code = 201, message = "Created : the resource has been created"),
      @ApiResponse(code = 200, message = "OK : the resource has been updated (if not strict POST mode)")
   })
   DCResource postDataInTypeOnGet(
         @ApiParam(value = "Model type to create (or update) it in", required = true) @PathParam("type") String modelType,
         @ApiParam(value = "HTTP method to tunnel over", required = true,
         allowableValues="POST") @QueryParam("method") String method,
         @Context UriInfo uriInfo)
         throws BadRequestException, NotFoundException, ClientErrorException;
   
   /**
    * WARNING commented for now because conflicts with getData(),
    * TODO write interceptor doing the routing making it work  (or client only helper ??)
    * 
    * Provides POST, PUT, PATCH, DELETE methods on GET for easier testing.
    * @param type
    * @param iri
    * @param method PUT, PATCH, DELETE
    * @param versionETag resource version provided as If-Match header in case of DELETE (see deleteData())
    * @param uriInfo to take DCData fields from HTTP request parameters
    * @return
    */
   //@Path("/type/{type}/{__unencoded__iri:.+}") // NB. :.+ to accept even / // TODO rather than :
   @Path("/change/type/{type}/{__unencoded__iri:.+}")
   @GET
   @ApiOperation(value = "NOT IMPLEMENTED YET, (Testing only) udpates or deletes "
            + "an existing data Resource in the given type.",
         notes = "This is a tunneled version of the PUT / PATCH and DELETE operations over GET "
               + "to ease up testing (e.g. on web browsers), prefer PUT / PATCH or DELETE if possible. "
               + "\n<br/><br/>\n"
               + "Resource URI (Model type and IRI i.e. Internal Resource Identifier) are required "
               + "but also up-to-date version (GET it first if not saved locally) "
               + "sent within each Resource as the o:version field, "
               + "TODO all fields must be provided OR PATCH behaviour differ from PUT's. "
               + "Delete doesn't check whether said data Resource exists beforehands.",
               response = DCResource.class, position = 8)
   @ApiImplicitParams({
      @ApiImplicitParam(name=HttpHeaders.AUTHORIZATION, paramType="header", dataType="string",
            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth", defaultValue=AUTHORIZATION_DEFAULT),
      @ApiImplicitParam(name=PROJECT_HEADER, paramType="header", dataType="string",
            value="Examples : oasis.test, oasis.main", defaultValue=PROJECT_DEFAULT)
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
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
         @ApiParam(value = "Type-relative resource id", required = true) @PathParam("__unencoded__iri") String iri,
         @ApiParam(value = "HTTP method to tunnel over", required = true,
         allowableValues="POST") @QueryParam("method") String method,
         @ApiParam(value = "Resource version", required = false) @PathParam(HttpHeaders.IF_MATCH) Long versionETag,
         @Context UriInfo uriInfo)
         throws BadRequestException, NotFoundException;


   
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
      notes = NATIVE_QUERY_DOC_TYPE,
            response = DCResource.class, responseContainer="List", position = 9)
   @ApiImplicitParams({
      @ApiImplicitParam(name=QUERY_PARAMETERS, paramType="query", dataType="string", allowMultiple=true,
            value="Each other HTTP query parameter (in Swagger UI, enter them "
                  + "as a single URL query string, ex. name=London&population=>100000) "
                  + "is a Datacore query criteria"),
      @ApiImplicitParam(name=DEBUG_HEADER, paramType="header", dataType="boolean", allowMultiple=false,
            value="Enable debug on query.", defaultValue="false"),//TODO:Default to true for test. Change in prod
      // NB. an implicit ACCEPT header param would not work with Swagger UI
      @ApiImplicitParam(name=HttpHeaders.AUTHORIZATION, paramType="header", dataType="string",
            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth", defaultValue=AUTHORIZATION_DEFAULT),
      @ApiImplicitParam(name=PROJECT_HEADER, paramType="header", dataType="string",
            value="Examples : oasis.test, oasis.main", defaultValue=PROJECT_DEFAULT),
      @ApiImplicitParam(name=VIEW_HEADER, paramType="header", dataType="string",
            value="Examples : nothing (all), '" + VIEW_HEADER_MINIMAL
                  + "' (@id, o:version), 'dc:DublinCore_0' (also "
                  + "@type, dc: fields), 'geo:Area_0,o:Ancestors_0' (with implied dc:DublinCore_0)")
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
   @ApiResponses(value = {
      @ApiResponse(code = 404, message = "Type model not found"),
      @ApiResponse(code = 400, message = "Bad request : field parsing errors (format, "
            + "consistency with Model & Mixin types...)"),
      @ApiResponse(code = 200, message = "OK : resources found and returned")
   })
   List<DCResource> findDataInType(@PathParam("type") String modelType, @Context UriInfo uriInfo,
         @ApiParam(value="Pagination start") @DefaultValue("0") @QueryParam(START_PARAM) Integer start,
         @ApiParam(value="Pagination limit") @DefaultValue("10") @QueryParam(LIMIT_PARAM) Integer limit,
         @ApiParam(value="Debug", required = false) @QueryParam(DEBUG_PARAM) boolean debug)
         throws BadRequestException, NotFoundException;


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
      notes = NATIVE_QUERY_DOC_TYPES,
            response = DCResource.class, responseContainer="List", position = 9)
   @ApiImplicitParams({
      @ApiImplicitParam(name=QUERY_PARAMETERS, paramType="query", dataType="string", allowMultiple=true,
            value="Each other HTTP query parameter (in Swagger UI, enter them "
                  + "as a single URL query string, ex. name=London&population=>100000) "
                  + "is a Datacore query criteria."),
      @ApiImplicitParam(name=HttpHeaders.AUTHORIZATION, paramType="header", dataType="string",
      value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth", defaultValue=AUTHORIZATION_DEFAULT),
      @ApiImplicitParam(name=PROJECT_HEADER, paramType="header", dataType="string",
            value="Examples : oasis.test, oasis.main", defaultValue=PROJECT_DEFAULT),
      @ApiImplicitParam(name=VIEW_HEADER, paramType="header", dataType="string",
            value="Examples : nothing (all), '" + VIEW_HEADER_MINIMAL
                  + "' (@id, o:version), 'dc:DublinCore_0' (also "
                  + "@type, dc: fields), 'geo:Area_0,o:Ancestors_0' (with implied dc:DublinCore_0)")
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
   @ApiResponses(value = {
      @ApiResponse(code = 404, message = "Type model not found"),
      @ApiResponse(code = 400, message = "Bad request : field parsing errors (format, "
            + "consistency with Model & Mixin types...)"),
      @ApiResponse(code = 200, message = "OK : resources found and returned")
   })
   List<DCResource> findData(@Context UriInfo uriInfo,
         @ApiParam(value="Pagination start") @DefaultValue("0") @QueryParam(START_PARAM) Integer start,
         @ApiParam(value="Pagination limit") @DefaultValue("10") @QueryParam(LIMIT_PARAM) Integer limit)
         throws BadRequestException, NotFoundException;

   /**
    * Returns all Datacore data of the given type that are found by the given query
    * written in the given language.
    * It is a "native" query in that it is implemented as a single MongoDB query
    * with pagination by default.
    * TODO "only Queriable fields" ?!?
    * 
    * Supported query languages are :
    * 
    * * LDPQL
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
   @ApiOperation(value = "Executes the query in the given Model type and returns found resources.",
      notes = "Returns all Datacore data of the given type that are found "
            + "by the given query written in the given language. "
            + "It is a non-'native', limited query in that it is implemented by several MongoDB queries "
            + "whose criteria on non-data (URI) fields are limited as defined in their model. Therefore "
            + "it is advised to rather do several findDataInType queries. "
            + "\n<br/><br/>\n"
            + "Supported query languages : "
            + "\n<br/><br/>\n"
            + "<strong>LDPQL :</strong>"
            + "\n<br/>\n"
            + "an LDPQL query is merely the HTTP query part of a GET /(type/${type}/) operation, "
            + "for instance : \"name=John&age=>10&age=<20\"."
            + "\n<br/><br/>\n"
            + "Possible future query languages : "
            + "\n<br/><br/>\n"
            + "<strong>SPARQL :</strong>"
            + "\n<br/>\n"
            + "this is a limited form of SPARQL's SELECT statement. By default, the first variable "
            + "receives an rdf:type predicate to the given model type. All other resource variables "
            + "must be referenced from within this first one (even indirectly)."
            + "Fields are adressed in their dotted form without root type."
            + "\n<br/>\n"
            + "Allowed : "
            + "\n<br/>\n"
            + "PREFIX ?, SELECT(more ??), FROM (type ?), WHERE "
            + "\n<br/>\n"
            + "a / rdf:type, rdfs:subClassOf ? (for types & aspects) ; always AND / && (never ||), OPTIONAL ?!?"
            + "\n<br/>\n"
            + " 1.0^^xsd:float ?? "
            + "\n<br/>\n"
            + "VALUES ?? "
            + "\n<br/>\n"
            + "FILTER : ! / NOT EXISTS ?, !=, NOT IN, <, > "
            + "\n<br/>\n"
            + "and string functions : REGEX, more ???, langMatches ??, IRI ?? "
            + "\n<br/>\n"
            + "count on lists, else no aggregate function (TODO LATER2 using map / reduce) "
            + "\n<br/>\n"
            + "LIMIT, OFFSET, ORDER BY",
            response = DCResource.class, responseContainer="List", position = 10)
   @ApiImplicitParams({
      @ApiImplicitParam(name=HttpHeaders.AUTHORIZATION, paramType="header", dataType="string",
            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth", defaultValue=AUTHORIZATION_DEFAULT),
      @ApiImplicitParam(name=PROJECT_HEADER, paramType="header", dataType="string",
            value="Examples : oasis.test, oasis.main", defaultValue=PROJECT_DEFAULT),
      @ApiImplicitParam(name=VIEW_HEADER, paramType="header", dataType="string",
            value="Examples : nothing (all), '" + VIEW_HEADER_MINIMAL
                  + "' (@id, o:version), 'dc:DublinCore_0' (also "
                  + "@type, dc: fields), 'geo:Area_0,o:Ancestors_0' (with implied dc:DublinCore_0)")
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
   @ApiResponses(value = {
      @ApiResponse(code = 404, message = "Type model not found"),
      @ApiResponse(code = 400, message = "Bad request : field parsing errors (format, "
            + "consistency with Model & Mixin types...)"),
      @ApiResponse(code = 200, message = "OK : resources found and returned")
   })
   List<DCResource> queryDataInType(@PathParam("type") String modelType,
         @QueryParam("query") String query,
         @DefaultValue("SPARQL") @QueryParam("language") String language)
         throws BadRequestException, NotFoundException;


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
            + "\n<br/><br/>\n"
            + "Supported query languages could be : "
            + "\n<br/><br/>\n"
            + "<strong>LDPQL :</strong>"
            + "\n<br/>\n"
            + "an LDPQL query is merely the HTTP query part of a GET /(type/${type}/) operation, "
            + "for instance : \"name=John&age=>10&age=<20\"."
            + "\n<br/><br/>\n"
            + "<strong>SPARQL :</strong>"
            + "\n<br/>\n"
            + "this is a limited form of SPARQL's SELECT statement. When not referenced from within "
            + "another root resource variable whose model type is specified (even indirectly), "
            + "other root resource variables must have their model type provided in an rdf:type predicate."
            + "Fields are adressed in their dotted form with root type."
            + "\n<br/>\n"
            + "Allowed : "
            + "\n<br/>\n"
            + "PREFIX ?, SELECT(more ??), FROM (type ?), WHERE "
            + "\n<br/>\n"
            + "a / rdf:type, rdfs:subClassOf ? (for types & aspects) ; always AND / && (never ||), OPTIONAL ?!?"
            + "\n<br/>\n"
            + " 1.0^^xsd:float ?? "
            + "\n<br/>\n"
            + "VALUES ?? "
            + "\n<br/>\n"
            + "FILTER : ! / NOT EXISTS ?, !=, NOT IN, <, > "
            + "\n<br/>\n"
            + "and string functions : REGEX, more ???, langMatches ??, IRI ?? "
            + "\n<br/>\n"
            + "count on lists, else no aggregate function (TODO LATER2 using map / reduce) "
            + "\n<br/>\n"
            + "LIMIT, OFFSET, ORDER BY",
            response = DCResource.class, responseContainer="List", position = 11)
   @ApiImplicitParams({
      @ApiImplicitParam(name=HttpHeaders.AUTHORIZATION, paramType="header", dataType="string",
            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth", defaultValue=AUTHORIZATION_DEFAULT),
      @ApiImplicitParam(name=PROJECT_HEADER, paramType="header", dataType="string",
            value="Examples : oasis.test, oasis.main", defaultValue=PROJECT_DEFAULT),
      @ApiImplicitParam(name=VIEW_HEADER, paramType="header", dataType="string",
            value="Examples : nothing (all), '" + VIEW_HEADER_MINIMAL
                  + "' (@id, o:version), 'dc:DublinCore_0' (also "
                  + "@type, dc: fields), 'geo:Area_0,o:Ancestors_0' (with implied dc:DublinCore_0)")
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
   @ApiResponses(value = {
      @ApiResponse(code = 404, message = "Type model not found"),
      @ApiResponse(code = 400, message = "Bad request : field parsing errors (format, "
            + "consistency with Model & Mixin types...)"),
      @ApiResponse(code = 200, message = "OK : resources found and returned")
   })
   List<DCResource> queryData(@QueryParam("query") String query,
         @DefaultValue("SPARQL") @QueryParam("language") String language)
         throws BadRequestException, NotFoundException;
   
   
   @Path("/h/{type}/{__unencoded__iri:.+}/{version}")
   @GET
   @ApiOperation(value = "Return a matching resource of the given iri and version.",
      			 notes = "Resources of a model can be historized by defining the isHistorizable field to true."
      					 + "If a resource is historized it can be retrieved with this operation."
      					 + "The resource is identified by an IRI and a version of resources."
      					 + "e.g. if resource URI = http://data-test.ozwillo.com/dc/type/sample.marka.company/1 and you want version = 0,"
      					 + "the parameters would be : "
      					 + " - type : sample.marka.company"
      					 + " - iri : 1 (the URI is made like this : /dc/type/{type}/{__unencoded__iri})"
      					 + " - version : 0 (or whatever version you need)",
      			 response = DCResource.class, position = 12)
   @ApiImplicitParams({
      @ApiImplicitParam(name=HttpHeaders.AUTHORIZATION, paramType="header", dataType="string",
            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth", defaultValue=AUTHORIZATION_DEFAULT),
      @ApiImplicitParam(name=PROJECT_HEADER, paramType="header", dataType="string",
            value="Examples : oasis.test, oasis.main", defaultValue=PROJECT_DEFAULT),
      @ApiImplicitParam(name=VIEW_HEADER, paramType="header", dataType="string",
            value="Examples : nothing (all), '" + VIEW_HEADER_MINIMAL
                  + "' (@id, o:version), 'dc:DublinCore_0' (also "
                  + "@type, dc: fields), 'geo:Area_0,o:Ancestors_0' (with implied dc:DublinCore_0)")
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
   @ApiResponses(value = {
      @ApiResponse(code = 404, message = "Resource not found with this IRI and version"),
      @ApiResponse(code = 400, message = "Bad request : non-existent model"),
      @ApiResponse(code = 200, message = "OK : resource found and returned")
   })
   DCResource findHistorizedResource(@ApiParam(value = "Resource's model type", required = true) @PathParam("type") String modelType,
		   							 @ApiParam(value = "Type-relative resource id", required = true) @PathParam("__unencoded__iri") String iri,
		   							 @ApiParam(value = "Resource version", required = true) @PathParam("version") Integer version) throws BadRequestException, NotFoundException;

   @Path("/aliases/{type}/{__unencoded__iri:.+}")
   @GET
   @ApiOperation(value = "Return the aliases an existing data Resource",
                 response = String.class, responseContainer = "List", position = 13)
   @ApiImplicitParams({
      @ApiImplicitParam(name=HttpHeaders.AUTHORIZATION, paramType="header", dataType="string",
            value="OAuth2 Bearer or (DEV MODE ONLY) Basic Auth", defaultValue=AUTHORIZATION_DEFAULT),
      @ApiImplicitParam(name=PROJECT_HEADER, paramType="header", dataType="string",
            value="Examples : oasis.test, oasis.main", defaultValue=PROJECT_DEFAULT),
      // NB. @ApiImplicitParam.dataType MUST be provided, see https://github.com/wordnik/swagger-core/issues/312
   })
   @ApiResponses(value = {
      @ApiResponse(code = 404, message = "Resource does not exist, or type model not found"),
      @ApiResponse(code = 304, message = "Resource not modified (client can reuse its cache's)"),
      @ApiResponse(code = 200, message = "OK : resource found and returned")
   })
   List<String> getAliases(
        @ApiParam(value = "Model type to look up in", required = true) @PathParam("type") String modelType,
        @ApiParam(value = "Type-relative resource id", required = true) @PathParam("__unencoded__iri") String iri)
        throws NotFoundException;
}