package org.oasis.datacore.model.resource;

import java.util.List;
import java.util.Map;

import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
import org.oasis.datacore.core.meta.model.DCI18nField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.resource.ValueParsingService;
import org.oasis.datacore.sample.ResourceModelIniter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

@Component
public class ModelResourceMappingService {
   
   /** to be able to build a full uri, to avoid using ResourceService
    * BUT COULD SINCE NOT IN ResourceModelIniter ANYMORE */
   ///@Value("${datacoreApiClient.baseUrl}") 
   ///private String baseUrl; // useless
   /////@Value("${datacoreApiClient.containerUrl}") // DOESN'T WORK 
   @Value("${datacoreApiServer.containerUrl}")
   private String containerUrl = "";

   @Autowired
   private DataModelServiceImpl dataModelService;
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
   public String buildMixinUri(DCResource modelResource) {
      return UriHelper.buildUri(containerUrl, "dcmi:mixin_0",
         (String) modelResource.get("dcmo:name")/* + '_' + modelResource.get("dcmo:majorVersion")*/); // LATER refactor
   }
   
   /** public for tests */
   public String buildFieldUriPrefix(DCURI dcUri) {
      return new DCURI(dcUri.getContainer(), "dcmf:field_0",
            dcUri.getType() + '/' + dcUri.getId()).toString();
   }
   
   
   //////////////////////////////////////////////////////
   // Resource to DCModel (used in ModelResourceDCListener) :

   /**
    * Creates DCModel or DCMixin, then calls resourceToModelOrMixin()
    * and modelOrMixinToResource() (to enrich model resource by fields
    * that are computed by DCModelBase)
    * @param r
    * @param isModel
    * @return
    * @throws ResourceParsingException
    */
   public DCModelBase toModelOrMixin(DCResource r, boolean isModel) throws ResourceParsingException {
      // TODO check non required fields : required queryLimit openelec maxScan resourceType
      DCModelBase modelOrMixin;
      String typeName = (String) r.get("dcmo:name");
      if (isModel) {
         DCModel model = new DCModel(typeName);
         modelOrMixin = model;
         // NB. collectionName is deduced from typeName
         model.setMaxScan((int) r.get("dcmo:maxScan"));
         model.setHistorizable((boolean) r.get("dcmo:isHistorizable"));
         model.setContributable((boolean) r.get("dcmo:isContributable"));
      } else {
         modelOrMixin = new DCMixin(typeName);
      }

      this.resourceToModelOrMixin(modelOrMixin, r);

      // enrich model resource by fields that are computed by DCModelBase ;
      this.mrMappingService1.modelOrMixinToResource(modelOrMixin, r);
      
      return modelOrMixin;
   }
   
   /**
    * 
    * @param modelOrMixin created by TODO
    * @param r
    * @throws ResourceParsingException
    */
   public void resourceToModelOrMixin(DCModelBase modelOrMixin, DCResource r) throws ResourceParsingException {
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
      
      /* NO NEED to get this prop, fieldAndMixinNames is enough IF ALL MIXINS ARE IN ModelService
      @SuppressWarnings("unchecked")
      List<String> mixinNames = (List<String>) r.get("dcmo:mixins");
      modelOrMixin.addMixin(mixin); // TODO TODO lazy withModelAndMixins*/
      
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
         DCModelBase mixin = dataModelService.getMixin(fieldAndMixinName);
         if (mixin == null) {
            mixin = dataModelService.getModel(fieldAndMixinName);
         }
         if (mixin != null) {
            modelOrMixin.addMixin(mixin);
            continue;
         }
         // TODO TODO logger, and even tag error / log on Model Resource as info & state for further handling !!
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
