package org.oasis.datacore.rest.server.resource;

import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.rest.api.DCResource;


/**
 * TODO or ResourceConflictException for also tx-like & locks ??
 * 
 * @author mdutoo
 *
 */
public class ResourceObsoleteException extends ResourceException {
   private static final long serialVersionUID = -8420233789294850929L;
   
   public ResourceObsoleteException(String message, Throwable t, DCResource resource, DCProject project) {
      super(message, t, resource, project);
   }
   public ResourceObsoleteException(String message, DCResource resource, DCProject project) {
      super(message, resource, project);
   }


}
