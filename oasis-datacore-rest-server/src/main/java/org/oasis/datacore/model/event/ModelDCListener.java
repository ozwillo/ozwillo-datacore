package org.oasis.datacore.model.event;

import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.rest.server.event.AbortOperationEventException;
import org.oasis.datacore.rest.server.event.DCEvent;
import org.oasis.datacore.rest.server.event.DCEventListener;
import org.oasis.datacore.rest.server.event.DCEventListenerBase;
import org.oasis.datacore.sample.ResourceModelIniter;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Updates collection & index etc. once model or mixin has been updated (or drops them) 
 * 
 * @author mdutoo
 *
 */
public class ModelDCListener extends DCEventListenerBase implements DCEventListener {
   
   @Autowired
   private ResourceModelIniter modelIndexService; // TODO refactor methods there
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
         if (modelOrMixin.isStorage()) { // TODO also on changes of index conf on inherited storage and stored models
            modelIndexService.ensureCollectionAndIndices(modelOrMixin, false);
         }
         break;
      case ModelDCEvent.DELETED :
         me = (ModelDCEvent) event;
         modelOrMixin = me.getModel(); // TODO or get it from name since now persisted ???
         if (modelOrMixin.isStorage()) { // TODO also on changes of index conf on inherited storage and stored models
            modelIndexService.cleanModels(modelOrMixin);
         }
         break;
      }
   }

}
