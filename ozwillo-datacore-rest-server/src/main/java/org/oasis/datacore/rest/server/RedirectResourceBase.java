package org.oasis.datacore.rest.server;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;


/**
 * Resource that redirects GET to a given (absolute or relative) target URI.
 * 
 * TODO LATER2 extract to shared project...
 * 
 * @author mdutoo
 *
 */
//@Path("...")
//@Component("...") // else can't autowire Qualified
public abstract class RedirectResourceBase {

   @GET
   public void redirectToSwagger() {
      try {
         throw new WebApplicationException(Response.seeOther(new URI(getTargetUri())).build());
      } catch (URISyntaxException usex) {
         throw new WebApplicationException(usex, Response.serverError()
               .entity("Redirection target " + getTargetUri() + " should be an URI").build());
      }
   }

   public abstract String getTargetUri();
   
}
