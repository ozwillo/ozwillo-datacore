package org.oasis.datacore.rest.server.event;

import java.util.Map;

import org.oasis.datacore.rest.api.DCResource;


/**
 * Resource event.
 * Extends DCPropertiesEvent to make it easier to customize it, but try to keep properties null
 * for better performances.
 * 
 * @author mdutoo
 *
 */
public class DCResourceEvent extends DCPropertiesEvent {

   enum Types{
      ABOUT_TO_CREATE(RESOURCE_PREFIX + "aboutToCreate"),
      CREATED(RESOURCE_PREFIX + "created"),
      READ(RESOURCE_PREFIX + "created"), // ???
      ABOUT_TO_UPDATE(RESOURCE_PREFIX + "aboutToUpdate"),
      UPDATED(RESOURCE_PREFIX + "created"),
      ABOUT_TO_DELETE(RESOURCE_PREFIX + "aboutToDelete"),
      DELETED(RESOURCE_PREFIX + "deleted"); // ??
      String name;
      Types(String s) { name = s; }
   }

   public static final String RESOURCE_PREFIX = "resource.";

   private DCResource resource;
   /** in case of aboutToXXX event */
   private DCResource previousResource;

   /**
    * 
    * @param type
    * @param topic
    * @param resource
    * @param properties try to keep it null for better performances
    */
   public DCResourceEvent(Types type, String topic,
         DCResource resource, DCResource previousResource, Map<String,Object> properties) {
      super(type.name, topic, properties);
      this.resource = resource;
      this.previousResource = previousResource;
   }

   public DCResourceEvent(Types type, String topic, DCResource resource, DCResource previousResource) {
      this(type, topic, resource, previousResource, null);
   }

   public DCResourceEvent(Types type, String topic, DCResource resource) {
      this(type, topic, resource, null, null);
   }

   public DCResource getResource() {
      return resource;
   }

   public DCResource getPreviousResource() {
      return previousResource;
   }

   @Override
   protected StringBuilder toStringBuilder() {
      StringBuilder sb = super.toStringBuilder();
      sb.append(" about ");
      sb.append(resource.getUri());
      return sb;
   }

}
