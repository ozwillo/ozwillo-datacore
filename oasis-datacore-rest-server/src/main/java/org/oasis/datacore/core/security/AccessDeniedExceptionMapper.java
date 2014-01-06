package org.oasis.datacore.core.security;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


/**
 * Translates spring security rights error to HTTP 403 Forbidden
 * (and not 401, which are only for auth services, see
 * http://stackoverflow.com/questions/3297048/403-forbidden-vs-401-unauthorized-http-responses ).
 * Must be registered as JAXRS Provider.
 * Inspired by http://cxf.apache.org/docs/jax-rs-basics.html & http://svn.apache.org/repos/asf/cxf/trunk/systests/jaxrs/src/test/java/org/apache/cxf/systest/jaxrs/security/SecurityExceptionMapper.java
 * 
 * @author mdutoo
 *
 */
public class AccessDeniedExceptionMapper implements ExceptionMapper<AccessDeniedException> {

   @Override
   public Response toResponse(AccessDeniedException adex) {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      String username = (authentication != null) ? authentication.getName() : "no user";
      return Response.status(Response.Status.FORBIDDEN)
            .entity(adex.getMessage() + " (as " + username + ").") // "Access is denied"
            .type(MediaType.TEXT_PLAIN).build();
   }

}
