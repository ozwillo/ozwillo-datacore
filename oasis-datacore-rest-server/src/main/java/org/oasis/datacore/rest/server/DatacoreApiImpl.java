package org.oasis.datacore.rest.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.model.ResourceTypes;
import org.joda.time.DateTime;
import org.oasis.datacore.core.entity.DCEntityService;
import org.oasis.datacore.core.entity.EntityQueryService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.query.QueryException;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryService;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.server.event.DCResourceEvent;
import org.oasis.datacore.rest.server.event.DCResourceEvent.Types;
import org.oasis.datacore.rest.server.event.EventService;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.parsing.model.DCResourceParsingContext;
import org.oasis.datacore.rest.server.parsing.service.QueryParsingService;
import org.oasis.datacore.rest.server.resource.ResourceException;
import org.oasis.datacore.rest.server.resource.ResourceNotFoundException;
import org.oasis.datacore.rest.server.resource.ResourceObsoleteException;
import org.oasis.datacore.rest.server.resource.ResourceService;
import org.oasis.datacore.rest.server.resource.ResourceTypeNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;


/**
 * Returned HTTP status : follows recommended standards, see
 * http://stackoverflow.com/questions/2342579/http-status-code-for-update-and-delete/18981344#18981344
 * 
 * Specific HTTP Status (and custom response content) are returned by throwing
 * WebApplicationException subclasses, rather than having ugly Response-typed returns in
 * all operations. Only drawback is more logs, which is mitigated by custom CXF
 * Datacore FaultListener. An alternative would have been to inject HttpServletResponse
 * and set its status : response.setStatus(Response.Status.CREATED.getStatusCode()).
 * 
 * TODOs :
 * * error messages as constants ? or i18n'd ???
 * 
 * @author mdutoo
 *
 */
@Path("dc") // relative path among other OASIS services
@Consumes(MediaType.APPLICATION_JSON) // TODO finer media type ex. +oasis-datacore ??
@Produces(MediaType.APPLICATION_JSON)
///@Component("datacoreApiServer") // else can't autowire Qualified ; TODO @Service ?
public class DatacoreApiImpl implements DatacoreApi {

   private boolean strictPostMode = false;
   
   @Autowired
   private ResourceService resourceService;
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
   private MongoOperations mgo; // TODO remove it by hiding it in (entity) services
   @Autowired
   private EventService eventService; // TODO remove it by hiding it in (entity, resource ?) services
   // NB. MongoTemplate would be required to check last operation result, but we rather use WriteConcerns
   @Autowired
   private DCModelService modelService;
   
