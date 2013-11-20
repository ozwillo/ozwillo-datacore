package org.oasis.datacore.rest.server;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.joda.time.DateTime;
import org.oasis.datacore.core.entity.DCEntityService;
import org.oasis.datacore.core.entity.EntityQueryService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.model.DCURI;
import org.oasis.datacore.core.entity.query.QueryException;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryService;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.server.parsing.DCResourceParsingContext;
import org.oasis.datacore.rest.server.parsing.ResourceParsingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
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

   /** Base URL of this endpoint. If broker mode enabled, used to detect when to use it.. */
   @Value("${datacoreApiServer.baseUrl}") 
   private String baseUrl; // "http://" + "data-lyon-1.oasis-eu.org" + "/"
   /** Unique per container, defines it. To be able to build a full uri
    * (for GET, DELETE, possibly to build missing or custom / relative URI...) */
   @Value("${datacoreApiServer.containerUrl}") 
   private String containerUrl; // "http://" + "data.oasis-eu.org" + "/"

   private boolean strictPostMode = false;
   
   @Autowired
   private DCEntityService entityService;
   @Autowired
   private LdpEntityQueryService ldpEntityQueryService;
   @Autowired
   @Qualifier("datacore.entityQueryService")
   private EntityQueryService entityQueryService;
   //@Autowired
   //private DCDataEntityRepository dataRepo; // NO rather for (meta)model, for data can't be used because can't specify collection
   @Autowired
   private MongoOperations mgo; // TODO remove it by hiding it in services
   @Autowired
   private DCModelService modelService;

   /** injecting single application-wide mapper (which is fine because thread-safe, see
    * http://stackoverflow.com/questions/18611565/how-do-i-correctly-reuse-jackson-objectmapper
    * NB. otherwise would need to have same configuration ;
    * pre-request configuration can be done using .writer() and .reader()) */
   @Autowired
   @Qualifier("datacoreApiServer.objectMapper")
   public ObjectMapper mapper = new ObjectMapper(); // TODO move it in ResourceParsingService and make it private again
   
   private static int typeIndexInType = UriHelper.DC_TYPE_PREFIX.length(); // TODO use Pattern & DC_TYPE_PATH

   private static Set<String> resourceNativeJavaFields = new HashSet<String>();
   static {
      // TODO rather using Enum, see BSON$RegexFlag
      resourceNativeJavaFields.add("uri");
      resourceNativeJavaFields.add("version");
      resourceNativeJavaFields.add("types");
      resourceNativeJavaFields.add("created");
      resourceNativeJavaFields.add("lastModified");
      resourceNativeJavaFields.add("createdBy");
      resourceNativeJavaFields.add("lastModifiedBy");
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
      boolean matchBaseUrlMode = true;
      boolean normalizeUrlMode = true;
      boolean brokerMode = false;

      boolean isCreation = true;

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
      
      DCModel dcModel = modelService.getModel(modelType); // NB. type can't be null thanks to JAXRS
      if (dcModel == null) {
         throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
             .entity("Unknown Model type " + modelType).type(MediaType.TEXT_PLAIN).build());
      }
      modelType = dcModel.getName(); // normalize ; TODO useful ?
      
      String stringUri = resource.getUri();
      DCURI uri;
      try {
         uri = normalizeAdaptCheckTypeOfUri(stringUri, modelType, normalizeUrlMode, matchBaseUrlMode);
         stringUri = uri.toString();
      } catch (ResourceParsingException rpex) {
         // TODO LATER rather context & for multi post
         throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
               .entity("Error while parsing root URI '" + stringUri + "' : " + rpex.getMessage())
               .type(MediaType.TEXT_PLAIN).build());
      }
      
      // TODO extract to checkContainer()
      if (!containerUrl.equals(uri.getContainer())) {
         if (!brokerMode) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                  .entity("In non-broker mode, can only serve data from own container "
                        + containerUrl + " but URI container is " + uri.getContainer())
                  .type(MediaType.TEXT_PLAIN).build());
         }
         // else TODO LATER OPT broker mode
      }

      DCEntity dataEntity = null;
      if (!canUpdate || !isCreation) {
         dataEntity = entityService.getByUriId(stringUri, dcModel);
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
         dataEntity.setUri(stringUri);
      }
      dataEntity.setVersion(version);

      Map<String, Object> dataProps = resource.getProperties();
      
      dataEntity.setModelName(dcModel.getName()); // TODO LATER2 check that same (otherwise ex. external approvable contrib ??)
      dataEntity.setVersion(version);
      
      // parsing resource according to model :
      DCResourceParsingContext resourceParsingContext = new DCResourceParsingContext(dcModel, stringUri);
      //List<DCEntity> embeddedEntitiesToAlsoUpdate = new ArrayList<DCEntity>(); // TODO embeddedEntitiesToAlsoUpdate ??
      //resourceParsingContext.setEmbeddedEntitiesToAlsoUpdate(embeddedEntitiesToAlsoUpdate);
      resourceToEntityFields(dataProps, dataEntity.getProperties(), dcModel.getFieldMap(),
            resourceParsingContext); // TODO dcModel.getFieldNames(), abstract DCModel-DCMapField ??
      
      if (resourceParsingContext.hasErrors()) {
         String msg = DCResourceParsingContext.formatParsingErrorsMessage(resourceParsingContext, detailedErrorsMode);
         throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
               .entity(msg).type(MediaType.TEXT_PLAIN).build());
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
            throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
                  .entity("Trying to update data resource without up-to-date version but "
                        + dataEntity.getVersion()).type(MediaType.TEXT_PLAIN).build());
         }
      }

      // TODO LATER 2nd pass : post save hooks

      //Map<String, Object> dcDataProps = new HashMap<String,Object>(dataEntity.getProperties());
      //DCData dcData = new DCData(dcDataProps);
      //dcData.setUri(uri);
      resource.setVersion(dataEntity.getVersion());
      
      resource.setCreated(dataEntity.getCreated()); // NB. if already provided, only if creation
      resource.setCreatedBy(dataEntity.getCreatedBy()); // NB. if already provided, only if creation
      resource.setLastModified(dataEntity.getLastModified());
      resource.setLastModifiedBy(dataEntity.getLastModifiedBy());
      
      // TODO setProperties if changed ex. default values ? or copied queriable fields, or on behaviours ???
      // ex. for model.getDefaultValues() { dcData.setProperty(name, defaultValue);
      
      return resource;
   }

   // TODO extract to ResourceBuilder static helper using in ResourceServiceImpl
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
               entityItem = resourceToEntityValue(resourceItem, listElementField, resourceParsingContext);
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
            DCURI dcUri = normalizeAdaptCheckTypeOfUri(stringUriValue, null,
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
            // "framed" (JSONLD) / embedded : embedded subresource (potentially may be (partial) copy)
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
            DCURI dcUri = normalizeAdaptCheckTypeOfUri((String) entityValueUri, null,
                  normalizeUrlMode, replaceBaseUrlMode);
            // TODO LATER OPT if not (partial) copy, check container vs brokerMode
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
    * TODO extract to UriService
    * Used
    * * by internalPostDataInType on root posted URI (where type is known and that can be relative)
    * * to check referenced URIs and URIs post/put/patch where type is not known (and are never relative)
    * @param stringUriValue
    * @param modelType if any should already have been checked and should be same as uri's,
    * else uri's will be checked
    * @param normalizeUrlMode
    * @param replaceBaseUrlMode
    * @return
    * @throws ResourceParsingException
    */
   private DCURI normalizeAdaptCheckTypeOfUri(String stringUri, String modelType,
         boolean normalizeUrlMode, boolean matchBaseUrlMode)
               throws ResourceParsingException {
      if (stringUri == null || stringUri.length() == 0) {
         // TODO LATER2 accept empty uri and build it according to model type (governance)
         // for now, don't support it :
         throw new ResourceParsingException("Missing uri");
      }
      String[] containerAndPathWithoutSlash;
      try {
         containerAndPathWithoutSlash = UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
            stringUri, this.containerUrl, normalizeUrlMode, matchBaseUrlMode);
      } catch (URISyntaxException usex) {
         throw new ResourceParsingException("Bad URI syntax", usex);
      } catch (MalformedURLException muex) {
         throw new ResourceParsingException("Malformed URL", muex);
      }
      String urlContainer = containerAndPathWithoutSlash[0];
      if (urlContainer == null) {
         // accept local type-relative uri (type-less iri) :
         if (modelType == null) {
            throw new ResourceParsingException("URI can't be relative when no target model type is provided");
         }
         return new DCURI(this.containerUrl, modelType, stringUri);
         // TODO LATER accept also iri including type
      }
      
      // otherwise absolute uri :
      String urlPathWithoutSlash = containerAndPathWithoutSlash[1];
      ///stringUri = this.containerUrl + urlPathWithoutSlash; // useless
      ///return stringUri;

      // check URI model type : against provided one if any, otherwise against known ones
      int iriSlashIndex = urlPathWithoutSlash .indexOf('/', typeIndexInType); // TODO use Pattern & DC_TYPE_PATH
      String uriType = urlPathWithoutSlash.substring(typeIndexInType, iriSlashIndex);
      if (modelType != null) {
         if (!modelType.equals(uriType)) {
            throw new ResourceParsingException("URI resource model type " + uriType
                  + " does not match provided target one " + modelType);
         }
      } else {
         DCModel uriModel = modelService.getModel(uriType);
         if (uriModel == null) {
            throw new ResourceParsingException("Can't find resource model type " + uriType);
         }
      }
      String iri = urlPathWithoutSlash.substring(iriSlashIndex + 1); // TODO useful ??
      return new DCURI(this.containerUrl, uriType, iri/*, refModel*/); // TODO LATER cached model ref
   }

   // TODO extract to ResourceBuilder static helper using in ResourceServiceImpl
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
            uri = normalizeAdaptCheckTypeOfUri(resource.getUri(), null, 
                  normalizeUrlMode, replaceBaseUrlMode);
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
      String uri = new DCURI(this.containerUrl, modelType, iri).toString();

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
      
      // NB. no cache control max age, else HTTP clients won't even send new requests until period ends !
      //CacheControl cc = new CacheControl();
      //cc.setMaxAge(600);
      //builder.cacheControl(cc);
      
      // NB. lastModified would be pretty but not used at HTTP level
      //builder.lastModified(person.getUpdated());
      
      throw new WebApplicationException(builder.build());
   }
   
   public void deleteData(String modelType, String iri, HttpHeaders httpHeaders) {
      String uri = new DCURI(this.containerUrl, modelType, iri).toString();
      
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
      // TODO get operation result (using native executeCommand instead ?) and if failed throw error status

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
      boolean detailedErrorMode = true; // TODO
      boolean explainSwitch = false; // TODO LATER
      
      DCModel dcModel = modelService.getModel(modelType); // NB. type can't be null thanks to JAXRS
      if (dcModel == null) {
         throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
             .entity("Unknown model type " + modelType).type(MediaType.TEXT_PLAIN).build());
      }
      
      // TODO request priority : privilege INDEXED (Queriable) fields for query & sort !!!
      
      MultivaluedMap<String, String> params = uriInfo.getQueryParameters(true);
      // NB. to be able to refactor in LdpNativeQueryServiceImpl, could rather do :
      /// params = JAXRSUtils.getStructuredParams((String)message.get(Message.QUERY_STRING), "&", decode, decode);
      // (as getQueryParameters itself does, or as is done in LdpEntityQueryServiceImpl)
      // by getting query string from either @Context-injected CXF Message or uriInfo.getRequestUri()
      // BUT this would be costlier and the ideal, unified solution is rather having a true object model
      // of parsed queries
      List<DCEntity> foundEntities;
      try {
         foundEntities = ldpEntityQueryService.findDataInType(dcModel, params, start, limit);
      } catch (QueryException qex) {
         throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
               .entity(qex.getMessage()).type(MediaType.TEXT_PLAIN).build());
         // TODO if warnings return them as response header ?? or only if failIfWarningsMode ??
         // TODO better support for query parsing errors / warnings / detailedMode & additional
         // non-error behaviours of query engines like "explain" switch
      }
      
      List<DCResource> foundDatas = entitiesToResources(foundEntities);
      return foundDatas;
   }

   
   /**
    * WARNING Requires ALLOW_NUMERIC_LEADING_ZEROS enabled on JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS
    * else fails when years serialized with wrong leading zeroes ex. 0300 :
    * JsonParseException: Invalid numeric value: Leading zeroes not allowed
    * in ReaderBasedJsonParser._verifyNoLeadingZeroes()
    * @param stringValue
    * @return
    * @throws ResourceParsingException
    */
   // TODO move to Resource(Primitive)ParsingService
   public DateTime parseDate(String stringValue) throws ResourceParsingException {
      try {
         return mapper.readValue(stringValue, DateTime.class);
      } catch (IOException ioex) {
         throw new ResourceParsingException("IO error while reading ISO 8601 Date-formatted string : "
               + stringValue, ioex);
      } catch (Exception e) {
         throw new ResourceParsingException("date Field value is not "
               + "an ISO 8601 Date-formatted string : " + stringValue, e);
      }
   }

   // TODO move to Resource(Primitive)(Query)ParsingService ; used only by query
   public Boolean parseBoolean(String stringBoolean) throws ResourceParsingException {
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

   // TODO move to Resource(Primitive)(Query)ParsingService ; used only by query
   public Integer parseInteger(String stringInteger) throws ResourceParsingException {
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
      } catch (QueryException qex) {
         throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
               .entity(qex.getMessage()).type(MediaType.TEXT_PLAIN).build());
         // TODO if warnings return them as response header ?? or only if failIfWarningsMode ??
         // TODO better support for query parsing errors / warnings / detailedMode & additional
         // non-error behaviours of query engines like "explain" switch
      }
      return entitiesToResources(entities);
   }
   @Override
   public List<DCResource> queryData(String query, String language) {
      List<DCEntity> entities;
      try {
         entities = this.entityQueryService.query(query, language);
      } catch (QueryException qex) {
         throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
               .entity(qex.getMessage()).type(MediaType.TEXT_PLAIN).build());
         // TODO if warnings return them as response header ?? or only if failIfWarningsMode ??
         // TODO better support for query parsing errors / warnings / detailedMode & additional
         // non-error behaviours of query engines like "explain" switch
      }
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
