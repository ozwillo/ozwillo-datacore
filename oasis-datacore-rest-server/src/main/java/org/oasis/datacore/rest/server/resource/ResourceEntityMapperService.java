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

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.oasis.datacore.core.entity.EntityService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCI18nField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.server.BadUriException;
import org.oasis.datacore.rest.server.cxf.CxfJaxrsApiProvider;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.parsing.model.DCResourceParsingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class ResourceEntityMapperService {

   private static final Logger logger = LoggerFactory.getLogger(ResourceEntityMapperService.class);

   private static Set<String> resourceNativeJavaFields = new HashSet<String>();
   static {
      // TODO rather using Enum, see BSON$RegexFlag
      resourceNativeJavaFields.add(DCResource.KEY_URI);
      resourceNativeJavaFields.add(DCResource.KEY_VERSION);
      resourceNativeJavaFields.add(DCResource.KEY_TYPES);
      // (at top level) computed ones :
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

   /** to know whether expected output is semantic (JSON-LD, RDF) */
   @Autowired
   private CxfJaxrsApiProvider cxfJaxrsApiProvider;
   
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
         if (dcField.isRequired()) {
            throw new ResourceParsingException("required Field value is null");
         }
         // accepting null (for non required fields) if only to allow to remove / clear values
         // TODO rather enforce even non required list & maps to be inited to empty ??
         // defaulting (usually null ; NB. there are only values provided in resources here) :
         ///entityValue = dcField.getDefaultValue(); // NO rather only if value not provided
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
         
      } else if ("map".equals(dcField.getType())) {
         // NB. already tested not to be null
         if (!(resourceValue instanceof Map<?,?>)) {
            throw new ResourceParsingException("map Field value is not a JSON Object : " + resourceValue);
         }
         @SuppressWarnings("unchecked")
         Map<String, Object> dataMap = (Map<String,Object>) resourceValue;
         HashMap<String, Object> entityMap = new HashMap<String,Object>(dataMap.size());
         resourceToEntityFields(dataMap, entityMap, ((DCMapField) dcField).getMapFields(),
               resourceParsingContext, putRatherThanPatchMode, false);
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
               resourceParsingContext.enter(listElementField, resourceItem, i);
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
         
      } else if ("i18n".equals(dcField.getType())) {
         if (!(resourceValue instanceof List<?>)) {
            throw new ResourceParsingException("list Field value is not a JSON Array : " + resourceValue);
         }
         List<?> dataList = (List<?>) resourceValue;
         ArrayList<Object> entityList = new ArrayList<Object>(dataList.size());

         for (Object resourceItem : dataList) {
            if(resourceItem instanceof Map<?, ?>) {
               @SuppressWarnings("unchecked")
               Map<String, Object> mapResourceItem = (Map<String,Object>) resourceItem;
               Object i18nLanguage = mapResourceItem.get(DCResource.KEY_I18N_LANGUAGE); // native DC Resource support
               if (i18nLanguage == null) {
                  i18nLanguage = mapResourceItem.get(DCResource.KEY_I18N_LANGUAGE_JSONLD); // JSON-LD support (required when JSONLD-supported format ex. RDF)
               }
               Object i18nValue = mapResourceItem.get(DCResource.KEY_I18N_VALUE); // native DC Resource support
               if (i18nValue == null) {
                  i18nValue = mapResourceItem.get(DCResource.KEY_I18N_VALUE_JSONLD); // JSON-LD support (required when JSONLD-supported format ex. RDF)
               }

               if (!(i18nLanguage instanceof String)) {
                  throw new ResourceParsingException("i18n Field language is not a JSON string : " + i18nLanguage);
               }
               String i18nEntityLanguage = (String) i18nLanguage;
               if (!(i18nValue instanceof String)) {
                  throw new ResourceParsingException("i18n Field value is not a JSON string : " + i18nValue);
               }
               String i18nEntityValue = (String) i18nValue;
               HashMap<String, String> entityMap = new HashMap<String,String>();
               entityMap.put(DCI18nField.KEY_LANGUAGE, i18nEntityLanguage);
               entityMap.put(DCI18nField.KEY_VALUE, i18nEntityValue);
               entityList.add(entityMap);
            } else {
               resourceParsingContext.addError("Error while parsing i18n list element Field value as map" + resourceItem
                     + " of JSON type " + ((resourceItem == null) ? "null" : resourceItem.getClass()));
               resourceParsingContext.exit();
            }
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

         } else if (resourceValue instanceof DCResource) {
            // (tests only) "framed" (JSONLD) / embedded : embedded subresource (potentially may be (partial) copy)
            DCResource subResource = (DCResource) resourceValue;
            String entityValueUri = subResource.getUri();
            Long entityValueVersion = subResource.getVersion();
            List<String> entityValueTypesFound = subResource.getTypes();
            entityValue = subResourceToEntityFields(entityValueUri, entityValueVersion, entityValueTypesFound,
                  subResource.getProperties(), dcResourceField, resourceParsingContext, putRatherThanPatchMode);
            
         } else if (resourceValue instanceof Map<?,?>) {
            // "framed" (JSONLD) / embedded : embedded subresource (potentially may be (partial) copy)
            boolean normalizeUrlMode = true;
            boolean replaceBaseUrlMode = true;
            boolean allowNew = false;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String,Object>) resourceValue;
            Object entityValueUriFound = dataMap.get(DCResource.KEY_URI);
            if (entityValueUriFound == null) {
               throw new ResourceParsingException("Can't find uri among properties of Object value of "
                     + "resource Field but only " + dataMap);
            } else if (!(entityValueUriFound instanceof String)) {
               throw new ResourceParsingException("Property uri of Object value of "
                     + "resource Field is not a String but " + entityValueUriFound
                     + " (" + entityValueUriFound.getClass() + ")");
            }
            String entityValueUri = (String) entityValueUriFound;
            
            // get version if any, LATER might be used for transaction :
            // TODO parseLong
            Object entityValueVersionFound = dataMap.get(DCResource.KEY_VERSION);
            Long entityValueVersion = null;
            if (entityValueVersionFound instanceof Long) { // tests only ?
               entityValueVersion = (Long) entityValueVersionFound;
            } else if (entityValueVersionFound instanceof Integer) { // TODO refactor to parseInt ?
               entityValueVersion = new Long((Integer) entityValueVersionFound);
            } else if (entityValueVersionFound instanceof String) { // TODO refactor to parseLongFromString ?
               try {
                  // Javascript don't support Long so allow them as String :
                  entityValueVersion = Long.parseLong((String) entityValueVersionFound);
               } catch (NumberFormatException nfex) {
                  resourceParsingContext.addWarning("Embedded resource version is not "
                        + "a long-formatted String but " + entityValueVersion);
               }
            } else if (entityValueVersionFound != null) {
               resourceParsingContext.addWarning("Embedded resource version is not "
                     + "an int, a long or a long-formatted String but " + entityValueVersion);
            } // else handled in subResourceToEntityFields

            // get types if any :
            Object entityValueTypesFound = dataMap.get(DCResource.KEY_TYPES);
            List<String> entityValueTypes = null;
            if (entityValueTypesFound instanceof List<?>) {
               entityValueTypes = (List<String>) entityValueTypesFound;
               if (entityValueTypes.isEmpty()) {
                  // allowed
                  resourceParsingContext.addWarning("No embedded resource types");
               } else {
                  // entityEntityValue.setTypes(entityValueTypes); // TODO or no modelType, or remove modelName ??
               }
            } else if (entityValueTypesFound != null) {
               resourceParsingContext.addWarning("Embedded resource types is not "
                     + "a String list but " + entityValueTypesFound);
            } // else handled in subResourceToEntityFields

            entityValue = subResourceToEntityFields(entityValueUri, entityValueVersion, entityValueTypes,
                  dataMap, dcResourceField, resourceParsingContext, putRatherThanPatchMode);
            
         } else {
            // unknown type for resource value : to help debug model design, add error to context
            throw new ResourceParsingException("Unknown resource Field value "
                  + "(should be string, Map or DCResource) :" + resourceValue);
         }
         
      } else {
         // unknown type IN MODEL : to help debug model design, add error to context
         throw new ResourceParsingException("Unknown Field type " + dcField.getType()
               + ". Supported field types are : TODO"); // TODO better
      }
      
      return entityValue;
   }


   // TODO rm entityValueValue OR impl rights etc. !
   private HashMap<String, Object>/*DCEntity*/ subResourceToEntityFields(String entityValueUri,
         Long entityValueVersion, List<String> entityValueTypes,
         Map<String, Object> properties, DCResourceField dcResourceField,
         DCResourceParsingContext resourceParsingContext, boolean putRatherThanPatchMode)
               throws ResourceParsingException {
      // "framed" (JSONLD) / embedded : embedded subresource (potentially may be (partial) copy)
      boolean normalizeUrlMode = true;
      boolean replaceBaseUrlMode = true;
      boolean allowNew = true; // TODO
      
      Map<String, Object> dataMap = properties;
      if (entityValueUri == null) {
         throw new ResourceParsingException("Can't find uri among properties of Object value of "
               + "resource Field but only " + dataMap);
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
      // 2 cases : 1. embedded referencing / partially copied Resource = Model, 2. fully embedded Resource / typed map = Mixins
      // TODO more mixins / aspects / type constraints
      DCModel valueModel = modelService.getModel(dcUri.getType()); // TODO LATER from cached model ref in DCURI
      // NB. type has been checked in uriService above
      /*if (!dcUri.getType().equals(((DCResourceField) dcField).getResourceType())) {
      //if (!modelService.hasType(refEntity, ((DCResourceField) dcField).getTypeConstraints())) {
         throw new ResourceParsingException("Target resource referenced by resource Field of URI value "
               + dcUri + " is not of required type " + ((DCResourceField) dcField).getResourceType());
         // does not match type constraints TODO
      }*/
      DCModelBase valueModelOrMixin = valueModel;
      DCEntity entityEntityValue = null;
      if (valueModel != null) {
         // embedded referencing Resource
   
         // checking that it exists :
         entityEntityValue = entityService.getByUri(dcUri.toString(), valueModel);
         // TODO TODO
         
      } else {
         // assuming fully embedded Resource, similar to new entity
         // TODO check mixin(s)
         valueModelOrMixin = modelService.getMixin(entityValueTypes.get(0));
         if (valueModelOrMixin == null) {
            throw new ResourceParsingException("Can't find Mixin " + valueModelOrMixin
                  + " nor Model " + dcUri.getType() + " for Embedded resource wth uri "
                  + dcUri.toString());
         }
      }
         
      // TODO TODO provide refEntity in model (ex. DCResourceValue) OR IN GRAPH,
      // ex. for 2nd pass or in case of expanded + embedded return ?!?

      // supporting PUT vs default PATCH-like POST mode :
      if (entityEntityValue == null) {
         // TODO TODOO allow not exists & creation ????!?
         if (!allowNew) {
            throw new ResourceParsingException("Can't find data of resource type " + dcUri.getType()
                  + " referenced by resource Field of Object value with URI property " + dcUri);
         }
         entityEntityValue = new DCEntity();
         entityEntityValue.setUri(dcUri.toString());
         if (valueModel != null) {
            entityEntityValue.setCachedModel(valueModel); // TODO or in DCEntityService ?
            entityEntityValue.setModelName(valueModel.getName()); // TODO LATER2 check that same (otherwise ex. external approvable contrib ??)
         } // else fully embedded subresource
         // NB. no version yet
         
      } else if (putRatherThanPatchMode) {
         entityEntityValue.getProperties().clear();
      } // else reuse existing entity as base : PATCH-like behaviour
      
      // get version if any, LATER might be used for transaction :
      // TODO parseLong
      if (entityValueVersion == null) {
         // allowed (NB. but then can't update ??!!)
         resourceParsingContext.addWarning("No embedded resource version");
      } else if (valueModel != null) {
         entityEntityValue.setVersion(entityValueVersion);
      } // else valueModel == null means fully embedded Resource so version is its container's

      // get types if any :
      if (entityValueTypes == null || entityValueTypes.isEmpty()) {
         // allowed
         resourceParsingContext.addWarning("No embedded resource types");
      } else {
         entityEntityValue.setTypes(entityValueTypes); // TODO or no modelType, or remove modelName ??
      }
      
      // parse other fields :
      // TODO for what use, multiple update or even creation (rather flattened ?) ?? constraints ???
      // TODO do it later to allow all-encompassing transaction ???

      HashMap<String, Object> entityMap = new HashMap<String,Object>(dataMap.size());
      entityMap.put(DCResource.KEY_TYPES, entityValueTypes);
      if (entityValueUri != null) { // OR REQUIRED NOT NULL ??
         entityMap.put(DCResource.KEY_URI, entityValueUri);
      }
      if (entityValueVersion != null) {
         entityMap.put(DCResource.KEY_VERSION, entityValueVersion);
      }
      // TODO also handle other native fields (ex. copy computed ones ex. created) ??
      // NOT FOR NOW hard to copy computed ones after save...
      
      // checking values against expected sub Model :
      resourceToEntityFields(dataMap, entityMap, valueModelOrMixin.getGlobalFieldMap(),
            resourceParsingContext, putRatherThanPatchMode, false);
      return entityMap;
      
      /*resourceToEntityFields(dataMap, entityEntityValue.getProperties(),
            valueModelOrMixin.getGlobalFieldMap(), resourceParsingContext, putRatherThanPatchMode);
      // TODO TODO add entityEntityValue to graph ?! again, for what use :
      // multiple update or even creation (rather flattened ?) ??
      //embeddedEntitiesToAlsoUpdate.add(entityEntityValue);
      // constraints ex. "update only if these fields have these values (in addition to version)" ???
      // cache it in DCURI for ex. rendering returned value ??
      // dcUri.setEntity(entityEntityValue);
      
      /////entityValue = dcUri.toString();
      return entityEntityValue;*/
   }


   /**
    * For now only 404 or 410 are errors, meaning that private or offline resources
    * are only warnings and don't break ex. offline testing.
    * Public method only for testing ; or TODO in UriService or ... ??
    * TODO LATER OPT2 also private resources, with autn/z according to knownWebContainers.
    * @param dcUri
    * @param resourceParsingContext
    * @throws ResourceParsingException containing full uri because it is not fully
    * said in containing resource
    */
   public void checkWebHttpUrl(DCURI dcUri, DCResourceParsingContext resourceParsingContext)
         throws ResourceParsingException {
      URL httpUrl;
      try {
         httpUrl = dcUri.toURI().toURL();
      } catch (MalformedURLException | URISyntaxException ex) {
         // already tested, should not happen
         throw new ResourceParsingException("Unexpected error checking referenced public HTTP URL " + dcUri, ex);
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
                  + "at its URI " + dcUri);
         case 410 :
            throw new ResourceParsingException("Referenced external web resource gone "
                  + "at its URI " + dcUri);
         case 401 :
         case 403 :
            resourceParsingContext.addWarning("Referenced external web resource is private "
                  + "at its URI " + dcUri + ", check not supported yet");
            break;
         }
         if (responseCode % 100 == 5) {
            resourceParsingContext.addWarning("Can't check referenced external web resource URI "
                  + dcUri + "because returns server error " + responseCode);
         } else {
            // including 2 which should not happen
            resourceParsingContext.addWarning("Referenced external web resource URI "
                  + dcUri + "returns unexpected code " + responseCode);
         }
      } catch (IOException ex) {
         // ProtocolException because of "HEAD" so should not happen
         resourceParsingContext.addWarning("Can't check referenced external web resource "
               + dcUri + "because IO error ", ex);
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
         boolean putRatherThanPatchMode, boolean isTopLevel) {
      
      // gathering required fields :
      // TODO DCFields, cache & for mixins
      Set<String> missingRequiredFieldNames = new HashSet<String>();
      Set<DCField> missingDefaultFields = new HashSet<DCField>();
      for (String key : mapFields.keySet()) {
         DCField dcField = mapFields.get(key);
         if (dcField.isRequired()) {
            missingRequiredFieldNames.add(key);
         } else if (dcField.getDefaultValue() != null) {
            missingDefaultFields.add(dcField);
         }
      }
      
      // handling each value :
      for (String key : resourceMap.keySet()) {
         if (!isTopLevel && resourceNativeJavaFields.contains(key)) {
            // skip native fields in subResource case :
            // (they are handled above at top or in subResourceXXX())
            continue;
            
         } else if (!mapFields.containsKey(key)) {
            Object value = resourceMap.get(key);
            resourceParsingContext.addError("Unknown field " + key
                  + "[" + ((value == null) ? "null" : ((value instanceof String) ?
                        "'" + value + "'" : value)) + "]");
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

      for (DCField defaultDcField : missingDefaultFields) { // set default values :
         entityMap.put(defaultDcField.getName(), defaultDcField.getDefaultValue());
      }
      
      if (!missingRequiredFieldNames.isEmpty()) {
         resourceParsingContext.addError("Some Fields are missing : " + missingRequiredFieldNames); 
      }
   }

   
   
   
   /**
    * Special handling :
    * - non-JODA Dates made JODA
    * - if within JSONLD REST Exchange, converts i18n map keys to JSONLD
    * @param entity must have its model cached
    * @return
    */
   public DCResource entityToResource(DCEntity entity) {
      DCResource resource = new DCResource(entityToResourceProps(
            entity.getProperties(), entity.getCachedModel().getGlobalFieldMap()));
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
   
   /**
    * 
    * @param entityProperties
    * @param map
    * @return
    */
   private Map<String, Object> entityToResourceProps(
         Map<String, Object> entityProperties, Map<String, DCField> dcFieldMap) {
      Map<String, Object> resourceProps = new HashMap<String,Object>(
            entityProperties.size()); // (copy because resource != entity)
      // iterating only over existing values (not to provide any others)  :
      for (String fieldName : entityProperties.keySet()) {
         DCField dcField = dcFieldMap.get(fieldName);
         if (dcField == null) {
            // may be a native field, has to be handled above as subresource
            continue;
         }
         Object resourcePropValue = entityToResourceProp(entityProperties.get(fieldName), dcField);
         if (resourcePropValue != null) {
            resourceProps.put(fieldName, resourcePropValue);
         }
      }
      return resourceProps;
   }


   /**
    * Special handling :
    * - non-JODA Dates made JODA
    * - if within JSONLD REST Exchange, converts i18n map keys to JSONLD
    * @param entityPropValue
    * @param dcField
    * @return
    */
   private Object entityToResourceProp(Object entityPropValue, DCField dcField) {
      if (entityPropValue == null) {
         return entityPropValue;
      }
      switch (dcField.getType()) {
      case "date" :
         if (!(entityPropValue instanceof DateTime)) {
            // TODO better in mongo persistence
            entityPropValue = new DateTime((Date) entityPropValue, DateTimeZone.UTC);
            // NB. if not UTC, default timezone has a non-integer time shift
         }
         return entityPropValue;
      case "i18n" :
         Exchange exchange = cxfJaxrsApiProvider.getExchange();
         if (exchange != null // else not called through REST)
               && !MediaType.APPLICATION_JSON.isCompatibleWith(
                     MediaType.valueOf((String) exchange.getInMessage().get(Message.ACCEPT_CONTENT_TYPE)))) {
            // TODO better : all JSONLD-backed content types ex. nquads, see JsonLdJavaRdfProvider
            @SuppressWarnings("unchecked")
            List<Object> entityI18nPropValue = (List<Object>) entityPropValue;
            ArrayList<Object> resourceI18nPropValue = new ArrayList<Object>();
            
            for (Object mapResourceElement : entityI18nPropValue) {
               if(mapResourceElement instanceof Map<?, ?>) {
                  Map<String, String> tempMap = new HashMap<String, String>();
                  
                  @SuppressWarnings("unchecked")
                  Map<String, String> mapResourceItem = (Map<String, String>) mapResourceElement;
                  tempMap.put("@language", mapResourceItem.get("l"));
                  tempMap.put("@value", mapResourceItem.get("v"));
                  resourceI18nPropValue.add(tempMap);
               } else {
                  logger.error("i18n value list element is not a map but " + mapResourceElement);
               }
            }
            entityPropValue = resourceI18nPropValue;
         }
         return entityPropValue;
      case "map" :
         @SuppressWarnings("unchecked")
         Map<String, Object> entityPropMapValue = (Map<String, Object>) entityPropValue;
         return entityToResourceProps(entityPropMapValue,
               ((DCMapField) dcField).getMapFields());
      case "list" :
         DCField dcListElementField = ((DCListField) dcField).getListElementField();
         @SuppressWarnings("unchecked")
         List<Object> entityListPropValue = (List<Object>) entityPropValue;
         ArrayList<Object> resourceListPropValue = new ArrayList<Object>();
         
         for (Object entityListElement : entityListPropValue) {
            resourceListPropValue.add(entityToResourceProp(entityListElement, dcListElementField));
         }
         return resourceListPropValue;
      case "resource" :
         if (entityPropValue instanceof String) {
            return entityPropValue;
         } else if (entityPropValue instanceof Map<?,?>) {
            String resourceType = ((DCResourceField) dcField).getResourceType();
            DCModelBase mixinOrModel = modelService.getMixin(resourceType);
            if (mixinOrModel == null) {
               mixinOrModel = modelService.getModel(resourceType);
            }
            if (mixinOrModel == null) {
               throw new IllegalArgumentException("Can't find mixin or model "
                     + resourceType + " referenced by subresource field "
                     + dcField.getName()); // TODO better log error & report
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> subentityValue = (Map<String, Object>) entityPropValue;
            Map<String, Object> subresourceValue = entityToResourceProps(
                  subentityValue, mixinOrModel.getGlobalFieldMap());
            subresourceValue.put(DCResource.KEY_URI, subentityValue.get(DCResource.KEY_URI)); // TODO DCEntity.KEY_URI... 
            subresourceValue.put(DCResource.KEY_VERSION, subentityValue.get(DCResource.KEY_VERSION)); // TODO DCEntity.KEY_V...
            subresourceValue.put(DCResource.KEY_TYPES, subentityValue.get(DCResource.KEY_TYPES)); // TODO DCEntity.KEY_T...
            // TODO other native fields : changedAt...
            return subresourceValue;
         } else if (entityPropValue instanceof DCEntity) {
            // TODO LATER
         }
      default :
         return entityPropValue;
      }
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