   @Autowired
   private QueryParsingService queryParsingService;

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
      Status status = (postedResource.getVersion() == 0) ? Response.Status.CREATED : Response.Status.OK;
      throw new WebApplicationException(Response.status(status)
            .tag(eTag) // .lastModified(dataEntity.getLastModified().toDate())
            .entity(postedResource)
            .type(MediaType.APPLICATION_JSON).build());
   }

   /**
    * Does the actual work of postDataInType except returning status & etag,
    * so it can also be used in post/putAllData(InType)
    * @param resource
    * @param modelType
    * @param canCreate
    * @param canUpdate
    * @return
    */
   public DCResource internalPostDataInType(DCResource resource, String modelType,
         boolean canCreate, boolean canUpdate) {
      try {
         return createOrUpdate(resource, modelType, canCreate, canUpdate);
         
      } catch (ResourceTypeNotFoundException rtnfex) {
         throw new NotFoundException(Response.status(Response.Status.NOT_FOUND)
               .entity(rtnfex.getMessage()).type(MediaType.TEXT_PLAIN).build());
         
      } catch (ResourceNotFoundException rnfex) {
         throw new NotFoundException(Response.status(Response.Status.NOT_FOUND)
               .entity(rnfex.getMessage()).type(MediaType.TEXT_PLAIN).build());
         
      } catch (ResourceObsoleteException roex) {
         throw new ClientErrorException(Response.status(Response.Status.CONFLICT)
               .entity(roex.getMessage()).type(MediaType.TEXT_PLAIN).build());
         
      } catch (BadUriException buex) {
         throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
               .entity("Error while parsing root URI '" + buex.getUri() + "' : " + buex.getOwnMessage())
               .type(MediaType.TEXT_PLAIN).build());
         
      } catch (ResourceException rex) {
         throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
               .entity(rex.getMessage()).type(MediaType.TEXT_PLAIN).build());
      }
   }
   
   /**
    * @param resource
    * @param modelType
    * @param canCreate
    * @param canUpdate
    * @return
    * @throws ResourceTypeNotFoundException if unknown model type
    * @throws ResourceNotFoundException
    * @throws BadUriException if missing (null), malformed, syntax,
    * relative without type, uri's and given modelType don't match
    * @throws ResourceParsingException
    * @throws ResourceObsoleteException
    * @throws ResourceException if no data (null resource), forbidden version in strict POST mode,
    * missing required version in PUT mode,
    */
   public DCResource createOrUpdate(DCResource resource, String modelType,
         boolean canCreate, boolean canUpdate) throws ResourceTypeNotFoundException,
         ResourceNotFoundException, BadUriException, ResourceObsoleteException, ResourceException {
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
         throw new ResourceException("No data (modelType = " + modelType + ")", null);
      }
      
      Long version = resource.getVersion();
      if (version != null && version >= 0) {
         if (!canUpdate) {
            throw new ResourceException("Version is forbidden in POSTed data resource "
                  + "to create in strict POST mode", resource);
         } else {
            isCreation = false;
         }
      } else if (!canCreate) {
         throw new ResourceException("Version of data resource to update is required in PUT", resource);
      }
      
      DCModel dcModel = modelService.getModel(modelType); // NB. type can't be null thanks to JAXRS
      if (dcModel == null) {
         throw new ResourceTypeNotFoundException(modelType, null, null, resource);
      }
      modelType = dcModel.getName(); // normalize ; TODO useful ?
      
      
      // TODO in resourceService.build() :
      List<String> resourceTypes = resource.getTypes(); // TODO use it to know what decorating sub mixin to write only... 
      resourceTypes.clear(); /// TODO else re-added
      if (/*resourceTypes.isEmpty()*/true) { // NB. can't be null ; TODO or always ?? because in DCResource.create()...
         resourceTypes.add(modelType);
         resourceTypes.addAll(dcModel.getGlobalMixinNameSet());
      } // else TODO better add missing ones, or allow them if optional...
      // pre parse hooks (before parsing, to give them a chance to still patch resource ex. auto set uri & id / iri)
      // TODO better as 2nd pass / within parsing ?? or rather on entity ????
      eventService.triggerResourceEvent(DCResourceEvent.Types.ABOUT_TO_BUILD, resource);
      
      
      String stringUri = resource.getUri();
      DCURI uri = resourceService.normalizeAdaptCheckTypeOfUri(stringUri, modelType, normalizeUrlMode, matchBaseUrlMode);
      stringUri = uri.toString();
      
      // TODO extract to checkContainer()
      if (!resourceService.getContainerUrl().equals(uri.getContainer())) {
         if (!brokerMode) {
            throw new ResourceException("In non-broker mode, can only serve data from own container "
                        + resourceService.getContainerUrl() + " but URI container is "
                        + uri.getContainer(), resource);
         }
         // else TODO LATER OPT broker mode
      }

      DCEntity dataEntity = null;
      if (!canUpdate || !isCreation) {
         dataEntity = entityService.getByUriId(stringUri, dcModel);
         if (dataEntity != null) {
            if (!canUpdate) {
               // already exists, but only allow creation
               throw new ResourceException("Already exists at uri (forbidden in strict POST mode) :\n"
                           + dataEntity.toString(), resource); // TODO TODO security check access first !!!
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
               throw new ResourceNotFoundException("Data resource doesn't exist (forbidden in PUT)", resource);
            }
         }
      }
      
      /**/
      
      if (dataEntity == null) {
         dataEntity = new DCEntity();
         dataEntity.setUri(stringUri);
      }

      Map<String, Object> dataProps = resource.getProperties();
      
      dataEntity.setModelName(dcModel.getName()); // TODO LATER2 check that same (otherwise ex. external approvable contrib ??)
      dataEntity.setVersion(version);
      
      // parsing resource according to model :
      DCResourceParsingContext resourceParsingContext = new DCResourceParsingContext(dcModel, stringUri);
      //List<DCEntity> embeddedEntitiesToAlsoUpdate = new ArrayList<DCEntity>(); // TODO embeddedEntitiesToAlsoUpdate ??
      //resourceParsingContext.setEmbeddedEntitiesToAlsoUpdate(embeddedEntitiesToAlsoUpdate);
      resourceToEntityFields(dataProps, dataEntity.getProperties(), dcModel.getGlobalFieldMap(),
            resourceParsingContext); // TODO dcModel.getFieldNames(), abstract DCModel-DCMapField ??
      
      if (resourceParsingContext.hasErrors()) {
         String msg = DCResourceParsingContext.formatParsingErrorsMessage(resourceParsingContext, detailedErrorsMode);
         throw new ResourceException(msg, resource);
      } // else TODO if warnings return them as response header ?? or only if failIfWarningsMode ??

      // pre save hooks
      // TODO better as 2nd pass / within parsing ?? or rather on entity ????
      Types aboutToEventType = isCreation ? DCResourceEvent.Types.ABOUT_TO_CREATE : DCResourceEvent.Types.ABOUT_TO_UPDATE;
      eventService.triggerResourceEvent(aboutToEventType, resource);
      
      String collectionName = dcModel.getCollectionName(); // or mere type ?
      ///dataEntity.setId(stringUri); // NOO "invalid Object Id" TODO better
      dataEntity.setTypes(resource.getTypes()); // TODO or no modelType, or remove modelName ??
      if (isCreation) {
         // will fail if exists
         mgo.insert(dataEntity, collectionName); // TODO does it enforce version if exists ??
         
      } else {
         try {
            mgo.save(dataEntity, collectionName);
         } catch (OptimisticLockingFailureException olfex) {
            throw new ResourceObsoleteException("Trying to update data resource "
                  + "without up-to-date version but " + dataEntity.getVersion(), resource);
         }
      }

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

      // 2nd pass : post save hooks
      // TODO better
      Types doneEventType = isCreation ? DCResourceEvent.Types.CREATED : DCResourceEvent.Types.UPDATED;
      eventService.triggerResourceEvent(doneEventType, resource);
      
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
         entityValue = queryParsingService.parseValue(DCFieldTypeEnum.LONG, (String)resourceValue);
         
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
            DCURI dcUri;
            try {
               dcUri = resourceService.normalizeAdaptCheckTypeOfUri(stringUriValue, null,
                     normalizeUrlMode, replaceBaseUrlMode);
            } catch (BadUriException buex) {
               throw new ResourceParsingException(buex.getMessage(), buex.getCause());
            }
            DCModel refModel = modelService.getModel(dcUri.getType()); // TODO LATER from cached model ref in DCURI
            
            // checking that it exists :
            DCEntity refEntity = entityService.getByUriId(dcUri.toString(), refModel);
            if (refEntity == null) {
               throw new ResourceParsingException("Can't find data of resource type " + refModel.getName()
                     + " referenced by resource Field of URI value " + dcUri.toString());
            }
            // check type :
            // TODO mixins / aspects / type constraints
            //if (!(dcField instanceof DCResourceField)) { // never happens, TODO rather check it at model creation / change
            //   throw new ModelConfigurationException("Fields of type resource should be DCResourceFields instance");
            //}
            if (!dcUri.getType().equals(((DCResourceField) dcField).getResourceType())) {
            //if (!modelService.hasType(refEntity, ((DCResourceField) dcField).getTypeConstraints())) {
               throw new ResourceParsingException("Target resource referenced by resource Field of URI value "
                     + dcUri + " is not of required type " + ((DCResourceField) dcField).getResourceType());
               // does not match type constraints TODO
            }
            
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
            DCURI dcUri;
            try {
               dcUri = resourceService.normalizeAdaptCheckTypeOfUri((String) entityValueUri, null,
                     normalizeUrlMode, replaceBaseUrlMode);
            } catch (BadUriException buex) {
               throw new ResourceParsingException(buex.getMessage(), buex.getCause());
            }
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
               //if (!(dcField instanceof DCResourceField)) { // never happens, TODO rather check it at model creation / change
               //   throw new ModelConfigurationException("Fields of type resource should be DCResourceFields instance");
               //}
               if (!dcUri.getType().equals(((DCResourceField) dcField).getResourceType())) {
               //if (!modelService.hasType(refEntity, ((DCResourceField) dcField).getTypeConstraints())) {
                  throw new ResourceParsingException("Target resource referenced by resource Field of URI value "
                        + dcUri + " is not of required type " + ((DCResourceField) dcField).getResourceType());
                  // does not match type constraints TODO
               }
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
                  valueModel.getGlobalFieldMap(), resourceParsingContext);
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
      List<DCResource> res = this.internalPostAllDataInType(resources, modelType, true, !this.strictPostMode);
      throw new WebApplicationException(Response.status(Response.Status.CREATED)
            ///.tag(new EntityTag(httpEntity)) // .lastModified(dataEntity.getLastModified().toDate())
            .entity(res)
            .type(MediaType.APPLICATION_JSON).build());
   }
   public List<DCResource> internalPostAllDataInType(List<DCResource> resources, String modelType,
         boolean canCreate, boolean canUpdate) {
      if (resources == null || resources.isEmpty()) {
         throw new WebApplicationException(Response.status(Response.Status.NO_CONTENT)
               .type(MediaType.APPLICATION_JSON).build());
      }
      ArrayList<DCResource> res = new ArrayList<DCResource>(resources.size());
      for (DCResource resource : resources) {
         // TODO LATER rather put exceptions in context to allow always checking multiple POSTed resources
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
      return res;
   }
   public List<DCResource> postAllData(List<DCResource> resources/*, Request request*/) {
      List<DCResource> res = this.internalPostAllData(resources, true, !this.strictPostMode);
      throw new WebApplicationException(Response.status(Response.Status.CREATED)
            ///.tag(new EntityTag(httpEntity)) // .lastModified(dataEntity.getLastModified().toDate())
            .entity(res)
            .type(MediaType.APPLICATION_JSON).build());
   }
   public List<DCResource> internalPostAllData(List<DCResource> resources, boolean canCreate, boolean canUpdate) {
      if (resources == null || resources.isEmpty()) {
         throw new WebApplicationException(Response.status(Response.Status.NO_CONTENT)
               .type(MediaType.APPLICATION_JSON).build());
      }
      
      boolean replaceBaseUrlMode = true;
      boolean normalizeUrlMode = true;
      
      ArrayList<DCResource> res = new ArrayList<DCResource>(resources.size());
      for (DCResource resource : resources) {
         DCURI uri;
         try {
            uri = resourceService.normalizeAdaptCheckTypeOfUri(resource.getUri(), null, 
                  normalizeUrlMode, replaceBaseUrlMode);
         } catch (BadUriException rpex) {
            // TODO LATER rather context & for multi post
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
                  .entity(rpex.getMessage()).type(MediaType.TEXT_PLAIN).build());
         }
         res.add(internalPostDataInType(resource, uri.getType(), canCreate, canUpdate));
         // NB. no ETag validation support, see discussion in internalPostAllDataInType()
      }
      return res;
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
      String uri = resourceService.buildUri(modelType, iri);

      DCModel dcModel = modelService.getModel(modelType); // NB. type can't be null thanks to JAXRS
      if (dcModel == null) {
         throw new NotFoundException(Response.status(Response.Status.NOT_FOUND)
             .entity("Unknown Model type " + modelType).type(MediaType.TEXT_PLAIN).build());
      }
      modelType = dcModel.getName(); // normalize ; TODO useful ?

      DCEntity entity = entityService.getByUriId(uri, dcModel);
      
      if (entity == null) {
         //return Response.noContent().build();
         throw new NotFoundException();
         // rather than NO_CONTENT ; like Atol ex. deleteApplication in
         // https://github.com/pole-numerique/oasis/blob/master/oasis-webapp/src/main/java/oasis/web/apps/ApplicationDirectoryResource.java
      }

      // TODO ETag jaxrs caching :
      // on GET, If-None-Match precondition else return 304 Not Modified http://stackoverflow.com/questions/2021882/is-my-implementation-of-http-conditional-get-answers-in-php-is-ok/2049723#2049723
      // see for jaxrs https://devcenter.heroku.com/articles/jax-rs-http-caching#conditional-cache-headers
      // and for http http://odino.org/don-t-rape-http-if-none-match-the-412-http-status-code/
      // also http://stackoverflow.com/questions/2085411/how-to-use-cxf-jax-rs-and-http-caching
      String httpEntity = entity.getVersion().toString(); // no need of additional uri because only for THIS resource
      EntityTag eTag = new EntityTag(httpEntity);
      //ResponseBuilder builder = request.evaluatePreconditions(dataEntity.getUpdated(), eTag);
      ResponseBuilder builder = (request != null) ?
            request.evaluatePreconditions(eTag) : null; // else not deployed as jaxrs
      
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
      String uri = resourceService.buildUri(modelType, iri);
      
      String etag = httpHeaders.getHeaderString(HttpHeaders.IF_MATCH);
      if (etag == null) {
         throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
               .entity("Missing If-Match=version header to delete " + uri).type(MediaType.TEXT_PLAIN).build());
      }
      Long version;
      try {
         version = Long.parseLong(etag);
      } catch (NumberFormatException e) {
         throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
               .entity("If-Match header should be a long to delete " + uri
                     + " but is " + etag).type(MediaType.TEXT_PLAIN).build());
      }

      DCModel dcModel = modelService.getModel(modelType); // NB. type can't be null thanks to JAXRS
      if (dcModel == null) {
         throw new NotFoundException(Response.status(Response.Status.NOT_FOUND)
             .entity("Unknown Model type " + modelType).type(MediaType.TEXT_PLAIN).build());
      }
      modelType = dcModel.getName(); // normalize ; TODO useful ?
      String collectionName = modelType; // TODO or = dcModel.getCollectionName(); for weird type names ??

      Query query = new Query(Criteria.where("_uri").is(uri).and("_v").is(version));
      mgo.remove(query, collectionName);
      // NB. obviously won't conflict / throw MongoDataIntegrityViolationException
      // alt : first get it by _uri and check version, but in Mongo & REST spirit it's enough to
      // merely ensure that it doesn't exist at the end
      
      // NB. for proper error handling, we use WriteConcerns that ensures that errors are raised,
      // rather than barebone MongoTemplate.getDb().getLastError() (which they do),
      // see http://hackingdistributed.com/2013/01/29/mongo-ft/
      // However if we did it, it would be :
      // get operation result (using spring WriteResultChecking or native mt.getDb().getLastError())
      // then handle it / if failed throw error status :
      //if (notfound) {
      //   throw new WebApplicationException(Response.Status.NOT_FOUND);
      //   // rather than NO_CONTENT ; like Atol ex. deleteApplication in
      //   // https://github.com/pole-numerique/oasis/blob/master/oasis-webapp/src/main/java/oasis/web/apps/ApplicationDirectoryResource.java
      //}

      throw new WebApplicationException(Response.status(Response.Status.NO_CONTENT).build());
   }


   @Override
   public List<DCResource> updateDataInTypeWhere(DCResource dcDataDiff,
         String modelType, UriInfo uriInfo) {
      throw new InternalServerErrorException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity("Feature not implemented").type(MediaType.TEXT_PLAIN).build());
   }

   @Override
   public void deleteDataInTypeWhere(String modelType, UriInfo uriInfo) {
      throw new InternalServerErrorException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity("Feature not implemented").type(MediaType.TEXT_PLAIN).build());
   }
   
   
   public DCResource postDataInTypeOnGet(String modelType, String method, UriInfo uriInfo/*, Request request*/) {
      if ("POST".equalsIgnoreCase(method)) {
         DCResource resource = null;
         // TODO impl like find
         return this.postDataInType(resource, modelType);
      } else {
         throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
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
         this.deleteData(modelType, iri, httpHeaders); // will throw an exception to return the result code
         return null; // so will never go there
      } else {
         throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
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
         throw new NotFoundException(Response.status(Response.Status.NOT_FOUND)
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
         throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
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
         return (DateTime)queryParsingService.parseValue(DCFieldTypeEnum.DATE, stringValue);
      } catch (Exception e) {
         throw new ResourceParsingException("date Field value is not "
               + "an ISO 8601 Date-formatted string : " + stringValue, e);
      }
   }

   // TODO move to Resource(Primitive)(Query)ParsingService ; used only by query
   public Boolean parseBoolean(String stringBoolean) throws ResourceParsingException {
      try {
          return (Boolean)queryParsingService.parseValue(DCFieldTypeEnum.BOOLEAN, stringBoolean);
      } catch (Exception e) {
         throw new ResourceParsingException("Not a boolean-formatted string : "
               + stringBoolean, e);
      }
   }

   // TODO move to Resource(Primitive)(Query)ParsingService ; used only by query
   public Integer parseInteger(String stringInteger) throws ResourceParsingException {
      try {
          return (Integer)queryParsingService.parseValue(DCFieldTypeEnum.INTEGER, stringInteger);
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
         throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
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
         throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
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
      //resource.setId(entity.getIri()); // TODO ??
      /*ArrayList<String> types = new ArrayList<String>(entity.getTypes().size() + 1);
      types.add(entity.getModelName()); // TODO ??
      types.addAll(entity.getTypes());
      resource.setTypes(types);*/
      resource.setTypes(entity.getTypes()); // TODO or as above, or from model ?
      
      resource.setVersion(entity.getVersion());
      
      resource.setCreated(entity.getCreated()); // NB. if already provided, only if creation
      resource.setCreatedBy(entity.getCreatedBy()); // NB. if already provided, only if creation
      resource.setLastModified(entity.getLastModified());
      resource.setLastModifiedBy(entity.getLastModifiedBy());
      
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
