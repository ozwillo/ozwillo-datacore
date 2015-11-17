package org.oasis.datacore.rest.server.event;


public interface DCEventListener {

   /**
    * 
    * @param event
    * @throws AbortOperationEventException if event-triggering operation should
    * be aborted (other exceptions are caught and operation proceeds as usual)
    */
   public void handleEvent(DCEvent event) throws AbortOperationEventException;

   
   /**
    * To give the listener impl a chance to ex. register itself to eventService,
    * - which must have been autowired first, using AutowireCapableBeanFactory
    * see http://stackoverflow.com/questions/3813588/how-to-inject-dependencies-into-a-self-instantiated-object-in-spring
    * @param eventService
    */
   public void init();
   
}
