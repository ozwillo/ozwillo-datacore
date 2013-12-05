package org.oasis.datacore.rest.server.event;


public interface DCEventListener {

   /**
    * 
    * @param event
    * @throws AbortOperationEventException if event-triggering operation should
    * be aborted (other exceptions are caught and operation proceeds as usual)
    */
   public void handleEvent(DCEvent event) throws AbortOperationEventException;
   
}
