package org.oasis.datacore.rest.server.resource;

import org.oasis.datacore.rest.api.DCResource;

public class ResourceException extends Exception {
   private static final long serialVersionUID = -3244361074758795252L;
   
   private String resourceMessage;
   private DCResource resource;

   /**
    * 
    * @param message should not be null
    * @param cause
    * @param resource
    */
   public ResourceException(String resourceMessage, Throwable cause, DCResource resource) {
      super(buildMessageFromResourceMessage(resourceMessage, resource), cause);
      this.resourceMessage = resourceMessage;
      this.resource = resource;
   }

   /**
    * 
    * @param resourceMessage should not be null
    * @param resource
    */
   public ResourceException(String resourceMessage, DCResource resource) {
      super(buildMessageFromResourceMessage(resourceMessage, resource));
      this.resourceMessage = resourceMessage;
      this.resource = resource;
   }

   public DCResource getResource() {
      return resource;
   }

   public String getResourceMessage() {
      return resourceMessage;
   }

   private static String buildMessageFromResourceMessage(String resourceMessage, DCResource resource) {
      StringBuilder sb;
      if (resource == null) {
         sb = new StringBuilder();
      } else {
         sb = new StringBuilder("On resource ");
         sb.append(resource.getUri());
      }
      sb.append(" : ");
      sb.append(resourceMessage);
      return sb.toString();
   }

}
