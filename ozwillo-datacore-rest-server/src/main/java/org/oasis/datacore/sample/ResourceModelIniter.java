package org.oasis.datacore.sample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.oasis.datacore.common.context.DCRequestContextProvider;
import org.oasis.datacore.common.context.SimpleRequestContextProvider;
import org.oasis.datacore.core.meta.SimpleUriService;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCI18nField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.core.meta.pov.ProjectException;
import org.oasis.datacore.model.event.ModelDCEvent;
import org.oasis.datacore.model.event.ModelDCListener;
import org.oasis.datacore.model.event.ModelResourceDCListener;
import org.oasis.datacore.model.resource.ModelResourceMappingService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.resource.ResourceException;
import org.oasis.datacore.rest.server.resource.ResourceNotFoundException;
import org.oasis.datacore.rest.server.resource.ValueParsingService;
import org.oasis.datacore.sample.meta.ProjectInitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;


/**
 * Creates metamodel (BUT disabled unless overriden in ResourceMetamodelIniter) and fills it,
 * therefore called last after all other (Datacore)Sample(Base)s.
 * 
 * TODO make sets out of DCModel lists, which requires import to use PUT (already done in DCProject)
 * TODO move most to ModelResourceMappingService (& Model(Admin)Service)
 * TODO LATER disable it once mongo becomes reference rather than code-defined DCModel, at least for metamodel
 * @author mdutoo
 *
 */
@Component
public class ResourceModelIniter extends DatacoreSampleBase {

   public static final String MODEL_MODEL_NAME = DCModelService.MODEL_MODEL_NAME;
   public static final String MODEL_NAME_PROP = "dcmo:name";
   public static final String MODEL_MIXIN_NAME = "dcmi:mixin_0";
   public static final String MODEL_FIELD_NAME = "dcmf:field_0";
   public static final String MODEL_SECURITY_NAME = "dcms:Security_0";
   public static final String MODEL_STORAGE_NAME = MODEL_MIXIN_NAME;
   public static final String MODEL_POINTOFVIEW_NAME = "dcmpv:PointOfView_0";
   public static final String POINTOFVIEW_NAME_PROP = "dcmpv:name";
   public static final String MODEL_DATABASEPOINTOFVIEW_NAME = "dcmpvdb:DatabasePointOfView_0";
   public static final String MODEL_PROJECT_NAME = "dcmp:Project_0";
   public static final String MODEL_USECASEPOINTOFVIEW_NAME = "dcmpv:UseCasePointOfView_0";
   public static final String MODEL_USECASEPOINTOFVIEWELEMENT_NAME = "dcmpv:UseCasePointOfViewElement_0";

   // features :
   public static final String MODEL_DISPLAYABLE_NAME = "odisp:Displayable_0";
   public static final String DISPLAYABLE_NAME_PROP = "odisp:name";
   public static final String MODEL_COUNTRYLANGUAGESPECIFIC_NAME = "dcmls:CountryLanguageSpecific_0";
   public static final String COUNTRYLANGUAGESPECIFIC_PROP_NAME = "dcmls:code";
   public static final String MODEL_ANCESTORS_NAME = "oanc:Ancestor_0";
   public static final String ANCESTORS_NAME_PROP = "oanc:ancestors";

   @Autowired
   private ModelResourceMappingService mrMappingService;
   @Autowired
   private ProjectInitService projectInitService;
   @Autowired
   private ValueParsingService valueService;
   
   
   /** after all java-generated models, but before loading other persisted ones */
   @Override
   public int getOrder() {
      return 100000;
   }
   
   /** has its own project */
   @Override
   protected DCProject getProject() {
      return projectInitService.getMetamodelProject();
   }

   // TODO rm, rather in dedicated initer / service 
   @Override
   protected void doInit() {
      super.doInit();
      eventService.init(new ModelResourceDCListener(MODEL_MODEL_NAME)); // TODO or from listeners set in DCModel ??
      eventService.init(new ModelDCListener(ModelDCEvent.MODEL_DEFAULT_BUSINESS_DOMAIN));
      // NB. DON'T enable project reloading yet, rather in LoadPersistedModelsAtInit :
      // (else when persisting hardcoded projects they may be reloaded with additional
      // visibleProjects that will not yet have been loaded from persistence)
   }
   
