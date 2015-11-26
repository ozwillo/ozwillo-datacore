package org.oasis.datacore.model.event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.oasis.datacore.common.context.DCRequestContextProvider;
import org.oasis.datacore.common.context.SimpleRequestContextProvider;
import org.oasis.datacore.core.init.InitService;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.model.resource.ModelResourceMappingService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.server.event.AbortOperationEventException;
import org.oasis.datacore.rest.server.event.DCEvent;
import org.oasis.datacore.rest.server.event.DCEventListener;
import org.oasis.datacore.rest.server.event.DCResourceEvent;
import org.oasis.datacore.rest.server.event.DCResourceEventListener;
import org.oasis.datacore.rest.server.resource.ResourceEntityMapperService;
import org.oasis.datacore.rest.server.resource.ResourceException;
import org.oasis.datacore.rest.server.resource.ResourceNotFoundException;
import org.oasis.datacore.rest.server.resource.ResourceObsoleteException;
import org.oasis.datacore.rest.server.resource.ResourceService;
import org.oasis.datacore.sample.ResourceModelIniter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

import com.google.common.collect.ImmutableMap;


/**
 * Inits from Resource and replaces DCModel (or removes it)
 * NB. at startup done by LoadPersistedModelsAtInit
 * TODO LATER check BEFOREHANDS for update compatibility, else abort using AbortOperationEventException
 * also updates models (& mixins) that reuse the updated one, including in other projects
 * (TODO BETTER (transactional ??) sync task engine that avoids doing it in the wrong order
 * or even merges tasks) , and abort on delete !
 * TODO move most to Model(Admin)Service !
 * 
 * @author mdutoo
 *
 */
public class ModelResourceDCListener extends DCResourceEventListener implements DCEventListener {

   @Autowired
   private DataModelServiceImpl dataModelAdminService;
   @Autowired
   private ModelResourceMappingService mrMappingService;
   @Autowired
   private ResourceService resourceService;
   @Autowired
   private ResourceEntityMapperService reMappingService;
   /** to know whether inited, until when we can't updateDirectlyImpactedModels
    * (because some are probably not even persisted yet) */
   @Autowired
   private InitService initService;

   public ModelResourceDCListener() {
      super();
   }

   public ModelResourceDCListener(String resourceType) {
      super(resourceType);
   }

