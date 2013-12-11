package org.oasis.datacore.rest.server.event;


/**
 * Raise it in an EventListener impl to abort the event-triggering operation.
 * Other exceptions are caught and operation proceeds as usual.
 * Only really useful in aboutToXXX events.
 * 
 * @author mdutoo
 *
 */
public class AbortOperationEventException extends Exception {
   private static final long serialVersionUID = -8536958346614113087L;

   /**
    * Allows to provide the "real" exception (if not null) to be unwrapped
    * once EventService has aborted 
    * @param cause
    */
   public AbortOperationEventException(Throwable cause) {
      super(cause);
   }
   /**
    * IF NULL MESSAGE allows to provide the "real" exception (if not null) to be unwrapped
    * once EventService has aborted
    * @param message
    * @param cause
    */
   public AbortOperationEventException(String message, Throwable cause) {
      super(message, cause);
   }

   public AbortOperationEventException(String message) {
      super(message);
   }

}
