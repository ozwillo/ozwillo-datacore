package org.oasis.datacore.model.event;

import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.rest.server.event.DCEvent;


/**
 * Model event.
 * 
 * (could extend DCPropertiesEvent to make it easier to customize it, but try to keep properties null
 * for better performances).
 * 
 * @author mdutoo
 *
 */
public class ModelDCEvent extends DCEvent {
   
   public static final String MODEL_DEFAULT_BUSINESS_DOMAIN = "model.*"; // TODO more ex. business domain
   
   public static final String MODEL_PREFIX = "model.";

   //public static final String ABOUT_TO_BUILD = MODEL_PREFIX + "aboutToBuild";
   public static final String ABOUT_TO_CREATE = MODEL_PREFIX + "aboutToCreate";
   public static final String CREATED = MODEL_PREFIX + "created";
   //public static final String READ = MODEL_PREFIX + "read";
   public static final String ABOUT_TO_UPDATE = MODEL_PREFIX + "aboutToUpdate";
   public static final String UPDATED = MODEL_PREFIX + "updated";
   public static final String ABOUT_TO_DELETE = MODEL_PREFIX + "aboutToDelete";
   public static final String DELETED = MODEL_PREFIX + "deleted";
   // TODO more for model lifecycle beyond draft step :
   public static final String ABOUT_TO_PUBLISH = MODEL_PREFIX + "aboutToPublish";
   public static final String PUBLISHED = MODEL_PREFIX + "published";
   public static final String ABOUT_TO_OBSOLETE = MODEL_PREFIX + "aboutToObsolete";
   public static final String OBSOLETED = MODEL_PREFIX + "obsoleted";
   
   private DCModelBase model;
   /** in case of aboutToXXX event */
   private DCModelBase previousModel;

   /**
    * 
    * @param type
    * @param resourceType
    * @param resource
    * @param properties try to keep it null for better performances
    */
   public ModelDCEvent(String eventType, String businessDomain,
         DCModelBase model, DCModelBase previousModel) {
      super(eventType, businessDomain);
      this.model = model;
      this.previousModel = previousModel;
   }

   public DCModelBase getModel() {
      return model;
   }

   public DCModelBase getPreviousModel() {
      return previousModel;
   }

   @Override
   protected StringBuilder toStringBuilder() {
      StringBuilder sb = super.toStringBuilder();
      sb.append(" about ");
      sb.append(model.getName());
      return sb;
   }

}
