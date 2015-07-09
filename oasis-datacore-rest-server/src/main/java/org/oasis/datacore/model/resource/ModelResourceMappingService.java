package org.oasis.datacore.model.resource;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.oasis.datacore.core.meta.SimpleUriService;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCI18nField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.core.meta.model.DCSecurity;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.parsing.model.DCResourceParsingContext;
import org.oasis.datacore.rest.server.resource.ResourceEntityMapperService;
import org.oasis.datacore.rest.server.resource.ResourceException;
import org.oasis.datacore.rest.server.resource.ValueParsingService;
import org.oasis.datacore.sample.ResourceModelIniter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

@Component
public class ModelResourceMappingService {
   
   @Autowired
   private DCModelService dataModelService;
   @Autowired
   private ValueParsingService valueService;
   /** to parse i18n default value according to default language
    * LATER use it in the whole model mapping process ? */
   @Autowired
   private ResourceEntityMapperService resourceEntityMapperService;
   
   
   //////////////////////////////////////////////////////
   // DCModel to Resource (used in ResourceModelIniter) :

   /** public for tests */
   public String buildModelUri(DCResource modelResource) {
      return SimpleUriService.buildModelUri(
            (String) modelResource.get("dcmo:name")/* + '_' + modelResource.get("dcmo:majorVersion")*/); // LATER refactor
   }
   /** only for tests */
   public String buildModelUri(DCModelBase model) {
      return SimpleUriService.buildModelUri(
            (String) model.getName()/* + '_' + model.getMajorVersion()*/); // LATER refactor
   }
   
   /** public for tests */
   public String buildFieldUriPrefix(DCURI dcUri) {
      return SimpleUriService.buildUri(ResourceModelIniter.MODEL_FIELD_NAME,
            dcUri.getType() + '/' + dcUri.getId()).toString();
   }

   /**
    * Used by all POVs
    * @param modelType
    * @param name
    * @return
    */
   public String buildNameUri(String modelType, String name) {
      return SimpleUriService.buildUri(modelType, name);
   }
   
   /**
    * NOT USED
    * public for tests
    * @param projectResource
    * @return
    * @throws RuntimeException if missing model type, TODO better ex. ResourceParsingException
    */
   public String buildPointOfViewUri(DCResource projectResource) throws RuntimeException {
      List<String> types = projectResource.getTypes();
      if (types == null || types.isEmpty()) {
         throw new RuntimeException("Missing Model type");
      }
      return SimpleUriService.buildUri(types.get(0),
            (String) projectResource.get(ResourceModelIniter.POINTOFVIEW_NAME_PROP));
   }

