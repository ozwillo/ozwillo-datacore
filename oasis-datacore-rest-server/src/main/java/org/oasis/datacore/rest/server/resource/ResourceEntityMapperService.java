package org.oasis.datacore.rest.server.resource;

import java.util.ArrayList;
import java.util.Date;
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
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.server.BadUriException;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.parsing.model.DCResourceParsingContext;
import org.oasis.datacore.rest.server.parsing.service.QueryParsingService;
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
   private ResourceService resourceService;

   /** TODO replace by resourceService to check rights on refs */
   @Autowired
   private DCEntityService entityService;
   
   @Autowired
   private DCModelService modelService;
   
   @Autowired
   private QueryParsingService queryParsingService;
   
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
   public void resourceToEntityFields(Map<String, Object> resourceMap,
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

   
   
   
   public DCResource entityToResource(DCEntity entity) {
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
   
   public List<DCResource> entitiesToResources(List<DCEntity> entities) {
      ArrayList<DCResource> datas = new ArrayList<DCResource>(entities.size());
      for (DCEntity entity : entities) {
         DCResource resource = entityToResource(entity);
         datas.add(resource);
      }
      return datas;
   }
   
}
