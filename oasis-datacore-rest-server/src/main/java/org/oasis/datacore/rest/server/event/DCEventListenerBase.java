package org.oasis.datacore.rest.server.event;

import javax.annotation.PostConstruct;

import org.oasis.datacore.rest.server.resource.ResourceService;
import org.oasis.datacore.rest.server.resource.UriService;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * TODO TODO or store in Model event listener impls' confs, possibly shared & serializable ?!? YES
 * 
 * * Extend it and impl it
 * * either instantiate it using @PostConstruct
 * * or manually but then init it (to register it) : init()
 * 
 * @author mdutoo
 *
 */
public abstract class DCEventListenerBase implements DCEventListener {

   /** ex. for Resource event listeners, modelType ; null means all events */
   private String topic = null;
   
   @Autowired
   protected EventService eventService;
   @Autowired
   protected ResourceService resourceService;
   @Autowired
   protected UriService uriService;
   
   public DCEventListenerBase() {
      
   }
   
   /** helper to create it programmatically (in tests...) */
   public DCEventListenerBase(String topic) {
      this.setTopic(topic);
   }

   /**
    * Don't call it manually (it requires being autowired), rather call init(eventService)
    */
   @PostConstruct
   public void init() {
      eventService.register(this, this.getTopic());
   }

   public String getTopic() {
      return topic;
   }

   public void setTopic(String topic) {
      this.topic = topic;
   }

   public void setEventService(EventService eventService) {
      this.eventService = eventService;
   }

   public void setResourceService(ResourceService resourceService) {
      this.resourceService = resourceService;
   }

   /** to check if already autowired */
   public EventService getEventService() {
      return eventService;
   }

}