   /**
    * 
    * @param project
    * @return
    * @throws ResourceParsingException
    */
   public DCResource projectToResource(DCProject project) throws ResourceParsingException {
      DCResource projectResource = DCResource.create(null, ResourceModelIniter.MODEL_PROJECT_NAME);
      return projectToResource(project, projectResource);
   }
   public DCResource projectToResource(DCProject project, DCResource projectResource) throws ResourceParsingException {
      projectResource
            .set("dcmpv:name", project.getName())
            .set("dcmpv:majorVersion", project.getMajorVersion())
            .set("dcmpv:unversionedName", project.getUnversionedName())
            // NB. NOT o:version which is only in DCProject for info purpose
         
            // NB. minor version is Resource version
            .set("dcmpv:documentation", project.getDocumentation())// TODO in another collection for performance
            
            // POLY cache : TODO
            ;
      
      String uri = this.buildPointOfViewUri(projectResource);

      //projectResource.set("dcmpv:pointOfViews", pointOfViewsToNames(project.getLocalVisibleProjects())); // TODO as cache ?!
      projectResource.set("dcmp:localVisibleProjects", project.getLocalVisibleProjects().stream()
            .map(p -> this.buildNameUri(ResourceModelIniter.MODEL_PROJECT_NAME, p.getName())).collect(Collectors.toList())); // NOT toSet else JSON'd as HashMap
      projectResource.set("dcmp:visibleProjectNames", new ArrayList<String>(
            dataModelService.getVisibleProjectNames(project.getName()))); // only to display for now
      projectResource.set("dcmp:forkedUris", new ArrayList<String>(
            project.getForkedUris())); // only to display for now
      projectResource.set("dcmp:frozenModelNames", new ArrayList<String>(
            project.getFrozenModelNames()));
      projectResource.set("dcmp:useCasePointOfViews", project.getUseCasePointOfViews().stream()
            .map(ucpov -> {
               //useCasePointOfViewToResource(ucpov); // TODO TODO & useCasePointOfViewElementToResource(ucpovelt)
               //ucpov.getPointOfViews().stream().map(ucpovelt -> mrMappingService.buildPointOfViewUri(ucpovelt)).collect(Collectors.toList())); // NOT toSet else JSON'd as HashMap
               return this.buildNameUri(ResourceModelIniter.MODEL_POINTOFVIEW_NAME, ucpov.getName());
            }).collect(Collectors.toList())); // NOT toSet else JSON'd as HashMap
      if (project.getSecurityConstraints() != null) {
         projectResource.set("dcmp:securityConstraints", securityToResource(
               project.getSecurityConstraints(), uri, "dcmp:securityConstraints"));
      }
      if (project.getSecurityDefaults() != null) {
         projectResource.set("dcmp:securityDefaults", securityToResource(
               project.getSecurityDefaults(), uri, "dcmp:securityDefaults"));
      }
      projectResource.set("dcmp:modelLevelSecurityEnabled", project.isModelLevelSecurityEnabled());
      if (project.getSecurityDefaults() != null) {
         projectResource.set("dcmp:visibleSecurityConstraints", securityToResource(
               project.getVisibleSecurityConstraints(), uri, "dcmp:visibleSecurityConstraints"));
      }

      projectResource.setUri(uri);
      return projectResource;
   }
   
   
   /**
    * 
    * @param model uses its project
    * @param modelResource if not null, is updated
    * @return
    * @throws ResourceParsingException
    */
   public DCResource modelToResource(DCModelBase model, DCResource modelResource) throws ResourceParsingException {
      if (modelResource == null) {
         modelResource = DCResource.create(null, ResourceModelIniter.MODEL_MODEL_NAME);
      }
      
      DCModelBase definitionModel = dataModelService.getDefinitionModel(model); // uses its project
      DCModelBase storageModel = dataModelService.getStorageModel(model); // null if abstract ; uses its project
      
      DCSecurity security = model.getSecurity();
      
      // filling model's provided props :
      modelResource
            .set("dcmo:name", model.getName())
            .set("dcmo:pointOfViewAbsoluteName", model.getPointOfViewAbsoluteName())
            .set("dcmo:majorVersion", model.getMajorVersion())
            // NB. NOT o:version which is only in DCModelBase for info purpose
            
             // POLY
            .set("dcmo:isDefinition", model.isDefinition()) // = !dcmo:isStorageOnly
            .set("dcmo:isStorage", model.isStorage())
            .set("dcmo:isInstanciable", model.isInstanciable())
            .set("dcmo:isMultiProjectStorage", model.isMultiProjectStorage())
         
            // NB. minor version is Resource version
            .set("dcmo:documentation", model.getDocumentation())// TODO in another collection for performance
            
            // storage :
            ///.set("dcmo:collectionName", model.getCollectionName()) // POLY RATHER storageModel.getName()
            .set("dcmo:maxScan", model.getMaxScan())
            
            // TODO if instanciable :
            .set("dcmo:isHistorizable", model.isHistorizable())
            .set("dcmo:isContributable", model.isContributable())
            
            // POLY cache :
            .set("dcmo:definitionModel", definitionModel.getName()) // all models should have one !!
            ;
      if (storageModel != null) {
         modelResource.set("dcmo:storageModel", storageModel.getName());
      }
      securityToResource(security, modelResource);
      if (model.getVersion() >= 0) {
         modelResource.setVersion(model.getVersion());
      } // not at creation, rather update DCModel.version from its resource after each put

      // once id source props are complete, build URI out of them :
      String uri = this.buildModelUri(modelResource);
      modelResource.setUri(uri);
      
      this.modelFieldsAndMixinsToResource(model, modelResource);
      
      return modelResource;
   }

   private void securityToResource(DCSecurity security, DCResource modelResource) {
      modelResource
         // (writing set as list else is json map)
         .set("dcms:isAuthentifiedReadable", (security != null) ? security.isAuthentifiedReadable() : null)
         .set("dcms:isAuthentifiedCreatable", (security != null) ? security.isAuthentifiedCreatable() : null)
         .set("dcms:isAuthentifiedWritable", (security != null) ? security.isAuthentifiedWritable() : null)
         .set("dcms:resourceCreators", (security != null) ? new ArrayList<String>(security.getResourceCreators()) : null)
         .set("dcms:resourceCreationOwners", (security != null) ? new ArrayList<String>(security.getResourceCreationOwners()) : null)
         .set("dcms:resourceReaders", (security != null) ? new ArrayList<String>(security.getResourceReaders()) : null)
         .set("dcms:resourceWriters", (security != null) ? new ArrayList<String>(security.getResourceWriters()) : null)
         .set("dcms:resourceOwners", (security != null) ? new ArrayList<String>(security.getResourceOwners()) : null);
   }
   private Object securityToResource(DCSecurity securityConstraints, String uri, String subId) {
      try {
         DCResource securityResource = DCResource.create(SimpleUriService.getContainerUrl(),
               ResourceModelIniter.MODEL_SECURITY_NAME, UriHelper.parseUri(uri), subId);
         securityResource.setVersion(0l); // dummy (OR should be top level's ??)
         securityToResource(securityConstraints, securityResource);
         return securityResource;
      } catch (URISyntaxException | MalformedURLException ex) {
         throw new RuntimeException(ex);
      }
   }
   
