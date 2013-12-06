package org.oasis.datacore.rest.server.event;



/**
 * Only to provide typing when used from ex. DCModelBase
 * @author mdutoo
 *
 */
public abstract class DCResourceEventListener extends DCEventListenerBase {
   
   public DCResourceEventListener() {
      super();
   }

   public DCResourceEventListener(String resourceType) {
      super(resourceType);
   }

   /**
    * Shortcut to getTopic()
    * @return
    */
   public String getResourceType() {
      return this.getTopic();
   }

}
