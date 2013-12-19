package org.oasis.datacore.rest.server.resource;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.oasis.datacore.core.entity.DCEntityService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.historization.exception.HistorizationException;
import org.oasis.datacore.historization.service.HistorizationService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.server.BadUriException;
import org.oasis.datacore.rest.server.event.DCResourceEvent;
import org.oasis.datacore.rest.server.event.DCResourceEvent.Types;
import org.oasis.datacore.rest.server.event.EventService;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.parsing.model.DCResourceParsingContext;
import org.oasis.datacore.rest.server.parsing.service.QueryParsingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * TODO LATER2 once ModelService & Models are exposed as REST and available on client side,
 * make this code also available on client side
 * 
 * @author mdutoo
 *
 */
@Component // TODO @Service ??
public class ResourceService {

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
   
   /** Base URL of this endpoint. If broker mode enabled, used to detect when to use it.. */
   @Value("${datacoreApiServer.baseUrl}") 
   private String baseUrl; // "http://" + "data-lyon-1.oasis-eu.org" + "/"
   /** Unique per container, defines it. To be able to build a full uri
    * (for GET, DELETE, possibly to build missing or custom / relative URI...) */
   @Value("${datacoreApiServer.containerUrl}") 
   private String containerUrl; // "http://" + "data.oasis-eu.org" + "/"

   @Autowired
   private DCEntityService entityService;
   //@Autowired
   //private DCDataEntityRepository dataRepo; // NO rather for (meta)model, for data can't be used because can't specify collection
   @Autowired
   private MongoOperations mgo; // TODO remove it by hiding it in (entity) services
   
   @Autowired
   private EventService eventService;
   
   @Autowired
   private DCModelService modelService;

   @Autowired
   private QueryParsingService queryParsingService;
   
   @Autowired
   private HistorizationService historizationService;
   
   
   public String getBaseUrl() {
      return baseUrl;
   }

   public String getContainerUrl() {
      return containerUrl;
   }
   
   /** helper method to create (fluently) new Resources FOR TESTING */
   public DCResource create(String modelType, String id) {
      DCResource r = DCResource.create(this.containerUrl, modelType, id);
      // init iri field :
      // TODO NO rather using creation (or builder) event hooked behaviours
      /*DCModel model = modelService.getModel(modelType);
      String iriFieldName = model.getIriFieldName();
      if (iriFieldName != null) {
         r.set(iriFieldName, iri);
      }*/
      return r;
   }
   
   /**
    * Helper for building Datacore maps
    * ex. resourceService.propertiesBuilder().put("name", "John").put("age", 18).build()
    * @return
    */
   public Builder<String, Object> propertiesBuilder() {
      return new ImmutableMap.Builder<String, Object>();
   }

   /**
    * helper method to create (build) new Resources FOR TESTING
    * triggers build event
    * @throws ResourceException
    * @throws RuntimeException wrapping AbortOperationEventException if a listener aborts it
    */
   public DCResource build(DCResource resource) throws ResourceException, RuntimeException {
      //if (resource.getUri() == null) {
      this.eventService.triggerResourceEvent(DCResourceEvent.Types.ABOUT_TO_BUILD, resource);
      //modelAdminService.getModel(resource).triggerEvent()
      return resource;
   }
   /** helper method to create (build) new Resources FOR TESTING
    * @obsolete use rather DCResource.create() & build() */
   public DCResource build(String type, String id, Map<String,Object> fieldValues) {
      DCResource resource = new DCResource();
      resource.setUri(this.buildUri(type, id));
      //resource.setVersion(-1l);
      resource.setProperty("type", type);
      for (Map.Entry<String, Object> fieldValueEntry : fieldValues.entrySet()) {
         resource.setProperty(fieldValueEntry.getKey(), fieldValueEntry.getValue());
      }
      return resource;
   }

   public String buildUri(String modelType, String id) {
      return new DCURI(this.containerUrl, modelType, id).toString();
   }

   /**
    * 
    * @param uri
    * @return
    * @throws BadUriException if missing (null), malformed, syntax,
    * relative without type, uri's and given modelType don't match
    */
   public DCURI parseUri(String uri) throws BadUriException {
      return normalizeAdaptCheckTypeOfUri(uri, null, false, true);
   }