   @Override
   public void handleEvent(DCEvent event) throws AbortOperationEventException {
      switch (event.getType()) {
      case DCResourceEvent.ABOUT_TO_BUILD :
         // Adding computed fields (computed by DCModelBase) and doing consistency checks :
         // NB. computed fields (majorVersion, pointOfViewAbsoluteName) must NOT be required
         // NB. not done on ABOUT_TO_CREATE/UPDATE else computed fields won't be added to
         // entity that has already been parsed and will be persisted as is
         DCResourceEvent re = (DCResourceEvent) event;
         DCResource r = re.getResource();
         try {
            
            // first cleaning up resource ; required else default values have not been set by parsing yet
            // which makes toModelOrMixin fail on ex. maxScan (null because unset) : (BEWARE therefore PATCH won't work on default values !)
            reMappingService.entityToResource(resourceService.resourceToEntity(r), r, false); // TODO write
            
            // map to model & check : 
            DCModelBase modelOrMixin = mrMappingService.toModelOrMixin(r); // checks project !
            // update computed fields & clean resource
            // (pointOfViewAbsoluteName, globalFields... required by resourceToEntity()) : (BEWARE this hampers PATCH !)
            mrMappingService.modelToResource(modelOrMixin, r);
            // model consistency check : (NOT on startup else order of load can make it fail) (BEWARE this forbids PATCH ?!!)
            mrMappingService.checkModelOrMixin(modelOrMixin, r);
            
         } catch (ResourceObsoleteException roex) { // specific handling for better logging
            // abort else POSTer won't know that his model can't be used (to POST resources)
            throw new AbortOperationEventException(new ResourceObsoleteException("Resource obsolete error ("
                  + roex.getMessage() + ") while converting "
                  + "or enriching to model or mixin " + r.get(ResourceModelIniter.MODEL_NAME_PROP)
                  + " with dcmo:pointOfViewAbsoluteName=" + r.get("dcmo:pointOfViewAbsoluteName")
                  + " in project " + dataModelAdminService.getProject().getName()
                  + ", aborting POSTing resource", roex, r, dataModelAdminService.getProject()));
         } catch (ResourceException rex) {
            // abort else POSTer won't know that his model can't be used (to POST resources)
            throw new AbortOperationEventException(rex);
         } catch (AccessDeniedException adex) { // specific handling for better logging
            // abort else POSTer won't know that his model can't be used (to POST resources)
            throw new AbortOperationEventException(new AccessDeniedException("Access denied error ("
                  + adex.getMessage() + ") while converting "
                  + "or enriching to model or mixin " + r.get(ResourceModelIniter.MODEL_NAME_PROP)
                  + " with dcmo:pointOfViewAbsoluteName=" + r.get("dcmo:pointOfViewAbsoluteName")
                  + " in project " + dataModelAdminService.getProject().getName()
                  + ", aborting POSTing resource", adex));
         } catch (Throwable t) {
            // abort else POSTer won't know that his model can't be used (to POST resources)
            throw new AbortOperationEventException(new ResourceException("Unknown error while converting "
                  + "or enriching to model or mixin " + r.get(ResourceModelIniter.MODEL_NAME_PROP)
                  + " with dcmo:pointOfViewAbsoluteName=" + r.get("dcmo:pointOfViewAbsoluteName")
                  + " in project " + dataModelAdminService.getProject().getName()
                  + ", aborting POSTing resource", t, r, dataModelAdminService.getProject()));
         }
         return;
      case DCResourceEvent.CREATED :
      case DCResourceEvent.UPDATED :
         re = (DCResourceEvent) event;
         r = re.getResource();
         handleModelResourceCreatedOrUpdated(r);
         // TODO if (used as) mixin, do it also for all impacted models (& mixins) !
         return;
      case DCResourceEvent.DELETED :
         re = (DCResourceEvent) event;
         r = re.getResource();
         // TODO LATER check that is not used as mixin in any other models (& mixins),
         // else require that they be deleted first !
         handleModelResourceDeleted(r);
         return;
      }
   }

   /**
    * TODO move to DCModelService ; must be done in modelOrMixin project
    * @param modelOrMixin
    * @param r to allow to create ResourceException
    * @throws AbortOperationEventException
    */
   public void createOrUpdate(DCModelBase modelOrMixin, DCResource r) throws AbortOperationEventException {
      // replacing it :
      // TODO TODO RATHER ALL AT ONCE to avoid
      // * having an inconsistent set of models when ResourceService parses Resources
      // * and having one model that refers to another that is not yet there compute its caches too early
      // * AND HAVING OBSOLETE INDEXES !
      DCProject project = dataModelService.getProject(modelOrMixin.getProjectName());
      DCModelBase previousModel = project.getModel(modelOrMixin.getName());

      String aboutToEventType;
      if (previousModel == null) {
         // new model
         aboutToEventType = ModelDCEvent.ABOUT_TO_CREATE;
      } else {
         // TODO also update all impacted models !!
         aboutToEventType = ModelDCEvent.ABOUT_TO_UPDATE;
      }
      
      // let's actually register the DCModel :
      project.addLocalModel(modelOrMixin);

      try {
         // let's update storage (index...) :
         eventService.triggerEvent(new ModelDCEvent(aboutToEventType,
               ModelDCEvent.MODEL_DEFAULT_BUSINESS_DOMAIN, modelOrMixin, previousModel));
      } catch (Throwable e) {
         // TODO abort to say error to client ?
         // TODO save state in model or wrapper stored in mongo ?? log all events in mongo ???
         // TODO try to restore previousDocument ???
         throw new AbortOperationEventException(new ResourceException("Aborting as asked for in aboutTo event "
               + "of model or mixin " + modelOrMixin, e, r, dataModelAdminService.getProject()));
      }
      
      String doneEventType = (previousModel == null) ?
            ModelDCEvent.CREATED : ModelDCEvent.UPDATED;
      try {
         eventService.triggerEvent(new ModelDCEvent(doneEventType,
               ModelDCEvent.MODEL_DEFAULT_BUSINESS_DOMAIN, modelOrMixin, previousModel));
      } catch (Throwable e) {
         // TODO abort to say error to client ?
         // TODO save state in model or wrapper stored in mongo ?? log all events in mongo ???
         // TODO try to restore previousDocument ???
         throw new AbortOperationEventException(new ResourceException("Aborting as asked for in done event "
               + "of model or mixin " + modelOrMixin, e, r, dataModelAdminService.getProject()));
      }

      if (initService.isInited()
            && doneEventType == ModelDCEvent.UPDATED) {
         // updating impacted models and their persistence :
         updateDirectlyImpactedModels(modelOrMixin);
      } // else creation which has no impact
   }

