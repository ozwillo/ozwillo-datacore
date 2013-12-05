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

   public AbortOperationEventException(String message, Throwable cause) {
      super(message, cause);
   }

   public AbortOperationEventException(String message) {
      super(message);
   }

}
