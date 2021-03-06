package org.oasis.datacore.rest.server.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.server.event.DCResourceEvent.Types;
import org.oasis.datacore.rest.server.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component // TODO @Service ??
public class EventService {
   
   private static final Logger logger = LoggerFactory.getLogger(EventService.class);

   /** to autowire programmatically-created listeners */
   @Autowired
   private AutowireCapableBeanFactory beanFactory;
   
   private Map<String,List<DCEventListener>> topic2EventListenerMap = new HashMap<String,List<DCEventListener>>();

   
   /**
    * Dispatches the given event to the registered listeners.
    * @param event
    * @throws AbortOperationEventException, or its enclosed Exception if any & if there is no message
    */
   public void triggerEvent(DCEvent event) throws AbortOperationEventException, Throwable {
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
    * @throws AbortOperationEventException, or its enclosed Exception if any & if there is no message
    */
   public void triggerEvent(DCEvent event, List<DCEventListener> listeners)
         throws AbortOperationEventException, Throwable {
      for (DCEventListener listener : listeners) {
         try {
            listener.handleEvent(event);
         } catch (AbortOperationEventException aoeex) {
            if (aoeex.isWrapperException()) {
               throw aoeex.getCause();
            }
            throw aoeex;
         } catch (Exception e) {
            logger.warn("Error while event " + event + " handled by listener "
                  + listener + " : " + e.getMessage());
            if (logger.isDebugEnabled()) {
               logger.debug("   details : ", e);
            }
         }
      }
   }


   /**
    * Resource event-specific trigger
    * TODO move to ResourceEventService (better than ResourceService) ?
    * @param aboutToEventType
    * @param resource
    * @throws ResourceException when a triggered listener aborted it with a ResourceException cause
    * @throws RuntimeException when a triggered listener aborted it with another cause
    */
   public void triggerResourceEvent(Types aboutToEventType, DCResource resource) throws ResourceException {
      this.triggerResourceEvent(aboutToEventType, resource, null);
   }
   /**
    * Resource event-specific trigger
    * TODO move to ResourceEventService (better than ResourceService) ?
    * @param aboutToEventType
    * @param resource
    * @param previousResource
    * @throws ResourceException
    * @throws ResourceException when a triggered listener aborted it with a ResourceException cause
    * @throws RuntimeException when a triggered listener aborted it with another cause
    */
   public void triggerResourceEvent(Types aboutToEventType,
         DCResource resource, DCResource previousResource) throws ResourceException {
      try {
         this.triggerEvent(new DCResourceEvent(aboutToEventType, resource, previousResource));
      } catch (ResourceException rex) {
         throw rex;
      } catch (AccessDeniedException adex) {
         throw adex;
      } catch (Throwable e) { // includes regular AbortOperationEventException
         throw new RuntimeException("Aborted in " + aboutToEventType + " resource "
               + resource.getUri(), e); // TODO ResourceAbortException ??
      }
   }
   
   
   /** helper to init
    * TODO replace it by in DCModelBase.register*/
   /*public DCEventListenerBase initialize(DCEventListenerBase listener) {
      listener.setEventService(this);
      listener.setResourceService(resourceService);
      return listener;
   }*/
   
   /**
    * not thread safe, but not a problem if only called at init
    * @param dcInitIriEventListener 
    * @param name
    */
   public void register(DCEventListener eventListener, String eventTypeName) {
      if (eventTypeName == null || eventTypeName.trim().isEmpty()) {
         throw new IllegalArgumentException("Can't register listener "
               + eventListener + " against an empty event / topic");
      }
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
   
   /**
    * To autowire the given listener then call its init to give it a chance to register
    * (call it only if listener is created programmatically and not by Spring annotation)
    * @param eventService
    */
   public void init(DCEventListener eventListener) {
      beanFactory.autowireBean(eventListener);
      eventListener.init();
   }
   
}