   /**
    * Does not use modelService or any project
    * @param model
    * @param modelResource
    * @throws ResourceParsingException
    */
   private void modelFieldsAndMixinsToResource(DCModelBase model, DCResource modelResource) throws ResourceParsingException {
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
      modelResource.set("dcmo:fieldAndMixins", new ArrayList<String>(model.getFieldAndMixinNames())); // ordered set
      
      // caches (so that they can be queried for introspection as well) :
      modelResource.set("dcmo:globalMixins",
            new ArrayList<String>(model.getGlobalMixinNames())); // TODO order
      modelResource.set("dcmo:globalFields", fieldsToProps(model.getGlobalFieldMap(),
            fieldUriPrefix, null)); // computed, so don't enrich existing props
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

   /**
    * public for tests 
    * @param field
    * @param fieldUriPrefix
    * @param existingProps
    * @return
    * @throws ResourceParsingException
    */
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
            .put("@type", DCResource.listBuilder().add(ResourceModelIniter.MODEL_FIELD_NAME).build()) // OR default one ?
            .put("o:version", 0l) // dummy (OR should be top level's ??)
            .put("dcmf:name", field.getName())
            .put("dcmf:type", field.getType())
            .put("dcmf:required", field.isRequired()) // TODO not for list ; for map ?!?
            .put("dcmf:queryLimit", field.getQueryLimit()); // TODO not for list, map
      if (field.getAliasedStorageNames() != null) {
         fieldPropBuilder.put("dcmf:aliasedStorageNames", new ArrayList<String>(field.getAliasedStorageNames()));
         // (written as list else json map)
      }
      fieldPropBuilder.put("dcmf:readonly", field.isReadonly());
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
    * (not done in the context of another / current project)
    * @param r
    * @return
    * @throws ResourceException if can't find local model or visible project
    * @throws URISyntaxException 
    * @throws MalformedURLException 
    */
   public DCProject toProject(DCResource r) throws ResourceException, MalformedURLException, URISyntaxException {
      // project is always be oasis.main for projects
      //String pointOfViewAbsoluteName = dataModelService.getProject().getAbsoluteName();
      String name = (String) r.get(ResourceModelIniter.POINTOFVIEW_NAME_PROP);
      DCProject project = new DCProject(name);
      return toProject(r, project);
   }
   public DCProject toProject(DCResource r, DCProject project) throws ResourceException, MalformedURLException, URISyntaxException {
      // project is always be oasis.main for projects
      //String pointOfViewAbsoluteName = dataModelService.getProject().getAbsoluteName();

      Long version = r.getVersion();
      if (version == null) {
         version = -1l; // assuming new
      }
      project.setVersion(version); // only in DCModelBase for info purpose
      // LET MAJOR VERSION & UNVERSIONED NAME BE PARSED FROM NAME
      /*try {
         // parsing major version : (could be Long if locally built ex. in model listener)
         Long majorVersion = valueService.parseLong(r.get("dcmpv:majorVersion"), null);
         if (majorVersion == null) {
            majorVersion = 0l; // TODO LATER compute & check : parse from dcmo:name
         }
         project.setMajorVersion(majorVersion); // LATER
      } catch (ResourceParsingException rpex) {
         throw new ResourceException("Error parsing project", rpex, r, project);
      }
      if (r.get("dcmpv:unversionedName") != null) {
         project.setUnversionedName((String) r.get("dcmpv:unversionedName"));
      }*/
      
      @SuppressWarnings("unchecked")
      //List<Map<String, Object>> visibleProjects = (List<Map<String, Object>>) r.get("dcmp:visibleProjects");
      List<String> visibleProjectUris = (List<String>) r.get("dcmp:localVisibleProjects");
      if (visibleProjectUris != null) {
         //for (Map<String, Object> visibleProject : visibleProjects) {
         for (String visibleProjectUri : visibleProjectUris) {
            String visibleProjectName = UriHelper.parseUri(visibleProjectUri).getId();
            DCProject visibleProject = dataModelService.getProject(visibleProjectName);
            if (visibleProject == null) {
               throw new ResourceException("Can't find visibleProject " + visibleProjectName,
                     null, r, project);
            }
            project.addLocalVisibleProject(visibleProject);
            // TODO isDef/Storage...
            /*boolean vprName = vpr.get("dcmvp:name");
            DCVisibleProject dcVisibleProject = new DCVisibleProject();
            boolean isStorage = vpr.get("dcmvp:isStorage");*/
         }
      } // else allowing model without visible projects ex. Phase 1 sandbox

      // TODO POV ?!
      
      // NB. local models loaded independently in LoadPersistedModelsAtInit
      // TODO LATER local models : use project.localModels to conf relationship
      // (their fork modes i.e. description of what must be done to achieve the fork)

      @SuppressWarnings("unchecked")
      List<String> forkedUris = (List<String>) r.get("dcmp:forkedUris");
      project.setForkedUris(new HashSet<String>(forkedUris));
      @SuppressWarnings("unchecked")
      List<String> frozenModelNames = (List<String>) r.get("dcmp:frozenModelNames");
      project.setFrozenModelNames(new HashSet<String>(frozenModelNames));
      project.setModelLevelSecurityEnabled((boolean) r.get("dcmp:modelLevelSecurityEnabled"));
      
      return project;
   }
   
   /**
    * Uses model resource's pointOfViewAbsoluteName as project BUT must be currentProjectName.
    * Creates and fills DCModel or DCMixin (by calling resourceToFieldsAndMixins())
    * from given resource.
    * BUT does not clean / update r with ex. DCModelBase-computed fields (globalFields...
    * for this call afterwards modelFieldsAndMixinsToResource()), nor check consistency
    * (country / language specific model... for this call afterwards checkModelOrMixin()).
    * 
    * TODO LATER gather all ResourceParsingException in a ParsingContext
    * like in ResourceEntityMappingService 
    * @param r
    * @param isModel
    * @return
    * @throws ResourceException if wrapped ResourceParsingException, or
    * pointOfViewAbsoluteName not currentProjectName
    */
   @SuppressWarnings("unchecked")
   public DCModelBase toModelOrMixin(DCResource r) throws ResourceException {
      // TODO check non required fields : required queryLimit openelec's maxScan resourceType

      String typeName = (String) r.get("dcmo:name");
      
      DCProject project = getAndCheckModelResourceProject(r);
      
      DCModelBase modelOrMixin = new DCModel(typeName, project.getName());

      Long version = r.getVersion();
      if (version == null) {
         version = -1l; // assuming new
      }
      modelOrMixin.setVersion(version); // only in DCModelBase for info purpose
      try {
         // parsing major version : (could be Long if locally built ex. in model listener)
         Long majorVersion = valueService.parseLong(r.get("dcmo:majorVersion"), null);
         if (majorVersion == null) {
            majorVersion = 0l; // TODO LATER compute & check : parse from dcmo:name
         }
         modelOrMixin.setMajorVersion(majorVersion); // LATER
      } catch (ResourceParsingException rpex) {
         throw new ResourceException("Error parsing model", rpex, r, project);
      }
      
      modelOrMixin.setDocumentation((String) r.get("dcmo:documentation")); // OPT

      // storage :
      // NB. collectionName is deduced from storage typeName
      modelOrMixin.setMaxScan((int) r.get("dcmo:maxScan"));
      
      // TODO if instanciable :
      modelOrMixin.setHistorizable((boolean) r.get("dcmo:isHistorizable"));
      modelOrMixin.setContributable((boolean) r.get("dcmo:isContributable"));

      // POLY :
      modelOrMixin.setDefinition((boolean) r.get("dcmo:isDefinition"));
      modelOrMixin.setStorage((boolean) r.get("dcmo:isStorage"));
      modelOrMixin.setInstanciable((boolean) r.get("dcmo:isInstanciable"));
      modelOrMixin.setMultiProjectStorage(r.get("dcmo:isMultiProjectStorage") != null ?
            (boolean) r.get("dcmo:isMultiProjectStorage") : false);
      
      // features :
      modelOrMixin.setCountryLanguage((String) r.get("dcmls:code"));

      // security :
      if (r.get("dcms:isAuthentifiedReadable") != null) {
         getOrSetSecurity(modelOrMixin).setAuthentifiedReadable((boolean) r.get("dcms:isAuthentifiedReadable"));
      }
      if (r.get("dcms:isAuthentifiedCreatable") != null) {
         getOrSetSecurity(modelOrMixin).setAuthentifiedCreatable((boolean) r.get("dcms:isAuthentifiedCreatable"));
      }
      if (r.get("dcms:isAuthentifiedWritable") != null) {
         getOrSetSecurity(modelOrMixin).setAuthentifiedWritable((boolean) r.get("dcms:isAuthentifiedWritable"));
      }
      if (r.get("dcms:resourceCreators") != null) {
         getOrSetSecurity(modelOrMixin).setResourceCreators(new LinkedHashSet<String>((List<String>) r.get("dcms:resourceCreators")));
      }
      if (r.get("dcms:resourceCreationOwners") != null) {
         getOrSetSecurity(modelOrMixin).setResourceCreationOwners(new LinkedHashSet<String>((List<String>) r.get("dcms:resourceCreationOwners")));
      }
      if (r.get("dcms:resourceReaders") != null) {
         getOrSetSecurity(modelOrMixin).setResourceReaders(new LinkedHashSet<String>((List<String>) r.get("dcms:resourceReaders")));
      }
      if (r.get("dcms:resourceWriters") != null) {
         getOrSetSecurity(modelOrMixin).setResourceWriters(new LinkedHashSet<String>((List<String>) r.get("dcms:resourceWriters")));
      }
      if (r.get("dcms:resourceOwners") != null) {
         getOrSetSecurity(modelOrMixin).setResourceOwners(new LinkedHashSet<String>((List<String>) r.get("dcms:resourceOwners")));
      }
      
      try {
         this.resourceToFieldsAndMixins(modelOrMixin, r);
      } catch (ResourceParsingException rpex) {
         throw new ResourceException("Error while loading DCModel from Resource",
               rpex, r, project);
         // happens when not yet loaded or obsolete (i.e. invalid though persisted not as such)
      }
      
      return modelOrMixin;
   }
   
   private DCSecurity getOrSetSecurity(DCModelBase modelOrMixin) {
      DCSecurity security = modelOrMixin.getSecurity();
      if (security == null) {
         security = new DCSecurity();
         modelOrMixin.setSecurity(security);
      }
      return security;
   }
   public DCProject getAndCheckModelResourceProject(DCResource r) throws ResourceException {
      String pointOfViewAbsoluteName = (String) r.get("dcmo:pointOfViewAbsoluteName");
      DCProject currentProject = dataModelService.getProject();
      if (pointOfViewAbsoluteName != null
            && !pointOfViewAbsoluteName.equals(currentProject.getAbsoluteName())) {
         throw new ResourceException("Writing model " + r.get("dcmo:name")
               + " belonging to another project " + pointOfViewAbsoluteName
               + " than current one", r, currentProject);
      }
      return currentProject;
   }
   /**
    * Checks model consistency ; NOT on startup else order of load can make it fail.
    * - dcmls:CountryLanguageSpecific : only by having this mixin and another generic mixin
    * can its modelType have non-URL safe chars
    * TODO LATER :
    * - that ref'd model exists at computing time OR BETTER make resourceType a link (DCResourceField)
    * - ...
    * Uses modelOrMixin's project.
    * @param modelOrMixin checked
    * @param r for logging purpose
    * @throws ResourceException if consistency check fails
    */
   public void checkModelOrMixin(DCModelBase modelOrMixin, DCResource r) throws ResourceException {
      DCProject project = dataModelService.getProject(modelOrMixin.getProjectName());
      
      if (project.getFrozenModelNames().contains(modelOrMixin.getName())) {
         throw new AccessDeniedException("Can't update frozen model "
               + modelOrMixin.getName() + " in project " + project.getName());
      }
      
      if (modelOrMixin.getCountryLanguage() != null) {
      //if (r.get("dcmls:code") != null) {
         boolean hasGenericMixin = false;
         for (String mixinName : modelOrMixin.getGlobalMixinNames()) { // OR not global to avoid
            // language-specific inheritance trees with a single generic mixin, but not perfect either
            if (mixinName.equals(ResourceModelIniter.MODEL_COUNTRYLANGUAGESPECIFIC_NAME)) {
               continue;
            }
            DCModelBase mixin = project.getModel(mixinName);
            if (mixin.getCountryLanguage() == null) {
            //if (!ldpEntityQueryService.find("dcmi:mixin_0", { 'dcmls:code' : '$exist' }).isEmpty()) {
               hasGenericMixin = true;
               break;
            }
         }
      /*if (modelOrMixin.hasMixin("dcmls:CountryLanguageSpecific")) { // LATER WOULD REQUIRED ANONYMOUS TYPES / OPTIONAL / CANDIDATE MIXINS
         boolean hasGenericMixin = false;
         for (String mixinName : modelOrMixin.getGlobalMixinNames()) { // OR not global to avoid
            // language-specific inheritance trees with a single generic mixin, but not perfect either
            if (mixinName.equals("dcmls:CountryLanguageSpecific")) {
               continue;
            }
            DCModelBase mixin = project.getModelBase(mixinName);
            if (!mixin.hasMixin("dcmls:CountryLanguageSpecific")) { // LATER WOULD REQUIRED ANONYMOUS TYPES / OPTIONAL / CANDIDATE MIXINS
               hasGenericMixin = true;
               break;
            }
         }*/
         if (!hasGenericMixin) {
            throw new ResourceException("Country / language-specific (with  "
                  + "dcmls:CountryLanguageSpecific mixin) model type has no generic mixin",
                  null, r, project);
         }
      } else {
         if (!UriHelper.hasUrlAlwaysSafeCharacters(modelOrMixin.getName())) {
            throw new ResourceException("Non country / language-specific (with "
                  + "dcmls:CountryLanguageSpecific mixin) model type does not follow best practice rule "
                  + "of not containing URL always safe or colon characters i.e. "
                  + UriHelper.NOT_URL_ALWAYS_SAFE_OR_COLON_CHARACTERS_REGEX,
                  null, r, project);
         }
      }
   }

   /**
    * Uses modelOrMixin's project i.e. dcmo:pointOfViewAbsoluteName ; public only for tests.
    * @param modelOrMixin created by TODO
    * @param r
    * @throws ResourceParsingException
    */
   public void resourceToFieldsAndMixins(DCModelBase modelOrMixin, DCResource r) throws ResourceParsingException {
      DCProject project = dataModelService.getProject(modelOrMixin.getProjectName());
      // TODO (computed) security (version)
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> fieldResources = (List<Map<String, Object>>) r.get("dcmo:fields");
      if (fieldResources == null) {
         // (allowing model without fields but ex. a mixin)
         fieldResources = new ImmutableList.Builder<Map<String, Object>>().build();
      }

      // map fields :
      Map<String, DCField> fieldMap;
      try {
         fieldMap = this.propsToFields(fieldResources);
      } catch (ResourceParsingException rpex) {
         throw new ResourceParsingException("Error when parsing fields of model resource "
               + r, rpex);
      }
      
      // map mixins :
      @SuppressWarnings("unchecked")
      List<String> mixinNames = (List<String>) r.get("dcmo:mixins"); // TODO or list of boolean maps ??
      if (mixinNames == null) { // TODO when is tag-like mixin ? ; NB. import tool does not send them if none
         mixinNames = new ImmutableList.Builder<String>().build();
      }
      Builder<String, DCModelBase> mixinMapBuilder = new ImmutableMap.Builder<String, DCModelBase>();
      for (String mixinName : mixinNames) {
         DCModelBase mixin;
         if (mixinName.equals(modelOrMixin.getName())) {
            // means to look in the current project's visible project (not to load itself)
            mixin = project.getNonLocalModel(mixinName);
         } else {
            mixin = project.getModel(mixinName);
         }
         if (mixin == null) {
            throw new ResourceParsingException("Can't find mixin "
                  + mixinName + " when updating DCModelBase from resource " + r);
            // happens when not yet loaded or obsolete (i.e. invalid though persisted not as such)
            // ex. Can't find mixin geo:District_0 when updating DCModelBase from resource {"@id":"http://data.ozwillo.com/dc/type/dcmo:model_0/geo:%C4%B0l%C3%A7e_0","o:version":12,"@type":["dcmo:model_0","dcmi:mixin_0","o:Displayable_0","dcmoid:Identification","dcmls:CountryLanguageSpecific"],"dc:created":"2015-01-07T17:26:29.998+01:00","dc:modified":"2015-01-15T10:27:39.611+01:00","dc:creator":"admin","dc:contributor":"admin","dcmo:mixins":["geo:District_0"],"dcmo:isHistorizable":false,"dcmo:storageModel":"geo:Area_0","dcmo:name":"geo:İlçe_0","dcmo:fields":[{"dcmf:type":"i18n","dcmf:queryLimit":0,"@type":["dcmf:field_0"],"dcmf:name":"geo_district:name","dcmf:required":false,"o:version":0,"dcmf:defaultLanguage":"tr","dcmf:documentation":"TR.  ~= City. Parent : TRProvince. NB. Zipcode is only SubDistrict","@id":"http://data.ozwillo.com/dc/type/dcmf:field_0/dcmo:model_0/geo:%C4%B0l%C3%A7e_0/geo_district:name","dcmf:listElementField":{"dcmf:type":"map","dcmf:queryLimit":0,"@type":["dcmf:field_0"],"dcmf:name":"i18nMap","dcmf:required":false,"o:version":0,"@id":"http://data.ozwillo.com/dc/type/dcmf:field_0/dcmo:model_0/geo:%C4%B0l%C3%A7e_0/geo_district:name/i18nMap","dcmf:mapFields":[{"dcmf:type":"string","dcmf:queryLimit":0,"@type":["dcmf:field_0"],"dcmf:name":"v","dcmf:required":true,"o:version":0,"@id":"http://data.ozwillo.com/dc/type/dcmf:field_0/dcmo:model_0/geo:%C4%B0l%C3%A7e_0/geo_district:name/i18nMap/v"},{"dcmf:type":"string","dcmf:queryLimit":0,"@type":["dcmf:field_0"],"dcmf:name":"l","dcmf:required":false,"o:version":0,"@id":"http://data.ozwillo.com/dc/type/dcmf:field_0/dcmo:model_0/geo:%C4%B0l%C3%A7e_0/geo_district:name/i18nMap/l"}]},"dcmfid:indexInId":1},{"dcmf:type":"resource","dcmf:queryLimit":0,"@type":["dcmf:field_0"],"dcmf:name":"geo_district:country","dcmf:required":false,"o:version":0,"@id":"http://data.ozwillo.com/dc/type/dcmf:field_0/dcmo:model_0/geo:%C4%B0l%C3%A7e_0/geo_district:country","dcmf:resourceType":"geo:Country_0"},{"dcmf:type":"resource","dcmf:queryLimit":0,"@type":["dcmf:field_0"],"dcmf:name":"geo_district:nuts3","dcmf:required":false,"o:version":0,"dcmf:documentation":"Ref to its parent NUTS3. NO internal field name but subresource linking","@id":"http://data.ozwillo.com/dc/type/dcmf:field_0/dcmo:model_0/geo:%C4%B0l%C3%A7e_0/geo_district:nuts3","dcmfid:indexInId":0,"dcmf:resourceType":"geo:Nuts3_0"}],"dcmo:majorVersion":0,"dcmo:isDefinition":true,"dcmoid:useIdForParent":true,"dcmoid:idFieldNames":["geo_district:nuts3","geo_district:name"],"dcmoid:parentFieldNames":["geo_district:nuts3"],"dcmo:globalMixins":["geo:District_0","geo:Area_0","o:Displayable_0","o:Ancestor_0"],"dcmo:isInstanciable":true,"dcmo:importDefaultOnly":false,"dcmo:maxScan":0,"dcmo:definitionModel":"geo:İlçe_0","dcmo:globalFields":[{"dcmf:type":"i18n","dcmf:queryLimit":0,"@type":["dcmf:field_0"],"dcmf:name":"odisp:name","dcmf:required":false,"o:version":0,"@id":"http://data.ozwillo.com/dc/type/dcmf:field_0/dcmo:model_0/geo:%C4%B0l%C3%A7e_0/odisp:name","dcmf:listElementField":{"dcmf:type":"map","dcmf:queryLimit":0,"@type":["dcmf:field_0"],"dcmf:name":"i18nMap","dcmf:required":false,"o:version":0,"@id":"http://data.ozwillo.com/dc/type/dcmf:field_0/dcmo:model_0/geo:%C4%B0l%C3%A7e_0/odisp:name/i18nMap","dcmf:mapFields":[{"dcmf:type":"string","dcmf:queryLimit":0,"@type":["dcmf:field_0"],"dcmf:name":"v","dcmf:required":true,"o:version":0,"@id":"http://data.ozwillo.com/dc/type/dcmf:field_0/dcmo:model_0/geo:%C4%B0l%C3%A7e_0/odisp:name/i18nMap/v"},{"dcmf:type":"string","dcmf:queryLimit":0,"@type":["dcmf:field_0"],"dcmf:name":"l","dcmf:required":false,"o:version":0,"@id":"http://data.ozwillo.com/dc/type/dcmf:field_0/dcmo:model_0/geo:%C4%B0l%C3%A7e_0/odisp:name/i18nMap/l"}]}},{"dcmf:type":"list","dcmf:queryLimit":0,"@type":["dcmf:field_0"],"dcmf:name":"o:ancestors","dcmf:required":false,"o:version":0,"@id":"http://data.ozwillo.com/dc/type/dcmf:field_0/dcmo:model_0/geo:%C4%B0l%C3%A7e_0/o:ancestors","dcmf:listElementField":{"dcmf:type":"resource","dcmf:queryLimit":0,"@type":["dcmf:field_0"],"dcmf:name":"o:ancestors","dcmf:required":false,"o:version":0,"@id":"http://data.ozwillo.com/dc/type/dcmf:field_0/dcmo:model_0/geo:%C4%B0l%C3%A7e_0/o:ancestors/o:ancestors","dcmf:resourceType":"o:Ancestor_0"}},{"dcmf:type":"string","dcmf:queryLimit":0,"@type":["dcmf:field_0"],"dcmf:name":"geo_area:idIso","dcmf:required":false,"o:version":0,"@id":"http://data.ozwillo.com/dc/type/dcmf:field_0/dcmo:model_0/geo:%C4%B0l%C3%A7e_0/geo_area:idIso"},{"dcmf:type":"resource","dcmf:queryLimit":0,"@type":["dcmf:field_0"],"dcmf:name":"geo_area:country","dcmf:required":false,"o:version":0,"@id":"http://data.ozwillo.com/dc/type/dcmf:field_0/dcmo:model_0/geo:%C4%B0l%C3%A7e_0/geo_area:country","dcmf:resourceType":"geo:Country_0"},{"dcmf:type":"i18n","dcmf:queryLimit":0,"@type":["dcmf:field_0"],"dcmf:name":"geo_district:name","dcmf:required":false,"o:version":0,"dcmf:defaultLanguage":"tr","@id":"http://data.ozwillo.com/dc/type/dcmf:field_0/dcmo:model_0/geo:%C4%B0l%C3%A7e_0/geo_district:name","dcmf:listElementField":{"dcmf:type":"map","dcmf:queryLimit":0,"@type":["dcmf:field_0"],"dcmf:name":"i18nMap","dcmf:required":false,"o:version":0,"@id":"http://data.ozwillo.com/dc/type/dcmf:field_0/dcmo:model_0/geo:%C4%B0l%C3%A7e_0/geo_district:name/i18nMap","dcmf:mapFields":[{"dcmf:type":"string","dcmf:queryLimit":0,"@type":["dcmf:field_0"],"dcmf:name":"v","dcmf:required":true,"o:version":0,"@id":"http://data.ozwillo.com/dc/type/dcmf:field_0/dcmo:model_0/geo:%C4%B0l%C3%A7e_0/geo_district:name/i18nMap/v"},{"dcmf:type":"string","dcmf:queryLimit":0,"@type":["dcmf:field_0"],"dcmf:name":"l","dcmf:required":false,"o:version":0,"@id":"http://data.ozwillo.com/dc/type/dcmf:field_0/dcmo:model_0/geo:%C4%B0l%C3%A7e_0/geo_district:name/i18nMap/l"}]}},{"dcmf:type":"resource","dcmf:queryLimit":0,"@type":["dcmf:field_0"],"dcmf:name":"geo_district:country","dcmf:required":false,"o:version":0,"@id":"http://data.ozwillo.com/dc/type/dcmf:field_0/dcmo:model_0/geo:%C4%B0l%C3%A7e_0/geo_district:country","dcmf:resourceType":"geo:Country_0"},{"dcmf:type":"resource","dcmf:queryLimit":0,"@type":["dcmf:field_0"],"dcmf:name":"geo_district:nuts3","dcmf:required":false,"o:version":0,"@id":"http://data.ozwillo.com/dc/type/dcmf:field_0/dcmo:model_0/geo:%C4%B0l%C3%A7e_0/geo_district:nuts3","dcmf:resourceType":"geo:Nuts3_0"}],"dcmo:isContributable":false,"dcmo:importAutolinkedOnly":false,"dcmls:code":"TR","dcmo:isStorage":false,"dcmo:pointOfViewAbsoluteName":"oasis.main","dcmo:fieldAndMixins":["geo:District_0","geo_district:name","geo_district:country","geo_district:nuts3"]}
         }
         mixinMapBuilder.put(mixin.getName(), mixin);
      }
      Map<String, DCModelBase> mixinMap = mixinMapBuilder.build();
      
      // NB. since ModelService checks at add() that all of a Model's mixins are already known by it,
      // dcmo:fieldAndMixinNames is enough to know mixins and no need to get the dcmo:mixins prop
      
      // add fields and mixins, in order :
      // TODO TODO mixins rather lazy in DCModelBase !!!!
      @SuppressWarnings("unchecked")
      List<String> fieldAndMixinNames = (List<String>) r.get("dcmo:fieldAndMixins"); // TODO or list of boolean maps ??
      if (fieldAndMixinNames == null) { // TODO when is tag-like mixin ?
         fieldAndMixinNames = new ImmutableList.Builder<String>().build();
      }
      for (String fieldAndMixinName : fieldAndMixinNames) {
         if (fieldMap != null) {
            DCField field = fieldMap.get(fieldAndMixinName);
            if (field != null) {
               modelOrMixin.addField(field); // and NOT getFieldMap().put() because more must be done
               continue;
            }
         }
         DCModelBase mixin = mixinMap.get(fieldAndMixinName); // does also model
         if (mixin != null
               // can't have itself as mixin unless comes from a different (visible) project
               && !(fieldAndMixinName.equals(modelOrMixin.getName())
                     && modelOrMixin.getProjectName().equals(mixin.getProjectName()))) {
            modelOrMixin.addMixin(mixin);
            continue;
         }
         // TODO may be a previously existing field or mixin that has been removed
         // by client side which has then not recomputed fieldAndMixinName
         ///throw new ResourceParsingException("Can't find field or mixin "
         ///      + fieldAndMixinName + " when updating DCModelBase from resource " + r);
      }

      // in case of obsolete and not recomputed fieldAndMixins, adding new (i.e. remaining)
      // fields in case of obsolete and not recomputed fieldAndMixins :
      Set<String> fieldAndMixinNameSet = new LinkedHashSet<String>(fieldAndMixinNames);
      for (String fieldName : fieldMap.keySet()) {
         if (fieldAndMixinNameSet != null && fieldAndMixinNameSet.contains(fieldName)) {
            continue;
         }
         modelOrMixin.addField(fieldMap.get(fieldName)); // and NOT getFieldMap().put() because more must be done
      }
      
      // adding new (i.e. remaining) mixins in case of obsolete and not recomputed fieldAndMixins :
      for (String mixinName : mixinNames) {
         if (fieldAndMixinNameSet.contains(mixinName)) {
            continue;
         }
         DCModelBase mixin = mixinMap.get(mixinName); // does also model
         if (mixin == null) {
            throw new ResourceParsingException("Can't find mixin " + mixinName
                  + " when updating DCModelBase from resource " + r); // should not happen, as said above
         }
         modelOrMixin.addMixin(mixin);
      }
   }
   
   /**
    * public for tests ; doesn't use modelService nor any project
    * @param fieldResource
    * @return
    * @throws ResourceParsingException
    */
   public DCField propsToField(Map<String, Object> fieldResource) throws ResourceParsingException {
      String fieldName = (String) fieldResource.get("dcmf:name");
      String fieldType = (String) fieldResource.get("dcmf:type");
      boolean fieldRequired = (boolean) fieldResource.get("dcmf:required"); // TODO or default value
      int fieldQueryLimit = (int) fieldResource.get("dcmf:queryLimit"); // TODO or default value
      
      DCField field;
      switch (fieldType ) {
      case "map" :
         field = propsToMapField(fieldResource, fieldName);
         break;
      case "list" :
         field = propsToListField(fieldResource, fieldName);
         break;
      case "i18n" :
         // getting query limit (of its "v" list subfield) :
         DCListField tmpField = propsToListField(fieldResource, fieldName);
         fieldQueryLimit = ((DCMapField) tmpField.getListElementField()).getMapFields()
               .get(DCI18nField.KEY_VALUE).getQueryLimit();
         // building :
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
                  + fieldName);
         }
         field = new DCResourceField(fieldName, fieldResourceType, fieldRequired, fieldQueryLimit);
         /*if (!fieldResourceType.equals(topLevelModelType) && project.getModelBase(fieldResourceType)) {
            throw new ResourceParsingException("model " + fieldResourceType
                  + " linked by field " + fieldName + " can't be found");
         }*/ // TODO rather check subresource mixins
         break;
      default :
         field = new DCField(fieldName, fieldType, fieldRequired, fieldQueryLimit);
      }

