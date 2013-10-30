package org.oasis.datacore.rest.server;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.oasis.datacore.core.entity.DCEntityService;
import org.oasis.datacore.core.entity.EntityQueryService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.model.DCURI;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.oasis.datacore.rest.server.parsing.DCQueryParsingContext;
import org.oasis.datacore.rest.server.parsing.DCResourceParsingContext;
import org.oasis.datacore.rest.server.parsing.ResourceParsingException;
import org.oasis.datacore.rest.server.parsing.ResourceParsingLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Returned HTTP status : follows recommended standards, see
 * http://stackoverflow.com/questions/2342579/http-status-code-for-update-and-delete/18981344#18981344
 * 
 * TODOs :
 * * error messages as constants ?
 * 
 * @author mdutoo
 *
 */
@Path("dc") // relative path among other OASIS services
@Consumes(MediaType.APPLICATION_JSON) // TODO finer media type ex. +oasis-datacore ??
@Produces(MediaType.APPLICATION_JSON)
///@Component("datacoreApiServer") // else can't autowire Qualified ; TODO @Service ?
public class DatacoreApiImpl implements DatacoreApi {

   /** to be able to build a full uri (for GET, DELETE, possibly to build missing or custom URI...) */
   @Value("${datacoreApiServer.baseUrl}") 
   private String baseUrl; // "http://" + "data.oasis-eu.org"

   private boolean strictPostMode = false;
   
   @Autowired
   private DCEntityService entityService;
   @Autowired
   @org.springframework.beans.factory.annotation.Qualifier("datacore.entityQueryService")
   private EntityQueryService entityQueryService;
   //@Autowired
   //private DCDataEntityRepository dataRepo; // NO rather for (meta)model, for data can't be used because can't specify collection
   @Autowired
   private MongoOperations mgo; // ???
   @Autowired
   private DCModelService modelService;

   private ObjectMapper mapper = new ObjectMapper(); // or per-request ??
   private int typeIndexInType = "dc/type/".length(); // TODO use Pattern & DC_TYPE_PATH

   private static Set<String> resourceNativeJavaFields = new HashSet<String>();
   private static Set<String> findConfParams = new HashSet<String>();
   static {
      resourceNativeJavaFields.add("uri");
      resourceNativeJavaFields.add("version");
      resourceNativeJavaFields.add("types");
      resourceNativeJavaFields.add("created");
      resourceNativeJavaFields.add("lastModified");
      resourceNativeJavaFields.add("createdBy");
      resourceNativeJavaFields.add("lastModifiedBy");
      findConfParams.add("start");
      findConfParams.add("limit");
      findConfParams.add("sort");
   }
   
   public DCResource postDataInType(DCResource resource, String modelType/*, Request request*/) {
      DCResource postedResource = this.internalPostDataInType(resource, modelType, true, !this.strictPostMode);
      
      //return dcData; // rather 201 Created with ETag header :
      String httpEntity = postedResource.getVersion().toString(); // no need of additional uri because only for THIS resource
      EntityTag eTag = new EntityTag(httpEntity);
      throw new WebApplicationException(Response.status(Response.Status.CREATED)
            .tag(eTag) // .lastModified(dataEntity.getLastModified().toDate())
            .entity(postedResource)
            .type(MediaType.APPLICATION_JSON).build());
   }
   
   /**
    * Does the actual work of postDataInType except returning status & etag,
    * so it can also be used in post/putAllData(InType)
    * @param resource
    * @param modelType
    * @return
    */
   public DCResource internalPostDataInType(DCResource resource, String modelType,
         boolean canCreate, boolean canUpdate) {
      // TODO pass request to validate ETag,
      // or rather in a CXF ResponseHandler (or interceptor but closer to JAXRS 2) see http://cxf.apache.org/docs/jax-rs-filters.html
      // by getting result with outMessage.getContent(Object.class), see ServiceInvokerInterceptor.handleMessage() l.78
      
      // conf :
      // TODO header or param...
      boolean detailedErrorsMode = true;
      boolean replaceBaseUrlMode = true;
      boolean normalizeUrlMode = true;

      boolean isCreation = true;
      
      DCModel dcModel = modelService.getModel(modelType); // NB. type can't be null thanks to JAXRS
      if (dcModel == null) {
         throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
             .entity("Unknown Model type " + modelType).type(MediaType.TEXT_PLAIN).build());
      }
      modelType = dcModel.getName(); // normalize ; TODO useful ?

