package org.oasis.datacore.rest.server.resource;

import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.rest.api.DCResource;

public class ResourceException extends Exception {
   private static final long serialVersionUID = -3244361074758795252L;
   
   private DCProject project;
   private String resourceMessage;
   private DCResource resource;

   /**
    * 
    * @param message should not be null
    * @param cause
    * @param resource
    */
   public ResourceException(String resourceMessage, Throwable cause, DCResource resource, DCProject project) {
      super(buildMessageFromResourceMessage(resourceMessage, resource, project), cause);
      this.resourceMessage = resourceMessage;
      this.resource = resource;
      this.project = project;
   }

   /**
    * 
    * @param resourceMessage should not be null
    * @param resource
    */
   public ResourceException(String resourceMessage, DCResource resource, DCProject project) {
      super(buildMessageFromResourceMessage(resourceMessage, resource, project));
      this.resourceMessage = resourceMessage;
      this.resource = resource;
      this.project = project;
   }

   public DCProject getProject() {
      return project;
   }

   public DCResource getResource() {
      return resource;
   }

   public String getResourceMessage() {
      return resourceMessage;
   }

   private static String buildMessageFromResourceMessage(
         String resourceMessage, DCResource resource, DCProject project) {
      StringBuilder sb;
      if (resource == null) {
         sb = new StringBuilder();
      } else {
         sb = new StringBuilder("On resource ");
         sb.append(resource.getUri());
         sb.append(" ");
      }
      sb.append("in project ");
      sb.append(project.getName());
      sb.append(": ");
      sb.append(resourceMessage);
      return sb.toString();
   }

}
