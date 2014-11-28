package org.oasis.datacore.model.resource;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
import org.oasis.datacore.core.meta.model.DCI18nField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.resource.ResourceException;
import org.oasis.datacore.rest.server.resource.ValueParsingService;
import org.oasis.datacore.sample.ResourceModelIniter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

@Component
public class ModelResourceMappingService {
   
   /////@Value("${datacoreApiClient.containerUrl}") // DOESN'T WORK 
   @Value("${datacoreApiServer.containerUrl}")
   private String containerUrlString = "";
   //@Value("#{new java.net.URI('{datacoreApiServer.containerUrl)}')}")
   @Value("#{uriService.getContainerUrl()}")
   private URI containerUrl = null;
   
   @Autowired
   private DCModelService dataModelService;
   @Autowired
   private ValueParsingService valueService;
   /** TODO rm merge */
   @Autowired
   private ResourceModelIniter mrMappingService1;
   
   
   //////////////////////////////////////////////////////
   // DCModel to Resource (used in ResourceModelIniter) :

   /** public for tests */
   public String buildModelUri(DCResource modelResource) {
      return UriHelper.buildUri(containerUrl, "dcmo:model_0",
            (String) modelResource.get("dcmo:name")/* + '_' + modelResource.get("dcmo:majorVersion")*/); // LATER refactor
   }
   
   /** public for tests */
   public String buildFieldUriPrefix(DCURI dcUri) {
      return new DCURI(dcUri.getContainerUrl(), "dcmf:field_0",
            dcUri.getType() + '/' + dcUri.getId()).toString();
   }

   /**
    * Used by all POVs
    * @param modelType
    * @param name
    * @return
    */
   public String buildNameUri(String modelType, String name) {
      return UriHelper.buildUri(containerUrl, modelType, name);
   }
   /**
    * NOT USED
    * public for tests
    * @param modelResource
    * @return
    * @throws RuntimeException if missing model type, TODO better ex. ResourceParsingException
    */
   public String buildPointOfViewUri(DCResource modelResource) throws RuntimeException {
      List<String> types = modelResource.getTypes();
      if (types == null || types.isEmpty()) {
         throw new RuntimeException("Missing Model type");
      }
      return UriHelper.buildUri(containerUrl, types.get(0),
            (String) modelResource.get("dcmpv:name"));
   }
   
   
   //////////////////////////////////////////////////////
   // Resource to DCModel (used in ModelResourceDCListener) :

   /**
    * Creates DCModel or DCMixin, then calls resourceToModelOrMixin()
    * and modelOrMixinToResource() (to enrich model resource by fields
    * that are computed by DCModelBase).
    * TODO LATER gather all ResourceParsingException in a ParsingContext
    * like in ResourceEntityMappingService 
    * @param r
    * @param isModel
    * @return
    * @throws ResourceException (NOT ResourceParsingException which must be wrapped)
    */
   public DCModelBase toModelOrMixin(DCResource r) throws ResourceException {
      // TODO check non required fields : required queryLimit openelec's maxScan resourceType
      
      // get project :
      //String pointOfViewAbsoluteName = (String) r.get("dcmo:pointOfViewAbsoluteName");
      // NOO rather from context :
      String pointOfViewAbsoluteName = dataModelService.getProject().getAbsoluteName();
      
      
      DCModelBase modelOrMixin;
      String typeName = (String) r.get("dcmo:name");
      modelOrMixin = new DCModel(typeName, pointOfViewAbsoluteName);
      // NB. collectionName is deduced from storage typeName
      modelOrMixin.setMaxScan((int) r.get("dcmo:maxScan"));
      modelOrMixin.setHistorizable((boolean) r.get("dcmo:isHistorizable"));
      modelOrMixin.setContributable((boolean) r.get("dcmo:isContributable"));

      // POLY :
      modelOrMixin.setDefinition((boolean) r.get("dcmo:isDefinition"));
      modelOrMixin.setStorage((boolean) r.get("dcmo:isStorage"));
      modelOrMixin.setInstanciable((boolean) r.get("dcmo:isInstanciable"));

      try {
         this.resourceToFieldsAndMixins(modelOrMixin, r);

         // enrich model resource by fields that are computed by DCModelBase ;
         this.mrMappingService1.modelFieldsAndMixinsToResource(modelOrMixin, r);
         
      } catch (ResourceParsingException rpex) {
         throw new ResourceException("Error while loading DCModel from Resource", rpex, r, dataModelService.getProject());
      }
      // (therefore don't load their caches from r)
      
      return modelOrMixin;
   }
   