      if (resource == null) {
         throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
             .entity("No data").type(MediaType.TEXT_PLAIN).build());
      }
      
      Long version = resource.getVersion();
      if (version != null && version >= 0) {
         if (!canUpdate) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                  .entity("Version is forbidden in POSTed data resource to create in strict POST mode")
                  .type(MediaType.TEXT_PLAIN).build());
         } else {
            isCreation = false;
         }
      } else if (!canCreate) {
         throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
               .entity("Version of data resource to update is required in PUT")
               .type(MediaType.TEXT_PLAIN).build());
      }
      
      String uri = resource.getUri();
      if (uri == null || uri.length() == 0) {
         // TODO LATER accept only iri from resource
         // TODO LATER2 accept empty uri and build it according to type (governance)
         // for now, don't support it :
         throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
               .entity("Missing uri").type(MediaType.TEXT_PLAIN).build());
      }
      if (!uri.startsWith("http://")) { // TODO or accept & replace https ?
         // accept local type-relative uri :
         uri = new DCURI(this.baseUrl, modelType, uri).toString();
      } else {
         try {
            uri = normalizeAdaptUri(uri, normalizeUrlMode, replaceBaseUrlMode);
         } catch (ResourceParsingException rpex) {
            // TODO LATER rather context & for multi post
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                  .entity(uri + " should be an uri but " + rpex.getMessage()).type(MediaType.TEXT_PLAIN).build());
         }
      }

      DCEntity dataEntity = null;
      if (!canUpdate || !isCreation) {
         dataEntity = entityService.getByUriId(uri, dcModel);
         if (dataEntity != null) {
            if (!canUpdate) {
               // already exists, but only allow creation
               throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                     .entity("Already exists at uri (forbidden in strict POST mode) :\n"
                           + dataEntity.toString()).type(MediaType.TEXT_PLAIN).build());
            }/* else {
               // HTTP ETag checking (provided in an If-Match header) :
               // NB. DISABLED FOR NOW because will be checked at db save time anyway
               // (and to support multiple POSTs, would have to be complex or use
               // the given dataResource.version instead)
               String httpEntity = dataEntity.getVersion().toString(); // no need of additional uri because only for THIS resource
               EntityTag eTag = new EntityTag(httpEntity);
               ResponseBuilder builder = request.evaluatePreconditions(eTag); // NB. request is injected
               if(builder != null) {
                  // client is not up to date (send back 412)
                  throw new WebApplicationException(builder.build());
               }
            }*/
         } else {
            if (!canCreate) {
               throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                     .entity("Data resource doesn't exist (forbidden in PUT)")
                     .type(MediaType.TEXT_PLAIN).build());
            }
         }
      }
      
      if (dataEntity == null) {
         dataEntity = new DCEntity();
         dataEntity.setUri(uri);
      }
      dataEntity.setVersion(version);

      Map<String, Object> dataProps = resource.getProperties();
      
      dataEntity.setModelName(dcModel.getName()); // TODO LATER2 check that same (otherwise ex. external approvable contrib ??)
      dataEntity.setVersion(version);
      
      // parsing resource according to model :
      DCResourceParsingContext resourceParsingContext = new DCResourceParsingContext(dcModel, uri);
      //List<DCEntity> embeddedEntitiesToAlsoUpdate = new ArrayList<DCEntity>(); // TODO embeddedEntitiesToAlsoUpdate ??
      //resourceParsingContext.setEmbeddedEntitiesToAlsoUpdate(embeddedEntitiesToAlsoUpdate);
      resourceToEntityFields(dataProps, dataEntity.getProperties(), dcModel.getFieldMap(),
            resourceParsingContext); // TODO dcModel.getFieldNames(), abstract DCModel-DCMapField ??
      
      if (resourceParsingContext.hasErrors()) {
         // TODO or render (HTML ?) template ?
         StringBuilder sb = new StringBuilder("Parsing aborted, found "
               + resourceParsingContext.getErrors().size() + " errors "
               + ((resourceParsingContext.hasWarnings()) ? "(and "
               + resourceParsingContext.getWarnings().size() + " warnings) " : "")
               + "while parsing resource with uri " + uri + " according to type Model "
               + dcModel.getName() + ".\nErrors:");
         for (ResourceParsingLog error : resourceParsingContext.getErrors()) {
            sb.append("\n   - for field ");
            sb.append(error.getFieldFullPath());
            sb.append(" : ");
            sb.append(error.getMessage());
            if (error.getException() != null) {
               sb.append(". Exception message : ");
               sb.append(error.getException().getMessage());
               if (detailedErrorsMode) {
                  sb.append("\n      Exception details : \n\n");
                  sb.append(ExceptionUtils.getFullStackTrace(error.getException()));
                  sb.append("\n");
               }
            }
         }
         
         if (resourceParsingContext.hasWarnings()) {
            // TODO or render (HTML ?) template ?
            sb.append("\nWarnings:");
            for (ResourceParsingLog error : resourceParsingContext.getErrors()) {
               sb.append("\n   - for field ");
               sb.append(error.getFieldFullPath());
               sb.append(" : ");
               sb.append(error.getMessage());
               if (error.getException() != null) {
                  sb.append(". Exception message : ");
                  sb.append(error.getException().getMessage());
                  if (detailedErrorsMode) {
                     sb.append("\n      Exception details : \n\n");
                     sb.append(ExceptionUtils.getFullStackTrace(error.getException()));
                     sb.append("\n");
                  }
               }
            }
         }
         
         throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
               .entity(sb.toString()).type(MediaType.TEXT_PLAIN).build());
      } // else TODO if warnings return them as response header ?? or only if failIfWarningsMode ??
      
      // TODO LATER 2nd pass : pre save hooks

      String collectionName = dcModel.getCollectionName(); // or mere type ?
      if (isCreation) {
         // will fail if exists
         mgo.insert(dataEntity, collectionName); // TODO does it enforce version if exists ??
         
      } else {
         try {
            mgo.save(dataEntity, collectionName);
         } catch (OptimisticLockingFailureException olfex) {
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                  .entity("Trying to update data resource without up-to-date version but "
                        + dataEntity.getVersion()).type(MediaType.TEXT_PLAIN).build());
         }
      }

      // TODO LATER 2nd pass : post save hooks

      //Map<String, Object> dcDataProps = new HashMap<String,Object>(dataEntity.getProperties());
      //DCData dcData = new DCData(dcDataProps);
      //dcData.setUri(uri);
      resource.setVersion(dataEntity.getVersion());
      // TODO setProperties if changed ex. default values ? or copied queriable fields, or on behaviours ???
      // for model.getDefaultValues() { dcData.setProperty(name, defaultValue);
      return resource;
   }
   
   private Object resourceToEntityValue(Object resourceValue, DCField dcField,
         DCResourceParsingContext resourceParsingContext) throws ResourceParsingException {
      Object entityValue;
      if (resourceValue == null) {
         if (!dcField.isRequired()) { // TODO ? for all ?! or only for MapField ??
            throw new ResourceParsingException("map Field value is not a JSON Object : " + resourceValue);
         }
         entityValue = null;
         
      } else if ("string".equals(dcField.getType())) {
         if (!(resourceValue instanceof String)) {
            throw new ResourceParsingException("string Field value is not a JSON string : " + resourceValue);
         }
         entityValue = (String) resourceValue;
         
      } else if ("boolean".equals(dcField.getType())) {
         if (!(resourceValue instanceof Boolean)) {
            throw new ResourceParsingException("boolean Field value is not a JSON boolean : " + resourceValue);
         }
         entityValue = (Boolean) resourceValue; // TODO LATER also allow String  ??
         
      } else if ("int".equals(dcField.getType())) {
         if (!(resourceValue instanceof Integer)) {
            throw new ResourceParsingException("int Field value is not a JSON integer : " + resourceValue);
         }
         entityValue = (Integer) resourceValue; // TODO LATER also allow String  ??
         
      } else if ("float".equals(dcField.getType())) {
         if (!(resourceValue instanceof Float)) {
            if (resourceValue instanceof Integer) {
               entityValue = new Float((Integer) resourceValue);
               resourceParsingContext.addWarning("float Field value is a JSON integer : " + resourceValue
                     + ", which allowed as fallback be should rather be a JSON float");
            }
            throw new ResourceParsingException("float Field value is not a JSON float "
                  + "(nor integer, which is allowed as fallback) : " + resourceValue);
         }
         entityValue = (Float) resourceValue; // TODO LATER also allow String  ??
         
      } else if ("long".equals(dcField.getType())) {
         // Javascript has no long : http://javascript.about.com/od/reference/g/rlong.htm
         // so supported through String instead
         if (!(resourceValue instanceof String)) {
            throw new ResourceParsingException("long Field value is not a string : " + resourceValue);
         }
         try {
            entityValue = mapper.readValue((String) resourceValue, Long.class);
         } catch (IOException ioex) {
            throw new ResourceParsingException("IO error while reading long-formatted string : "
                  + resourceValue, ioex);
         } catch (Exception ex) {
            throw new ResourceParsingException("long Field value is not a long-formatted string : "
                  + resourceValue, ex);
         }
         
      } else if ("double".equals(dcField.getType())) {
         if (!(resourceValue instanceof Double)) {
            throw new ResourceParsingException("double Field value is not a JSON double : " + resourceValue);
         }
         entityValue = (Double) resourceValue; // TODO LATER also allow String  ??
         
      } else if ("date".equals(dcField.getType())) {
         if (!(resourceValue instanceof String)) {
            throw new ResourceParsingException("date Field value is not a string : " + resourceValue);
         }
         entityValue = parseDate((String) resourceValue);
         
      /*} else if ("i18n".equals(dcField.getType())) { // TODO i18n better
         entityValue = (HashMap<?,?>) resourceValue; // TODO NOOOO _i18n
         // TODO locale & fallback
         
      } else if ("wkt".equals(dcField.getType())) { // TODO LATER2 ??
         entityValue = (String) resourceValue;*/
         
      } else if ("map".equals(dcField.getType())
            || "i18n".equals(dcField.getType())) { // TODO i18n better
         // NB. already tested not to be null
         if (!(resourceValue instanceof Map<?,?>)) {
            throw new ResourceParsingException("map Field value is not a JSON Object : " + resourceValue);
         }
         @SuppressWarnings("unchecked")
         Map<String, Object> dataMap = (Map<String,Object>) resourceValue;
         HashMap<String, Object> entityMap = new HashMap<String,Object>(dataMap.size());
         resourceToEntityFields(dataMap, entityMap, ((DCMapField) dcField).getMapFields(),
               resourceParsingContext);
         entityValue = entityMap;
         
      } else if ("list".equals(dcField.getType())) {
         if (!(resourceValue instanceof List<?>)) {
            throw new ResourceParsingException("list Field value is not a JSON Array : " + resourceValue);
         }
         List<?> dataList = (List<?>) resourceValue;
         ArrayList<Object> entityList = new ArrayList<Object>(dataList.size());
         DCField listElementField = ((DCListField) dcField).getListElementField(); // all items are of the same type
         int i = 0;
         for (Object resourceItem : dataList) {
            Object entityItem;
            try {
               resourceParsingContext.enter(listElementField, listElementField);
               entityItem = resourceToEntityValue(resourceValue, listElementField, resourceParsingContext);
            } catch (ResourceParsingException rpex) {
               resourceParsingContext.addError("Error while parsing list element Field value " + resourceItem
                     + " of JSON type " + ((resourceItem == null) ? "null" : resourceItem.getClass())
                     + " at position " + i, rpex);
               continue;
            } finally {
               resourceParsingContext.exit();
               i++;
            }
            entityList.add(entityItem);
         }
         entityValue = entityList;
         
      } else if ("resource".equals(dcField.getType())) { // or "reference" ?
         if (resourceValue instanceof String) {
            // "flattened" (JSONLD) : uri ref
            String stringUriValue = (String) resourceValue;
            
            // normalize, adapt & check TODO in DCURI :
            boolean normalizeUrlMode = true;
            boolean replaceBaseUrlMode = true;
            DCURI dcUri = normalizeAdaptCheckTypeOfUri(stringUriValue,
                  normalizeUrlMode, replaceBaseUrlMode);
            DCModel refModel = modelService.getModel(dcUri.getType()); // TODO LATER from cached model ref in DCURI
            
            // checking that it exists :
            DCEntity refEntity = entityService.getByUriId(dcUri.toString(), refModel);
            if (refEntity == null) {
               throw new ResourceParsingException("Can't find data of resource type " + refModel.getName()
                     + " referenced by resource Field of URI value " + dcUri.toString());
            }
            // check type :
            // TODO mixins / aspects / type constraints
            /*if (!dcModelService.hasType(refEntity, ((DCResourceField) dcField).getTypeConstraints())) {
               throw new ResourceParsingException("Target resource referenced by resource Field of URI value "
                     + dcUri + " does not match type constraints TODO");
            }*/
            
            // TODO TODO provide refEntity in model (ex. DCResourceValue) OR IN GRAPH,
            // ex. for 2nd pass or in case of expanded + embedded return ?!?
            entityValue = refEntity;
            entityValue = dcUri.toString();
            
         } else if (resourceValue instanceof Map<?,?>) {
            // "framed" (JSONLD) / embedded : (partial) copy
            boolean normalizeUrlMode = true;
            boolean replaceBaseUrlMode = true;
            boolean allowNew = false;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String,Object>) resourceValue;
            Object entityValueUri = dataMap.get("uri");
            if (entityValueUri == null) {
               throw new ResourceParsingException("Can't find uri among properties of Object value of "
                     + "resource Field but only " + dataMap);
            } else if (!(entityValueUri instanceof String)) {
               throw new ResourceParsingException("Property uri of Object value of "
                     + "resource Field is not a String but " + entityValueUri
                     + " (" + entityValueUri.getClass() + ")");
            }
            // check uri, type & get entity (as above) :
            DCURI dcUri = normalizeAdaptCheckTypeOfUri((String) entityValueUri,
                  normalizeUrlMode, replaceBaseUrlMode);
            DCModel valueModel = modelService.getModel(dcUri.getType()); // TODO LATER from cached model ref in DCURI

            // checking that it exists :
            DCEntity entityEntityValue = entityService.getByUriId(dcUri.toString(), valueModel);
            
            // TODO TODO provide refEntity in model (ex. DCResourceValue) OR IN GRAPH,
            // ex. for 2nd pass or in case of expanded + embedded return ?!?
            
            if (entityEntityValue == null) {
               // TODO allow not exists & creation ????!?
               if (!allowNew) {
                  throw new ResourceParsingException("Can't find data of resource type " + dcUri.getType()
                        + " referenced by resource Field of Object value with URI property " + dcUri);
               }
               entityEntityValue = new DCEntity();
               entityEntityValue.setModelName(dcUri.getType());
               entityEntityValue.setUri(dcUri.toString());
               // NB. no version yet
               
            } else {
               // reuse existing entity as base : PATCH-like behaviour
               // check type :
               // TODO mixins / aspects / type constraints
               /*if (!dcModelService.hasType(entityEntityValue, ((DCResourceField) dcField).getTypeConstraints())) {
                  throw new ResourceParsingException("Target resource " + dcUri.getType()
                        + " referenced by resource Field of Object value with URI property " + dcUri
                        + " does not match type constraints TODO");
               }*/
            }
            
            // get version if any, may be used for transaction :
            // TODO parseLong
            Object entityValueVersion = dataMap.get("version");
            if (entityValueVersion == null) {
               // allowed (NB. but then can't update)
            } else if (entityValueVersion instanceof Integer) { // TODO refactor to parseInt ?
               entityEntityValue.setVersion(new Long((Integer) entityValueVersion));
            } else if (entityValueVersion instanceof String) { // TODO refactor to parseLongFromString ?
               try {
                  // Javascript don't support Long so allow them as String :
                  entityEntityValue.setVersion(Long.parseLong((String) entityValueVersion));
               } catch (NumberFormatException nfex) {
                  // TODO add error to context
               }
            } else {
               // TODO add error to context
            }
            if (entityValueVersion != null) {
               // TODO LATER set up transaction ?!
            }
            
            // parse other fields :
            // TODO for what use, multiple update or even creation (rather flattened ?) ?? constraints ???
            // TODO do it later to allow all-encompassing transaction ???
            // clean by removing native fields : (TODO or clone first, ex. if immutable ??)
            for (String resourceNativeJavaField : resourceNativeJavaFields) {
               dataMap.remove(resourceNativeJavaField);
            }
            resourceToEntityFields(dataMap, entityEntityValue.getProperties(),
                  valueModel.getFieldMap(), resourceParsingContext);
            // TODO TODO add entityEntityValue to graph ?! again, for what use :
            // multiple update or even creation (rather flattened ?) ??
            //embeddedEntitiesToAlsoUpdate.add(entityEntityValue);
            // constraints ex. "update only if these fields have these values (in addition to version)" ???
            // cache it in DCURI for ex. rendering returned value ??
            // dcUri.setEntity(entityEntityValue);
            
            entityValue = dcUri.toString();
            
         } else {
            // unknown type for resource value : to help debug model design, add error to context
            throw new ResourceParsingException("Unknown Field type " + dcField.getType());
         }
         
      } else {
         // unknown type IN MODEL : to help debug model design, add error to context
         throw new ResourceParsingException("Unknown Field type " + dcField.getType()
               + ". Supported field types are : TODO"); // TODO better
      }
      
      return entityValue;
   }
   
   /**
    * TODO move in DCURI
    * @param stringUriValue
    * @param normalizeUrlMode
    * @param replaceBaseUrlMode
    * @return
    * @throws ResourceParsingException
    */
   private String normalizeAdaptUriToPathWithoutSlash(String stringUriValue,
         boolean normalizeUrlMode, boolean replaceBaseUrlMode) throws ResourceParsingException {
      String urlPathWithoutSlash = null;
      
      // normalize
      if (normalizeUrlMode) {
         URL urlValue;
         try {
            // NB. Datacore URIs should ALSO be URLs
            urlValue = new URI(stringUriValue).normalize().toURL(); // from ex. http://localhost:8180//dc/type//country/UK
            stringUriValue = urlValue.toString(); // ex. http://localhost:8180/dc/type/country/UK
            urlPathWithoutSlash = urlValue.getPath().substring(1); // ex. dc/type/country/UK
         } catch (URISyntaxException muex) {
            throw new ResourceParsingException("Value of field of type resource is has not URI syntax", muex);
         } catch (MalformedURLException muex) {
            throw new ResourceParsingException("Value of field of type resource is a malformed URL", muex);
         }
      }
      
      // adapt
      if (replaceBaseUrlMode && !normalizeUrlMode) {
         //String defaultBaseUrlPatternString = "http[s]?://data\\.oasis-eu\\.org/"; // TODO start
         String defaultBaseUrlPatternString = "http[s]?://[^/]+/"; // TODO start
         Pattern replaceBaseUrlPattern = Pattern.compile(defaultBaseUrlPatternString);
         urlPathWithoutSlash = replaceBaseUrlPattern.matcher(stringUriValue).replaceFirst("");
         stringUriValue = baseUrl + urlPathWithoutSlash;
      }
      if (urlPathWithoutSlash == null) { // i.e. not replaced, yet
         urlPathWithoutSlash = stringUriValue.substring(baseUrl.length()); //
      }
      return urlPathWithoutSlash;
   }
   /**
    * TODO move in DCURI
    * @param stringUriValue
    * @param normalizeUrlMode
    * @param replaceBaseUrlMode
    * @return
    * @throws ResourceParsingException
    */
   private DCURI normalizeAdaptCheckTypeOfUri(String stringUriValue,
         boolean normalizeUrlMode, boolean replaceBaseUrlMode) throws ResourceParsingException {
      String urlPathWithoutSlash = this.normalizeAdaptUriToPathWithoutSlash(
            stringUriValue, normalizeUrlMode, replaceBaseUrlMode);

      // check type
      int iriSlashIndex = urlPathWithoutSlash.indexOf('/', typeIndexInType); // TODO use Pattern & DC_TYPE_PATH
      String refType = urlPathWithoutSlash.substring(typeIndexInType, iriSlashIndex);
      DCModel refModel = modelService.getModel(refType);
      if (refModel == null) {
         throw new ResourceParsingException("Can't find resource type " + refType
               + " of resource Field with URL value " + stringUriValue);
      }
      String refIri = urlPathWithoutSlash.substring(iriSlashIndex + 1); // TODO useful ??
      return new DCURI(this.baseUrl, refType, refIri/*, refModel*/); // TODO LATER cached model ref
   }
   private String normalizeAdaptUri(String stringUriValue,
         boolean normalizeUrlMode, boolean replaceBaseUrlMode) throws ResourceParsingException {
      String urlPathWithoutSlash = this.normalizeAdaptUriToPathWithoutSlash(
            stringUriValue, normalizeUrlMode, replaceBaseUrlMode);
      return this.baseUrl + urlPathWithoutSlash;
   }

   private void resourceToEntityFields(Map<String, Object> resourceMap,
         Map<String, Object> entityMap, Map<String, DCField> mapFields,
         // TODO mapFieldNames ; orderedMap ? abstract Field-Model ??
         DCResourceParsingContext resourceParsingContext) {
      
      // gathering required fields :
      // TODO DCFields, cache & for mixins
      Set<String> missingRequiredFieldNames = new HashSet<String>();
      for (String key : mapFields.keySet()) {
         DCField dcField = mapFields.get(key);
         if (dcField.isRequired()) {
            missingRequiredFieldNames.add(key);
         }
      }
      
      // handling each value :
      for (String key : resourceMap.keySet()) {
         if (!mapFields.containsKey(key)) {
            resourceParsingContext.addError("Unkown field " + key);
            continue;
         }
         
         missingRequiredFieldNames.remove(key);
         
         Object resourceValue = resourceMap.get(key);
         DCField dcField = mapFields.get(key); // TODO DCModel.getField(key)
         try {
            resourceParsingContext.enter(dcField, resourceValue);
            Object entityValue = resourceToEntityValue(resourceValue, dcField, resourceParsingContext);
            entityMap.put(key, entityValue);
         } catch (ResourceParsingException rpex) {
            resourceParsingContext.addError("Error while parsing Field value " + resourceValue
                  + " of JSON type " + ((resourceValue == null) ? "null" : resourceValue.getClass()), rpex);
         } finally {
            resourceParsingContext.exit();
         }
      }
      
      if (!missingRequiredFieldNames.isEmpty()) {
         resourceParsingContext.addError("Some Fields are missing : " + missingRequiredFieldNames); 
      }
   }
   
   public List<DCResource> postAllDataInType(List<DCResource> resources, String modelType/*, Request request*/) {
      return this.internalPostAllDataInType(resources, modelType, true, !this.strictPostMode);
   }
   public List<DCResource> internalPostAllDataInType(List<DCResource> resources, String modelType,
         boolean canCreate, boolean canUpdate) {
      ArrayList<DCResource> res = new ArrayList<DCResource>(resources.size());
      for (DCResource resource : resources) {
         res.add(internalPostDataInType(resource, modelType, canCreate, canUpdate));
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
      throw new WebApplicationException(Response.status(Response.Status.CREATED)
            ///.tag(new EntityTag(httpEntity)) // .lastModified(dataEntity.getLastModified().toDate())
            .entity(res)
            .type(MediaType.APPLICATION_JSON).build());
   }
   public List<DCResource> postAllData(List<DCResource> resources/*, Request request*/) {
      return this.internalPostAllData(resources, true, !this.strictPostMode);
   }
   public List<DCResource> internalPostAllData(List<DCResource> resources, boolean canCreate, boolean canUpdate) {
      boolean replaceBaseUrlMode = true;
      boolean normalizeUrlMode = true;
      
      ArrayList<DCResource> res = new ArrayList<DCResource>(resources.size());
      for (DCResource resource : resources) {
         DCURI uri;
         try {
            uri = normalizeAdaptCheckTypeOfUri(resource.getUri(), normalizeUrlMode, replaceBaseUrlMode);
         } catch (ResourceParsingException rpex) {
            // TODO LATER rather context & for multi post
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                  .entity(resource.getUri() + " should be an uri but " + rpex.getMessage())
                  .type(MediaType.TEXT_PLAIN).build());
         }
         res.add(internalPostDataInType(resource, uri.getType(), canCreate, canUpdate));
         // NB. no ETag validation support, see discussion in internalPostAllDataInType()
      }
      throw new WebApplicationException(Response.status(Response.Status.CREATED)
            ///.tag(new EntityTag(httpEntity)) // .lastModified(dataEntity.getLastModified().toDate())
            .entity(res)
            .type(MediaType.APPLICATION_JSON).build());
   }
   
   public DCResource putDataInType(DCResource resource, String modelType, String iri/*, Request request*/) {
      // NB. no ETag validation support (same as POST), see discussion in internalPostAllDataInType()
      return internalPostDataInType(resource, modelType, false, true);
   }
   public List<DCResource> putAllDataInType(List<DCResource> resources, String modelType/*, Request request*/) {
      // NB. no ETag validation support (same as POST), see discussion in internalPostAllDataInType()
      return internalPostAllDataInType(resources, modelType, false, true);
   }
   public List<DCResource> putAllData(List<DCResource> resources/*, Request request*/) {
      // NB. no ETag validation support (same as POST), see discussion in internalPostAllDataInType()
      return internalPostAllData(resources, false, true);
   }
   
   public DCResource getData(String modelType, String iri, Request request) {
      String uri = new DCURI(this.baseUrl, modelType, iri).toString();

      DCModel dcModel = modelService.getModel(modelType); // NB. type can't be null thanks to JAXRS
      if (dcModel == null) {
         throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
             .entity("Unknown Model type " + modelType).type(MediaType.TEXT_PLAIN).build());
      }
      modelType = dcModel.getName(); // normalize ; TODO useful ?

      DCEntity entity = entityService.getByUriId(uri, dcModel);
      
      if (entity == null) {
         //return Response.noContent().build();
         throw new WebApplicationException(Response.Status.NO_CONTENT); // TODO or :
         //throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
         //      .entity("No data at uri " + uri).type(MediaType.TEXT_PLAIN).build());
      }

      // TODO ETag jaxrs caching :
      // on GET, If-None-Match precondition else return 304 Not Modified http://stackoverflow.com/questions/2021882/is-my-implementation-of-http-conditional-get-answers-in-php-is-ok/2049723#2049723
      // see for jaxrs https://devcenter.heroku.com/articles/jax-rs-http-caching#conditional-cache-headers
      // and for http http://odino.org/don-t-rape-http-if-none-match-the-412-http-status-code/
      // also http://stackoverflow.com/questions/2085411/how-to-use-cxf-jax-rs-and-http-caching
      String httpEntity = entity.getVersion().toString(); // no need of additional uri because only for THIS resource
      EntityTag eTag = new EntityTag(httpEntity);
      //ResponseBuilder builder = request.evaluatePreconditions(dataEntity.getUpdated(), eTag);
      ResponseBuilder builder = request.evaluatePreconditions(eTag);
      
      if (builder == null) {
         // (if provided) If-None-Match precondition OK (resource did change), so serve updated content
         DCResource resource = entityToResource(entity);
         builder = Response.ok(resource).tag(eTag); // .lastModified(dataEntity.getLastModified().toDate())
      }

      // else provided If-None-Match precondition KO (resource didn't change),
      // so return 304 Not Modified (and don't send the dcData back)
      
      CacheControl cc = new CacheControl();
      cc.setMaxAge(600); // TODO ??!!
      //return builder.cacheControl(cc).lastModified(person.getUpdated()).build(); // NB. lastModified would be pretty but not used at HTTP level
      throw new WebApplicationException(builder.cacheControl(cc).build());
   }
   
   public void deleteData(String modelType, String iri, HttpHeaders httpHeaders) {
      String uri = new DCURI(this.baseUrl, modelType, iri).toString();
      
      String etag = httpHeaders.getHeaderString(HttpHeaders.IF_MATCH);
      if (etag == null) {
         throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
               .entity("Missing If-Match=version header to delete " + uri).type(MediaType.TEXT_PLAIN).build());
      }
      Long version;
      try {
         version = Long.parseLong(etag);
      } catch (NumberFormatException e) {
         throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
               .entity("If-Match header should be a long to delete " + uri
                     + " but is " + etag).type(MediaType.TEXT_PLAIN).build());
      }

      DCModel dcModel = modelService.getModel(modelType); // NB. type can't be null thanks to JAXRS
      if (dcModel == null) {
         throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
             .entity("Unknown Model type " + modelType).type(MediaType.TEXT_PLAIN).build());
      }
      modelType = dcModel.getName(); // normalize ; TODO useful ?
      String collectionName = modelType; // TODO or = dcModel.getCollectionName(); for weird type names ??

      Query query = new Query(Criteria.where("_uri").is(uri).and("_v").is(version));
      mgo.remove(query, collectionName);

      throw new WebApplicationException(Response.status(Response.Status.NO_CONTENT).build());
   }


   @Override
   public List<DCResource> updateDataInTypeWhere(DCResource dcDataDiff,
         String modelType, UriInfo uriInfo) {
      throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity("Feature not implemented").type(MediaType.TEXT_PLAIN).build());
   }

   @Override
   public void deleteDataInTypeWhere(String modelType, UriInfo uriInfo) {
      throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity("Feature not implemented").type(MediaType.TEXT_PLAIN).build());
   }
   
   
   public DCResource postDataInTypeOnGet(String modelType, String method, UriInfo uriInfo/*, Request request*/) {
      if ("POST".equalsIgnoreCase(method)) {
         DCResource resource = null;
         // TODO impl like find
         return this.postDataInType(resource, modelType);
      } else {
         throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
               .entity("method query parameter should be POST but is "
                     + method).type(MediaType.TEXT_PLAIN).build());
      }
   }
   
   public DCResource putPatchDeleteDataOnGet(String modelType, String iri,
         String method, UriInfo uriInfo, HttpHeaders httpHeaders) {
      if ("PUT".equalsIgnoreCase(method)
            || "PATCH".equalsIgnoreCase(method)) {
         DCResource resource = new DCResource();
         // TODO impl like find
         return this.putDataInType(resource, modelType, iri);
      } else if ("DELETE".equalsIgnoreCase(method)) {
         this.deleteData(modelType, iri, httpHeaders);
         return null;
      } else {
         throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
               .entity("method query parameter should be PUT, PATCH, DELETE but is "
                     + method).type(MediaType.TEXT_PLAIN).build());
      }
   }

   // TODO "native" query (W3C LDP-like) also refactored within a dedicated query engine ??
   public List<DCResource> findDataInType(String modelType, UriInfo uriInfo, Integer start, Integer limit) {

      DCModel dcModel = modelService.getModel(modelType); // NB. type can't be null thanks to JAXRS
      if (dcModel == null) {
         throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
             .entity("Unknown model type " + modelType).type(MediaType.TEXT_PLAIN).build());
      }
      modelType = dcModel.getName(); // normalize ; TODO useful ?
      String collectionName = dcModel.getCollectionName(); // TODO getType() or getCollectionName(); for weird type names ??

      // parsing query parameters criteria according to model :
      DCQueryParsingContext queryParsingContext = new DCQueryParsingContext(dcModel, null);
      
      // TODO privilege indexed fields !!!!!!!!!!
      ///Criteria springMongoCriteria = new Criteria();
      // TODO sort, on INDEXED (Queriable) field
      ///Sort sort = null;
      
      MultivaluedMap<String, String> params = uriInfo.getQueryParameters(true);
      parameterLoop : for (String fieldPath : params.keySet()) {
         if (findConfParams.contains(fieldPath)) {
            // skip find conf params
            continue;
         }
         
         //DCField dcField = dcModelService.getFieldByPath(dcModel, fieldPath); // TODO TODOOOOOOOOOOOOO
         // TODO move impl :
         String[] fieldPathElements = fieldPath.split("\\."); // (escaping regex) mongodb field path syntax
         // TODO LATER also XML fieldPath.split("/") ??
         if (fieldPathElements.length == 0) {
            continue; // should not happen
         }
         DCField dcField = dcModel.getField(fieldPathElements[0]);
         if (dcField == null) {
            queryParsingContext.addError("In type " + modelType + ", can't find field with path elements"
                  + fieldPathElements + ": can't find field for first path element " + fieldPathElements[0]);
            continue;
         }
         
         // finding the leaf field
         
         // finding the latest higher list field :
         DCListField dcListField = null;
         if ("list".equals(dcField.getType())) {
            dcListField = (DCListField) dcField;
            do {
               dcField = ((DCListField) dcField).getListElementField();
            } while ("list".equals(dcField.getType()));
         }
         
         // loop on path elements for finding the leaf field :
         for (int i = 1; i < fieldPathElements.length; i++) {
            String fieldPathElement = fieldPathElements[i];
            if ("map".equals(dcField.getType())) {
               dcField = ((DCMapField) dcField).getMapFields().get(fieldPathElement);
               if (dcField == null) {
                  queryParsingContext.addError("In type " + modelType + ", can't find field with path elements"
                        + fieldPathElements + ": can't go below " + i + "th path element "
                        + fieldPathElement + ", because field is unkown");
                  continue parameterLoop;
               }
            } else if ("resource".equals(dcField.getType())) {
               queryParsingContext.addError("Found criteria requiring join : in type " + modelType + ", field "
                     + fieldPath + "can't be done in findDataInType, do it rather in all-around finData");
               continue parameterLoop; // TODO boum
            } else {
               queryParsingContext.addError("In type " + modelType + ", can't find field with path elements"
                     + fieldPathElements + ": can't go below " + i + "th element " 
                     + fieldPathElement + ", because field is neither map nor list but " + dcField.getType());
               continue parameterLoop; // TODO boum
            }

            if ("list".equals(dcField.getType())) {
               // finding the latest higher list field :
               dcListField = (DCListField) dcField;
               do {
                  dcField = ((DCListField) dcField).getListElementField();
                  // TODO TODO check that indexed (or set low limit) ??
               } while ("list".equals(dcField.getType()));
            } else {
               dcListField = null;
            }
         }
         
         List<String> values = params.get(fieldPath);
         if (values == null || values.size() == 0) {
            queryParsingContext.addError("Missing value for parameter " + fieldPath);
            continue;
         } // should not happen
         String operatorAndValue = values.get(0);
         if (operatorAndValue == null) {
            queryParsingContext.addError("Missing value for parameter " + fieldPath);
            continue; // should not happen
         }
         
         // parsing query parameter criteria according to model field :
         // TODO LATER using ANTLR ?!?
         // recognizes MongoDB criteria (operators & values), see http://docs.mongodb.org/manual/reference/operator/query/
         // and fills Spring Criteria with them
         
         try {
            parseCriteriaFromQueryParameter(fieldPath, operatorAndValue,
                  dcField, dcListField, queryParsingContext);
         } catch (Exception ex) {
            queryParsingContext.addError("Error while parsing query criteria " + fieldPath
                  + operatorAndValue, ex);
         }
      }
      
      // adding paging & sorting :
      if (start > 500) {
         start = 500; // max (conf'ble in model ?), else prefer ranged query ; TODO or error message ?
      }
      if (limit > 50) {
         limit = 50; // max (conf'ble in model ?), else prefer ranged query ; TODO or error message ?
      }
      Sort sort = queryParsingContext.getSort();
      if (sort == null) {
         // TODO sort by default : configured in model (uri, last modified date, iri?, other fields...)
         sort = new Sort(Direction.ASC, "uri");
      }
      Query springMongoQuery = new Query(queryParsingContext.getCriteria())
         .with(sort).skip(start).limit(limit); // TODO rather range query, if possible on sort field
         
      // executing the mongo query :
      List<DCEntity> foundEntities = mgo.find(springMongoQuery, DCEntity.class, collectionName);
      
      List<DCResource> foundDatas = entitiesToResources(foundEntities);
      return foundDatas;
   }

   private void parseCriteriaFromQueryParameter(String fieldPath,
         String operatorAndValue, DCField dcField, DCListField dcListField,
         DCQueryParsingContext queryParsingContext) throws ResourceParsingException {
      // TODO (mongo)operator for error & in parse ?
      String entityFieldPath = "_p." + fieldPath;
      
      if (operatorAndValue.startsWith("=")
            || operatorAndValue.startsWith("==")) { // java-like
         // TODO check that indexed ??
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // NB. can't sort a single value
         String stringValue = operatorAndValue.substring(1); // TODO more than mongodb
         Object value = checkAndParseFieldValue(dcField, stringValue);
         queryParsingContext.getCriteria().and(entityFieldPath).is(value); // TODO same fieldPath for mongodb ??
         
      } else if (operatorAndValue.equals("+")) {
         checkComparable(dcField, fieldPath + operatorAndValue);
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ?!??
         queryParsingContext.addSort(new Sort(Direction.ASC, entityFieldPath));

      } else if (operatorAndValue.equals("-")) {
         checkComparable(dcField, fieldPath + operatorAndValue);
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that not i18n (which is map ! ; or allow fallback, order for locale) ???
         // TODO check that indexed (or set low limit) ??
         queryParsingContext.addSort(new Sort(Direction.ASC, entityFieldPath));
         
      } else if (operatorAndValue.startsWith(">")
            || operatorAndValue.startsWith("&gt;") // xml // TODO ; ?
            || operatorAndValue.startsWith("$gt")) { // mongodb
         checkComparable(dcField, fieldPath + operatorAndValue);
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
         int sortIndex = addSort(entityFieldPath, operatorAndValue, queryParsingContext);
         String stringValue = operatorAndValue.substring(3, sortIndex); // TODO more than mongodb
         Object value = parseFieldValue(dcField, stringValue);
         queryParsingContext.getCriteria().and(entityFieldPath).gt(value); // TODO same fieldPath for mongodb ??
         
      } else if (operatorAndValue.startsWith("<")
            || operatorAndValue.startsWith("&lt;") // xml // TODO ; ?
            || operatorAndValue.startsWith("$lt")) { // mongodb
         checkComparable(dcField, fieldPath + operatorAndValue);
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
         int sortIndex = addSort(entityFieldPath, operatorAndValue, queryParsingContext);
         String stringValue = operatorAndValue.substring(3, sortIndex); // TODO more than mongodb
         Object value = parseFieldValue(dcField, stringValue);
         queryParsingContext.getCriteria().and(entityFieldPath).lt(value); // TODO same fieldPath for mongodb ??
         
      } else if (operatorAndValue.startsWith(">=")
            || operatorAndValue.startsWith("&gt;=") // xml // TODO ; ?
            || operatorAndValue.startsWith("$gte")) { // mongodb
         checkComparable(dcField, fieldPath + operatorAndValue);
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
         int sortIndex = addSort(entityFieldPath, operatorAndValue, queryParsingContext);
         String stringValue = operatorAndValue.substring(4, sortIndex); // TODO more than mongodb
         Object value = parseFieldValue(dcField, stringValue);
         queryParsingContext.getCriteria().and(entityFieldPath).gte(value); // TODO same fieldPath for mongodb ??
         
      } else if (operatorAndValue.startsWith("<=")
            || operatorAndValue.startsWith("&lt;=") // xml // TODO ; ?
            || operatorAndValue.startsWith("$lte")) { // mongodb
         checkComparable(dcField, fieldPath + operatorAndValue);
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
         int sortIndex = addSort(entityFieldPath, operatorAndValue, queryParsingContext);
         String stringValue = operatorAndValue.substring(4, sortIndex); // TODO more than mongodb
         Object value = parseFieldValue(dcField, stringValue);
         queryParsingContext.getCriteria().and(entityFieldPath).lte(value); // TODO same fieldPath for mongodb ??

      } else if (operatorAndValue.startsWith("<>")
            || operatorAndValue.startsWith("&lt;&gt;") // xml // TODO may happen, ';' ??
            || operatorAndValue.startsWith("$ne") // mongodb
            || operatorAndValue.startsWith("!=")) { // java-like
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
         int sortIndex = addSort(entityFieldPath, operatorAndValue, queryParsingContext); // TODO ???????
         String stringValue = operatorAndValue.substring(3, sortIndex); // TODO more than mongodb
         Object value = checkAndParseFieldValue(dcField, stringValue);
         queryParsingContext.getCriteria().and(entityFieldPath).ne(value); // TODO same fieldPath for mongodb ??
         
      } else if (operatorAndValue.startsWith("$in")) { // mongodb
         if ("map".equals(dcField.getType()) || "list".equals(dcField.getType())) {
            throw new ResourceParsingException("$in can't be applied to a " + dcField.getType() + " Field");
         }
         // TODO check that not date ???
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
         int sortIndex = addSort(entityFieldPath, operatorAndValue, queryParsingContext); // TODO ???
         String stringValue = operatorAndValue.substring(3, sortIndex); // TODO more than mongodb
         List<Object> value = parseFieldListValue(dcField, stringValue);
         queryParsingContext.getCriteria().and(entityFieldPath).in(value); // TODO same fieldPath for mongodb ??
         
      } else if (operatorAndValue.startsWith("$nin")) { // mongodb
         if ("map".equals(dcField.getType()) || "list".equals(dcField.getType())) {
            throw new ResourceParsingException("$nin can't be applied to a " + dcField.getType() + " Field");
         }
         // TODO check that not date ???
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
         int sortIndex = addSort(entityFieldPath, operatorAndValue, queryParsingContext); // TODO ???
         String stringValue = operatorAndValue.substring(4, sortIndex); // TODO more than mongodb
         List<Object> value = parseFieldListValue(dcField, stringValue);
         queryParsingContext.getCriteria().and(entityFieldPath).nin(value); // TODO same fieldPath for mongodb ??
         
      } else if (operatorAndValue.startsWith("$regex")) { // mongodb
         if (!"string".equals(dcField.getType())) {
            throw new ResourceParsingException("$regex can only be applied to a string but found "
                  + dcField.getType() + " Field");
         }
         int sortIndex = addSort(entityFieldPath, operatorAndValue, queryParsingContext); // TODO ????
         String value = operatorAndValue.substring(6, sortIndex); // TODO more than mongodb
         String options = null;
         if (value.length() != 0 && value.charAt(0) == '/') {
            int lastSlashIndex = value.lastIndexOf('/');
            if (lastSlashIndex != 0) {
               options = value.substring(lastSlashIndex + 1);
               value = value.substring(1, lastSlashIndex - 1);
            }
         }
         // TODO prevent or warn if first character(s) not provided in regex (making it much less efficient)
         if (options == null) {
            queryParsingContext.getCriteria().and(entityFieldPath).regex(value); // TODO same fieldPath for mongodb ??
         } else {
            queryParsingContext.getCriteria().and(entityFieldPath).regex(value, options); // TODO same fieldPath for mongodb ??
         }
         
      } else if (operatorAndValue.startsWith("$exists")) { // mongodb field
         // TODO TODO rather hasAspect / mixin / type !!!!!!!!!!!!!!!
         // TODO AND / OR field value == null
         // TODO sparse index ?????????
         Boolean value = parseBoolean(operatorAndValue.substring(7));
         // TODO TODO can't return false because already failed to find field
         queryParsingContext.getCriteria().and(entityFieldPath).exists(value); // TODO same fieldPath for mongodb ??
         
      //////////////////////////////////
      // list (array) operators :
         
      // NB. all items are of the same type
         
      } else if (operatorAndValue.startsWith("$all")) { // mongodb array
         // parsing using the latest upmost list field :
         List<Object> value = checkAndParseFieldPrimitiveListValue(dcListField, dcListField.getListElementField(),
               operatorAndValue.substring(4)); // TODO more than mongodb
         // TODO LATER $all with $elemMatch
         queryParsingContext.getCriteria().and(entityFieldPath).all(value); // TODO same fieldPath for mongodb ??
         
      } else if (operatorAndValue.startsWith("$elemMatch")) { // mongodb array
         if (!"list".equals(dcField.getType())) {
            throw new ResourceParsingException("$elemMatch can only be applied to a list but found "
                  + dcField.getType() + " Field");
         }
         // parsing using the latest upmost list field :
         // WARNING the first element in the array must be selective, because all documents
         // containing it are scanned
         
         // TODO which syntax ??
         // TODO alt 1 : don't support it as a different operator but auto use it on list fields
         // (save if ex. boolean mode...) ; and add OR syntax here using ex. &join=OR syntax NO WRONG 
         // alt 2 : support it explicitly, with pure mongo syntax NO NOT IN HTTP GET QUERY
         // alt 3 : support it explicitly, with Datacore query syntax MAYBE ex. :
         
         // (((TODO rather using another syntax ?? NOT FOR NOW rather should first add OR syntax
         // (i.e. autojoins ?!) to OR-like 'equals' on list elements, or prevent them)))
         // parsing using the mongodb syntax (so no sort) :
         String stringValue = operatorAndValue.substring(10);
         /*
         Criteria value = parseCriteria(dcListField.getListElementField(),
               operatorAndValue.substring(10)); // TODO more than mongodb
         ///Map<String,Object> value = parseMapValue(listField, stringValue);
         Map<String,Object> mapValue;
         try {
            mapValue = mapper.readValue((String) stringValue, Map.class);
         } catch (IOException ioex) {
            throw new ResourceParsingException("IO error while Object-formatted string : "
                  + stringValue, ioex);
         } catch (Exception e) {
            throw new ResourceParsingException("$elemMatch operator value is not "
                  + "an Object-formatted string : " + stringValue, e);
         }
         for (String criteriaKey : mapValue.keySet()) {
            Object criteriaValue = mapValue.get(criteriaKey);
            DCField criteriaField = dcListField.getListElementField();
            parseCriteriaFromQueryParameter(fieldPath + '.' + criteriaKey,
                  criteriaValue, criteriaField, null, queryParsingContext); // or next highest dcListField rather than null ??
         }
         queryParsingContext.getCriteria().and(fieldPath).elemMatch(value); // TODO same fieldPath for mongodb ??
         */
         // TODO more than mongodb
         
      } else if (operatorAndValue.startsWith("$size")) { // mongodb array
         // TODO (mongo)operator for error & in parse ?
         if (!"list".equals(dcField.getType())) {
            throw new ResourceParsingException("$size can only be applied to a list but found "
                  + dcField.getType() + " Field");
         }
         // parsing using the latest upmost list field :
         // NB. mongo arrays with millions of items are supported, but let's not go in the Long area
         Integer value = parseInteger(operatorAndValue.substring(5)); 
         queryParsingContext.getCriteria().and(entityFieldPath).size(value); // TODO same fieldPath for mongodb ??
         
      } else {
         // defaults to "equals"
         // TODO check that indexed ??
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // NB. can't sort a single value
         Object value = checkAndParseFieldValue(dcField, operatorAndValue);
         queryParsingContext.getCriteria().and(entityFieldPath).is(value); // TODO same fieldPath for mongodb ??
      }
   }

   private void checkComparable(DCField dcField, String stringCriteria)
         throws ResourceParsingException {
      if ("map".equals(dcField.getType())
            || "list".equals(dcField.getType())
         // NB. though could be applied on array, since in mongo, sort of an array is based
         // on the min or max value as said at https://jira.mongodb.org/browse/SERVER-5596
            || "resource".equals(dcField.getType())) {
         // TODO LATER allow (joined) resource and order per its default order field ??
         throw new ResourceParsingException("Field of type " + dcField.getType() + " is not comparable");
      }
      // NB. date allowed for range check, see http://cookbook.mongodb.org/patterns/date_range/
      // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
      // TODO check that indexed (or set low limit) ?!??
   }

   /**
    * 
    * @param fieldPath TODO or better DCField ?
    * @param operatorAndValue
    * @param queryParsingContext
    * @return
    */
   private int addSort(String fieldPath, String operatorAndValue,
         DCQueryParsingContext queryParsingContext) {
      int operatorAndValueLength = operatorAndValue.length();
      char lastChar = operatorAndValue.charAt(operatorAndValueLength - 1);
      switch (lastChar) {
      case '+' :
         queryParsingContext.addSort(new Sort(Direction.ASC, fieldPath));
         break;
      case '-' :
         queryParsingContext.addSort(new Sort(Direction.DESC, fieldPath));
         break;
      }
      return operatorAndValueLength;
   }

   private Object checkAndParseFieldValue(DCField dcField, String stringValue) throws ResourceParsingException {
      if ("map".equals(dcField.getType()) || "list".equals(dcField.getType())) {
         throw new ResourceParsingException("operator can't be applied to a " + dcField.getType() + " Field");
      }
      return parseFieldValue(dcField, stringValue);
   }
   private Object parseFieldValue(DCField dcField, String stringValue) throws ResourceParsingException {
      try {
         if ("string".equals(dcField.getType())) {
            return stringValue;
            
         } else if ("boolean".equals(dcField.getType())) {
            return parseBoolean(stringValue);
            
         } else if ("int".equals(dcField.getType())) {
            return parseInteger(stringValue);
            
         } else if ("float".equals(dcField.getType())) {
            return mapper.readValue(stringValue, Float.class);
            
         } else if ("long".equals(dcField.getType())) {
            return mapper.readValue(stringValue, Long.class);
            
         } else if ("double".equals(dcField.getType())) {
            return mapper.readValue(stringValue, Double.class);
            
         } else if ("date".equals(dcField.getType())) {
            return parseDate(stringValue);
            
         } else if ("resource".equals(dcField.getType())) {
            // TODO resource better ex. allow auto joins ?!?
            return stringValue;
            
         /*} else if ("i18n".equals(dcField.getType())) { // TODO i18n better
            entityValue = (HashMap<?,?>) resourceValue; // TODO NOOOO _i18n
            // TODO locale & fallback
            
         } else if ("wkt".equals(dcField.getType())) { // TODO LATER2 ??
            entityValue = (String) resourceValue;*/
         }
         
      } catch (ResourceParsingException rpex) {
         throw rpex;
      } catch (IOException ioex) {
         throw new ResourceParsingException("IO error while reading integer-formatted string : "
               + stringValue, ioex);
      } catch (Exception ex) {
         throw new ResourceParsingException("Not an integer-formatted string : "
               + stringValue, ex);
      }
      
      throw new ResourceParsingException("Unsupported field type " + dcField.getType());
   }

   private DateTime parseDate(String stringValue) throws ResourceParsingException {
      try {
         return mapper.readValue((String) stringValue, DateTime.class);
      } catch (IOException ioex) {
         throw new ResourceParsingException("IO error while reading ISO 8601 Date-formatted string : "
               + stringValue, ioex);
      } catch (Exception e) {
         throw new ResourceParsingException("date Field value is not "
               + "an ISO 8601 Date-formatted string : " + stringValue, e);
      }
   }

   private Boolean parseBoolean(String stringBoolean) throws ResourceParsingException {
      try {
         return mapper.readValue(stringBoolean, Boolean.class);
      } catch (IOException e) {
         throw new ResourceParsingException("IO error while reading boolean-formatted string : "
               + stringBoolean, e);
      } catch (Exception e) {
         throw new ResourceParsingException("Not a boolean-formatted string : "
               + stringBoolean, e);
      }
   }

   private Integer parseInteger(String stringInteger) throws ResourceParsingException {
      try {
         return mapper.readValue(stringInteger, Integer.class);
      } catch (IOException e) {
         throw new ResourceParsingException("IO error while reading integer-formatted string : "
               + stringInteger, e);
      } catch (Exception e) {
         throw new ResourceParsingException("Not an integer-formatted string : "
               + stringInteger, e);
      }
   }

   // TODO in query context !
   private List<Object> checkAndParseFieldPrimitiveListValue(DCField dcField, DCField listElementField,
         String stringListValue) throws ResourceParsingException {
      if (!"list".equals(dcField.getType())) {
         throw new ResourceParsingException("list operator can only be applied to a list but found "
               + dcField.getType() + " Field");
      }
      if ("map".equals(listElementField.getType()) || "list".equals(listElementField.getType())) {
         throw new ResourceParsingException("list operator can't be applied to a "
               + listElementField.getType() + " list element Field");
      }
      return parseFieldListValue(listElementField, stringListValue);
   }

   @SuppressWarnings("unchecked")
   private List<Object> parseFieldListValue(DCField listElementField,
         String stringListValue) throws ResourceParsingException {
      try {
         return mapper.readValue(stringListValue, List.class);
      } catch (IOException e) {
         throw new ResourceParsingException("IO error while reading list-formatted string : "
               + stringListValue, e);
      } catch (Exception e) {
         throw new ResourceParsingException("Not a list-formatted string : "
               + stringListValue, e);
      }
   }

   public List<DCResource> findData(UriInfo uriInfo, Integer start, Integer limit) {
      // TODO same as findData, but parse model names in front of field names,
      // get collection names from their type field,
      // and apply limited sort join algo
      return new ArrayList<DCResource>();
   }

   @Override
   public List<DCResource> queryDataInType(String modelType, String query, String language) {
      List<DCEntity> entities = this.entityQueryService.queryInType(modelType, query, language);
      return entitiesToResources(entities);
   }
   @Override
   public List<DCResource> queryData(String query, String language) {
      List<DCEntity> entities = this.entityQueryService.query(query, language);
      return entitiesToResources(entities);
   }

   
   private DCResource entityToResource(DCEntity entity) {
      Map<String, Object> resourceProps = new HashMap<String,Object>(entity.getProperties().size());
      // TODO better for properties (especially aspects ?!!)
      Map<String, Object> entityProps = entity.getProperties();
      for (String entityPropName : entity.getProperties().keySet()) { // or iterate on fields ?
         Object resourcePropValue = entityProps.get(entityPropName);
         /*if (resourcePropValue instanceof DCEntity) {
            
         } else */if (resourcePropValue instanceof Date) {
            if (!(resourcePropValue instanceof DateTime)) {
               resourcePropValue = new DateTime((Date) resourcePropValue); // TODO better in mongo persistence
            }
         }
         resourceProps.put(entityPropName, resourcePropValue);
      }
      DCResource resource = new DCResource(resourceProps);
      resource.setUri(entity.getUri());
      //resource.setIri(entity.getIri()); // TODO ??
      //resource.setType(entity.getModelName()); // TODO ??
      resource.setVersion(entity.getVersion());
      /*resource.setModelName(entity.getModelName());
      resource.setTypes(entity.getTypes());*/ //TODO !!
      return resource;
   }
   
   private List<DCResource> entitiesToResources(List<DCEntity> entities) {
      ArrayList<DCResource> datas = new ArrayList<DCResource>(entities.size());
      for (DCEntity entity : entities) {
         DCResource resource = entityToResource(entity);
         datas.add(resource);
      }
      return datas;
   }

   
   public boolean isStrictPostMode() {
      return strictPostMode;
   }

   public void setStrictPostMode(boolean strictPostMode) {
      this.strictPostMode = strictPostMode;
   }
   
}
