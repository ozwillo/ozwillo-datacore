package org.oasis.datacore.model.event;

import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.model.resource.ModelResourceMappingService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.server.event.AbortOperationEventException;
import org.oasis.datacore.rest.server.event.DCEvent;
import org.oasis.datacore.rest.server.event.DCEventListener;
import org.oasis.datacore.rest.server.event.DCResourceEvent;
import org.oasis.datacore.rest.server.event.DCResourceEventListener;
import org.oasis.datacore.rest.server.resource.ResourceException;
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
   private DataModelServiceImpl dataModelService;
   @Autowired
   private ModelResourceMappingService mrMappingService;

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
         DCResourceEvent re = (DCResourceEvent) event;
         DCResource r = re.getResource();
         try {
            mrMappingService.toModelOrMixin(r); // enriches r with fields computed by DCModelBase
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
         // TODO check that is not used as mixin in any other models (& mixins) !
         handleModelResourceDeleted(typeName);
         // TODO if (used as) mixin, do it also for all impacted models (& mixins) !
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
      dataModelService.addModel(modelOrMixin);
      
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
   }

   private void handleModelResourceCreatedOrUpdated(DCResource r) throws AbortOperationEventException {
      try {
         DCModelBase modelOrMixin = mrMappingService.toModelOrMixin(r);
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
      /*if (isModel) {
         dataModelService.removeModel(typeName);
      } else {
         dataModelService.removeMixin(typeName);
      }*/
      dataModelService.removeModel(typeName);
      
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
