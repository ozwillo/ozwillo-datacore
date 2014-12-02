package org.oasis.datacore.model.resource;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
import org.oasis.datacore.core.meta.model.DCI18nField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.core.meta.pov.DCProject;
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

import com.google.common.collect.ImmutableList;
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

   /** TODO move to ModelResourceMappingService */
   public DCResource projectToResource(DCProject project) throws ResourceParsingException {
      DCResource projectResource = DCResource.create(null, ResourceModelIniter.MODEL_PROJECT_NAME)
            .set("dcmpv:name", project.getName())
         
            // NB. minor version is Resource version
            .set("dcmpv:documentation", project.getDocumentation())// TODO in another collection for performance
         
            // TODO security
            
            // POLY cache : TODO
            ;

      //projectResource.set("dcmpv:pointOfViews", pointOfViewsToNames(project.getLocalVisibleProjects())); // TODO as cache ?!
      projectResource.set("dcmp:localVisibleProjects", project.getLocalVisibleProjects().stream()
            .map(p -> this.buildNameUri(ResourceModelIniter.MODEL_PROJECT_NAME, p.getName())).collect(Collectors.toList())); // NOT toSet else JSON'd as HashMap
      projectResource.set("dcmp:useCasePointOfViews", project.getUseCasePointOfViews().stream()
            .map(ucpov -> {
               //useCasePointOfViewToResource(ucpov); // TODO TODO & useCasePointOfViewElementToResource(ucpovelt)
               //ucpov.getPointOfViews().stream().map(ucpovelt -> mrMappingService.buildPointOfViewUri(ucpovelt)).collect(Collectors.toList())); // NOT toSet else JSON'd as HashMap
               return this.buildNameUri(ResourceModelIniter.MODEL_POINTOFVIEW_NAME, ucpov.getName());
            }).collect(Collectors.toList())); // NOT toSet else JSON'd as HashMap

      projectResource.setUri(this.buildPointOfViewUri(projectResource));
      return projectResource;
   }
   
   
   /** TODO move to ModelResourceMappingService */
   public DCResource modelToResource(DCModelBase model) throws ResourceParsingException {
      DCModelBase definitionModel = dataModelService.getDefinitionModel(model.getName());
      DCModelBase storageModel = dataModelService.getStorageModel(model.getName());
      
      // filling model's provided props :
      DCResource modelResource = DCResource.create(null, ResourceModelIniter.MODEL_MODEL_NAME)
            .set("dcmo:name", model.getName())
            .set("dcmo:pointOfViewAbsoluteName", model.getPointOfViewAbsoluteName())
            .set("dcmo:majorVersion", model.getMajorVersion())
            
             // POLY
            .set("dcmo:isDefinition", model.isDefinition()) // = !dcmo:isStorageOnly
            .set("dcmo:isStorage", model.isStorage())
            .set("dcmo:isInstanciable", model.isInstanciable())
         
            // NB. minor version is Resource version
            .set("dcmo:documentation", model.getDocumentation())// TODO in another collection for performance
            
            // storage :
            ///.set("dcmo:collectionName", model.getCollectionName()) // POLY RATHER storageModel.getName()
            .set("dcmo:maxScan", model.getMaxScan())
            
            // TODO security
            
            // instanciable :
            .set("dcmo:isHistorizable", model.isHistorizable())
            .set("dcmo:isContributable", model.isContributable())
            
            // POLY cache :
            .set("dcmo:definitionModel", definitionModel.getName())
            ;
      if (storageModel != null) {
         modelResource.set("dcmo:storageModel", storageModel.getName());
      }
      //modelResource.setVersion(model.getVersion()); // not at creation (or < 0),
      // rather update DCModel.version from its resource after each put

      // once id source props are complete, build URI out of them :
      String uri = this.buildModelUri(modelResource);
      modelResource.setUri(uri);
      return modelResource;
   }

   /** TODO move to ModelResourceMappingService */
   public void modelFieldsAndMixinsToResource(DCModelBase model, DCResource modelResource) throws ResourceParsingException {
      // still fill other props, including uri-depending ones :
      DCURI dcUri;
      try {
         dcUri = UriHelper.parseUri(modelResource.getUri());
      } catch (MalformedURLException | URISyntaxException e) {
         throw new RuntimeException(e);
      }
      String fieldUriPrefix = this.buildFieldUriPrefix(dcUri);
      // allow to enrich existing fields' props :
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> existingFieldsProps = (List<Map<String, Object>>) modelResource.get("dcmo:fields");
      modelResource.set("dcmo:fields", fieldsToProps(model.getFieldMap(),
            fieldUriPrefix, existingFieldsProps));
      
      ImmutableList.Builder<Object> mixinsPropBuilder = DCResource.listBuilder();
      for (DCModelBase mixin : model.getMixins()) {
         mixinsPropBuilder.add(mixin.getName());
      }
      modelResource.set("dcmo:mixins", mixinsPropBuilder.build());
      
      // fieldsAndMixinNames (to allow to rebuild them in DCModel in the right order,
      // and for introspection) :
      ImmutableList.Builder<Object> fieldAndMixinNamesBuilder = DCResource.listBuilder();
      for (Object fieldOrMixin : model.getFieldAndMixins()) {
         if (fieldOrMixin instanceof DCModelBase) { // TODO LATER common DCNamedBase with getName() ?
            fieldAndMixinNamesBuilder.add(((DCModelBase) fieldOrMixin).getName());
         } else { // assuming DCField
            fieldAndMixinNamesBuilder.add(((DCField) fieldOrMixin).getName());
         }
      }
      modelResource.set("dcmo:fieldAndMixins", fieldAndMixinNamesBuilder.build());
      
      // caches (so that they can be queried for introspection as well) :
      modelResource.set("dcmo:globalMixins",
            new ArrayList<String>(model.getGlobalMixinNames())); // TODO order
      modelResource.set("dcmo:globalFields", fieldsToProps(model.getGlobalFieldMap(),
            fieldUriPrefix, null)); // computed, so don't enrich existing props
      
      // filling company's resource props and missing referencing Mixin props : 
      // - by building URI from known id/iri if no missing referencing Mixin prop,
      // - or by looking up referenced resource with another field as criteria
      
      /*
      // NB. single Italia country has to be filled at "install" time
      company.set("!plo:country", UriHelper.buildUri(containerUrl, "!plo:country",
            (String) company.get("!plo:country_name")));

      company.set("!pli:city", UriHelper.buildUri(containerUrl, "!pli:city",
            (String) company.get("!plo:country_name") + '/' + (String) company.get("!pli:city_name")));

      // NB. ateco Model has to be filled at "install" time else code is not known
      List<DCEntity> atecos = ldpEntityQueryService.findDataInType(modelAdminService.getModel("!coita:ateco"), new HashMap<String,List<String>>() {{
               put("!?coita:atecoDescription", new ArrayList<String>() {{ add((String) company.get("!?coita:atecoDescription")); }}); }}, 0, 1);
      DCResource ateco;
      if (atecos != null && !atecos.isEmpty()) {
         ateco = resourceEntityMapperService.entityToResource(atecos.get(0));
      } else {
         ///throw new RuntimeException("Unknown ateco description " + company.get("!?coita:atecoDescription"));
         // WORKAROUND TO WRONG DESCRIPTIONS : (!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!)
         // filling company's provided props :
         ateco = DCResource.create(null, "!coita:ateco")
               .set("!coita:atecoCode", ((String) company.get("!?coita:atecoDescription")).replace(' ', '_'))
               .set("!?coita:atecoDescription", company.get("!?coita:atecoDescription"));
         // once props are complete, build URI out of them and post :
         ateco.setUriFromId(containerUrl, (String) ateco.get("!coita:atecoCode"));
         /datacoreApiClient./postDataInType(ateco);
         //resourcesToPost.add(ateco); // post and NOT schedule else can't be found in loop
      }
      company.set("!coita:atecoCode", ateco.get("!coita:atecoCode"));
      company.set("!coita:ateco", ateco.getUri());
      */

      
      // filling other Models that this table is a source of :
      // TODO mixins ; fields ??
      /*
      try {
         resourceService.get((String) company.get("!pli:city"), "!pli:city");
      } catch (ResourceNotFoundException rnfex) {
         /if (Response.Status.NOT_FOUND.getStatusCode() != waex.getResponse().getStatus()) {
            throw new RuntimeException("Unexpected error", waex.getResponse().getEntity());
         }/
         DCResource city = DCResource.create((String) company.get("!pli:city"))
               .set("!pli:city_name", company.get("!pli:city_name"))
               .set("!plo:country_name", (String) company.get("!plo:country_name"))
               .set("!plo:country", (String) company.get("!plo:country"));
         // once props are complete, build URI out of them and (schedule) post :
         ///city.setUriFromId(containerUrl, (String) company.get("!plo:country_name") + '/' + (String) company.get("!pli:city_name"));
         /datacoreApiClient./postDataInType(city);
         //resourcesToPost.add(city); // BEWARE must be posted before company else resource reference check fails
      }
      */
   }
   
   
   /** public for tests 
    * @throws ResourceParsingException */
   // TODO move to ModelResourceMappingService & modelAdminService.addModel() !
   public ImmutableList<Object> fieldsToProps(Map<String, DCField> mapFields,
         String fieldUriPrefix, List<Map<String, Object>> existingFieldsProps) throws ResourceParsingException {
      ImmutableList.Builder<Object> fieldsPropBuilder = DCResource.listBuilder();
      for (String fieldName : mapFields.keySet()) { // NB. ordered
         DCField field = mapFields.get(fieldName);
         Map<String,Object> existingFieldProps = (existingFieldsProps != null)
               ? findFieldProps(fieldName, existingFieldsProps) : null;
         fieldsPropBuilder.add(fieldToProps(field, fieldUriPrefix, existingFieldProps));
      }
      return fieldsPropBuilder.build();
   }
   
   private Map<String,Object> findFieldProps(String fieldName,
         List<Map<String, Object>> existingFieldsProps) {
      for (Map<String, Object> existingFieldProps : existingFieldsProps) {
         if (fieldName.equals(existingFieldProps.get("dcmf:name"))) {
            return existingFieldProps;
         }
      }
      return null;
   }

   /** public for tests 
    * @param existingProps 
    * @throws ResourceParsingException */
   // TODO move to ModelResourceMappingService & modelAdminService.addModel() !
   public /*Immutable*/Map<String, Object> fieldToProps(DCField field,
         String fieldUriPrefix, Map<String,Object> existingProps) throws ResourceParsingException {
      // building (dummy ?) field uri :
      //String uri = aboveUri + '/' + field.getName();
      // NO TODO pb uri must be of its own type and not its container's
      String fieldUri = fieldUriPrefix + '/' + field.getName();
      // TODO also for lists : /i
      //ImmutableMap.Builder<String, Object> fieldPropBuilder = DCResource.propertiesBuilder();
      DCResource.MapBuilder<String, Object> fieldPropBuilder;
      if (existingProps != null) {
         fieldPropBuilder = new DCResource.MapBuilder<String,Object>(existingProps);
      } else {
         fieldPropBuilder = new DCResource.MapBuilder<String,Object>();
      }
      fieldPropBuilder
            //.put("@id", field.getName().replaceAll(":", "__")
            //      .replaceAll("[!?]", "")) // relative (?), TODO TODO better solution for '!' start
            .put("@id", fieldUri)
            .put("@type", DCResource.listBuilder().add("dcmf:field_0").build()) // OR default one ?
            .put("o:version", 0l) // dummy (OR should be top level's ??)
            .put("dcmf:name", field.getName())
            .put("dcmf:type", field.getType())
            .put("dcmf:required", field.isRequired()) // TODO not for list ; for map ?!?
            .put("dcmf:queryLimit", field.getQueryLimit()); // TODO not for list, map
      String defaultStringValue = valueService.valueToString(field.getDefaultValue()); // TODO or not for list & map ?!?
      if (defaultStringValue != null) {
         fieldPropBuilder.put("dcmf:defaultStringValue", defaultStringValue);
      } // else null means no value, but ImmutableMap.Builder doesn't accept null
      switch(field.getType()) {
      case "map" :
         // NB. including DCI18nField's submap
         DCMapField mapField = (DCMapField) field;
         @SuppressWarnings("unchecked")
         List<Map<String,Object>> existingMapFieldProps = (existingProps != null)
               ? (List<Map<String,Object>>) existingProps.get("dcmf:mapFields") : null;
         fieldPropBuilder.put("dcmf:mapFields",
               fieldsToProps(mapField.getMapFields(), fieldUri, existingMapFieldProps));
         break;
      case "i18n" :
         fieldPropBuilder.put("dcmf:defaultLanguage", ((DCI18nField) field).getDefaultLanguage());
      case "list" : // and also i18n
         @SuppressWarnings("unchecked")
         Map<String,Object> existingListElementFieldProps = (existingProps != null)
            ? (Map<String,Object>) existingProps.get("dcmf:listElementField") : null;
         fieldPropBuilder.put("dcmf:listElementField", fieldToProps(
               ((DCListField) field).getListElementField(), fieldUri, existingListElementFieldProps));
         break;
      case "resource" :
         fieldPropBuilder.put("dcmf:resourceType", ((DCResourceField) field).getResourceType());
         // TODO LATER OPT also embedded Resource as map
         break;
      }
      return fieldPropBuilder.build();
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
         this.modelFieldsAndMixinsToResource(modelOrMixin, r);
         
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
         DCI18nField i18nField = new DCI18nField(fieldName, fieldQueryLimit);
         String defaultLanguage = (String) fieldResource.get("dcmf:defaultLanguage");
         if (defaultLanguage != null) {
            i18nField.setDefaultLanguage(defaultLanguage);
         }
         field = i18nField;
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
