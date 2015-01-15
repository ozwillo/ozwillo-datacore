package org.oasis.datacore.model.event;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.model.DCResourceField;
import org.oasis.datacore.model.resource.ModelResourceMappingService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.server.event.AbortOperationEventException;
import org.oasis.datacore.rest.server.event.DCEvent;
import org.oasis.datacore.rest.server.event.DCEventListener;
import org.oasis.datacore.rest.server.event.DCResourceEvent;
import org.oasis.datacore.rest.server.event.DCResourceEventListener;
import org.oasis.datacore.rest.server.resource.ResourceEntityMapperService;
import org.oasis.datacore.rest.server.resource.ResourceException;
import org.oasis.datacore.rest.server.resource.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Inits from Resource and replaces DCModel (or removes it)
 * TODO TODO also at startup !!
 * TODO LATER checks BEFOREHANDS for update compatibility, else abort using AbortOperationEventException
 * TODO LATER also update models (& mixins) that reuse the updated one
 * (DispatchModelUpdateListener that on ModelDCEvent.UPDATED looks for inheriting models and recreates
 * them, OR BETTER (transactional ??) sync task engine that avoids doing it in the wrong order
 * or even merges tasks) , and abort on delete !
 * TODO move most to ModelResourceMappingService & Model(Admin)Service !
 * 
 * @author mdutoo
 *
 */
public class ModelResourceDCListener extends DCResourceEventListener implements DCEventListener {

   @Autowired
   private DCModelService dataModelService;
   @Autowired
   private DataModelServiceImpl dataModelAdminService;
   @Autowired
   private ModelResourceMappingService mrMappingService;
   @Autowired
   private ResourceService resourceService;
   @Autowired
   private ResourceEntityMapperService reMappingService;

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
            // which makes toModelOrMixin fail on ex. maxScan (null because unset) :
            reMappingService.entityToResource(resourceService.resourceToEntity(r), r);
            
