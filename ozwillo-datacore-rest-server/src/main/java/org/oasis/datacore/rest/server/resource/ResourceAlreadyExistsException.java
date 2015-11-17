package org.oasis.datacore.rest.server.resource;

import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.rest.api.DCResource;

public class ResourceAlreadyExistsException extends ResourceException {
   private static final long serialVersionUID = -4949574481527795965L;
   
   public ResourceAlreadyExistsException(String message, Throwable t,
         DCResource resource, DCProject project) {
      super(message, t, resource, project);
   }
   public ResourceAlreadyExistsException(String message,
         DCResource resource, DCProject project) {
      super(message, resource, project);
   }


}
