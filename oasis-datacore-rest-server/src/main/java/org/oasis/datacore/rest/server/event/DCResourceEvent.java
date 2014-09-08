package org.oasis.datacore.rest.server.event;

import org.oasis.datacore.rest.api.DCResource;


/**
 * Resource event.
 * Could
 * Extends DCPropertiesEvent to make it easier to customize it, but try to keep properties null
 * for better performances.
 * 
 * TODO move event to generic package, and this to resource.event
 * 
 * @author mdutoo
 *
 */
public class DCResourceEvent extends DCEvent { // NB. DCPropertiesEvent is overkill here

   // NB. ENUM is bad for that because event types should be enrichable
   // for businesses beyond ResourceService's, and because since DCEvent
   // works with a String eventType there has to be ENUM parsing which
   // is not optimal, because using values() :
   // http://stackoverflow.com/questions/17481016/how-to-convert-string-value-into-enum-in-java
   public enum Types{
      /** resourceService.build(resource) */
      ABOUT_TO_BUILD(DCResourceEvent.ABOUT_TO_BUILD),
      /** about to POST */
      ABOUT_TO_CREATE(DCResourceEvent.ABOUT_TO_CREATE),
      /** POSTed */
      CREATED(DCResourceEvent.CREATED),
      /** GET */
      READ(DCResourceEvent.READ), // ???
      /** about to PUT */
      ABOUT_TO_UPDATE(DCResourceEvent.ABOUT_TO_UPDATE),
      /** PUT */
      UPDATED(DCResourceEvent.UPDATED),
      /** about to DELETE */
      ABOUT_TO_DELETE(DCResourceEvent.ABOUT_TO_DELETE),
      /** DELETEd */
      DELETED(DCResourceEvent.DELETED); // ??
      String name;
      Types(String s) { name = s; }
   }

   public static final String RESOURCE_PREFIX = "resource.";

   public static final String ABOUT_TO_BUILD = RESOURCE_PREFIX + "aboutToBuild";
   public static final String ABOUT_TO_CREATE = RESOURCE_PREFIX + "aboutToCreate";
   public static final String CREATED = RESOURCE_PREFIX + "created";
   public static final String READ = RESOURCE_PREFIX + "read";
   public static final String ABOUT_TO_UPDATE = RESOURCE_PREFIX + "aboutToUpdate";
   public static final String UPDATED = RESOURCE_PREFIX + "updated";
   public static final String ABOUT_TO_DELETE = RESOURCE_PREFIX + "aboutToDelete";
   public static final String DELETED = RESOURCE_PREFIX + "deleted";
   
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
         DCResource resource, DCResource previousResource) {
      super(type.name, resourceType);
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
      this(type, resource.getModelType(), resource, previousResource);
   }

   public DCResourceEvent(Types type, String topic, DCResource resource) {
      this(type, topic, resource, null);
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
