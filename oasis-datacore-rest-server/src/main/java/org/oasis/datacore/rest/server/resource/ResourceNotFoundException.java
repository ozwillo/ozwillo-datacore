package org.oasis.datacore.rest.server.resource;

import org.oasis.datacore.rest.api.DCResource;

public class ResourceNotFoundException extends ResourceException {
   private static final long serialVersionUID = -4949574481527795965L;
   
   public ResourceNotFoundException(String message, Throwable t, DCResource resource) {
      super(message, t, resource);
   }
   public ResourceNotFoundException(String message, DCResource resource) {
      super(message, resource);
   }


}
