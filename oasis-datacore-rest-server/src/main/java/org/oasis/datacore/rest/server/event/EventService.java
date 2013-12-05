package org.oasis.datacore.rest.server.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component // TODO @Service ??
public class EventService {
   
   private static final org.slf4j.Logger logger = LoggerFactory.getLogger(EventService.class);
         
   private Map<String,List<DCEventListener>> topic2EventListenerMap = new HashMap<String,List<DCEventListener>>();

   /**
    * Dispatches the given event to the registered listeners.
    * @param event
    * @throws AbortOperationEventException
    */
   public void triggerEvent(DCEvent event) throws AbortOperationEventException {
      List<DCEventListener> listeners = topic2EventListenerMap.get(event.getTopic());
      if (listeners == null) {
         return;
      }
      this.triggerEvent(event, listeners);
   }
   
   /**
    * Dispatches the given event to the given listeners,
    * allows to use the given listener list instead of the EventService's registry.
    * @param event
    * @param listeners
    * @throws AbortOperationEventException
    */
   public void triggerEvent(DCEvent event, List<DCEventListener> listeners) throws AbortOperationEventException {
      for (DCEventListener listener : listeners) {
         try {
            listener.handleEvent(event);
         } catch (AbortOperationEventException aoeex) {
            throw aoeex;
         } catch (Exception e) {
            logger.warn("Error while event " + event + " handled by listener " + listener);
         }
      }
   }

   /**
    * not thread safe, but not a problem if only called at init
    * @param dcInitIriEventListener 
    * @param name
    */
   public void register(DCEventListener eventListener, String eventTypeName) {
      List<DCEventListener> listeners = this.topic2EventListenerMap.get(eventTypeName);
      if (listeners == null) {
         listeners = new ArrayList<DCEventListener>();
         this.topic2EventListenerMap.put(eventTypeName, listeners);
      }
      listeners.add(eventListener);
   }
   public void register(DCEventListener eventListener, String ... eventTypeNames) {
      for (String eventTypeName : eventTypeNames) {
         this.register(eventListener, eventTypeName);
      }
   }
   
}
