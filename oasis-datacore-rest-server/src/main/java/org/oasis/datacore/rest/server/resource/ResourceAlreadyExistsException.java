package org.oasis.datacore.rest.server.resource;

import org.oasis.datacore.rest.api.DCResource;

public class ResourceAlreadyExistsException extends ResourceException {
   private static final long serialVersionUID = -4949574481527795965L;
   
   public ResourceAlreadyExistsException(String message, Throwable t, DCResource resource) {
      super(message, t, resource);
   }
   public ResourceAlreadyExistsException(String message, DCResource resource) {
      super(message, resource);
   }


}
