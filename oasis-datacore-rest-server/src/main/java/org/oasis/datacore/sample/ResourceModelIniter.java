package org.oasis.datacore.sample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCI18nField;
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
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.resource.ResourceNotFoundException;
import org.oasis.datacore.rest.server.resource.ValueParsingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * TODO move most to ModelResourceMappingService (& Model(Admin)Service)
 * TODO LATER disable it once mongo becomes reference rather than code-defined DCModel, at least for metamodel
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
      
      DCMixin displayableModel = (DCMixin) new DCMixin("o:Displayable_0", project) // and not DCModel : fields exist within model & mixins
         .addField(new DCI18nField("odisp:name", 100))
      ;

      DCModel fieldIdentificationModel = (DCModel) new DCModel("dcmfid:Identification", project)
         .addField(new DCField("dcmfid:indexInId", "int", false, 0))
         .addField(new DCField("dcmfid:indexInParents", "int", false, 0))
         .addField(new DCListField("dcmfid:queryNames", new DCField("useless", "string", false, 0), false))
         ;
      
      DCMixin fieldModel = (DCMixin) new DCMixin("dcmf:field_0", project) // and not DCModel : fields exist within model & mixins
         .addMixin(displayableModel)
         .addMixin(fieldIdentificationModel)
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
         .addField(new DCField("dcmf:defaultStringValue", "string", false, 0))
         .addField(new DCField("dcmf:defaultLanguage", "string", false, 0))
         .addField(new DCField("dcmf:internalName", "string", false, 100))
      ;
      
      DCModel mixinBackwardCompatibilityModel = (DCModel) new DCModel(MODEL_MIXIN_NAME)
         .addMixin(displayableModel);
      ///mixinBackwardCompatibilityModel.setStorage(true);
      mixinBackwardCompatibilityModel.setInstanciable(false);

      DCModel modelIdentificationModel = (DCModel) new DCModel("dcmoid:Identification", project)
         .addField(new DCListField("dcmoid:idFieldNames", new DCField("useless", "string", false, 0), false))
         .addField(new DCListField("dcmoid:idGenJs", new DCField("useless", "string", false, 0), false))
         .addField(new DCListField("dcmoid:parentFieldNames", new DCField("useless", "string", false, 0), false))
         .addField(new DCListField("dcmoid:lookupQueries", new DCMapField("useless")
               .addField(new DCField("dcmoidlq:name", "string", false, 0))
               .addField(new DCListField("dcmoidlq:fieldNames", new DCField("useless", "string", false, 0), false)), false)
         )
         ;
      
      // Mixins (or only as names ??) model and at the same time modelBase (or in same collection ?) :
      DCModel modelOrMixinModel = (DCModel) new DCModel(MODEL_MODEL_NAME, project) // POLY MODEL_MIXIN_NAME // and not DCMixin, they must be introspectable
          // TODO security
         .addMixin(mixinBackwardCompatibilityModel)
         .addMixin(modelIdentificationModel)
         //.addMixin(displayableModel);
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
         .addMixin(displayableModel)
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
      
      modelsToCreate.addAll(Arrays.asList(displayableModel,
            fieldIdentificationModel, fieldModel,
            mixinBackwardCompatibilityModel, modelIdentificationModel, modelOrMixinModel,
            pointOfViewModel, projectModel, useCasePointOfViewModel, useCasePointOfViewElementModel));
   }

   @Override
   protected boolean createModels(List<DCModelBase> modelOrMixins, boolean deleteCollectionsFirst) {
      boolean res = super.createModels(modelOrMixins, deleteCollectionsFirst);
      
      // update (override ; i.e. fillData) metamodel resources always, for now :
      updateMetamodelResources(modelOrMixins);
      return res;
   }

   // TODO in service ?!
   private void updateMetamodelResources(List<DCModelBase> modelOrMixins) {
      for (DCModelBase modelToCreate : modelOrMixins) {
         try {
            // filling model's provided props :
            DCResource metamodelResource = mrMappingService.modelToResource(modelToCreate);
            mrMappingService.modelFieldsAndMixinsToResource(modelToCreate, metamodelResource);
            
            // once props are complete, post or update & put :
            // (only done on servers that store their own models, so no need to use client)
            DCResource existingResource;
            try {
               existingResource = resourceService.get(metamodelResource.getUri(), metamodelResource.getTypes().get(0));
               if (diff(metamodelResource, existingResource)) {
                  // PUT rather than merge and PATCH using POST
                  logger.debug("Persisting metamodel as update " + metamodelResource.getUri());
                  metamodelResource.setVersion(existingResource.getVersion());
                  resourceService.createOrUpdate(metamodelResource, metamodelResource.getTypes().get(0), false, true, true);
               } else {
                  logger.debug("No need to repersist metamodel, no change " + metamodelResource.getUri());
               }
            } catch (ResourceNotFoundException rnfex) {
               logger.debug("Persisting metamodel as new " + metamodelResource.getUri());
               /*datacoreApiClient.*/postDataInType(metamodelResource); // create new
            }
         } catch (ResourceParsingException rpex) {
            ///logger.error("Conversion error building Resource from meta DCModel " + modelToCreate, rpex);
            throw new RuntimeException("Conversion error updating meta DCModel " + modelToCreate, rpex); // TODO report errors ex. in list & abort once all are handled ?
         } catch (Throwable t) {
            ///logger.error("Unkown error building Resource from meta DCModel " + modelToCreate, t);
            throw new RuntimeException("Unknown error updating meta DCModel " + modelToCreate, t); // TODO report errors ex. in list & abort once all are handled ?
         }
      }
   }

   // TODO implement true diff : resourceService.build(modifiedResource), without created/modified props, with diff lib
   private boolean diff(DCResource metamodelResource, DCResource existingResource) {
      return true;
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
         logger.debug("Persisting model " + resource.getUri());
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
         DCResource projectResource = mrMappingService.projectToResource(project);
         resourcesToPost.add(projectResource);
      } catch (ResourceParsingException e) {
         logger.error("Conversion error building Resource from DCProject " + project, e);
         return; // TODO report errors ex. in list & abort once all are handled ?
      } catch (Throwable t) {
         logger.error("Unkown error building Resource from DCProject " + project, t);
         return; // TODO report errors ex. in list & abort once all are handled ?
      }
   }
   
   private void modelsToResources(Collection<DCModelBase> models, List<DCResource> resourcesToPost) {
      for (DCModelBase model : models) { // POLY getModels()
         modelToResource(model, resourcesToPost);
      }
   }
   private void modelToResource(DCModelBase model, List<DCResource> resourcesToPost) {
      try {
         // filling model's provided props :
         DCResource modelResource = mrMappingService.modelToResource(model);
         mrMappingService.modelFieldsAndMixinsToResource(model, modelResource);
         
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
   
}
