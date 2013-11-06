package org.oasis.datacore.rest.server;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;


/**
 * Resource that redirects root GET to a given (absolute or relative) target URI.
 * 
 * TODO LATER2 extract to shared project...
 * 
 * @author mdutoo
 *
 */
@Path("/")
///@Component("datacore.model.apiImpl") // else can't autowire Qualified ; TODO @Service ?
public class RootRedirectResource {

   private String targetUri;

   @GET
   public void redirectToSwaggert() {
      try {
         throw new WebApplicationException(Response.seeOther(new URI(targetUri)).build());
      } catch (URISyntaxException usex) {
         throw new WebApplicationException(usex, Response.serverError()
               .entity("Redirection target " + targetUri + " should be an URI").build());
      }
   }

   public String getTargetUri() {
      return targetUri;
   }

   public void setTargetUri(String targetUri) {
      this.targetUri = targetUri;
   }
   
}