   /**
    * must be done in modelOrMixin project
    * @param modelOrMixin
    */
   private void updateDirectlyImpactedModels(DCModelBase modelOrMixin) {
      // let's remember seen models :
      // (else the same models may be seen several times from different projects
      // having their project as visible)
      Set<String> modelOrMixinAbsoluteNameSeenSet = new HashSet<String>();
      
      // let's computed directly impacted models :
      // (whose update will trigger update of indirectly impacted ones, recursively)
      // LATER OPT rather not recursively but in one step (requires disable eventing),
      // by mongo query (BUT incomplete as is), or reverse index... ?
      List<DCModelBase> modelsWithImpactedGlobalFields = new ArrayList<DCModelBase>();
      List<DCModelBase> modelsWithOnlyImpactedSubresourceFields = new ArrayList<DCModelBase>();
      /*for (DCModelBase existingModel : project.getModels()) { // including (non overriden) visible projects'
         // adding directly impacted models :
         // (OR LATER disable eventing and add globally impacted models in one step)
         if (impactsGlobalFields(modelOrMixin, existingModel)) {
            modelsWithImpactedGlobalFields.add(existingModel);
         } else if (impactsSubresourceFields(modelOrMixin, existingModel)) {
            modelsWithOnlyImpactedSubresourceFields.add(existingModel);
         }
      }*/
      List<DCProject> projectsSeeingModel = dataModelAdminService.getProjectsSeeingModel(modelOrMixin);
      for (DCProject p : projectsSeeingModel) {
         for (DCModelBase existingModel : p.getModels()) { // including (non overriden) visible projects'
            if (!modelOrMixinAbsoluteNameSeenSet.add(existingModel.getAbsoluteName())) {
               continue; // already seen
            }
            // adding directly impacted models :
            // (OR LATER disable eventing and add globally impacted models in one step)
            if (impactsGlobalFields(modelOrMixin, existingModel)) {
               modelsWithImpactedGlobalFields.add(existingModel);
            } else if (impactsSubresourceFields(modelOrMixin, existingModel)) {
               modelsWithOnlyImpactedSubresourceFields.add(existingModel);
            }
         }
      }
      
      /////////////////////////
      // UPDATE GLOBAL FIELDS
      // let's reset impacted model caches, so that they will be updated :
      for (DCModelBase modelsWithImpactedGlobalField : modelsWithImpactedGlobalFields) {
         modelsWithImpactedGlobalField.resetGlobalCaches();
      }
      ///modelOrMixin.resetGlobalCaches(); // TODO ... including this one (in case it referred to itself ??) NOO would have to be repersisted
      if (logger.isDebugEnabled() && !modelsWithImpactedGlobalFields.isEmpty()) {
         logger.debug("updateDirectlyImpactedModels of " + modelOrMixin.getAbsoluteName()
               + " : " + modelsWithImpactedGlobalFields.stream().map(m -> {
                  return m.getAbsoluteName() + " (" + m.getVersion() + ")";
               }).collect(Collectors.toList()));
      }
      
      for (DCModelBase modelWithImpactedGlobalFields : modelsWithImpactedGlobalFields) {
         new SimpleRequestContextProvider<DCResource>() { // set context project beforehands :
            protected DCResource executeInternal() throws ResourceException {
               // retry loop if resolvable exception (ex. obsolete) :
               DCResource existingImpactedModelResource =  null;
               Exception nonResolvableObsoleteEx = null;
               int i = 0;
               for (; i < 100; i++) {
                  
               try {
                  // getting existing to get its version (required to update it) :
                  existingImpactedModelResource = resourceService.getIfVersionDiffers(
                        mrMappingService.buildModelUri(modelWithImpactedGlobalFields), "dcmo:model_0", -1l);
                  long currentVersion = existingImpactedModelResource.getVersion();
                  mrMappingService.modelToResource(modelWithImpactedGlobalFields,
                        existingImpactedModelResource); // beware, copies version !
                  existingImpactedModelResource.setVersion(currentVersion); // otherwise obsolete ex. 89!=91 IN ALL ITERATIONS
                  // TODO LATER check if consistency still valid
                  return resourceService.createOrUpdate(existingImpactedModelResource, "dcmo:model_0", false, true, true);
                  
               } catch (ResourceNotFoundException ex) {
                  logger.error("ResourceNotFoundException (ex. bad project though that should not happen here anymore) "
                        + "while repersisting model with impacted global fields "
                        + modelWithImpactedGlobalFields.getAbsoluteName() + " by change in model "
                        + modelOrMixin.getAbsoluteName(), ex);
                  return null;
               } catch (ResourceObsoleteException ex) {
                  nonResolvableObsoleteEx = ex;
                  String msg = "ResourceObsoleteException (" + existingImpactedModelResource.getVersion()
                        + "!=" + resourceService.getIfVersionDiffers(mrMappingService
                              .buildModelUri(modelWithImpactedGlobalFields), "dcmo:model_0", -1l).getVersion()
                        + ") while repersisting model with impacted global fields "
                        + modelWithImpactedGlobalFields.getAbsoluteName() + " by change in model "
                        + modelOrMixin.getAbsoluteName() + " (try " + i + ")";
                  logger.error(msg);
                  // let's try again in another loop iteration
               } catch (AccessDeniedException ex) {
                  String msg = "AccessDeniedException ex. frozen model (or duplicate gotten "
                        + "from wrong project i.e. implict fork though that should not happen here anymore) "
                        + "while repersisting model with impacted global fields "
                        + modelWithImpactedGlobalFields.getAbsoluteName() + " by change in model "
                        + modelOrMixin.getAbsoluteName();
                  logger.error(msg, ex);
                  throw new RuntimeException(msg, ex);
               } catch (Exception ex) {
                  String msg = "Unknown error while repersisting model with impacted global fields "
                        + modelWithImpactedGlobalFields.getAbsoluteName() + " by change in model "
                        + modelOrMixin.getAbsoluteName();
                  logger.error(msg, ex);
                  throw new RuntimeException(msg, ex);
               }
               
               }
               String msg = "ResourceObsoleteException while repersisting model with impacted global fields "
                     + modelWithImpactedGlobalFields.getAbsoluteName() + " by change in model "
                     + modelOrMixin.getAbsoluteName() + " after " + i + " tries";
               logger.error(msg, nonResolvableObsoleteEx);
               throw new RuntimeException(msg, nonResolvableObsoleteEx);
            }
         }.execInContext(new ImmutableMap.Builder<String, Object>()
               .put(DCRequestContextProvider.PROJECT, modelWithImpactedGlobalFields.getProjectName()).build());
      }

      /////////////////////////
      // UPDATE SUBRESOURCE INDEXES
      // TODO LATER modelsWithOnlyImpactedSubresourceFields, in projects that see this one...
   }

