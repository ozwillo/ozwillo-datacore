package org.oasis.datacore.model.event;

import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.rest.server.event.AbortOperationEventException;
import org.oasis.datacore.rest.server.event.DCEvent;
import org.oasis.datacore.rest.server.event.DCEventListener;
import org.oasis.datacore.rest.server.event.DCEventListenerBase;
import org.oasis.datacore.sample.ResourceModelIniter;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Updates index etc. once model or mixin has been updated
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
         break;
      default :
         return;
      }
      ModelDCEvent me = (ModelDCEvent) event;
      DCModelBase modelOrMixin = me.getModel(); // TODO or get it from name since now persisted ???
      
      if (modelOrMixin instanceof DCModel) {
         modelIndexService.ensureCollectionAndIndices((DCModel) modelOrMixin, false);
      }
   }

}