      if (fieldResource.get("dcmf:aliasedStorageNames") != null) {
         @SuppressWarnings("unchecked")
         LinkedHashSet<String> aliasedStorageNames = new LinkedHashSet<String>(
               (List<String>) fieldResource.get("dcmf:aliasedStorageNames"));
         field.setAliasedStorageNames(aliasedStorageNames);
      }
      if (fieldResource.get("dcmf:readonly") != null) {
         field.setReadonly((boolean) fieldResource.get("dcmf:readonly"));
      }
      
      // parse & set default value :
      // TODO LATER use DCResourceParsingContext in the whole model mapping process ?
      String defaultStringValue = (String) fieldResource.get("dcmf:defaultStringValue");
      if (defaultStringValue != null) {
         DCModelBase dcModel = null; // dcmo:model_80
         DCModelBase storageModel = null; // dcmf:field_0
         DCURI uri;
         try {
            uri = UriHelper.parseUri((String) fieldResource.get("@id"));
         } catch (MalformedURLException | URISyntaxException e) {
            throw new ResourceParsingException("Unknown error while parsing default value field URI ", e);
         }
         DCResourceParsingContext resourceParsingContext =
               new DCResourceParsingContext(dcModel, storageModel, uri);
         resourceParsingContext.enter(null, null, field, defaultStringValue);
         try {
            field.setDefaultValue(resourceEntityMapperService.parseValueFromJSONOrString(
                  defaultStringValue, field, resourceParsingContext));
         } finally {
            resourceParsingContext.exit();
         }
      }
      return field;
   }

   private DCMapField propsToMapField(Map<String, Object> fieldResource, String fieldName) throws ResourceParsingException {
      // NB. only used in building DCI18nField to get its (sublist field "v") queryLimit
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> mapFieldsProps = (List<Map<String, Object>>) fieldResource.get("dcmf:mapFields");
      DCMapField mapField = new DCMapField(fieldName);
      mapField.setMapFields(propsToFields(mapFieldsProps));
      return mapField;
   }
   private DCListField propsToListField(Map<String, Object> fieldResource, String fieldName) throws ResourceParsingException {
      @SuppressWarnings("unchecked")
      Map<String, Object> listElementFieldProps = (Map<String, Object>) fieldResource.get("dcmf:listElementField");
      return new DCListField(fieldName, propsToField(listElementFieldProps));
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
