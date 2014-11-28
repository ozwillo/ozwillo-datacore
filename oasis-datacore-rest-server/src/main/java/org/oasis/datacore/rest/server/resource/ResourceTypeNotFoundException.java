package org.oasis.datacore.rest.server.resource;

import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.rest.api.DCResource;

public class ResourceTypeNotFoundException extends ResourceException {
   private static final long serialVersionUID = 1611461505158718183L;

   private String ownMessage;
   private String modelType;
   
   public ResourceTypeNotFoundException(String modelType, String ownMessage, Throwable cause, DCResource resource, DCProject project) {
      super(buildMessage(modelType, ownMessage, project), cause, resource, project);
      this.ownMessage = ownMessage;
   }
   public ResourceTypeNotFoundException(DCResource resource, DCProject project) {
      super(buildMessage(resource, project), resource, project);
      this.modelType = resource.getModelType();
      this.ownMessage = null;
   }
   
   public String getOwnMessage() {
      return ownMessage;
   }
   
   public String getModelType() {
      return modelType;
   }

   private static String buildMessage(String modelType, String message, DCProject project) {
      String msg = "Unknown Model type " + modelType + " in project " + project.getName();
      if (message != null && message.length() != 0) {
         msg += " : " + message;
      }
      return msg;
   }
   private static String buildMessage(DCResource resource, DCProject project) {
      String modelType = null;
      if (resource != null) {
         modelType = resource.getModelType();
         if (modelType != null && modelType.length() != 0) {
            return buildMessage(modelType, null, project);
         }
      }
      return "Missing Model type in project " + project.getName();
   }

}
