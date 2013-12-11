package org.oasis.datacore.rest.server.event;

import org.oasis.datacore.core.meta.model.DCModelService;
import org.springframework.beans.factory.annotation.Autowired;



/**
 * Only to provide typing when used from ex. DCModelBase
 * @author mdutoo
 *
 */
public abstract class DCResourceEventListener extends DCEventListenerBase {
   
   @Autowired
   private DCModelService modelService;
   
   public DCResourceEventListener() {
      super();
   }

   public DCResourceEventListener(String resourceType) {
      super(resourceType);
   }

   @Override
   public void init() {
      super.init();
      //DCModel model = modelService.getModel(this.getResourceType());
      // TODO register also on inheriting types ?? or emit also to ancestor types ?!
   }

   /**
    * Shortcut to getTopic()
    * @return
    */
   public String getResourceType() {
      return this.getTopic();
   }
   /**
    * Shortcut to setTopic(), to allow setting it afterwards
    * @return
    */
   public void setResourceType(String resourceType) {
      this.setTopic(resourceType);
   }

}
