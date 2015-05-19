package org.oasis.datacore.model.event;

import org.oasis.datacore.core.entity.DatabaseSetupService;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.rest.server.event.AbortOperationEventException;
import org.oasis.datacore.rest.server.event.DCEvent;
import org.oasis.datacore.rest.server.event.DCEventListener;
import org.oasis.datacore.rest.server.event.DCEventListenerBase;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Updates collection & index etc. once model or mixin has been updated (or drops them) 
 * 
 * @author mdutoo
 *
 */
public class ModelDCListener extends DCEventListenerBase implements DCEventListener {
   
   @Autowired
   private DatabaseSetupService modelIndexService; // TODO refactor methods there
   @Autowired
   private DataModelServiceImpl dataModelService;

   public ModelDCListener() {
      super();
   }

   public ModelDCListener(String businessDomain) {
      super(businessDomain);
   }

   @Override
   public void handleEvent(DCEvent event) throws AbortOperationEventException {
      switch (event.getType()) {
      case ModelDCEvent.CREATED :
      case ModelDCEvent.UPDATED :
         ModelDCEvent me = (ModelDCEvent) event;
         DCModelBase modelOrMixin = me.getModel(); // TODO or get it from name since now persisted ???
         modelIndexService.ensureCollectionAndIndices(modelOrMixin, false); // applied on its storage model
         // NB. changes of index conf on inheriting (several levels) storage models
         // should be dispatched in ModelResourceDCListener.updateDirectlyImpactedModels() fashion
         break;
      case ModelDCEvent.DELETED :
         me = (ModelDCEvent) event;
         modelOrMixin = me.getModel(); // TODO or get it from name since now persisted ???
         modelIndexService.cleanModel(modelOrMixin); // if not storage, cleans data in its storage model
         // NB. about changes of index conf on inheriting (several levels) storage models :
         // they should be already done when this model has been removed from those model's mixins
         // (and otherwise should be dispatched in ModelResourceDCListener.updateDirectlyImpactedModels() fashion)
         break;
      }
   }

}
