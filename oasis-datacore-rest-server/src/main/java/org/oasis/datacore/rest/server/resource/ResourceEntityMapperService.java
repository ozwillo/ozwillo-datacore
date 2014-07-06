package org.oasis.datacore.rest.server.resource;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.oasis.datacore.core.entity.EntityService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.server.BadUriException;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.parsing.model.DCResourceParsingContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ResourceEntityMapperService {

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


   @Autowired
   private UriService uriService;

   /** TODO replace by resourceService to check rights on refs */
   @Autowired
   private EntityService entityService;
   
   @Autowired
   private DCModelService modelService;
   
   @Autowired
   private ValueParsingService valueParsingService;
   
   /**
    * Does 3 things :
    * * does additional parsing that Jackson-based DCResource mapping can't do
    * * checks that Resource complies with its Model
    * * converts to MongoDB-storable DCEntity
    * 
    * @param resourceValue
    * @param dcField
    * @param resourceParsingContext
    * @param putRatherThanPatchMode TODO or in resourceParsingContext ??
    * @return
    * @throws ResourceParsingException
    */
   // TODO extract to ResourceBuilder static helper using in ResourceServiceImpl
   private Object resourceToEntityValue(Object resourceValue, DCField dcField,
         DCResourceParsingContext resourceParsingContext, boolean putRatherThanPatchMode)
               throws ResourceParsingException {
      boolean checkExternalWebUri = true;
      boolean checkExternalDatacoreUri = false;
      
      Object entityValue;
      if (resourceValue == null) {
         if (!dcField.isRequired() && !"string".equals(dcField.getType())) { // TODO ? for all ?! or only for MapField ??
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
            if (resourceValue instanceof Double) {
               // BEWARE jackson parses as Double what is sent as Float !
               entityValue = (Double) resourceValue;
            } else if (resourceValue instanceof Integer) {
               entityValue = new Float((Integer) resourceValue);
               resourceParsingContext.addWarning("float Field value is a JSON integer : " + resourceValue
                     + ", which allowed as fallback but should rather be a JSON float");
            } else {
               // other types ex. Double, long are wrong
               throw new ResourceParsingException("float Field value is not a JSON float "
                     + "(nor integer, which is allowed as fallback) : " + resourceValue);
            }
         } else {
            entityValue = (Float) resourceValue; // TODO LATER also allow String  ?? 
         } 
      } else if ("long".equals(dcField.getType())) {
         // Javascript has no long : http://javascript.about.com/od/reference/g/rlong.htm
         // so supported through String instead (or Integer as fallback)
         if (!(resourceValue instanceof String)) {
            if (resourceValue instanceof Long) {
               entityValue = (Long) resourceValue; // can only happen if called locally, NOT remotely through jackson
            } else if (resourceValue instanceof Integer) {
               entityValue = new Long((Integer) resourceValue);
               resourceParsingContext.addWarning("long Field value is a JSON integer : " + resourceValue
                     + ", which allowed as fallback but should rather be a JSON long");
            } else {
               // other types ex. Double, float are wrong
               throw new ResourceParsingException("long Field value is not a string : " + resourceValue);
            }
         } else {
            entityValue = valueParsingService.parseLongFromString((String) resourceValue);
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
         entityValue = (DateTime) valueParsingService.parseDateFromString((String) resourceValue);
         
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
               resourceParsingContext, putRatherThanPatchMode);
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
               entityItem = resourceToEntityValue(resourceItem, listElementField,
                     resourceParsingContext, putRatherThanPatchMode);
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
         //if (!(dcField instanceof DCResourceField)) { // never happens, TODO rather check it at model creation / change
         //   throw new ModelConfigurationException("Fields of type resource should be DCResourceFields instance");
         //}
         DCResourceField dcResourceField = (DCResourceField) dcField;
         
         if (resourceValue instanceof String) {
            // "flattened" (JSONLD) : uri ref
            String stringUriValue = (String) resourceValue;
            
            // normalize, adapt & check :
            boolean normalizeUrlMode = true;
            boolean replaceBaseUrlMode = true;
            DCURI dcUri;
            try {
               dcUri = uriService.normalizeAdaptCheckTypeOfUri(stringUriValue,
                     dcResourceField.getResourceType(), normalizeUrlMode, replaceBaseUrlMode);
               // NB. this also checks model type if any
            } catch (BadUriException buex) {
               throw new ResourceParsingException(buex.getMessage(), buex.getCause());
            }
            
            if (dcUri.isExternalWebUri()) {
               if (checkExternalWebUri) {
                  checkWebHttpUrl(dcUri, resourceParsingContext);
               }
            } else {
               // Datacore URI (local or external)
               
               // check type :
               // TODO more mixins / aspects / type constraints
               DCModel refModel = modelService.getModel(dcUri.getType()); // TODO LATER from cached model ref in DCURI
               // NB. type has been checked in uriService above
               /*if (!dcUri.getType().equals(((DCResourceField) dcField).getResourceType())) {
               //if (!modelService.hasType(refEntity, ((DCResourceField) dcField).getTypeConstraints())) {
                  throw new ResourceParsingException("Target resource referenced by resource Field of URI value "
                        + dcUri + " is not of required type " + ((DCResourceField) dcField).getResourceType());
                  // does not match type constraints TODO
               }*/
               
               // checking that it exists :
               if (dcUri.isExternalDatacoreUri()) {
                  if (checkExternalDatacoreUri) {
                     // TODO LATER OPT check using (dynamic ??) CXF Datacore client that propagates auth
                     // TODO LATER OPT2 store and check also version, to allow consistent references ??
                     throw new UnsupportedOperationException("Not implemented yet");
                  }
                  
               } else {
                  // local Datacore URI
               
                  // (without rights ; only in checkMode, otherwise only warning)
                  if (entityService.getByUriUnsecured(dcUri.toString(), refModel) == null) {
                     // TODO rather still allow update / only warning ? & add /check API keeping it as error ??
                     throw new ResourceParsingException("Can't find data of resource type " + refModel.getName()
                           + " referenced by resource Field of URI value " + dcUri.toString());
                  }
               
               }
            }
            
            // TODO TODO provide refEntity in model (ex. DCResourceValue) OR IN GRAPH,
            // ex. for 2nd pass or in case of expanded + embedded return ?!?
            //entityValue = refEntity;
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
               dcUri = uriService.normalizeAdaptCheckTypeOfUri((String) entityValueUri,
                     dcResourceField.getResourceType(), normalizeUrlMode, replaceBaseUrlMode);
               // NB. this also checks model type if any
            } catch (BadUriException buex) {
               throw new ResourceParsingException(buex.getMessage(), buex.getCause());
            }
            
            if (dcUri.isExternalUri()) {
               throw new ResourceParsingException("Embedded resource should not have "
                     + "external uri but has " + dcUri.toString());
               // TODO LATER OPT2 or as local copy of an existing external resource ???
            }

            // check type :
            // TODO more mixins / aspects / type constraints
            DCModel valueModel = modelService.getModel(dcUri.getType()); // TODO LATER from cached model ref in DCURI
            // NB. type has been checked in uriService above
            /*if (!dcUri.getType().equals(((DCResourceField) dcField).getResourceType())) {
            //if (!modelService.hasType(refEntity, ((DCResourceField) dcField).getTypeConstraints())) {
               throw new ResourceParsingException("Target resource referenced by resource Field of URI value "
                     + dcUri + " is not of required type " + ((DCResourceField) dcField).getResourceType());
               // does not match type constraints TODO
            }*/

            // checking that it exists :
            DCEntity entityEntityValue = entityService.getByUri(dcUri.toString(), valueModel);
            
            // TODO TODO provide refEntity in model (ex. DCResourceValue) OR IN GRAPH,
            // ex. for 2nd pass or in case of expanded + embedded return ?!?

            // supporting PUT vs default PATCH-like POST mode :
            if (entityEntityValue == null) {
               // TODO allow not exists & creation ????!?
               if (!allowNew) {
                  throw new ResourceParsingException("Can't find data of resource type " + dcUri.getType()
                        + " referenced by resource Field of Object value with URI property " + dcUri);
               }
               entityEntityValue = new DCEntity();
               entityEntityValue.setCachedModel(valueModel); // TODO or in DCEntityService ?
               entityEntityValue.setUri(dcUri.toString());
               // NB. no version yet
               
            } else if (putRatherThanPatchMode) {
               entityEntityValue.getProperties().clear();
            } // else reuse existing entity as base : PATCH-like behaviour

            entityEntityValue.setModelName(valueModel.getName()); // TODO LATER2 check that same (otherwise ex. external approvable contrib ??)
            
            // get version if any, LATER might be used for transaction :
            // TODO parseLong
            Object entityValueVersion = dataMap.get("version");
            if (entityValueVersion == null) {
               // allowed (NB. but then can't update ??!!)
               resourceParsingContext.addWarning("No embedded resource version");
            } else if (entityValueVersion instanceof Integer) { // TODO refactor to parseInt ?
               entityEntityValue.setVersion(new Long((Integer) entityValueVersion));
            } else if (entityValueVersion instanceof String) { // TODO refactor to parseLongFromString ?
               try {
                  // Javascript don't support Long so allow them as String :
                  entityEntityValue.setVersion(Long.parseLong((String) entityValueVersion));
               } catch (NumberFormatException nfex) {
                  resourceParsingContext.addWarning("Embedded resource version is not "
                        + "a long-formatted String but " + entityValueVersion);
               }
            } else {
               resourceParsingContext.addWarning("Embedded resource version is not "
                     + "an int, a long or a long-formatted String but " + entityValueVersion);
            }

            // get types if any :
            Object entityValueTypesFound = dataMap.get("types");
            if (entityValueTypesFound == null) {
               // allowed
               resourceParsingContext.addWarning("No embedded resource types");
            } else if (entityValueTypesFound instanceof List<?>) {
               @SuppressWarnings("unchecked")
               List<String> entityValueTypes = (List<String>) entityValueTypesFound;
               entityEntityValue.setTypes(entityValueTypes); // TODO or no modelType, or remove modelName ??
            } else {
               resourceParsingContext.addWarning("Embedded resource types is not "
                     + "a String list but " + entityValueTypesFound);
            }
            
            // parse other fields :
            // TODO for what use, multiple update or even creation (rather flattened ?) ?? constraints ???
            // TODO do it later to allow all-encompassing transaction ???
            // clean by removing native fields : (TODO or clone first, ex. if immutable ??)
            for (String resourceNativeJavaField : resourceNativeJavaFields) {
               dataMap.remove(resourceNativeJavaField);
            }
            resourceToEntityFields(dataMap, entityEntityValue.getProperties(),
                  valueModel.getGlobalFieldMap(), resourceParsingContext,putRatherThanPatchMode);
            // TODO TODO add entityEntityValue to graph ?! again, for what use :
            // multiple update or even creation (rather flattened ?) ??
            //embeddedEntitiesToAlsoUpdate.add(entityEntityValue);
            // constraints ex. "update only if these fields have these values (in addition to version)" ???
            // cache it in DCURI for ex. rendering returned value ??
            // dcUri.setEntity(entityEntityValue);
            
            /////entityValue = dcUri.toString();
            entityValue = entityEntityValue;
            
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
    * For now only 404 or 410 are errors, meaning that private or offline resources
    * are only warnings and don't break ex. offline testing.
    * Public method only for testing ; or TODO in UriService or ... ??
    * TODO LATER OPT2 also private resources, with autn/z according to knownWebContainers.
    * @param dcUri
    * @param resourceParsingContext
    * @throws ResourceParsingException
    */
   public void checkWebHttpUrl(DCURI dcUri, DCResourceParsingContext resourceParsingContext)
         throws ResourceParsingException {
      URL httpUrl;
      try {
         httpUrl = dcUri.toURI().toURL();
      } catch (MalformedURLException | URISyntaxException ex) {
         // already tested, should not happen
         throw new ResourceParsingException("Unexpected error checking referenced public HTTP URL", ex);
      }
      try {
         HttpURLConnection httpConn = (HttpURLConnection) httpUrl.openConnection();
         httpConn.setInstanceFollowRedirects(false); // else redirection
         // to "unavailable" web page might be taken as 200 available
         httpConn.setRequestMethod("HEAD"); // optimized, doesn't retrieve content
         httpConn.connect();
         int responseCode = httpConn.getResponseCode();
         switch (responseCode) {
         case 200 :
            return; // OK
         case 404 :
            throw new ResourceParsingException("Referenced external web resource not found "
                  + "at its URI " + dcUri.toString());
         case 410 :
            throw new ResourceParsingException("Referenced external web resource gone "
                  + "at its URI " + dcUri.toString());
         case 401 :
         case 403 :
            resourceParsingContext.addWarning("Referenced external web resource is private "
                  + "at its URI " + dcUri.toString() + ", check not supported yet");
            break;
         }
         if (responseCode % 100 == 5) {
            resourceParsingContext.addWarning("Can't check referenced external web resource URI "
                  + "because returns server error " + responseCode);
         } else {
            // including 2 which should not happen
            resourceParsingContext.addWarning("Referenced external web resource URI "
                  + "returns unexpected code " + responseCode);
         }
      } catch (IOException ex) {
         // ProtocolException because of "HEAD" so should not happen
         resourceParsingContext.addWarning("Can't check referenced external web resource "
               + "because IO error ", ex);
      }
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
    * @param putRatherThanPatchMode TODO or in resourceParsingContext ??
    */
   // TODO extract to ResourceBuilder static helper using in ResourceServiceImpl
   public void resourceToEntityFields(Map<String, Object> resourceMap,
         Map<String, Object> entityMap, Map<String, DCField> mapFields,
         // TODO mapFieldNames ; orderedMap ? abstract Field-Model ??
         DCResourceParsingContext resourceParsingContext,
         boolean putRatherThanPatchMode) {
      
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
            resourceParsingContext.addError("Unknown field " + key);
            continue;
         }
         
         missingRequiredFieldNames.remove(key);
         
         Object resourceValue = resourceMap.get(key);
         DCField dcField = mapFields.get(key); // TODO DCModel.getField(key)
         try {
            resourceParsingContext.enter(dcField, resourceValue);
            Object entityValue = resourceToEntityValue(resourceValue, dcField,
                  resourceParsingContext, putRatherThanPatchMode);
            entityMap.put(key, entityValue);
         } catch (ResourceParsingException rpex) {
            resourceParsingContext.addError("Error while parsing Field value " + resourceValue
                  + " of JSON type " + ((resourceValue == null) ? "null" : resourceValue.getClass()), rpex);
         } catch (Exception ex) {
            resourceParsingContext.addError("Unknown error while parsing Field value " + resourceValue
                  + " of JSON type " + ((resourceValue == null) ? "null" : resourceValue.getClass()), ex);
         } finally {
            resourceParsingContext.exit();
         }
      }
      
      if (!missingRequiredFieldNames.isEmpty()) {
         resourceParsingContext.addError("Some Fields are missing : " + missingRequiredFieldNames); 
      }
   }

   
   
   
   public DCResource entityToResource(DCEntity entity) {
      Map<String, Object> resourceProps = new HashMap<String,Object>(entity.getProperties().size());
      // TODO better for properties (especially aspects ?!!)
      Map<String, Object> entityProps = entity.getProperties();
      for (String entityPropName : entity.getProperties().keySet()) { // or iterate on fields ?
         Object resourcePropValue = entityProps.get(entityPropName);
         /*if (resourcePropValue instanceof DCEntity) {
            
         } else */if (resourcePropValue instanceof Date) {
            if (!(resourcePropValue instanceof DateTime)) {
               // TODO better in mongo persistence
               resourcePropValue = new DateTime((Date) resourcePropValue, DateTimeZone.UTC);
               // NB. if not UTC, default timezone has a non-integer time shift
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
   
   public List<DCResource> entitiesToResources(List<DCEntity> entities) {
      ArrayList<DCResource> datas = new ArrayList<DCResource>(entities.size());
      for (DCEntity entity : entities) {
         DCResource resource = entityToResource(entity);
         datas.add(resource);
      }
      return datas;
   }
   
}
