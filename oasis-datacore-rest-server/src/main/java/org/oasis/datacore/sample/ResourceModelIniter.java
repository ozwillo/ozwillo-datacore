package org.oasis.datacore.sample;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.core.meta.pov.ProjectException;
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


/**
 * TODO move most to ModelResourceMappingService (& Model(Admin)Service)
 * @author mdutoo
 *
 */
@Component
public class ResourceModelIniter extends DatacoreSampleBase {

   public static final String MODEL_MODEL_NAME = "dcmo:model_0";
   public static final String MODEL_MIXIN_NAME = "dcmi:mixin_0";
   public static final String MODEL_STORAGE_NAME = MODEL_MIXIN_NAME;
   public static final String MODEL_POINTOFVIEW_NAME = "dcmpv:pointOfView_0";
   public static final String MODEL_PROJECT_NAME = "dcmp:project_0";
   public static final String MODEL_USECASEPOINTOFVIEW_NAME = "dcmpv:useCasePointOfView_0";
   public static final String MODEL_USECASEPOINTOFVIEWELEMENT_NAME = "dcmpv:useCasePointOfViewElement_0";

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
      eventService.init(new ModelResourceDCListener(MODEL_MODEL_NAME)); // TODO or from listeners set in DCModel ??
      eventService.init(new ModelDCListener(ModelDCEvent.MODEL_DEFAULT_BUSINESS_DOMAIN));
   }
   
   @Override
   public void buildModels(List<DCModelBase> modelsToCreate) {
      DCProject project = modelAdminService.getProject(DCProject.OASIS_MAIN);
      
      DCMixin fieldModel = (DCMixin) new DCMixin("dcmf:field_0", project) // and not DCModel : fields exist within model & mixins
         .addField(new DCField("dcmf:name", "string", true, 100))
         .addField(new DCField("dcmf:type", "string", true, 100))
         .addField(new DCField("dcmf:required", "boolean", (Object) false, 0)) // defaults to false, indexing would bring not much
         .addField(new DCField("dcmf:queryLimit", "int", 0, 0)) // defaults to 0, indexing would bring not much ??
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
         .addField(new DCField("dcmf:defaultStringValue", "string", false, 0))
         .addField(new DCField("dcmf:internalName", "string", false, 100))
      ;
      
      DCModel mixinBackwardCompatibilityModel = new DCModel(MODEL_MIXIN_NAME);
      ///mixinBackwardCompatibilityModel.setStorage(true);
      mixinBackwardCompatibilityModel.setInstanciable(false);

      // Mixins (or only as names ??) model and at the same time modelBase (or in same collection ?) :
      DCModel modelOrMixinModel = (DCModel) new DCModel(MODEL_MODEL_NAME, project) // POLY MODEL_MIXIN_NAME // and not DCMixin, they must be introspectable
          // TODO security
         .addMixin(mixinBackwardCompatibilityModel)
         .addField(new DCField("dcmo:name", "string", true, 100))
         .addField(new DCField("dcmo:pointOfViewAbsoluteName", "string", true, 100)) // TODO compound index on POV and name
         .addField(new DCField("dcmo:majorVersion", "long", true, 100)) // don't index and rather lookup on URI ??
         // NB. Resource version is finer but NOT the minorVersion of the majorVersion
         
         // POLY
         .addField(new DCField("dcmo:isDefinition", "boolean", (Object) true, 100)) // = !dcmo:isStorageOnly
         .addField(new DCField("dcmo:isStorage", "boolean", (Object) true, 100))
         .addField(new DCField("dcmo:isInstanciable", "boolean", (Object) true, 100))
         
         .addField(new DCField("dcmo:documentation", "string", false, 100)) // TODO LATER required, TODO in another collection for performance
         .addField(new DCListField("dcmo:fields", new DCResourceField("useless", "dcmf:field_0")))
         .addField(new DCListField("dcmo:mixins", new DCField("useless", "string", false, 100)))
         .addField(new DCListField("dcmo:fieldAndMixins", new DCField("useless", "string", false, 100), true))
         
         // storage :
         .addField(new DCField("dcmo:maxScan", "int", 0, 0)) // not "required"
         
         // instanciable :
         .addField(new DCField("dcmo:isHistorizable", "boolean", (Object) false, 100))
         .addField(new DCField("dcmo:isContributable", "boolean", (Object) false, 100))
         
         // TODO for app.js / openelec (NOT required) :
         .addField(new DCField("dcmo:idGenJs", "string", false, 100))
         
         // caches :
         .addField(new DCListField("dcmo:globalFields", new DCResourceField("useless", "dcmf:field_0"))) // TODO polymorphism
         .addField(new DCListField("dcmo:globalMixins", new DCField("useless", "string", false, 100)))
         .addField(new DCField("dcmo:definitionModel", "string", false, 100)) // TODO LATER
         .addField(new DCField("dcmo:storageModel", "string", false, 100)) // TODO LATER
         // embedded mixins, globalMixins ???
         // & listeners ??
         ;
      modelOrMixinModel.setStorage(false); // stored in dcmi:mixin_0
      modelOrMixinModel.setDocumentation("id = name + '_' + version"); // TODO LATER rathter '/' separator
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
      + "\"name\": \"France\" }");*/
      
      // TODO prefixes & namespaces, fields ??
      // TODO security, OPT private models ???

      DCModel pointOfViewModel = (DCModel) new DCModel(MODEL_POINTOFVIEW_NAME, project)
         .addField(new DCField("dcmpv:name", "string", true, 100))
         .addField(new DCField("dcmpv:documentation", "string", false, 100))
         .addField(new DCListField("dcmpv:pointOfViews", new DCResourceField("useless", MODEL_POINTOFVIEW_NAME))) // or not ???
         ///.addField(new DCListField("dcmp:localModels", new DCResourceField("useless", MODEL_MODEL_NAME))) // TODO or rather only dcmo:projectAbsoluteName ?
         ;
      pointOfViewModel.setInstanciable(false); // polymorphic root storage

      DCModel projectModel = (DCModel) new DCModel(MODEL_PROJECT_NAME, project)
         .addMixin(pointOfViewModel)
         ///.addField(new DCField("dcmp:name", "string", true, 100))
         ///.addField(new DCField("dcmp:documentation", "string", false, 100))
         .addField(new DCListField("dcmp:localVisibleProjects", new DCResourceField("useless", MODEL_PROJECT_NAME)))
         .addField(new DCListField("dcmp:useCasePointOfViews", new DCResourceField("useless", MODEL_PROJECT_NAME)))
         ///.addField(new DCListField("dcmp:localModels", new DCResourceField("useless", MODEL_MODEL_NAME))) // TODO or rather only dcmo:projectAbsoluteName ?
         ;
      projectModel.setStorage(false); // store in dcmpv

      DCModel useCasePointOfViewModel = (DCModel) new DCModel(MODEL_USECASEPOINTOFVIEW_NAME, project)
         .addMixin(pointOfViewModel)
         ///.addField(new DCField("dcmp:name", "string", true, 100))
         ///.addField(new DCField("dcmp:documentation", "string", false, 100))
         .addField(new DCListField("dcmp:useCasePointOfViewElements", new DCResourceField("useless", MODEL_USECASEPOINTOFVIEWELEMENT_NAME)))
         ;
      useCasePointOfViewModel.setStorage(false); // store in dcmpv

      DCModel useCasePointOfViewElementModel = (DCModel) new DCModel(MODEL_USECASEPOINTOFVIEWELEMENT_NAME, project)
         .addMixin(pointOfViewModel)
         ///.addField(new DCField("dcmp:name", "string", true, 100))
         ///.addField(new DCField("dcmp:documentation", "string", false, 100))
         ///.addField(new DCField("dcmpvuce:name", "string", true, 100)) // TODO or rather name is Java class ?!
         ///.addField(new DCListField("dcmpvuce:model", new DCResourceField("useless", MODEL_MODEL_NAME))) // TODO or rather only dcmo:projectAbsoluteName ?
         ;
      useCasePointOfViewElementModel.setStorage(false); // store in dcmpv
      
      modelsToCreate.addAll(Arrays.asList(fieldModel, mixinBackwardCompatibilityModel, modelOrMixinModel,
            pointOfViewModel, projectModel, useCasePointOfViewModel, useCasePointOfViewElementModel));
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
      
      Set<String> projectNameDoneSet = new HashSet<String>(modelAdminService.getProjects().size()); // prevents looping
      LinkedHashSet<String> projectNameBeingDoneSet = new LinkedHashSet<String>(); // detects circular references, ordered
      for (DCProject project : modelAdminService.getProjects()) {
         projectAndItsModelsToResource(project, resourcesToPost, projectNameDoneSet, projectNameBeingDoneSet);
      }

      //NB. mixins should be added before models containing them, checked in addModel

      for (DCResource resource : resourcesToPost) {
         /*String modelResourceName = (String) resource.get("dcmo:name"); // TODO can also be project with dcmp:name
         String[] modelType = modelResourceName.split("_", 2); // TODO better
         String modelName = (modelType.length == 2) ? modelType[0] : modelResourceName;
         String modelVersionIfAny = (modelType.length == 2) ? modelType[1] : null;
         String modelNameWithVersionIfAny = modelResourceName;*/
         /*datacoreApiClient.*/postDataInType(resource);
      }
   }
   
   /**
    * 
    * @param project
    * @param resourcesToPost
    * @param projectNameDoneSet prevents looping
    * @param projectNameBeingDoneSet detects circular references, ordered
    */
   private void projectAndItsModelsToResource(DCProject project, List<DCResource> resourcesToPost,
         Set<String> projectNameDoneSet, LinkedHashSet<String> projectNameBeingDoneSet) throws ProjectException {
      // prevent looping :
      if (projectNameDoneSet.contains(project.getName())) {
         return; // already done, don't loop
      }
      
      // detects circular references :
      if (projectNameBeingDoneSet.contains(project.getName())) {
         throw new ProjectException(project, "Detected circular reference up to "
               + "this project following the path " + projectNameBeingDoneSet);
      }
      projectNameBeingDoneSet.add(project.getName());
      
      for (DCProject visibleProject : project.getLocalVisibleProjects()) {
         projectAndItsModelsToResource(visibleProject, resourcesToPost,
               projectNameDoneSet, projectNameBeingDoneSet);
      }
      
      // persist project & its models (after those that are visible to it) :
      projectToResource(project, resourcesToPost);
      modelsToResources(project.getLocalModels(), resourcesToPost);
      project.getUseCasePointOfViews().forEach(ucPov -> {
         ucPov.getPointOfViews()
            .forEach(povElt -> {
               modelsToResources(povElt.getLocalModels(), resourcesToPost);
            });
         });
      
      projectNameDoneSet.add(project.getName());
   }
   
   private void projectToResource(DCProject project, List<DCResource> resourcesToPost) {
      try {
         final DCResource projectResource = projectToResource(project);
         resourcesToPost.add(projectResource);
      } catch (ResourceParsingException e) {
         logger.error("Conversion error building Resource from DCProject " + project, e);
         return; // TODO report errors ex. in list & abort once all are handled ?
      } catch (Throwable t) {
         logger.error("Unkown error building Resource from DCProject " + project, t);
         return; // TODO report errors ex. in list & abort once all are handled ?
      }
   }
   /** TODO move to ModelResourceMappingService */
   public DCResource projectToResource(DCProject project) throws ResourceParsingException {
      DCResource projectResource = DCResource.create(null, MODEL_PROJECT_NAME)
            .set("dcmpv:name", project.getName())
         
            // NB. minor version is Resource version
            .set("dcmpv:documentation", project.getDocumentation())// TODO in another collection for performance
         
            // TODO security
            
            // POLY cache : TODO
            ;

      //projectResource.set("dcmpv:pointOfViews", pointOfViewsToNames(project.getLocalVisibleProjects())); // TODO as cache ?!
      projectResource.set("dcmp:localVisibleProjects", project.getLocalVisibleProjects().stream()
            .map(p -> mrMappingService.buildNameUri(MODEL_PROJECT_NAME, p.getName())).collect(Collectors.toList())); // NOT toSet else JSON'd as HashMap
      projectResource.set("dcmp:useCasePointOfViews", project.getUseCasePointOfViews().stream()
            .map(ucpov -> {
               //useCasePointOfViewToResource(ucpov); // TODO TODO & useCasePointOfViewElementToResource(ucpovelt)
               //ucpov.getPointOfViews().stream().map(ucpovelt -> mrMappingService.buildPointOfViewUri(ucpovelt)).collect(Collectors.toList())); // NOT toSet else JSON'd as HashMap
               return mrMappingService.buildNameUri(MODEL_POINTOFVIEW_NAME, ucpov.getName());
            }).collect(Collectors.toList())); // NOT toSet else JSON'd as HashMap

      projectResource.setUri(mrMappingService.buildPointOfViewUri(projectResource));
      return projectResource;
   }
   
   private void modelsToResources(Collection<DCModelBase> models, List<DCResource> resourcesToPost) {
      for (DCModelBase model : models) { // POLY getModels()
         modelToResource(model, resourcesToPost);
      }
   }
   private void modelToResource(DCModelBase model, List<DCResource> resourcesToPost) {
      try {
         // filling model's provided props :
         final DCResource modelResource = modelToResource(model);
         modelFieldsAndMixinsToResource(model, modelResource);
         
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
   
   /** TODO move to ModelResourceMappingService */
   public DCResource modelToResource(DCModelBase model) throws ResourceParsingException {
      DCModelBase definitionModel = modelAdminService.getDefinitionModel(model.getName());
      DCModelBase storageModel = modelAdminService.getStorageModel(model.getName());
      
      // filling model's provided props :
      DCResource modelResource = DCResource.create(null, MODEL_MODEL_NAME)
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
      String uri = mrMappingService.buildModelUri(modelResource);
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
      String fieldUriPrefix = mrMappingService.buildFieldUriPrefix(dcUri);
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
      case "list" :
      case "i18n" :
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
   
}
