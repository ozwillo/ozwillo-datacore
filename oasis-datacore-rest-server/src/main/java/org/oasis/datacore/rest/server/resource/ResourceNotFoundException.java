package org.oasis.datacore.rest.server.resource;

import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.rest.api.DCResource;

public class ResourceNotFoundException extends ResourceException {
   private static final long serialVersionUID = -4949574481527795965L;
   private String uri;
   
   public ResourceNotFoundException(String message, Throwable t,
         String uri, DCResource resource, DCProject project) {
      super(buildRNFMessage(uri = ((uri != null) ? uri :
         ((resource != null) ? resource.getUri() : null)), message), t, resource, project);
      this.uri = uri;
   }
   public ResourceNotFoundException(String message,
         String uri, DCResource resource, DCProject project) {
      super(buildRNFMessage(uri = ((uri != null) ? uri :
         ((resource != null) ? resource.getUri() : null)), message), resource, project);
      this.uri = uri;
   }
   public ResourceNotFoundException(String message, Throwable t,
         DCResource resource, DCProject project) {
      super(buildRNFMessage(((resource != null) ? resource.getUri() : null), message), t, resource, project);
      this.uri = ((resource != null) ? resource.getUri() : null);
   }
   public ResourceNotFoundException(String message,
         DCResource resource, DCProject project) {
      super(buildRNFMessage(((resource != null) ? resource.getUri() : null), message), resource, project);
      this.uri = ((resource != null) ? resource.getUri() : null);
   }

   public String getUri() {
      return uri;
   }
   
   private static String buildRNFMessage(String uri, String message) {
      return ((uri !=null) ? "uri " + uri : "") + ((message != null) ? " : " + message : "");
   }

}
