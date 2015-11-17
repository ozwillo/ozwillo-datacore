package org.oasis.datacore.core.meta;

import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.pov.DCProject;

public class ModelNotFoundException extends ModelException {
   private static final long serialVersionUID = 2905107347911401501L;

   public ModelNotFoundException(DCModelBase model, DCProject project, String message, Throwable cause) {
      super(model, project, message, cause);
   }

   public ModelNotFoundException(DCModelBase model, DCProject project, String message) {
      super(model, project, message);
   }
   
   public ModelNotFoundException(String modelType, DCProject project, String message) {
      super(modelType, project, message);
   }

   public ModelNotFoundException(DCModelBase model, DCProject project, Throwable cause) {
      super(model, project, "Can't find model", cause);
   }
   
   public ModelNotFoundException(String modelType, DCProject project, Throwable cause) {
      super(modelType, project, "Can't find model type");
   }

   public ModelNotFoundException(DCModelBase model, DCProject project) {
      super(model, project, "Can't find model");
   }
   
   public ModelNotFoundException(String modelType, DCProject project) {
      super(modelType, project, "Can't find model type");
   }

}
