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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.oasis.datacore.core.context.DatacoreRequestContextService;
import org.oasis.datacore.core.entity.EntityModelService;
import org.oasis.datacore.core.entity.EntityService;
import org.oasis.datacore.core.entity.NativeModelService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.model.DCEntityBase;
import org.oasis.datacore.core.entity.query.fulltext.Tokenizer;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
import org.oasis.datacore.core.meta.model.DCI18nField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.parsing.model.DCResourceParsingContext;
import org.oasis.datacore.rest.server.resource.mapping.EmbeddedResourceTypeChecker;
import org.oasis.datacore.rest.server.resource.mapping.ExternalDatacoreLinkedResourceChecker;
import org.oasis.datacore.rest.server.resource.mapping.LocalDatacoreLinkedResourceChecker;
import org.oasis.datacore.server.uri.BadUriException;
import org.oasis.datacore.server.uri.UriService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ResourceEntityMapperService {

   private static final Logger logger = LoggerFactory.getLogger(ResourceEntityMapperService.class);

   /** Checks that linked Resource external web URI returns HTTP 200 (LATER check JSONLD & types ??) */
   @Value("${datacoreApiServer.checkExternalWebUri}")
   private boolean checkExternalWebUri = true;


   @Autowired
   private UriService uriService;

   /** TODO replace by resourceService to check rights on refs */
   @Autowired
   private EntityService entityService;
   
   @Autowired
   private DCModelService modelService;
   
   /** for using model caches of embedded referencing resources */
   @Autowired
   private EntityModelService entityModelService;
   
   @Autowired
   private NativeModelService nativeModelService;
   
   @Autowired
   private ValueParsingService valueParsingService;

   /** for fulltext */
   @Autowired
   public Tokenizer tokenizer;
   
   @Autowired
   private LocalDatacoreLinkedResourceChecker localDatacoreLinkedResourceChecker;
   @Autowired
   private ExternalDatacoreLinkedResourceChecker externalDatacoreLinkedResourceChecker;
   @Autowired
   private EmbeddedResourceTypeChecker embeddedResourceTypeChecker;

   /** to know whether expected output is semantic (JSON-LD, RDF), mixins view, putRatherThanPatchMode */
   @Autowired
   protected DatacoreRequestContextService serverRequestContext;


   /**
    * LATER use request context
    * @param dcI18nField
    * @param dcFieldModel
    * @return never null, at worse en
    */
   public String getDefaultLanguage(DCI18nField dcI18nField, DCModelBase dcFieldModel) {
      String defaultLanguage = null;
      // assume it is translated in the default language...
      // ...according to i18n field :
      if (dcI18nField != null) {
         defaultLanguage = dcI18nField.getDefaultLanguage();
      } // else ex. string field
      // ... else according to its model :
      if (defaultLanguage == null) {
         defaultLanguage = dcFieldModel.getCountryLanguage(); // TODO better model default language ?
      }
      if (defaultLanguage == null) {
         defaultLanguage = DCI18nField.DEFAULT_LANGUAGE; // global default
      }
      // TODO TODO let user change it ? request-scoped (from request context) ?
      return defaultLanguage;
   }
   private List<Map<String,Object>> toSingleLanguageI18n(String value,
         DCI18nField dcI18nField, DCModelBase dcFieldModel) {
      List<Map<String, Object>> i18nMapList = new ArrayList<Map<String,Object>>(2);
      i18nMapList.add(toLanguageI18nMap(getDefaultLanguage(dcI18nField, dcFieldModel), value));
      return i18nMapList;
   }
   private Map<String,Object> toLanguageI18nMap(String language, String value) {
      Map<String,Object> i18nLanguageMap = new HashMap<String,Object>(3, 1);
      i18nLanguageMap.put(DCI18nField.KEY_LANGUAGE, language);
      i18nLanguageMap.put(DCI18nField.KEY_VALUE, value);
      return i18nLanguageMap;
   }
   /** used for alias checking */
   public String getDefaultLanguageValue(DCResource resource, DCI18nField field, DCModelBase model) {
      String defaultLanguage = this.getDefaultLanguage((DCI18nField) field, model);
      String fieldName = field.getName();
      Object value = resource.get(fieldName);
      if (value == null) {
         return null;
      }
      @SuppressWarnings("unchecked")
      List<Map<String, String>> languageToValueMaps = (List<Map<String, String>>) value;
      String defaultValue = null;
      for (Map<String, String> languageToValueMap : languageToValueMaps) {
         if (defaultLanguage.equals(languageToValueMap.get(DCI18nField.KEY_LANGUAGE))) {
            return languageToValueMap.get(DCI18nField.KEY_VALUE);
         }
      }
      return null;
   }
   /** same as ValueParsingService's, but using context (field, global defaults, LATER request) ;
    * especially allows to parse (i18n) according to context
    * (i18n default language, from field or LATER request)*/
   public Object parseValueFromJSONOrString(String resourceValue, DCField dcField,
         DCResourceParsingContext resourceParsingContext) throws ResourceParsingException {
      DCFieldTypeEnum fieldTypeEnum = DCFieldTypeEnum.getEnumFromStringType(dcField.getType());
      Object res;
      try {
         res = valueParsingService.parseValueFromJSONOrString(fieldTypeEnum, resourceValue);
         if (fieldTypeEnum == DCFieldTypeEnum.I18N && res instanceof String) {
            return toSingleLanguageI18n((String) res, (DCI18nField) dcField, resourceParsingContext.peekModel());
         }
         return res;
      } catch (ResourceParsingException rpex) {
         if (fieldTypeEnum == DCFieldTypeEnum.I18N) {
            if (logger.isDebugEnabled()) {
               logger.debug("Failed to parse string as i18n, assuming as "
                     + "single default language value" + rpex.getMessage());
            }
            return toSingleLanguageI18n(resourceValue, (DCI18nField) dcField, resourceParsingContext.peekModel());
         }
         throw rpex;
      }
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
    * @param putRatherThanPatchMode if true, missing fields are allowed
    * (because assumed to be provided in existing data entity)
    * @return entity value ; with fulltext if i18n, without if string
    * @throws ResourceParsingException
    */
   // TODO extract to ResourceBuilder static helper using in ResourceServiceImpl
   private Object resourceToEntityValue(Object resourceValue, Object reusedExistingValue,
         DCField dcField, DCResourceParsingContext resourceParsingContext)
               throws ResourceParsingException {
      
      Object entityValue;
      if (resourceValue == null) {
         if (dcField.isRequired()) {
            throw new ResourceParsingException("required Field value is null");
         }
         // accepting null (for non required fields) if only to allow to remove / clear values
         // TODO rather enforce even non required list & maps to be inited to empty ??
         // defaulting (usually null ; NB. there are only values provided in resources here) :
         ///entityValue = dcField.getDefaultValue(); // NO rather only if value not provided (see below)
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
         entityValue = valueParsingService.parseLong(resourceValue, resourceParsingContext);
         
      } else if ("double".equals(dcField.getType())) {
         // TODOOOOOOOOOOOOOOOOOOOO Javascript has no double : http://javascript.about.com/od/reference/g/rlong.htm
         // so supported through String instead (or Float as fallback)
         entityValue = valueParsingService.parseDouble(resourceValue, resourceParsingContext);
         
      } else if ("date".equals(dcField.getType())) {
         if (!(resourceValue instanceof String)) {
            if (resourceValue instanceof DateTime) { // allow it when ex. in a local call
               entityValue = (DateTime) resourceValue;
            } else {
               throw new ResourceParsingException("date Field value is not a string : " + resourceValue);
            }
         } else {
            entityValue = (DateTime) valueParsingService.parseDateFromString((String) resourceValue);
         }
         
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
         
         Map<String, Object> reusedExistingEntityMap = null;
         if (reusedExistingValue instanceof Map<?,?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reusedExistingEntityMapFound = (Map<String,Object>) reusedExistingValue;
            reusedExistingEntityMap = reusedExistingEntityMapFound;
         } // else existing is overriden
         
         resourceToEntityFields(dataMap, entityMap, reusedExistingEntityMap,
               ((DCMapField) dcField).getMapFields(), resourceParsingContext);
         entityValue = entityMap;
         
      } else if ("list".equals(dcField.getType())) {
         if (!(resourceValue instanceof List<?>)) {
            throw new ResourceParsingException("list Field value is not a JSON Array : " + resourceValue);
         }
         List<?> dataList = (List<?>) resourceValue;
         ArrayList<Object> entityList = new ArrayList<Object>(dataList.size());
         
         DCListField listField = (DCListField) dcField;
         
         // preparing set list merge (if any) :
         LinkedHashMap<String, Object> reusedExistingEntitySetListMap = null;
         if (listField.isSet() && reusedExistingValue instanceof List<?>) { // NB. i18n fields are a set list
            @SuppressWarnings("unchecked")
            List<Object> reusedExistingEntitySetListFound = (List<Object>) reusedExistingValue;
            reusedExistingEntitySetListMap = resourceSetListToMap(reusedExistingEntitySetListFound, listField);
         } // else existing is overriden
         
         DCField listElementField = listField.getListElementField(); // all items are of the same type
         int i = 0;
         for (Object resourceItem : dataList) {
            Object entityItem;
            // set list merge : (if any)
            Object reusedItem = (reusedExistingEntitySetListMap != null) ?
                  reusedExistingEntitySetListMap.remove(getResourceSetListKey(resourceItem, i, listField)) : null;
            try {
               resourceParsingContext.enter(null, null, listElementField, resourceItem, i);
               entityItem = resourceToEntityValue(resourceItem, reusedItem, // list is replaced if provided
                     listElementField, resourceParsingContext);
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
         if (reusedExistingEntitySetListMap != null) {
            entityList.addAll(reusedExistingEntitySetListMap.values()); // add remaining ones if any (and if set list)
         }
         entityValue = entityList;
         
      } else if ("i18n".equals(dcField.getType())) {
         DCI18nField dcI18nField = (DCI18nField) dcField;
         entityValue = buildI18nEntityValue(resourceValue, dcI18nField, resourceParsingContext);
         
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
                  // TODO rather externalDatacoreWebUriChecker ?!
               }
            } else {
               // Datacore URI (local or external)
               
               // checking that it exists :
               if (dcUri.isExternalDatacoreUri()) {
                  externalDatacoreLinkedResourceChecker.check(dcUri, dcResourceField);
                  
               } else {
                  // local Datacore URI
                  if (!resourceParsingContext.hasEmbeddedUri(dcUri)) { // for now only in case of o:Ancestor self reference
                     localDatacoreLinkedResourceChecker.check(dcUri, dcResourceField);
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

            Map<String, Object> reusedExistingEntityResource = null;
            if (reusedExistingValue instanceof Map<?,?>) {
               @SuppressWarnings("unchecked")
               Map<String, Object> reusedExistingEntityResourceFound = (Map<String,Object>) reusedExistingValue;
               reusedExistingEntityResource = reusedExistingEntityResourceFound;
            } // else existing is overriden
            
            entityValue = subResourceToEntityFields(entityValueUri, entityValueVersion, entityValueTypesFound,
                  subResource.getProperties(), reusedExistingEntityResource,
                  dcResourceField, resourceParsingContext);
            
         } else if (resourceValue instanceof Map<?,?>) {
            // "framed" (JSONLD) / embedded : embedded subresource (potentially may be (partial) copy)
            boolean normalizeUrlMode = true;
            boolean replaceBaseUrlMode = true;
            boolean allowNew = false;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String,Object>) resourceValue;
            Object entityValueUriFound = dataMap.get(DCResource.KEY_URI);
            if (entityValueUriFound == null) {
               throw new ResourceParsingException("Can't find " + DCResource.KEY_URI
                     + " among properties of Object value of resource Field but only " + dataMap);
            } else if (!(entityValueUriFound instanceof String)) {
               throw new ResourceParsingException("Property " + DCResource.KEY_URI
                     + " of Object value of resource Field is not a String but "
                     + entityValueUriFound + " (" + entityValueUriFound.getClass() + ")");
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
                  resourceParsingContext.addWarning("Embedded resource " + DCResource.KEY_VERSION
                        + " is not a long-formatted String but " + entityValueVersion);
               }
            } else if (entityValueVersionFound != null) {
               resourceParsingContext.addWarning("Embedded resource " + DCResource.KEY_VERSION
                     + " is not an int, a long or a long-formatted String but " + entityValueVersion);
            } // else handled in subResourceToEntityFields

            // get types if any :
            Object entityValueTypesFound = dataMap.get(DCResource.KEY_TYPES);
            List<String> entityValueTypes = null;
            if (entityValueTypesFound instanceof List<?>) {
               @SuppressWarnings("unchecked")
               List<String> entityValueTypesFoundList = (List<String>) entityValueTypesFound;
               entityValueTypes = entityValueTypesFoundList;
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

            Map<String, Object> reusedExistingEntityResource = null;
            if (reusedExistingValue instanceof Map<?,?>) {
               @SuppressWarnings("unchecked")
               Map<String, Object> reusedExistingEntityResourceFound = (Map<String,Object>) reusedExistingValue;
               reusedExistingEntityResource = reusedExistingEntityResourceFound;
            } // else existing is overriden

            entityValue = subResourceToEntityFields(entityValueUri, entityValueVersion, entityValueTypes,
                  dataMap, reusedExistingEntityResource, dcResourceField, resourceParsingContext);
            
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

   private LinkedHashMap<String, Object> resourceSetListToMap(List<Object> resourceSetList,
         DCListField setListField) throws ResourceParsingException {
      LinkedHashMap<String, Object> res = new LinkedHashMap<String, Object>(resourceSetList.size());
      int i = 0;
      for (Object resourceSetListItem : resourceSetList) {
         String key = getResourceSetListKey(resourceSetListItem, i, setListField);
         res.put(key, resourceSetListItem);
         i++;
      }
      return res;
   }
   
   private String getResourceSetListKey(Object resourceSetListItem,
         int indexInList, DCListField setListField) throws ResourceParsingException {
      String key;
      if (resourceSetListItem instanceof Map<?,?>) {
         String keyFieldName = setListField.getKeyFieldName();
         if (keyFieldName == null) {
            keyFieldName = DCResource.KEY_URI;
         }
         @SuppressWarnings("unchecked")
         Map<String, Object> resourceOrMapItem = (Map<String,Object>) resourceSetListItem;
         key = (String) resourceOrMapItem.get(keyFieldName);
         if (key == null) { // case of mere DCMapField, or missing (non-URI) key field value
            //throw new ResourceParsingException("Can't find " + DCResource.KEY_URI
            //      + " among properties of Object value of set list resource Field but only "
            //      + resourceOrMapItem + " (set list TODO)");
            key = String.valueOf(indexInList); // reverting to index
         }
      } else if (resourceSetListItem instanceof DCResource) {
         key = ((DCResource) resourceSetListItem).getUri();
      } else if (resourceSetListItem instanceof List<?>) {
         throw new ResourceParsingException("set of list "
               + resourceSetListItem + " is not supported");
      } else {
         // TODO check that primitive ?
         key = String.valueOf(resourceSetListItem);
      }
      return key;
   }
   private Object buildI18nEntityValue(Object resourceValue, DCI18nField dcI18nField,
         DCResourceParsingContext resourceParsingContext) throws ResourceParsingException {
      List<Map<String, Object>> entityValue;
      if (resourceValue instanceof String) {
         entityValue = toSingleLanguageI18n((String) resourceValue, dcI18nField, resourceParsingContext.peekModel());
         
      } else {
      
      if (!(resourceValue instanceof List<?>)) {
         throw new ResourceParsingException("list Field value is not a JSON Array : " + resourceValue);
      }
      List<?> dataList = (List<?>) resourceValue;
      LinkedHashMap<String, Map<String, Object>> entityMapMap
            = new LinkedHashMap<String, Map<String, Object>>(dataList.size()); // and not ,String> because also used for fulltext tokens NOO

      // parse each language value, keep last one for each language, accept JSON-LD syntax :
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
            HashMap<String, Object> entityMap = new HashMap<String,Object>(); // and not ,String> because of fulltext
            entityMap.put(DCI18nField.KEY_LANGUAGE, i18nEntityLanguage);
            entityMap.put(DCI18nField.KEY_VALUE, i18nEntityValue);
            ///computeFulltextEntityField(i18nEntityValue, entityMap, dcField); // NOO does not work for multiple field fulltext
            entityMapMap.put(i18nEntityLanguage, entityMap); // keeping last one of each language
         } else {
            resourceParsingContext.addError("Error while parsing i18n list element Field value as map " + resourceItem
                  + " of JSON type " + ((resourceItem == null) ? "null" : resourceItem.getClass()));
         }
      }
      entityValue = new ArrayList<Map<String,Object>>(entityMapMap.values());
      
      }
      return entityValue;
   }
   
   
   // TODO rm entityValueValue OR impl rights etc. !
   private HashMap<String, Object>/*DCEntity*/ subResourceToEntityFields(String entityValueUri,
         Long entityValueVersion, List<String> entityValueTypes,
         Map<String, Object> properties, Map<String, Object> reusedExistingProperties,
         DCResourceField dcResourceField, DCResourceParsingContext resourceParsingContext)
               throws ResourceParsingException {
      // "framed" (JSONLD) / embedded : embedded subresource (potentially may be (partial) copy)
      boolean normalizeUrlMode = true;
      boolean replaceBaseUrlMode = true;
      boolean allowNew = true; // TODO
      
      Map<String, Object> dataMap = properties;
      if (entityValueUri == null) {
         throw new ResourceParsingException("Can't find " + DCResource.KEY_URI
               + " among properties of Object value of resource Field but only " + dataMap);
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

      resourceParsingContext.addEmbeddedUri(dcUri);

      // check type :
      // 2 cases : 1. embedded referencing / partially copied Resource = Model, 2. fully embedded Resource / typed map = Mixins
      // TODO more mixins / aspects / type constraints
      DCModelBase valueModelOrMixin = modelService.getModelBase(dcUri.getType()); // TODO LATER from cached model ref in DCURI
      // TODO rather :
      ///DCModel valueModel = (DCModel) embeddedResourceTypeChecker
      ///      .checkUriModel(dcUri, dcResourceField); // NB. cast checked within
      
      DCEntity entityEntityValue = null;
      if (valueModelOrMixin == null) {
         // TODO LATER OPT client side might deem it a data health / governance problem,
         // and put it in the corresponding inbox
         throw new ResourceParsingException("Can't find Model with type " + dcUri.getType()
               + " for Embedded resource wth uri " + dcUri.toString() + " . Maybe it is badly spelled, "
               + "or it has been deleted or renamed since (only in test). "
               + "In this case, the missing model must first be created again, "
               + "before patching the entity.");
         
      } else if (!valueModelOrMixin.isDefinition()) {
         // NOO ex. if data-level project fork
         //throw new ResourceParsingException("Can't use a non-Definition Model "
         //      + "as embedded resource Model, but such is " + valueModelOrMixin
         //      + " Model used for Embedded resource wth uri " + dcUri.toString());
         
      } else if (valueModelOrMixin.isInstanciable()) {
         // embedded referencing Resource
   
         // checking that it exists :
         entityEntityValue = entityService.getByUri(dcUri.toString(), valueModelOrMixin);
         if (entityEntityValue == null) {
            throw new ResourceParsingException("Can't find Resource with uri " + dcUri.toString()
                  + " referenced by embedded referencing (because its Model " + valueModelOrMixin
                  + " is Instanciable) Resource as embedded resource Model");
         }
         // TODO TODO
         
      } else {
         // assuming fully embedded Resource, similar to new entity
         valueModelOrMixin = modelService.getModelBase(entityValueTypes.get(0)); // ?? NB. does also model
         if (valueModelOrMixin.isInstanciable()) {
            // TODO KO ?
         }
         // ALLOWS EX. /dc/type/city/Paris/townhall embedded resource of mixin type townhall
         // TODO TODO better with polymorphic storage (rather DCModel.getStorageModel() than inherited) & contextual point of view
         // ex. of alias URI : /dc/type/city/FR/Valence#townhall or /dc/type/city/FR-Valence/townhall
         // NB. processing /dc/type/townhall/FR-Valence-townhall URI : get townhall model,
         // is not storage so get storage i.e. city model, in it TODO how to find townhall ??
         // (/dc/type/townhall/FR-Valence#townhall wouldn't be consistent)
         // possible with id building policy & better URI parsing !
         
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
         if (valueModelOrMixin.isInstanciable()) {
            entityModelService.checkAndFillDataEntityCaches(entityEntityValue);
            // NB. accepting even entities with wrong model type (not instanciable, no storage
            // because the model type or his mixins has changed, only in test),
            // so that they may be at least read in order to be patched.
            // However, we don't reassign model to a known one (ex. the storage that has been looked in)
            // otherwise it might break rights (though such wrong models should only happen
            // in sandbox / test / draft / not yet published phase).
            // TODO LATER better : put such cases in data health / governance inbox, through event
            
            ///entityEntityValue.setModelName(valueModel.getName()); // TODO LATER2 check that same (otherwise ex. external approvable contrib ??)
         } // else fully embedded subresource
         // NB. no version yet
         
      } else if (serverRequestContext.getPutRatherThanPatchMode()) {
         entityEntityValue.getProperties().clear();
      } // else reuse existing entity as base : PATCH-like behaviour
      
      // get version if any, LATER might be used for transaction :
      // TODO parseLong
      if (entityValueVersion == null) {
         // allowed (NB. but then can't update ??!!)
         resourceParsingContext.addWarning("No embedded resource version");
      } else if (valueModelOrMixin.isInstanciable()) {
         entityEntityValue.setVersion(entityValueVersion);
      } // else valueModel == null means fully embedded Resource so version is its container's

      // get types if any :
      if (entityValueTypes == null || entityValueTypes.isEmpty()) {
         // allowed
         resourceParsingContext.addWarning("No embedded resource types");
      } else {
         entityEntityValue.setTypes(entityValueTypes); // TODO or no modelType, or remove modelName ??
      }

      // check sub Resource type :
      embeddedResourceTypeChecker.checkTyping(dcUri, dcResourceField, valueModelOrMixin, entityEntityValue.getTypes());
      
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
      resourceToEntityFields(dataMap, entityMap, reusedExistingProperties,
            valueModelOrMixin.getGlobalFieldMap(), resourceParsingContext);
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
    * also handles field alias
    * 
    * @param resourceMap
    * @param entityMap
    * @param mapFields
    * @param resourceParsingContext
    * @param putRatherThanPatchMode TODO or in resourceParsingContext ??
    */
   // TODO extract to ResourceBuilder static helper using in ResourceServiceImpl
   public void resourceToEntityFields(Map<String, Object> resourceMap,
         Map<String, Object> entityMap, Map<String, Object> reusedExistingEntityMap,
         // TODO dcModel.getFieldNames(), abstract DCModel-DCMapField ??
         Map<String, DCField> mapFields, DCResourceParsingContext resourceParsingContext) {
      
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
         if (!resourceParsingContext.isTopLevel()
               && nativeModelService.getNativeFieldNames(null).contains(key)) { // TODO model
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
         missingDefaultFields.remove(dcField); // (and not only if null value, to allow setting null value)
         resourceToEntityField(resourceValue, entityMap,
               reusedExistingEntityMap != null ? reusedExistingEntityMap.get(key) : null,
               dcField, resourceParsingContext);
      }

      for (DCField defaultDcField : missingDefaultFields) { // set default values :
         String missingFieldName = defaultDcField.getName();
         if (reusedExistingEntityMap != null) {
            Object reusedExistingValue = reusedExistingEntityMap.get(missingFieldName);
            if (reusedExistingValue != null) {
               resourceToEntityField(reusedExistingValue, null, // don't add parsed value
                     null, defaultDcField, resourceParsingContext);
               continue; // POST/PATCH (not PUT) mode, will be added at permission checking time  
            }
         }
         entityMap.put(missingFieldName, defaultDcField.getDefaultValue());
      }
      
      if (!missingRequiredFieldNames.isEmpty()) {
         resourceParsingContext.addError("Some Fields are missing : " + missingRequiredFieldNames); 
      }
   }

   
   /**
    * 
    * @param resourceValue
    * @param entityMap if null, parsed value will not be kept / added in it
    * @param object
    * @param dcField
    * @param resourceParsingContext
    */
   private void resourceToEntityField(Object resourceValue,
         Map<String, Object> entityMap, Object reusedExistingEntityValue,
         DCField dcField, DCResourceParsingContext resourceParsingContext) {
      try {
         resourceParsingContext.enter(null, null, dcField, resourceValue);
         Object entityValue = resourceToEntityValue(resourceValue,
               reusedExistingEntityValue, dcField, resourceParsingContext);
         if (entityMap != null && !dcField.isReadonly()) {
            for (String storageName : dcField.getStorageNames()) { // NB. can't be null (but none means not stored i.e. soft computed)
               entityMap.put(storageName, entityValue); // field alias
            }
            computeFulltextEntityField(entityValue, entityMap, dcField, resourceParsingContext);
         } // else not needed ex. reused existing value
      } catch (ResourceParsingException rpex) {
         resourceParsingContext.addError("Error while parsing Field value " + resourceValue
               + " of JSON type " + ((resourceValue == null) ? "null" : resourceValue.getClass()), rpex);
      } catch (Exception ex) {
         // TODO should be HTTP 500 error rather than 400, but this allows to provide useful info
         resourceParsingContext.addError("Unknown error while parsing Field value " + resourceValue
               + " of JSON type " + ((resourceValue == null) ? "null" : resourceValue.getClass()), ex);
      } finally {
         resourceParsingContext.exit();
      }
   }
   
   private void computeFulltextEntityField(Object entityValue, Map<String, Object> entityMap,
         DCField dcField, DCResourceParsingContext resourceParsingContext) throws ResourceParsingException {
      if (dcField.isFulltext()) {
         List<Map<String,Object>> entityI18nValue;
         if ("string".equals(dcField.getType())) {
            entityI18nValue = toSingleLanguageI18n((String) entityValue, null,
                  resourceParsingContext.peekModel()); // (value type already checked)
         } else if ("i18n".equals(dcField.getType())) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tmp = (List<Map<String, Object>>) entityValue; // (value type already checked)
            entityI18nValue = tmp;
         } else {
            throw new ResourceParsingException("Fulltext is only available on string "
                  + "and i18n-typed fields"); // should not happen because checked at model level
         }

         // TODO support & test subresources
         @SuppressWarnings("unchecked")
         List<Map<String,Object>> entityI18nTokens = (List<Map<String,Object>>) entityMap.get(DCField.FULLTEXT_FIELD_NAME); // existing
         // NB. , Object> because both for language and token list
         if (entityI18nTokens == null) {
            entityI18nTokens = new ArrayList<Map<String,Object>>(4);
            entityMap.put(DCField.FULLTEXT_FIELD_NAME, entityI18nTokens);
         }
         for (Map<String,Object> entityI18nLanguageMap : entityI18nValue) {
            String language = (String) entityI18nLanguageMap.get(DCI18nField.KEY_LANGUAGE);
            String entityLanguageValue = (String) entityI18nLanguageMap.get(DCI18nField.KEY_VALUE);
            
            // looking for existing language map :
            Map<String, Object> entityI18nTokensLanguage = null;
            for (Map<String,Object> entityI18nTokensLanguageCur : entityI18nTokens) {
               if (language.equals(entityI18nTokensLanguageCur.get(DCI18nField.KEY_LANGUAGE))) {
                  entityI18nTokensLanguage = entityI18nTokensLanguageCur;
                  break;
               }
            }
            
            List<String> languageTokens = tokenizer.tokenize(entityLanguageValue); // additional
            if (entityI18nTokensLanguage == null) {
               entityI18nTokensLanguage = new HashMap<String, Object>(2, 1);
               entityI18nTokens.add(entityI18nTokensLanguage);
            } else {
               @SuppressWarnings("unchecked")
               List<String> entityLanguageTokens = (List<String>) entityI18nTokensLanguage.get(DCI18nField.KEY_VALUE);
               Set<String> entityLanguageTokensSet = new HashSet<String>(entityLanguageTokens);
               entityLanguageTokensSet.addAll(languageTokens);
               languageTokens = new ArrayList<String>(entityLanguageTokensSet);
            }
            entityI18nTokensLanguage.put(DCI18nField.KEY_LANGUAGE, language);
            entityI18nTokensLanguage.put(DCI18nField.KEY_VALUE, languageTokens);
         }
      }
   }
   /**
    * Special handling :
    * - non-JODA Dates made JODA
    * - if within JSONLD REST Exchange, converts i18n map keys to JSONLD.
    * NB. accepting even entities with wrong model type (not instanciable, no storage
    * because the model type or his mixins has changed, only in test),
    * so that they may be at least read in order to be patched.
    * However, we don't accept missing model (or reassign model to a known, wider one)
    * otherwise it might break rights. In this case, the missing model must first be
    * created again, before patching the entity. Anyway, such wrong models should only happen
    * in sandbox / test / draft / not yet published phase.
    * TODO LATER better : put such cases in data health / governance inbox, through event
    * @param entity must have its model cached
    * @param resource will have its props CLEARED first
    * @param applyView whether to apply view or not, ex. false in getData
    * (already applied by EntityPermissionEvaluator) but true in find()
    * @return
    */
   public DCResource entityToResource(DCEntityBase entity, DCResource resource, boolean applyView) {
      // building resource props :
      Map<String,Object> resourceProps = new HashMap<String,Object>(
            entity.getProperties().size()); // (copy because resource != entity)
      DCModelBase model = entityModelService.getModel(entity);
      LinkedHashSet<String> viewMixinNames = applyView ? serverRequestContext.getViewMixinNames() : null;
      if (viewMixinNames == null
            || viewMixinNames.contains(model.getName())) { // all ; NB. ex. viewMixinNames=sample.city.city
         entityToResourceProps(entity.getProperties(),
               model.getGlobalFieldMap(), resourceProps);
      } else {
         // #71 applying mixins view filtering :
         // TODO LATER (mixin) views also / rather as mongo field projection (and possibly even
         // to enforce "read" rights, though if resource-level rights are enabled entity is required
         // to compute rights, and anyway post filtering in EntityPermissionEvaluator & entityToResource
         // will still be required to apply model/mixin-level rights)
         // http://docs.mongodb.org/manual/tutorial/project-fields-from-query-results/
         // #71 apply mixins view filtering :
         
         for (DCModelBase mixin : model.getMixins()) {
            if (viewMixinNames != null && !viewMixinNames.contains(mixin.getName())) {
               continue;
            }
            // NB. ex. viewMixinNames=geo will show geo:name and not its override in geoci
            entityToResourceProps(entity.getProperties(),
                  mixin.getGlobalFieldMap(), resourceProps); // TODO getFieldMap OR dynamic / available mixins else model type's global fields = all fields
         }
      }
      
      if (resource == null) {
         resource = new DCResource(resourceProps);
      } else {
         resource.setProperties(resourceProps);
      }
      
      resource.setUri(entity.getUri());
      // NB. _id not exposed
      
      boolean isMinimal = viewMixinNames != null && viewMixinNames.isEmpty();
      if (isMinimal) {
         resource.setTypes(null);
      } else {
         resource.setTypes(entity.getTypes()); // TODO or as above, or from model ?
      }

      // NB. the most non-minimal minimal resource view is viewMixinNames=["dc:DublinCore_0"]
      entityToResourcePersistenceComputedFields(entity, resource, isMinimal);
      
      // if viewMixinNames, say that resource is partial :
      if (viewMixinNames != null) {
         resource.set("o:partial", true); // will prevent from POST/PUTting it as is right away
         // TODO better ex. response header, native DCField...
      }
      return resource;
   }
   
   /**
    * ALWAYS APPLIES VIEW (isMinimal)
    * @param entity
    * @param resource
    */
   public void entityToResourcePersistenceComputedFields(DCEntity entity, DCResource resource) {
      boolean applyView = true;
      LinkedHashSet<String> viewMixinNames = applyView ? serverRequestContext.getViewMixinNames() : null;
      boolean isMinimal = viewMixinNames != null && viewMixinNames.isEmpty();
      entityToResourcePersistenceComputedFields(entity, resource, isMinimal);
   }
   
   public void entityToResourcePersistenceComputedFields(DCEntityBase entity,
         DCResource resource, boolean isMinimal) {
      resource.setVersion(entity.getVersion());

      if (!isMinimal) {
         resource.setCreated(entity.getCreated()); // NB. if already provided, only if creation
         resource.setCreatedBy(entity.getCreatedBy()); // NB. if already provided, only if creation
         resource.setLastModified(entity.getLastModified());
         resource.setLastModifiedBy(entity.getLastModifiedBy());
      }
   }
   
   /**
    * Also handles field alias ; used to handle maps & subresources
    * @param entityProperties
    * @param dcFieldMap
    * @param resourceProps must not be null
    */
   private Map<String, Object> entityToResourceProps(
         Map<String, Object> entityProperties, Map<String, DCField> dcFieldMap) {
      Map<String,Object> resourceProps = new HashMap<String,Object>(
            entityProperties.size()); // (copy because resource != entity)
      entityToResourceProps(entityProperties, dcFieldMap, resourceProps);
      return resourceProps;
   }
   /**
    * Also handles field alias ; used at top level
    * @param entityProperties
    * @param dcFieldMap
    * @return new map
    */
   private void entityToResourceProps(
         Map<String, Object> entityProperties, Map<String, DCField> dcFieldMap,
         Map<String, Object> resourceProps) {
      for (DCField field : dcFieldMap.values()) {
         String fieldName = field.getName();
         if (resourceProps.containsKey(fieldName)) {
            continue; // may happen if iterating on each view mixin separately
         }
         String storageReadName = field.getStorageReadName();
         if (storageReadName != null
               && entityProperties.containsKey(storageReadName)) { // only filtered values (not to provide any others)
            Object resourcePropValue = entityToResourceProp(
                  entityProperties.get(storageReadName), field); // field alias
            if (resourcePropValue != null) {
               resourceProps.put(fieldName, resourcePropValue);
            }
         } // else not stored i.e. soft computed
      }
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
         // looking in Request : (Server in or Client Out, context aggregates both with CXF Exchange) 
         String acceptContentType = serverRequestContext.getAcceptContentType();
         if (acceptContentType != null) { // else not called through REST
            int indexOfComma = acceptContentType.indexOf(',');
            if (indexOfComma != -1) {
               // there are actually several, let's only check the first one :
               // (this is an optimization, the actual content type is chosen later in content negociation)
               acceptContentType = acceptContentType.substring(0, indexOfComma);
            }
            if (!MediaType.APPLICATION_JSON_TYPE.isCompatible(MediaType.valueOf(acceptContentType))) {
               // for all formats (including especially JSONLD & RDF ones), convert to JSONLD :
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
            } // else for (native) JSON format only keep l & v
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
            DCModelBase mixinOrModel = modelService.getModelBase(resourceType);
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


   /** see entityToResource() */
   public List<DCResource> entitiesToResources(List<DCEntity> entities) {
      return entitiesToResources(entities, false);
   }
   /** see entityToResource() */
   public List<DCResource> entitiesToResources(List<DCEntity> entities, boolean applyView) {
      ArrayList<DCResource> datas = new ArrayList<DCResource>(entities.size());
      for (DCEntity entity : entities) {
         DCResource resource = entityToResource(entity, null, applyView);
         datas.add(resource);
      }
      return datas;
   }
   
}
