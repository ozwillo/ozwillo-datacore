package org.oasis.datacore.rest.server;

import javax.ws.rs.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * Resource that redirects root GET to a given (absolute or relative) target URI.
 * 
 * TODO LATER2 extract to shared project...
 * 
 * @author mdutoo
 *
 */
@Path("/")
@Component("datacore.server.rootRedirectResource") // else can't autowire Qualified
public class RootRedirectResource extends RedirectResourceBase {

   @Value("${datacoreApiServer.baseUrl}/dc-ui/index.html")
   private String targetUri;

   public String getTargetUri() {
      return targetUri;
   }
   public void setTargetUri(String targetUri) { // setter is required
      this.targetUri = targetUri;
   }

}