   /**
    * Impacts global DCFields contained in a DCModel (which requires to 1. update
    * 1. persist them and 3. ensure their indexes)
    * ex. cityFR directly refers to city through mixins, city to country
    * through a (possibly embedded !) resource field...
    * @param modelOrMixin
    * @param existingModel
    * @return false if same, indirect or not at all
    */
   private boolean impactsGlobalFields(DCModelBase referredModel, DCModelBase existingModel) {
      if (existingModel.getMixinNames().contains(referredModel.getName())
            && !existingModel.getName().equals(referredModel.getName())) { // not if same else recursive !
      ///if (existingModel.getGlobalMixinNames().contains(modelOrMixin.getName())) {
         return true;
      }
      return false;
   }

   /**
    * TODO LATER Impacts subresource fields (which requires to ensure their indexes)
    * @param referredModel
    * @param existingModel
    * @return
    */
   private boolean impactsSubresourceFields(DCModelBase referredModel, DCModelBase existingModel) {
      // TODO LATER for indexes also in subresources (ex. dcmo:fields refers to dcmf:Field_0) :
      /*for (DCField field : existingModel.getFieldMap().values()) {
         if ("resource".equals(field.getType())) {
            String resourceType = ((DCResourceField) field).getResourceType();
            DCModelBase linkedModel = dataModelService.getModelBase(resourceType);
            if (linkedModel != null) {
               // TODO TODO if used as partial copy (embedded resource and not link) AND
               if (referredModel.getName().equals(linkedModel.getName())) {
               //if (indirectlyRefersTo(linkedModel, existingModel)) {
                  return true;
               }
            } // else wrong, TODO LATER check at computing time OR BETTER make resourceType a link (DCResourceField) 
         }
      }*/
      return false;
   }