   /** do neverCleanData */
   @Override
   protected boolean neverCleanData() {
      return true;
   }
   
   /** do refreshBeforePost (since neverCleanData) */
   @Override
   protected boolean refreshBeforePost() {
      return true;
   }
   
   /** do always fill data, so that generated models are repersisted
    * even if some are already there */
   protected boolean alwaysFillData() {
      return true;
   }
   
   /** override it to call internalBuildModels() */
   @Override
   public void buildModels(List<DCModelBase> modelsToCreate) {
      //internalBuildModels(modelsToCreate); // disabled
   }
   
   protected void internalBuildModels(List<DCModelBase> modelsToCreate) {
      // features used by the metamodel :
      
      DCModelBase displayableModel = new DCMixin(MODEL_DISPLAYABLE_NAME) // and not DCModel : fields exist within model & mixins
         .addField(new DCI18nField(DISPLAYABLE_NAME_PROP, 100)) // searchable, especially when is aliasedStorage of ex. geoco:name
      ;
      displayableModel.getField(DISPLAYABLE_NAME_PROP).setFulltext(true); // to ease use ; NB. requires queryLimit > 0

      DCModelBase countryLanguageSpecificModel = new DCMixin(MODEL_COUNTRYLANGUAGESPECIFIC_NAME) // and not DCModel : fields exist within model & mixins
         .addField(new DCField(COUNTRYLANGUAGESPECIFIC_PROP_NAME, "string", false, 0)) // LATER false & mixin optional
      ;
      
      // metamodel :
      
      DCModelBase fieldIdentificationModel = new DCMixin("dcmfid:Identification_0")
         .addField(new DCField("dcmfid:indexInId", "int", false, 0))
         .addField(new DCField("dcmfid:indexInParents", "int", false, 0))
         .addField(new DCListField("dcmfid:queryNames", new DCField("useless", "string", false, 0), false))
         ;
      
      DCModelBase fieldModel = new DCMixin(MODEL_FIELD_NAME) // and not DCModel : fields exist within model & mixins
         .addMixin(displayableModel)
         .addMixin(fieldIdentificationModel)
         .addField(new DCField("dcmf:name", "string", true, 100))
         .addField(new DCField("dcmf:type", "string", true, 100))
         .addField(new DCField("dcmf:required", "boolean", (Object) false, 0)) // defaults to false, indexing would bring not much
         .addField(new DCField("dcmf:queryLimit", "int", 0, 0)) // defaults to 0, indexing would bring not much ??
         .addField(new DCListField("dcmf:aliasedStorageNames", new DCField("useless", "string"))) // defaults to 0, indexing would bring not much ??
         .addField(new DCField("dcmf:readonly", "boolean", false, 0))
         .addField(new DCField("dcmf:fulltext", "boolean", false, 0)) // only on string(& i18n)-typed fields
         // list :
         .addField(new DCResourceField("dcmf:listElementField", MODEL_FIELD_NAME))
         .addField(new DCField("dcmf:isSet", "boolean", false, 0))
         .addField(new DCField("dcmf:keyFieldName", "string", false, 0))
         // map :
         .addField(new DCListField("dcmf:mapFields", new DCResourceField("useless", MODEL_FIELD_NAME)))
         // resource :
         .addField(new DCField("dcmf:resourceType", "string", false, 100)) // "required" would required polymorphism ; TODO rather "resource" type ?!
         
         // TODO for app.js / openelec (NOT required else other samples KO) :
         .addField(new DCField("dcmf:documentation", "string", false, 0))
         .addField(new DCField("dcmf:isInMixinRef", "boolean", false, 0))
         .addField(new DCField("dcmf:defaultStringValue", "string", false, 0))
         .addField(new DCField("dcmf:defaultLanguage", "string", false, 0))
         .addField(new DCField("dcmf:internalName", "string", false, 0)) // not searchable, rather belongs to import conf
      ;
      
      DCModelBase mixinBackwardCompatibilityModel = new DCMixin(MODEL_MIXIN_NAME)
         .addMixin(displayableModel);
      ///mixinBackwardCompatibilityModel.setStorage(true);
      mixinBackwardCompatibilityModel.setInstanciable(false);
      mixinBackwardCompatibilityModel.setStorage(true); // stores all models
      mixinBackwardCompatibilityModel.setInstanciable(false);
      mixinBackwardCompatibilityModel.setMultiProjectStorage(true); // stores for all projects !!

      DCModelBase modelIdentificationModel = new DCMixin("dcmoid:Identification_0")
         .addField(new DCListField("dcmoid:idFieldNames", new DCField("useless", "string", false, 0), false))
         .addField(new DCField("dcmoid:enforceIdFieldNames", "boolean", (Boolean.TRUE), 0)) // setting enforce... to default to true
         .addField(new DCField("dcmoid:queryBeforeCreate", "boolean", false, 0))
         .addField(new DCListField("dcmoid:idGenJs", new DCField("useless", "string", false, 0), false))
         .addField(new DCField("dcmoid:useIdForParent", "boolean", false, 0))
         .addField(new DCListField("dcmoid:parentFieldNames", new DCField("useless", "string", false, 0), false))
         .addField(new DCListField("dcmoid:lookupQueries", new DCMapField("useless")
               .addField(new DCField("dcmoidlq:name", "string", false, 0))
               .addField(new DCListField("dcmoidlq:fieldNames", new DCField("useless", "string", false, 0), false)), false)
         )
         ;

      DCModelBase modelSecurityModel = new DCMixin(MODEL_SECURITY_NAME)
         .addField(new DCField("dcms:isAuthentifiedReadable", "boolean", false, 0))
         .addField(new DCField("dcms:isAuthentifiedCreatable", "boolean", false, 0))
         .addField(new DCField("dcms:isAuthentifiedWritable", "boolean", false, 0))
         .addField(new DCListField("dcms:resourceCreators", new DCField("useless", "string", false, 0), false))
         .addField(new DCListField("dcms:resourceCreationOwners", new DCField("useless", "string", false, 0), false))
         .addField(new DCListField("dcms:resourceReaders", new DCField("useless", "string", false, 0), false))
         .addField(new DCListField("dcms:resourceWriters", new DCField("useless", "string", false, 0), false))
         .addField(new DCListField("dcms:resourceOwners", new DCField("useless", "string", false, 0), false))
         ;
      ((DCListField) modelSecurityModel.getField("dcms:resourceCreators")).setIsSet(true); // merge at restart, rather than override
      ((DCListField) modelSecurityModel.getField("dcms:resourceCreationOwners")).setIsSet(true); // merge at restart, rather than override
      ((DCListField) modelSecurityModel.getField("dcms:resourceReaders")).setIsSet(true); // merge at restart, rather than override
      ((DCListField) modelSecurityModel.getField("dcms:resourceWriters")).setIsSet(true); // merge at restart, rather than override
      ((DCListField) modelSecurityModel.getField("dcms:resourceOwners")).setIsSet(true); // merge at restart, rather than override
      
      // Mixins (or only as names ??) model and at the same time modelBase (or in same collection ?) :
      DCModelBase modelOrMixinModel = new DCMixin(MODEL_MODEL_NAME) // POLY MODEL_MIXIN_NAME // and not DCMixin, they must be introspectable
          // TODO security
         .addMixin(mixinBackwardCompatibilityModel)
         .addMixin(modelIdentificationModel)
         .addMixin(modelSecurityModel)
         .addMixin(countryLanguageSpecificModel)
         //.addMixin(displayableModel);
         .addField(new DCField(MODEL_NAME_PROP, "string", true, 100))
         .addField(new DCField("dcmo:pointOfViewAbsoluteName", "string", false, 100)) // TODO compound index on POV and name
         // NB. NOT required (for now), rather computed from current one, else should be
         // custom computed in ModelResourceDCListener in ABOUT_TO_BUILD step (but then
         // default values are not set by parsing yet which makes toModelOrMixin fail on ex. maxScan)
         .addField(new DCField("dcmo:majorVersion", "long", false, 0)) // don't index and rather lookup on URI
         // NB. Resource version is finer but NOT the minorVersion of the majorVersion
         // NB. NOT required (for now), rather computed from dcmo:name, else should be
         // custom computed in ModelResourceDCListener in ABOUT_TO_BUILD step (but then
         // default values are not set by parsing yet which makes toModelOrMixin fail on ex. maxScan)
         
         // POLY
         .addField(new DCField("dcmo:isDefinition", "boolean", (Object) true, 100)) // = !dcmo:isStorageOnly
         .addField(new DCField("dcmo:isStorage", "boolean", (Object) true, 100))
         .addField(new DCField("dcmo:isInstanciable", "boolean", (Object) true, 100))
         .addField(new DCField("dcmo:isMultiProjectStorage", "boolean", (Object) false, 0))
         
         .addField(new DCField("dcmo:documentation", "string", false, 0)) // TODO LATER required, TODO in another collection for performance
         .addField(new DCListField("dcmo:fields", new DCResourceField("useless", MODEL_FIELD_NAME)))
         .addField(new DCListField("dcmo:mixins", new DCField("useless", "string", false, 100))) // NB. also dcmo:globalMixins
         .addField(new DCListField("dcmo:fieldAndMixins", new DCField("useless", "string", false, 0), false)) // NOT required, only used if any to change override order, so not indexed
         
         .addField(new DCField("dcmo:importDefaultOnly", "boolean", (Object) false, 0))
         .addField(new DCField("dcmo:importAutolinkedOnly", "boolean", (Object) false, 0))
         
         // storage :
         .addField(new DCField("dcmo:maxScan", "int", 0, 0)) // not "required"
         
         // instanciable :
         .addField(new DCField("dcmo:isHistorizable", "boolean", (Object) false, 100))
         .addField(new DCField("dcmo:isContributable", "boolean", (Object) false, 100))
         
         // TODO for app.js / openelec (NOT required) :
         .addField(new DCField("dcmo:idGenJs", "string", false, 0))
         
         // caches :
         .addField(new DCListField("dcmo:globalFields", new DCResourceField("useless", MODEL_FIELD_NAME))) // TODO polymorphism
         .addField(new DCListField("dcmo:globalMixins", new DCField("useless", "string", false, 100)))
         .addField(new DCField("dcmo:definitionModel", "string", false, 100)) // TODO LATER
         .addField(new DCField("dcmo:storageModel", "string", false, 100)) // TODO LATER
         // embedded mixins, globalMixins ???
         // & listeners ??
         ;
      modelOrMixinModel.setStorage(false); // stored in dcmi:mixin_0
      modelOrMixinModel.setInstanciable(true);
      modelOrMixinModel.setDocumentation("id = name + '_' + version"); // TODO LATER rather '/' separator
      /*ignCommuneModel.setDocumentsetDocumentationation("{ \"uri\": \"http://localhost:8180/dc/type/country/France\", "
      + "\"name\": \"France\" }");*/
      
      // TODO prefixes & namespaces, fields ??
      // TODO security, OPT private models ???

      // project model :
      
      DCModelBase pointOfViewModel = new DCModel(MODEL_POINTOFVIEW_NAME)
         .addMixin(displayableModel)
         .addField(new DCField(POINTOFVIEW_NAME_PROP, "string", true, 100))
         .addField(new DCField("dcmpv:majorVersion", "long", false, 0)) // don't index and rather lookup on URI
         .addField(new DCField("dcmpv:unversionedName", "string", false, 0)) // for now rather lookup using regex on mere name
         .addField(new DCField("dcmpv:documentation", "string", false, 100))
         .addField(new DCListField("dcmpv:pointOfViews", new DCResourceField("useless", MODEL_POINTOFVIEW_NAME))) // or not ???
         ///.addField(new DCListField("dcmp:localModels", new DCResourceField("useless", MODEL_MODEL_NAME))) // TODO or rather only dcmo:projectAbsoluteName ?
         ;
      pointOfViewModel.setStorage(true);
      pointOfViewModel.setInstanciable(false); // polymorphic root storage

      DCModelBase databasePointOfViewModel = new DCMixin(MODEL_DATABASEPOINTOFVIEW_NAME)
         .addMixin(pointOfViewModel)
         .addField(new DCField("dcmpvdb:robust", "boolean", false, 0))
         .addField(new DCField("dcmpvdb:uri", "string", false, 0))
         ;
      databasePointOfViewModel.setStorage(false); // store in dcmpv
      databasePointOfViewModel.setInstanciable(false);
      
      DCModelBase projectModel = new DCMixin(MODEL_PROJECT_NAME)
         .addMixin(pointOfViewModel)
         .addMixin(databasePointOfViewModel)
         ///.addField(new DCField("dcmp:name", "string", true, 100))
         ///.addField(new DCField("dcmp:documentation", "string", false, 100))
         .addField(new DCListField("dcmp:localVisibleProjects", new DCResourceField("useless", MODEL_PROJECT_NAME)))
         .addField(new DCListField("dcmp:visibleProjectNames", new DCField("useless", "string", false, 0))) // only to display for now
         .addField(new DCListField("dcmp:forkedUris", new DCField("useless", "string", false, 0))) // only to display for now
         .addField(new DCListField("dcmp:frozenModelNames", new DCField("useless", "string", false, 0)))
         .addField(new DCListField("dcmp:allowedModelPrefixes", new DCField("useless", "string", false, 0)))
         .addField(new DCListField("dcmp:useCasePointOfViews", new DCResourceField("useless", MODEL_PROJECT_NAME)))
         ///.addField(new DCListField("dcmp:localModels", new DCResourceField("useless", MODEL_MODEL_NAME))) // TODO or rather only dcmo:projectAbsoluteName ?
         // security :
         .addField(new DCResourceField("dcmp:securityConstraints", MODEL_SECURITY_NAME, false, 0)) // embedded subresource
         .addField(new DCResourceField("dcmp:securityDefaults", MODEL_SECURITY_NAME, false, 0)) // embedded subresource
         .addField(new DCField("dcmp:modelLevelSecurityEnabled", "boolean", (Object) false, 0))
         .addField(new DCField("dcmp:useModelSecurity", "boolean", (Object) false, 0))
         .addField(new DCResourceField("dcmp:visibleSecurityConstraints", MODEL_SECURITY_NAME, false, 0)) // embedded subresource
         ;
      ((DCListField) projectModel.getField("dcmp:localVisibleProjects")).setIsSet(true); // merge at restart, rather than override
      ((DCListField) projectModel.getField("dcmp:visibleProjectNames")).setIsSet(true); // (not required because computed) merge at restart, rather than override
      ((DCListField) projectModel.getField("dcmp:forkedUris")).setIsSet(true); // merge at restart, rather than override
      ((DCListField) projectModel.getField("dcmp:frozenModelNames")).setIsSet(true); // merge at restart, rather than override
      ((DCListField) projectModel.getField("dcmp:allowedModelPrefixes")).setIsSet(true); // merge at restart, rather than override
      ((DCListField) projectModel.getField("dcmp:useCasePointOfViews")).setIsSet(true); // (not used yet) merge at restart, rather than override
      // TODO LATER also make other DCProject (& DCModel) list fields sets
      projectModel.setStorage(false); // store in dcmpv
      projectModel.setInstanciable(true);

      DCModelBase useCasePointOfViewModel = new DCMixin(MODEL_USECASEPOINTOFVIEW_NAME)
         .addMixin(pointOfViewModel)
         ///.addField(new DCField("dcmp:name", "string", true, 100))
         ///.addField(new DCField("dcmp:documentation", "string", false, 100))
         .addField(new DCListField("dcmp:useCasePointOfViewElements", new DCResourceField("useless", MODEL_USECASEPOINTOFVIEWELEMENT_NAME)))
         ;
      ((DCListField) useCasePointOfViewModel.getField("dcmp:useCasePointOfViewElements")).setIsSet(true); // (not used yet) merge at restart, rather than override
      useCasePointOfViewModel.setStorage(false); // store in dcmpv
      ///useCasePointOfViewModel.setInstanciable(true);

      DCModelBase useCasePointOfViewElementModel = new DCMixin(MODEL_USECASEPOINTOFVIEWELEMENT_NAME)
         .addMixin(pointOfViewModel)
         ///.addField(new DCField("dcmp:name", "string", true, 100))
         ///.addField(new DCField("dcmp:documentation", "string", false, 100))
         ///.addField(new DCField("dcmpvuce:name", "string", true, 100)) // TODO or rather name is Java class ?!
         ///.addField(new DCListField("dcmpvuce:model", new DCResourceField("useless", MODEL_MODEL_NAME))) // TODO or rather only dcmo:projectAbsoluteName ?
         ;
      ///((DCListField) useCasePointOfViewElementModel.getField("dcmpvuce:model")).setIsSet(true); // (not used yet) merge at restart, rather than override
      useCasePointOfViewElementModel.setStorage(false); // store in dcmpv
      useCasePointOfViewElementModel.setInstanciable(true);
      
      // other features :

      DCModelBase ancestorsModel = new DCMixin(MODEL_ANCESTORS_NAME) // and not DCModel : fields exist within model & mixins
         .addField(new DCListField(ANCESTORS_NAME_PROP, new DCResourceField("useless",
               MODEL_ANCESTORS_NAME, false, 100))) // searchable !
      ;
      ///((DCListField) ancestorsModel.getField(ANCESTORS_NAME_PROP)).setIsSet(true); // TODO LATER requires import using PUT
      
      modelsToCreate.addAll(Arrays.asList(displayableModel, countryLanguageSpecificModel,
            fieldIdentificationModel, fieldModel,
            mixinBackwardCompatibilityModel, modelIdentificationModel, modelSecurityModel, modelOrMixinModel,
            pointOfViewModel, databasePointOfViewModel, projectModel, useCasePointOfViewModel, useCasePointOfViewElementModel,
            ancestorsModel));
   }

