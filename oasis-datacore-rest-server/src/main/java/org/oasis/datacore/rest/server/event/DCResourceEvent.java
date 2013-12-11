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

   public enum Types{
      /** resourceService.build(resource) */
      ABOUT_TO_BUILD(RESOURCE_PREFIX + "aboutToBuild"),
      /** about to POST */
      ABOUT_TO_CREATE(RESOURCE_PREFIX + "aboutToCreate"),
      /** POSTed */
      CREATED(RESOURCE_PREFIX + "created"),
      /** GET */
      READ(RESOURCE_PREFIX + "read"), // ???
      /** about to PUT */
      ABOUT_TO_UPDATE(RESOURCE_PREFIX + "aboutToUpdate"),
      /** PUT */
      UPDATED(RESOURCE_PREFIX + "updated"),
      /** about to DELETE */
      ABOUT_TO_DELETE(RESOURCE_PREFIX + "aboutToDelete"),
      /** DELETEd */
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
    * @param resourceType
    * @param resource
    * @param properties try to keep it null for better performances
    */
   public DCResourceEvent(Types type, String resourceType,
         DCResource resource, DCResource previousResource, Map<String,Object> properties) {
      super(type.name, resourceType, properties);
      this.resource = resource;
      this.previousResource = previousResource;
   }

   /**
    * Creates a new ResourceEvent of the given ResourceEvent type targeting the
    * given resource's model type, with the given current and previous resource
    * and no event properties.
    * @param type
    * @param resource
    * @param previousResource
    */
   public DCResourceEvent(Types type, DCResource resource, DCResource previousResource) {
      this(type, resource.getModelType(), resource, previousResource, null);
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
