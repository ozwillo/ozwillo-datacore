package org.oasis.datacore.server.rest.core;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.oasis.datacore.core.entity.EntityException;

/**
 * Translates -core EntityException (which is Runtime) error to HTTP 404 Bad Request
 * i.e. when entity pb that should be thrown all the way up
 * & solved at resource level, ex. when implicit only fork.
 * Must be registered as JAXRS Provider.
 * TODO LATER OPT also for other core (DuplicateKey ?) & server (all in DatacoreApiImpl) exceptions
 * 
 * @author mdutoo
 */
public class EntityExceptionMapper implements ExceptionMapper<EntityException> {

   @Override
   public Response toResponse(EntityException eex) {
      return Response.status(Response.Status.BAD_REQUEST)
            .entity(eex.getMessage())
            .type(MediaType.TEXT_PLAIN).build();
   }

}