   /**
    * TODO extract to UriService
    * Used
    * * by internalPostDataInType on root posted URI (where type is known and that can be relative)
    * * to check referenced URIs and URIs post/put/patch where type is not known (and are never relative)
    * @param stringUriValue
    * @param modelType if any should already have been checked and should be same as uri's if not relative
    * @param normalizeUrlMode
    * @param replaceBaseUrlMode
    * @return
    * @throws BadUriException if missing (null), malformed, syntax,
    * relative without type, uri's and given modelType don't match
    */
   public DCURI normalizeAdaptCheckTypeOfUri(String stringUri, String modelType,
         boolean normalizeUrlMode, boolean matchBaseUrlMode) throws BadUriException {
      if (stringUri == null || stringUri.length() == 0) {
         // TODO LATER2 accept empty uri and build it according to model type (governance)
         // for now, don't support it :
         throw new BadUriException();
      }
      String[] containerAndPathWithoutSlash;
      try {
         containerAndPathWithoutSlash = UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
            stringUri, this.containerUrl, normalizeUrlMode, matchBaseUrlMode);
      } catch (URISyntaxException usex) {
         throw new BadUriException("Bad URI syntax", stringUri, usex);
      } catch (MalformedURLException muex) {
         throw new BadUriException("Malformed URL", stringUri, muex);
      }
      String urlContainer = containerAndPathWithoutSlash[0];
      if (urlContainer == null) {
         // accept local type-relative uri (type-less iri) :
         if (modelType == null) {
            throw new BadUriException("URI can't be relative when no target model type is provided", stringUri);
         }
         return new DCURI(this.containerUrl, modelType, stringUri);
         // TODO LATER accept also iri including type
      }
      
      // otherwise absolute uri :
      String urlPathWithoutSlash = containerAndPathWithoutSlash[1];
      ///stringUri = this.containerUrl + urlPathWithoutSlash; // useless
      ///return stringUri;

      // check URI model type : against provided one if any, otherwise against known ones
      DCURI dcUri = UriHelper.parseURI(
            urlContainer, urlPathWithoutSlash/*, refModel*/); // TODO LATER cached model ref ?! what for ?
      String uriType = dcUri.getType();
      if (modelType != null) {
         if (!modelType.equals(uriType)) {
            throw new BadUriException("URI resource model type " + uriType
                  + " does not match provided target one " + modelType, stringUri);
         }
      }
      return dcUri;
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
      DCURI uri = this.normalizeAdaptCheckTypeOfUri(stringUri, modelType, normalizeUrlMode, matchBaseUrlMode);
      stringUri = uri.toString();
      
      // TODO extract to checkContainer()
      if (!this.getContainerUrl().equals(uri.getContainer())) {
         if (!brokerMode) {
            throw new ResourceException("In non-broker mode, can only serve data from own container "
                        + this.getContainerUrl() + " but URI container is "
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
         try {
			historizationService.historize(dataEntity, dcModel);
         } catch (HistorizationException e) {
			e.printStackTrace();
         }
         
      } else {
         try {
            mgo.save(dataEntity, collectionName);
            try {
    			historizationService.historize(dataEntity, dcModel);
             } catch (HistorizationException e) {
    			e.printStackTrace();
             }
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

   
   /**
    * Does 3 things :
    * * does additional parsing that Jackson-based DCResource mapping can't do
    * * checks that Resource complies with its Model
    * * converts to MongoDB-storable DCEntity
    * 
    * @param resourceValue
    * @param dcField
    * @param resourceParsingContext
    * @return
    * @throws ResourceParsingException
    */
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
         entityValue = (DateTime) queryParsingService.parseValue(DCFieldTypeEnum.DATE, (String) resourceValue);
         
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
               dcUri = this.normalizeAdaptCheckTypeOfUri(stringUriValue, null,
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
               dcUri = this.normalizeAdaptCheckTypeOfUri((String) entityValueUri, null,
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


   /**
    * Does 3 things :
    * * does additional parsing that Jackson-based DCResource mapping can't do
    * * checks that Resource complies with its Model (including missing fields)
    * * converts to MongoDB-storable DCEntity
    * 
    * @param resourceMap
    * @param entityMap
    * @param mapFields
    * @param resourceParsingContext
    */
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
   
}