   @Override
   protected boolean createModels(List<DCModelBase> modelOrMixins, boolean deleteCollectionsFirst) {
      boolean res = super.createModels(modelOrMixins, deleteCollectionsFirst);
      
      // update (override ; i.e. fillData) metamodel resources always, for now :
      updateMetamodelResourcesInProject(modelOrMixins);
      
      // update (default) project resources, always for now :
      createDefaultProjects(); // NB. this will update their persisted Resources
      // in POST / PATCH-like merge mode, therefore keeping additional rights, visibleProjects etc.
      // (though not ostensible changes in security policy)
      allProjectsToResource(false);
      return res;
   }
   
   /** OBSOLETE for now */
   private List<DCProject> projectsNotToPersist = new ArrayList<DCProject>();

   /**
    * Create default projects, including geo & org(pri) for now.
    */
   private void createDefaultProjects() {
      DCProject geo0Project = projectInitService.buildContainerVersionedProjectDefaultConf("geo", 0, // geo_1 // NB. in geo_1.0, 0 would be minorVersion
            "Geographical jurisdictions", null); // "Geographical jurisdictions (" + toPlaygroundLink("geo:Area_0") + ")"
      DCProject geo1Project = projectInitService. buildContainerVersionedProjectDefaultConf("geo", 1, // geo_1 // NB. in geo_1.0, 0 would be minorVersion
            "Geographical jurisdictions", null); // "Geographical jurisdictions (" + toPlaygroundLink("geo:Area_0") + ")"
      DCProject geoProject = projectInitService.buildFacadeProjectDefaultConf(geo1Project);

      /*
      DCProject org0Project = projectInitService.buildContainerVersionedProjectDefaultConf("org", 0,
            "Organizations, public and private, as well as Persons", null, geo0Project);
      org0Project.setModelLevelSecurityEnabled(true);
      org0Project.getSecurityDefaults().setAuthentifiedCreatable(true); // anybody can create an org
      DCProject org1Project = projectInitService.buildContainerVersionedProjectDefaultConf("org", 1,
            "Organizations, public and private, as well as Persons", null, geo1Project);
      org1Project.setModelLevelSecurityEnabled(true);
      org1Project.getSecurityDefaults().setAuthentifiedCreatable(true); // anybody can create an org
      DCProject orgProject = projectInitService.buildFacadeProjectDefaultConf(org1Project);
      */

      DCProject citizenkin0Project = projectInitService.buildContainerVersionedProjectDefaultConf("citizenkin", 0,
            "Citizen Kin procedures", null, geoProject);
      citizenkin0Project.setModelLevelSecurityEnabled(true);
      citizenkin0Project.setUseModelSecurity(true); // the only one this way for now
      citizenkin0Project.getSecurityDefaults().setAuthentifiedCreatable(true); // anybody can create a procedure (?)
      citizenkin0Project.getSecurityDefaults().setResourceCreationOwners(new LinkedHashSet<String>()); // owner is u_user as before BUT THIS SHOULD NOT WORK ??
      // (for both to be used, security on CK models should be voided)
      DCProject citizenkinProject = projectInitService.buildFacadeProjectDefaultConf(citizenkin0Project);
      
      // create if not exist :
      DCProject[] defaultProjects = new DCProject[] { geo0Project, geo1Project, geoProject, // not commented for citizenkin
            ///org0Project, org1Project, orgProject, // could be put back if needed
            citizenkin0Project, citizenkinProject };
      for (DCProject defaultProject : defaultProjects) {
         try {
            resourceService.get(SimpleUriService.buildUri(ResourceModelIniter.MODEL_PROJECT_NAME,
                  defaultProject.getName()), ResourceModelIniter.MODEL_PROJECT_NAME);
            projectsNotToPersist.add(defaultProject); // (set lists would be merged, but this avoids overriding ex. booleans)
            // (though in POST / PATCH-like merge mode, rather than PUT replace mode)
         } catch (ResourceNotFoundException e) {
            // not found, persist
         } catch (ResourceException e) {
            logger.warn("Unkown error trying to get existing default project " + defaultProject.getName());
         }
      }
   }