            // map to model & check : 
            DCModelBase modelOrMixin = mrMappingService.toModelOrMixin(r);
            // clean resource / enrich with up-to-date computed fields
            // (pointOfViewAbsoluteName, globalFields...) :
            mrMappingService.modelToResource(modelOrMixin, r); 
            // model consistency check : (NOT on startup else order of load can make it fail)
            mrMappingService.checkModelOrMixin(modelOrMixin, r);
            
         } catch (ResourceException rex) {
            // abort else POSTer won't know that his model can't be used (to POST resources)
            throw new AbortOperationEventException(rex);
         } catch (Throwable t) {
            // abort else POSTer won't know that his model can't be used (to POST resources)
            throw new AbortOperationEventException("Unknown error while converting "
                  + "or enriching to model or mixin, aborting POSTing resource", t);
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
         String typeName = (String) r.get("dcmo:name");
         // TODO LATER check that is not used as mixin in any other models (& mixins),
         // else require that they be deleted first !
         handleModelResourceDeleted(typeName);
         return;
      }
   }

   /** TODO move to DCModelService */
   public void createOrUpdate(DCModelBase modelOrMixin) throws AbortOperationEventException {
      // replacing it :
      // TODO TODO RATHER ALL AT ONCE to avoid
      // * having an inconsistent set of models when ResourceService parses Resources
      // * and having one model that refers to another that is not yet there compute its caches too early
      // * AND HAVING OBSOLETE INDEXES !
      DCModelBase previousModel = dataModelService.getModelBase(modelOrMixin.getName());

      String aboutToEventType;
      if (previousModel == null) {
         // new model
         aboutToEventType = ModelDCEvent.ABOUT_TO_CREATE;
      } else {
         // TODO also update all impacted models !!
         aboutToEventType = ModelDCEvent.ABOUT_TO_UPDATE;
      }

      try {
         // let's update storage (index...) :
         eventService.triggerEvent(new ModelDCEvent(aboutToEventType,
               ModelDCEvent.MODEL_DEFAULT_BUSINESS_DOMAIN, modelOrMixin, previousModel));
      } catch (Throwable e) {
         // TODO abort to say error to client ?
         // TODO save state in model or wrapper stored in mongo ?? log all events in mongo ???
         // TODO try to restore previousDocument ???
         throw new AbortOperationEventException("Aborting as asked for in aboutTo event "
               + "of model or mixin " + modelOrMixin, e);
      }
      
      // let's actually register the DCModel :
      dataModelAdminService.addModel(modelOrMixin);
      
      String doneEventType = (previousModel == null) ?
            ModelDCEvent.CREATED : ModelDCEvent.UPDATED;
      try {
         eventService.triggerEvent(new ModelDCEvent(doneEventType,
               ModelDCEvent.MODEL_DEFAULT_BUSINESS_DOMAIN, modelOrMixin, previousModel));
      } catch (Throwable e) {
         // TODO abort to say error to client ?
         // TODO save state in model or wrapper stored in mongo ?? log all events in mongo ???
         // TODO try to restore previousDocument ???
         throw new AbortOperationEventException("Aborting as asked for in done event "
               + "of model or mixin " + modelOrMixin, e);
      }

      if (doneEventType == ModelDCEvent.UPDATED) {
         // updating impacted models and their persistence :
         updateDirectlyImpactedModels(modelOrMixin);
      } // else creation which has no impact
   }

   private void updateDirectlyImpactedModels(DCModelBase modelOrMixin) {
      // let's computed directly impacted models :
      // (whose update will trigger update of indirectly impacted ones, recursively)
      // LATER OPT rather not recursively but in one step (requires disable eventing),
      // by mongo query (BUT incomplete as is), or reverse index... ?
      List<DCModelBase> impactedModels = new ArrayList<DCModelBase>();
      for (DCModelBase existingModel : dataModelService.getModels()) {
         // adding directly impacted models :
         // (OR LATER disable eventing and add globally impacted models in one step)
         if (directlyRefersTo(existingModel, modelOrMixin)) {
            impactedModels.add(existingModel);
         }
      }
      // let's reset impacted model caches, so that they will be updated :
      for (DCModelBase impactedModel : impactedModels) {
         impactedModel.resetGlobalCaches();
      }
      if (logger.isDebugEnabled() && !impactedModels.isEmpty()) {
         logger.debug("updateDirectlyImpactedModels of " + modelOrMixin.getName()
               + " : " + impactedModels.stream().map(impactedModel -> {
                  return impactedModel.getName() + " (" + impactedModel.getVersion() + ")";
               }).collect(Collectors.toList()));
      }
      ///modelOrMixin.resetGlobalCaches(); // TODO ... including this one (in case it referred to itself ??) NOO would have to be repersisted
      for (DCModelBase impactedModel : impactedModels) {
         try {
            // getting existing to get its version (required to update it) :
            DCResource existingImpactedModelResource = resourceService.getIfVersionDiffers(
                  mrMappingService.buildModelUri(impactedModel), "dcmo:model_0", -1l);
            this.mrMappingService.modelToResource(impactedModel, existingImpactedModelResource);
            // TODO LATER check if consistency still valid
            resourceService.createOrUpdate(existingImpactedModelResource, "dcmo:model_0", false, true, true);
         } catch (Exception ex) {
            logger.error("Unknown error while repersisting impacted model "
                  + impactedModel.getName() + " by change in model " + modelOrMixin.getName(), ex);
         }
      }
   }

   /**
    * ex. cityFR directly refers to city, city to country...
    * @param modelOrMixin
    * @param existingModel
    * @return false if same, indirect or not at all
    */
   private boolean directlyRefersTo(DCModelBase existingModel, DCModelBase referredModel) {
      if (existingModel.getMixinNames().contains(referredModel.getName())
            && !existingModel.getName().equals(referredModel.getName())) { // not if same else recursive !
      ///if (existingModel.getGlobalMixinNames().contains(modelOrMixin.getName())) {
         return true;
      }
      // also in subresources (ex. dcmo:fields refers to dcmf:Field_0) :
      for (DCField field : existingModel.getFieldMap().values()) {
         if ("resource".equals(field.getType())) {
            String resourceType = ((DCResourceField) field).getResourceType();
            DCModelBase linkedModel = dataModelService.getModelBase(resourceType);
            if (linkedModel != null) {
               // if used as partial copy (embedded resource and not link) AND
               if (referredModel.getName().equals(linkedModel.getName())) {
               //if (indirectlyRefersTo(linkedModel, existingModel)) {
                  return true;
               }
            } // else wrong, TODO LATER check at computing time OR BETTER make resourceType a link (DCResourceField) 
         }
      }
      return false;
   }

   private void handleModelResourceCreatedOrUpdated(DCResource r) throws AbortOperationEventException {
      try {
         DCModelBase modelOrMixin = mrMappingService.toModelOrMixin(r);
         // NB. r has been cleaned up / enriched with up-to-date computed fields
         // (globalFields, pointOfViewAbsoluteName...) before in ABOUT_TO_CREATE/UPDATE
         // step
         createOrUpdate(modelOrMixin);
      } catch (ResourceException rex) {
         // abort else POSTer won't know that his model can't be used (to POST resources)
         throw new AbortOperationEventException(rex);
      } catch (Throwable t) {
         // abort else POSTer won't know that his model can't be used (to POST resources)
         throw new AbortOperationEventException("Unknown error while converting "
               + "or enriching to model or mixin, aborting POSTing resource", t);
      }
   }

   private void handleModelResourceDeleted(String typeName) throws AbortOperationEventException {
      DCModelBase modelOrMixin = dataModelService.getModelBase(typeName);
      if (modelOrMixin == null) {
         // happens at least when db is corrupted, TODO explode if !devmode
         logger.error("Can't find to be removed DCModelBase " + typeName
               + " whose resource has been deleted");
         return;
      }
      /*if (isModel) {
         dataModelService.removeModel(typeName);
      } else {
         dataModelService.removeMixin(typeName);
      }*/
      dataModelAdminService.removeModel(typeName);
      
      try {
         eventService.triggerEvent(new ModelDCEvent(ModelDCEvent.DELETED,
               ModelDCEvent.MODEL_DEFAULT_BUSINESS_DOMAIN, modelOrMixin, null));
      } catch (Throwable e) {
         // TODO abort to say error to client ?
         // TODO save state in model or wrapper stored in mongo ?? log all events in mongo ???
         // TODO try to restore previousDocument ???
         throw new AbortOperationEventException("Aborting as asked for in done event "
               + "of model or mixin " + modelOrMixin, e);
      }
   }

}
