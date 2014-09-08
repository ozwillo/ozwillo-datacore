package org.oasis.datacore.sample;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.model.event.ModelDCEvent;
import org.oasis.datacore.model.event.ModelDCListener;
import org.oasis.datacore.model.event.ModelResourceDCListener;
import org.oasis.datacore.model.resource.ModelResourceMappingService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.resource.ValueParsingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;


/**
 * TODO move most to ModelResourceMappingService (& Model(Admin)Service)
 * @author mdutoo
 *
 */
@Component
public class ResourceModelIniter extends DatacoreSampleBase {

   @Autowired
   private ModelResourceMappingService mrMappingService;
   @Autowired
   private ValueParsingService valueService;
   
   
   /** after all models */
   @Override
   public int getOrder() {
      return 100000;
   }

   // TODO rm, rather in dedicated initer / service 
   @Override
   protected void doInit() {
      super.doInit();
      eventService.init(new ModelResourceDCListener("dcmo:model_0")); // TODO or from listeners set in DCModel ??
      eventService.init(new ModelResourceDCListener("dcmi:mixin_0"));
      eventService.init(new ModelDCListener(ModelDCEvent.MODEL_DEFAULT_BUSINESS_DOMAIN));
   }
   
   @Override
   public void buildModels(List<DCModelBase> modelsToCreate) {
      
      DCModel fieldModel = (DCModel) new DCModel("dcmf:field_0")
         .addField(new DCField("dcmf:name", "string", true, 100))
         .addField(new DCField("dcmf:type", "string", true, 100))
         .addField(new DCField("dcmf:required", "boolean")) // defaults to false, indexing would bring not much
         .addField(new DCField("dcmf:queryLimit", "int")) // defaults to 0, indexing would bring not much
         // list :
         .addField(new DCResourceField("dcmf:listElementField", "dcmf:field_0"))
         // map :
         .addField(new DCListField("dcmf:mapFields", new DCResourceField("useless", "dcmf:field_0")))
         // resource :
         .addField(new DCField("dcmf:resourceType", "string", false, 100)) // "required" would required polymorphism ; TODO rather "resource" type ?!
         
         // TODO for app.js / openelec (NOT required else other samples KO) :
         .addField(new DCField("dcmf:documentation", "string", false, 0))
         .addField(new DCField("dcmf:isInMixinRef", "boolean", false, 0))
         .addField(new DCField("dcmf:indexInId", "int", false, 0))
         .addField(new DCField("dcmf:defaultStringValue", "string", false, 0)) // TODO implement
         .addField(new DCField("dcmf:internalName", "string", false, 100))
      ;

      // Mixins (or only as names ??) model and at the same time modelBase :
      DCModel mixinModel = (DCModel) new DCModel("dcmi:mixin_0")
         .addField(new DCField("dcmo:name", "string", true, 100))
         .addField(new DCField("dcmo:majorVersion", "long", true, 100)) // don't index and rather lookup on URI ??
         // NB. Resource version is finer but NOT the minorVersion of the majorVersion
         .addField(new DCField("dcmo:documentation", "string", false, 100)) // TODO LATER required, TODO in another collection for performance
         .addField(new DCListField("dcmo:fields", new DCResourceField("useless", "dcmf:field_0")))
         .addField(new DCListField("dcmo:mixins", new DCField("useless", "string")))
         .addField(new DCListField("dcmo:fieldAndMixins", new DCField("useless", "string"), true))
         
         // TODO for app.js / openelec (NOT required) :
         .addField(new DCField("dcmo:idGenJs", "string", false, 100))
         
         // caches :
         .addField(new DCListField("dcmo:globalFields", new DCResourceField("useless", "dcmf:field_0"))) // TODO polymorphism
         .addField(new DCListField("dcmo:globalMixins", new DCField("useless", "string")))
         // embedded mixins, globalMixins ???
         // & listeners ??
         ;
      mixinModel.setDocumentation("id = name + '_' + version"); // TODO LATER rathter '/' separator
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
      + "\"name\": \"France\" }");*/
   
