package org.oasis.datacore.core.meta;

import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.pov.DCProject;

public class ModelException extends RuntimeException {
   private static final long serialVersionUID = 5523029187762475316L;
   private DCProject project;
   private String modelType;
   private DCModelBase model;

   public ModelException(DCModelBase model, DCProject project, String message, Throwable cause) {
      super(buildMessageFromModelMessage(message, model, null, project), cause);
      this.project = project;
      this.model = model;
      this.modelType = model.getName();
   }

   public ModelException(DCModelBase model, DCProject project, String message) {
      super(buildMessageFromModelMessage(message, model, null, project));
      this.project = project;
      this.model = model;
      this.modelType = model.getName();
   }
   
   public ModelException(String modelType, DCProject project, String message) {
      super(buildMessageFromModelMessage(message, null, modelType, project));
      this.project = project;
      this.modelType = modelType;
   }

   public DCModelBase getModel() {
      return model;
   }

   public String getModelType() {
      return modelType;
   }

   public DCProject getProject() {
      return project;
   }

   private static String buildMessageFromModelMessage(
         String modelMessage, DCModelBase model, String modelType, DCProject project) {
      StringBuilder sb;
      if (model == null && modelType == null) {
         sb = new StringBuilder();
      } else {
         sb = new StringBuilder("On model ");
         if (model != null) {
            sb.append(model.getName());
         } else {
            sb.append(modelType);
         }
         sb.append(" ");
      }
      sb.append("in project ");
      sb.append(project.getName());
      sb.append(": ");
      sb.append(modelMessage);
      return sb.toString();
   }

}
