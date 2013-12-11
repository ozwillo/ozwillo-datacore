package org.oasis.datacore.rest.server.resource;

import org.oasis.datacore.rest.api.DCResource;

public class ResourceTypeNotFoundException extends ResourceException {
   private static final long serialVersionUID = 1611461505158718183L;

   private String ownMessage;
   private String modelType;
   
   public ResourceTypeNotFoundException(String modelType, String ownMessage, Throwable cause, DCResource resource) {
      super(buildMessage(modelType, ownMessage), cause, resource);
      this.ownMessage = ownMessage;
   }
   public ResourceTypeNotFoundException(DCResource resource) {
      super(buildMessage(resource), resource);
      this.modelType = resource.getModelType();
      this.ownMessage = null;
   }

   public String getOwnMessage() {
      return ownMessage;
   }
   
   public String getModelType() {
      return modelType;
   }

   private static String buildMessage(String modelType, String message) {
      String msg = "Unknown Model type " + modelType;
      if (message != null && message.length() != 0) {
         msg += " : " + message;
      }
      return msg;
   }
   private static String buildMessage(DCResource resource) {
      String modelType = null;
      if (resource != null) {
         modelType = resource.getModelType();
         if (modelType != null && modelType.length() != 0) {
            return buildMessage(modelType, null);
         }
      }
      return "Missing Model type";
   }

}