      DCModel metaModel = (DCModel) new DCModel("dcmo:model_0")
         // TODO security
         .addMixin(mixinModel)
         .addField(new DCField("dcmo:collectionName", "string", true, 100))
         .addField(new DCField("dcmo:maxScan", "int", 0, 0)) // not "required"
         .addField(new DCField("dcmo:isHistorizable", "boolean", true, 100))
         .addField(new DCField("dcmo:isContributable", "boolean", true, 100))
         ;
      metaModel.setDocumentation("id = name + '_' + version"); // TODO LATER rathter '/' separator
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
      + "\"name\": \"France\" }");*/
      
      // TODO prefixes & namespaces, fields ??
      // TODO security, OPT private models ???
      
      modelsToCreate.addAll(Arrays.asList(fieldModel, (DCModelBase) metaModel, mixinModel));
   }

   /** @obsolete */
   private DCMapField addFieldFields(DCMapField mapField) { // TODO polymorphism
      return addFieldFields(mapField, 3);
   }
   /** @obsolete */
   private DCMapField addFieldFields(DCMapField mapField, int depth) { // TODO polymorphism
      if (depth == 0) {
         return mapField;
      }
      mapField
         .addField(new DCField("dcmf:name", "string", true, 100))
         .addField(new DCField("dcmf:type", "string", true, 100))
         .addField(new DCField("dcmf:required", "boolean")) // defaults to false, indexing would bring not much
         .addField(new DCField("dcmf:queryLimit", "int")) // defaults to 0, indexing would bring not much
         .addField(addFieldFields(new DCMapField("dcmf:listElementField"),
               depth - 1)) // TODO allow (map) field to reuse Mixin to allow trees
         //.addField(addFieldFields(new DCMapField("dcmf:listElementField"),
         //      depth - 1)) // TODO allow (map) field to reuse Mixin to allow trees
         .addField(new DCListField("dcmf:mapFields", addFieldFields(new DCMapField("useless"),
               depth - 1))) // TODO allow map field to reuse Mixin to allow trees
         .addField(new DCField("dcmf:resourceType", "string", false, 100)) // "required" would required polymorphism ; TODO rather "resource" type ?!
      ;
      return mapField;
   }

   
   
   @Override
   public void fillData() {
      List<DCResource> resourcesToPost = new ArrayList<DCResource>();
      
      // TODO TODOOOOOOOOOOOOOOOOOO mixin for common fields between DCModel & DCMixin !!!
      
      for (DCModel model : modelAdminService.getModels()) {
         try {
            // filling model's provided props :
            final DCResource modelResource = modelToResource(model);
            modelOrMixinToResource(model, modelResource);
            
            // once props are complete, schedule post :
            resourcesToPost.add(modelResource);
         } catch (ResourceParsingException e) {
            logger.error("Conversion error building Resource from DCModel " + model, e);
            return; // TODO report errors ex. in list & abort once all are handled ?
         } catch (Throwable t) {
            logger.error("Unkown error building Resource from DCModel " + model, t);
            return; // TODO report errors ex. in list & abort once all are handled ?
         }
      }
      
      
      for (DCModel model : modelAdminService.getModels()) {
         registerMixins(model);
      }

      for (DCModelBase mixinModel : modelAdminService.getMixins()) {
         try {
            // filling model's provided props :
            final DCResource modelResource = mixinToResource(mixinModel);
            modelOrMixinToResource(mixinModel, modelResource);
            
            // once props are complete, schedule post :
            resourcesToPost.add(modelResource);
         } catch (ResourceParsingException e) {
            logger.error("Conversion error building Resource from DCMixin " + mixinModel, e);
            return; // TODO report errors ex. in list & abort once all are handled ?
         } catch (Throwable t) {
            logger.error("Unkown error building Resource from DCMixin " + mixinModel, t);
            return; // TODO report errors ex. in list & abort once all are handled ?
         }
      }


      for (DCResource resource : resourcesToPost) {
         String modelResourceName = (String) resource.get("dcmo:name");
         String[] modelType = modelResourceName.split("_", 2); // TODO better
         String modelName = (modelType.length == 2) ? modelType[0] : modelResourceName;
         String modelVersionIfAny = (modelType.length == 2) ? modelType[1] : null;
         String modelNameWithVersionIfAny = modelResourceName;
         if ("dcmf:field".equals(modelName)
               || "dcmo:model".equals(modelName)
               || "dcmi:mixin".equals(modelName)) {
            // for now non-recursive metaModel can't be posted,
            // LATER do it to document it (what is queriable etc.)
            continue;
         }
         /*datacoreApiClient.*/postDataInType(resource);
      }
   }


   private DCResource modelToResource(DCModel model) throws ResourceParsingException {
      // filling model's provided props :
      DCResource modelResource = DCResource.create(null, "dcmo:model_0")
            .set("dcmo:name", model.getName())
            .set("dcmo:majorVersion", model.getMajorVersion())
            // NB. minor version is Resource version
            .set("dcmo:documentation", model.getDocumentation())// TODO in another collection for performance
            // model only :
            .set("dcmo:collectionName", model.getCollectionName())
            .set("dcmo:maxScan", model.getMaxScan())
            // TODO security
            .set("dcmo:isHistorizable", model.isHistorizable())
            .set("dcmo:isContributable", model.isContributable())
            ;
      //modelResource.setVersion(model.getVersion()); // not at creation (or < 0),
      // rather update DCModel.version from its resource after each put

      // once id source props are complete, build URI out of them :
      String uri = mrMappingService.buildModelUri(modelResource);
      modelResource.setUri(uri);
      return modelResource;
   }

   private DCResource mixinToResource(DCModelBase mixinModel) {
      // filling model's provided props :
      final DCResource modelResource = DCResource.create(null, "dcmi:mixin_0")
            .set("dcmo:name", mixinModel.getName())
            .set("dcmo:majorVersion", mixinModel.getMajorVersion())
            // NB. minor version is Resource version
            .set("dcmo:documentation", mixinModel.getDocumentation())// TODO in another collection for performance
            // TODO security
            ;
      //modelResource.setVersion(model.getVersion()); // not at creation (or < 0),
      // rather update DCModel.version from its resource after each put

      // once id source props are complete, build URI out of them :
      String uri = mrMappingService.buildMixinUri(modelResource);
      modelResource.setUri(uri);
      return modelResource;
   }

   private void modelOrMixinToResource(DCModelBase model, DCResource modelResource) throws ResourceParsingException {
      // still fill other props, including uri-depending ones :
      DCURI dcUri;
      try {
         dcUri = UriHelper.parseUri(modelResource.getUri());
      } catch (MalformedURLException | URISyntaxException e) {
         throw new RuntimeException(e);
      }
      String fieldUriPrefix = mrMappingService.buildFieldUriPrefix(dcUri);
      modelResource.set("dcmo:fields", fieldsToProps(model.getFieldMap(), fieldUriPrefix));
      
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
      modelResource.set("dcmo:globalFields", fieldsToProps(model.getGlobalFieldMap(), fieldUriPrefix));
      
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

   // TODO move to ModelResourceMappingService & modelAdminService.addModel() !
   private void registerMixins(DCModelBase model) {
      for (DCModelBase mixin : model.getMixins()) {
         modelAdminService.addMixin((DCModelBase) mixin);
         registerMixins(mixin);
      }
   }
   
   /** public for tests 
    * @throws ResourceParsingException */
   // TODO move to ModelResourceMappingService & modelAdminService.addModel() !
   public ImmutableList<Object> fieldsToProps(Map<String, DCField> mapFields,
         String fieldUriPrefix) throws ResourceParsingException {
      ImmutableList.Builder<Object> fieldsPropBuilder = DCResource.listBuilder();
      for (String fieldName : mapFields.keySet()) { // NB. ordered
         DCField field = mapFields.get(fieldName);
         fieldsPropBuilder.add(fieldToProps(field, fieldUriPrefix));
      }
      return fieldsPropBuilder.build();
   }
   
   /** public for tests 
    * @throws ResourceParsingException */
   // TODO move to ModelResourceMappingService & modelAdminService.addModel() !
   public ImmutableMap<String, Object> fieldToProps(DCField field,
         String fieldUriPrefix) throws ResourceParsingException {
      // building (dummy ?) field uri :
      //String uri = aboveUri + '/' + field.getName();
      // NO TODO pb uri must be of its own type and not its container's
      String fieldUri = fieldUriPrefix + '/' + field.getName();
      // TODO also for lists : /i
      ImmutableMap.Builder<String, Object> fieldPropBuilder = DCResource.propertiesBuilder()
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
         fieldPropBuilder.put("dcmf:mapFields",
               fieldsToProps(mapField.getMapFields(), fieldUri));
         break;
      case "list" :
      case "i18n" :
         fieldPropBuilder.put("dcmf:listElementField",
               fieldToProps(((DCListField) field).getListElementField(), fieldUri));
         break;
      case "resource" :
         fieldPropBuilder.put("dcmf:resourceType", ((DCResourceField) field).getResourceType());
         // TODO LATER OPT also embedded Resource as map
         break;
      }
      return fieldPropBuilder.build();
   }
   
}
