package org.oasis.datacore.model.event;

import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.model.resource.ModelResourceMappingService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.server.event.AbortOperationEventException;
import org.oasis.datacore.rest.server.event.DCEvent;
import org.oasis.datacore.rest.server.event.DCEventListener;
import org.oasis.datacore.rest.server.event.DCResourceEvent;
import org.oasis.datacore.rest.server.event.DCResourceEventListener;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Inits from Resource and replaces DCModel (or removes it)
 * TODO TODO also at startup !!
 * TODO LATER checks BEFOREHANDS for update compatibility, else abort using AbortOperationEventException
 * TODO LATER also update models (& mixins) that reuse the updated one, and abort on delete !
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
   
   private boolean isModel;

   public ModelResourceDCListener() {
      super();
   }

   public ModelResourceDCListener(String resourceType) {
      super(resourceType);
   }

   @Override
   public void handleEvent(DCEvent event) throws AbortOperationEventException {
      switch (event.getType()) {
      case DCResourceEvent.CREATED :
      case DCResourceEvent.UPDATED :
         DCResourceEvent re = (DCResourceEvent) event;
         DCResource r = re.getResource();
         handleModelResourceCreatedOrUpdated(r);
         // TODO if (used as) mixin, do it also for all impacted models (& mixins) !
         return;
      case DCResourceEvent.DELETED :
         re = (DCResourceEvent) event;
         r = re.getResource();
         String typeName = (String) r.get("dcmo:name");
         // TODO check that is not used as mixin in any other models (& mixins) !
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
      DCModelBase previousModel = dataModelService.getMixin(modelOrMixin.getName());

      String aboutToEventType;
      if (previousModel == null) {
         // new model
         aboutToEventType = ModelDCEvent.ABOUT_TO_CREATE;
      } else {
         // TODO also update all impacted models !!
         aboutToEventType = ModelDCEvent.ABOUT_TO_UPDATE;
      }

      try {
         eventService.triggerEvent(new ModelDCEvent(aboutToEventType,
               ModelDCEvent.MODEL_DEFAULT_BUSINESS_DOMAIN, modelOrMixin, previousModel));
      } catch (Throwable e) {
         // TODO abort to say error to client ?
         // TODO save state in model or wrapper stored in mongo ?? log all events in mongo ???
         // TODO try to restore previousDocument ???
         throw new AbortOperationEventException("Aborting as asked for in aboutTo event "
               + "of model or mixin " + modelOrMixin, e);
      }
      
      if (isModel) {
         dataModelService.addModel((DCModel) modelOrMixin);
      } else {
         dataModelService.addMixin(modelOrMixin);
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
         throw new AbortOperationEventException("Aborting as asked for in done event "
               + "of model or mixin " + modelOrMixin, e);
      }
   }

   private void handleModelResourceCreatedOrUpdated(DCResource r) throws AbortOperationEventException {
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

      try {
         mrMappingService.resourceToModelOrMixin(modelOrMixin, r);
      } catch (ResourceParsingException rpex) {
         throw new AbortOperationEventException(rpex);
      }
      
      createOrUpdate(modelOrMixin);
   }

   private void handleModelResourceDeleted(String typeName) throws AbortOperationEventException {
      DCModelBase modelOrMixin = dataModelService.getMixin(typeName);
      if (isModel) {
         dataModelService.removeModel(typeName);
      } else {
         dataModelService.removeMixin(typeName);
      }
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

   @Override
   public void setTopic(String topic) {
      super.setTopic(topic);
      isModel = "dcmo:model_0".equals(topic);
   }

}