   /**
    * 
    * @param modelOrMixin created by TODO
    * @param r
    * @throws ResourceParsingException
    */
   public void resourceToFieldsAndMixins(DCModelBase modelOrMixin, DCResource r) throws ResourceParsingException {
      modelOrMixin.setMajorVersion(valueService.parseLong(r.get("dcmo:majorVersion"), null));
      modelOrMixin.setDocumentation((String) r.get("dcmo:documentation"));
      
      // TODO mixin (computed) security (version)
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> fieldResources = (List<Map<String, Object>>) r.get("dcmo:fields");
      Map<String, DCField> fields;
      try {
         fields = this.propsToFields(fieldResources);
      } catch (ResourceParsingException rpex) {
         throw new ResourceParsingException("Error when parsing fields of model resource "
               + r, rpex);
      }
      
      // NB. since ModelService checks at add() that all of a Model's mixins are already known by it,
      // dcmo:fieldAndMixinNames is enough to know mixins and no need to get the dcmo:mixins prop
      
      // add fields and mixins :
      // TODO TODO mixins rather lazy in DCModelBase !!!!
      @SuppressWarnings("unchecked")
      List<String> fieldAndMixinNames = (List<String>) r.get("dcmo:fieldAndMixins"); // TODO or list of boolean maps ??
      for (String fieldAndMixinName : fieldAndMixinNames) {
         DCField field = fields.get(fieldAndMixinName);
         if (field != null) {
            modelOrMixin.addField(field); // and NOT getFieldMap().put() because more must be done
            continue;
         }
         DCModelBase mixin = dataModelService.getMixin(fieldAndMixinName); // does also model
         if (mixin != null) {
            modelOrMixin.addMixin(mixin);
            continue;
         }
         // TODO TODO logger, in ParsingContext, and even tag error / log on Model Resource as info & state for further handling !!
         throw new ResourceParsingException("Can't find field or mixin "
               + fieldAndMixinName + " when updating DCModelBase from resource " + r);
      }
   }
   
   /** public for tests 
    * @throws ResourceParsingException */
   public DCField propsToField(Map<String, Object> fieldResource) throws ResourceParsingException {
      String fieldName = (String) fieldResource.get("dcmf:name");
      String fieldType = (String) fieldResource.get("dcmf:type");
      boolean fieldRequired = (boolean) fieldResource.get("dcmf:required"); // TODO or default value
      int fieldQueryLimit = (int) fieldResource.get("dcmf:queryLimit"); // TODO or default value
      DCField field;
      switch (fieldType ) {
      case "map" :
         // NB. NOT including DCI18nField's submap which is created auto
         @SuppressWarnings("unchecked")
         List<Map<String, Object>> mapFieldsProps = (List<Map<String, Object>>) fieldResource.get("dcmf:mapFields");
         DCMapField mapField = new DCMapField(fieldName);
         mapField.setMapFields(propsToFields(mapFieldsProps));
         field = mapField;
         break;
      case "list" :
         @SuppressWarnings("unchecked")
         Map<String, Object> listElementFieldProps = (Map<String, Object>) fieldResource.get("dcmf:listElementField");
         field = new DCListField(fieldName, propsToField(listElementFieldProps));
         break;
      case "i18n" :
         field = new DCI18nField(fieldName, fieldQueryLimit);
         break;
      case "resource" :
         String fieldResourceType = (String) fieldResource.get("dcmf:resourceType");
         if (fieldResourceType == null) {
            throw new ResourceParsingException("dcmf:resourceType can't be null on resource-typed field "
                  + fieldResourceType);
         }
         field = new DCResourceField(fieldName, fieldResourceType, fieldRequired, fieldQueryLimit);
         break;
      default :
         field = new DCField(fieldName, fieldType, fieldRequired, fieldQueryLimit);
      }
      
      String defaultStringValue = (String) fieldResource.get("dcmf:defaultStringValue");
      if (defaultStringValue != null) {
         field.setDefaultValue(valueService.parseValueFromJSONOrString(
               DCFieldTypeEnum.getEnumFromStringType(fieldType), defaultStringValue));
      }
      return field;
   }

   /** public for tests 
    * @throws ResourceParsingException */
   public Map<String, DCField> propsToFields(
         List<Map<String, Object>> mapFieldsProps) throws ResourceParsingException {
      Builder<String, DCField> fieldMapBuilder = new ImmutableMap.Builder<String, DCField>();
      for (Map<String, Object> fieldProps : mapFieldsProps) {
         DCField field = propsToField(fieldProps);
         fieldMapBuilder.put(field.getName(), field);
      }
      return fieldMapBuilder.build();
   }
   
}