   /**
    * Checks that model resource project is current one
    * @param r
    * @throws AbortOperationEventException
    */
   private void handleModelResourceCreatedOrUpdated(DCResource r) throws AbortOperationEventException {
      try {
         DCModelBase modelOrMixin = mrMappingService.toModelOrMixin(r); // checks project !
         // NB. r has been cleaned up / enriched with up-to-date computed fields
         // (globalFields, pointOfViewAbsoluteName...) before in ABOUT_TO_CREATE/UPDATE
         // step
         createOrUpdate(modelOrMixin, r);
      } catch (AbortOperationEventException aoeex) {
         throw aoeex;
      } catch (ResourceException rex) {
         // abort else POSTer won't know that his model can't be used (to POST resources)
         throw new AbortOperationEventException(rex);
      } catch (Throwable t) {
         // abort else POSTer won't know that his model can't be used (to POST resources)
         throw new AbortOperationEventException(new ResourceException(
               "Unknown error while converting or enriching to model or mixin, "
               + "aborting POSTing resource", t, r, dataModelAdminService.getProject()));
      }
   }

   /**
    * Checks that model resource project is current one
    * @param r
    * @throws AbortOperationEventException
    */
   private void handleModelResourceDeleted(DCResource r) throws AbortOperationEventException {
      String typeName = (String) r.get("dcmo:name");
      DCProject project;
      try {
         project = mrMappingService.getAndCheckModelResourceProject(r);
      } catch (Throwable e) {
         // TODO abort to say error to client ?
         // TODO save state in model or wrapper stored in mongo ?? log all events in mongo ???
         // TODO try to restore previousDocument ???
         throw new AbortOperationEventException(new ResourceException("Aborting as asked for in done event "
               + "of model or mixin " + typeName, e, r, dataModelAdminService.getProject()));
      }
      
      DCModelBase modelOrMixin = project.getModel(typeName);
      if (modelOrMixin == null) {
         // happens at least when db is corrupted, TODO explode if !devmode
         logger.error("Can't find to be removed DCModelBase " + r
               + " whose resource has been deleted");
         return;
      }

      // TODO LATER check that is not used as mixin in any other models (& mixins),
      // else require that they be deleted first !
      project.removeLocalModel(typeName);
      
      try {
         eventService.triggerEvent(new ModelDCEvent(ModelDCEvent.DELETED,
               ModelDCEvent.MODEL_DEFAULT_BUSINESS_DOMAIN, modelOrMixin, null));
      } catch (AbortOperationEventException aoeex) {
         throw aoeex;
      } catch (Throwable e) {
         // TODO abort to say error to client ?
         // TODO save state in model or wrapper stored in mongo ?? log all events in mongo ???
         // TODO try to restore previousDocument ???
         throw new AbortOperationEventException(new ResourceException("Aborting as asked for in done event "
               + "of model or mixin " + modelOrMixin, e, r, dataModelAdminService.getProject()));
      }
   }

}