   /*private String toPlaygroundLink(String modelType) {
      String modelTypeAndQuery = modelType + '?' + 
      return "<a href=\"/dc/type/" + modelTypeAndQuery + "\" class=\"dclink\" onclick=\""
            + "javascript:return findDataByType($(this).attr('href'));\">/dc/type/" + modelType + "</a>";
   }*/

   // TODO in service ?!
   private void updateMetamodelResourcesInProject(List<DCModelBase> modelOrMixins) {
      for (DCModelBase modelToCreate : modelOrMixins) {
         String modelProjectName = modelToCreate.getProjectName();
         if (modelProjectName == null || modelProjectName.equals(modelAdminService.getProject().getName())) {
            // reuse current project
            updateMetamodelResource(modelToCreate);
         } else { // use its own specific project (helper to sample writers)
            new SimpleRequestContextProvider<Object>() { // set context project beforehands :
               // (else Found model ... in wrong project in LoadPersistedModelsAtInit)
               protected Object executeInternal() {
                  updateMetamodelResource(modelToCreate); return null;
               }
            }.execInContext(new ImmutableMap.Builder<String, Object>()
                  .put(DCRequestContextProvider.PROJECT, modelProjectName).build());
         }
      }
   }
   private void updateMetamodelResource(DCModelBase modelToCreate) {
      try {
         // filling model's provided props :
         DCResource metamodelResource = mrMappingService.modelToResource(modelToCreate, null);
         
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

   // TODO implement true diff : resourceService.build(modifiedResource), without created/modified props, with diff lib
   private boolean diff(DCResource metamodelResource, DCResource existingResource) {
      return true;
   }
   // TODO better or https://github.com/SQiShER/java-object-diff/
   /*private List<String> diff(DCResource newResource, DCResource existing, boolean failFast) {
      if (!existing.getUri().equals(newResource.getUri())
            || !existing.getTypes().equals(newResource.getTypes())) {
         // BUT not (native or not) computed fields : version, crAt, crBy, chAt, chBy
         // nor deduced fields : id
         return true;
      }
      return diff(newResource.getProperties(), existing.getProperties(), failFast, null);
   }

   private boolean diff(Map<String, Object> newProps, Map<String, Object> existingProps, String path) {
      if (path == null) {
         path = "";
      }
      HashSet<String> missingNewProps = new HashSet<String>(newProps.keySet());
      for (Entry<String, Object> existingProp : existingProps.entrySet()) {
         
      }
      return false;
   }*/

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
      allProjectsToResource(true);
   }

   private void allProjectsToResource(boolean alsoTheirModels) {
      Set<String> projectsNotToPersistNames = new HashSet<String>(
            projectsNotToPersist.stream().map(p -> p.getName()).collect(Collectors.toSet()));
      Collection<DCProject> allProjects = modelAdminService.getProjects();
      logger.info("Persisting all projects : " + allProjects + " ; save : " + projectsNotToPersistNames);
      projectsToResource(alsoTheirModels, allProjects, projectsNotToPersistNames);
   }
   private void projectsToResource(boolean alsoTheirModels,
         Collection<DCProject> projects, Set<String> projectNameDoneSet) {
      if (projectNameDoneSet == null) {
         projectNameDoneSet = new HashSet<String>(projects.size()); // prevents looping
      }
      LinkedHashSet<String> projectNameBeingDoneSet = new LinkedHashSet<String>(); // detects circular references, ordered
      for (DCProject project : projects) {
         // NB. no project outside those, but they still must be loaded in the order of their deps
         projectAndItsDepsToResource(project, alsoTheirModels, projectNameDoneSet, projectNameBeingDoneSet);
      }
   }

   /**
    * Loads (all modelAdminService known) projects in the order of their deps
    * @param project
    * @param resourcesToPost
    * @param projectNameDoneSet prevents looping
    * @param projectNameBeingDoneSet detects circular references, ordered
    */
   private void projectAndItsDepsToResource(DCProject project, boolean alsoItsModels,
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
      
      // deps (i.e. those that are visible to it) :
      for (DCProject visibleProject : project.getLocalVisibleProjects()) {
         projectAndItsDepsToResource(visibleProject, alsoItsModels,
               projectNameDoneSet, projectNameBeingDoneSet);
      }
      
      // this project & its models :
      // (convert and persist project & its models, after deps)
      List<DCResource> resourcesToPost = new ArrayList<DCResource>();
      
      // 1. its povs (in oasis.main collections) :
      project.getUseCasePointOfViews().forEach(ucPov -> {
         ucPov.getPointOfViews()
            .forEach(povElt -> {
               modelsToResources(povElt.getLocalModels(), resourcesToPost);
            });
         });
      for (DCResource resource : resourcesToPost) {
         postDataInType(resource); // ex. orgpri2 project in oasis.main !!!
      }
      
      // 2. its models (in their own project collections) :
      if (alsoItsModels) {
         new SimpleRequestContextProvider<Object>() { // set context project beforehands :
            protected Object executeInternal() {
               // NB. calling modelsToResources() within project else getStorage/DefinitionModel() wrong
               // NB. mixins should be added before models containing them, checked in addModel
               resourcesToPost.clear();
               modelsToResources(project.getLocalModels(), resourcesToPost);
               
               // actual posting :
               for (DCResource resource : resourcesToPost) {
                  postDataInType(resource); // ex. orgpri2 model in orgpri2 project TODO already exists samples_org2.pri.org2:Organization_0
               }
               return null;
            }
         }.execInContext(new ImmutableMap.Builder<String, Object>()
               .put(DCRequestContextProvider.PROJECT, project.getName()).build());
      }
      
      // 2. this project itself :
      // (after, else can't find its visibleProjects in toProject() ; in oasis.main collections)
      resourcesToPost.clear();
      projectToResource(project, resourcesToPost);
      postDataInType(resourcesToPost.get(0));
      
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
         DCResource modelResource = mrMappingService.modelToResource(model, null);
         
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
