package org.oasis.datacore.rest.server.resource;

import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.DCURI;

/**
 * Resource has external (Datacore or Web) URI, meaning it should be handled by another server
 * @author mdutoo
 *
 */
public class ExternalResourceException extends ResourceException {
   private static final long serialVersionUID = 8649786268754374907L;
   
   private String ownMessage;
   private DCURI uri;
   
   public ExternalResourceException(DCURI uri, String ownMessage, Throwable cause,
         DCResource resource, DCProject project) {
      super(buildMessage(uri, ownMessage, project), cause, resource, project);
      this.ownMessage = ownMessage;
      this.uri = uri;
   }

   public String getOwnMessage() {
      return ownMessage;
   }
   
   public DCURI getUri() {
      return uri;
   }

   private static String buildMessage(DCURI uri, String message, DCProject project) {
      String msg = "External uri " + uri.toString() + " in project " + project.getName();
      if (message != null && message.length() != 0) {
         msg += " : " + message;
      }
      return msg;
   }

}
