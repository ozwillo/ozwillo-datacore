package org.oasis.datacore.rest.client.cxf.obsolete;

import java.lang.reflect.Constructor;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.oasis.datacore.rest.client.TextWebApplicationException;

/**
 * Improves exception logging output & readability :
 * if HTTP response is text, fills exception message with it.
 * 
 * @author mdutoo
 * 
 * @obsolete doesn't bring anything, unduly (<400) HTTP error logs are rather
 * removed on server by DatacoreFaultListener
 *
 */
public class TextResponseExceptionMapper
      implements org.apache.cxf.jaxrs.client.ResponseExceptionMapper<WebApplicationException> {

   @Override
   public WebApplicationException fromResponse(Response r) {
      if (r.getStatus() < 400) {
         // ex. 200, 201 Created, 304 Not Modified...
         //return null; // no error
      
      } else if (r != null && (r.getMediaType() == null || r.getMediaType().isCompatible(MediaType.TEXT_PLAIN_TYPE))) {
         String message = String.valueOf(r.getEntity());
         return new TextWebApplicationException(message, r); // TODO or (jaxrs 2-only) InternalServerErrorException ?
      }
      
      throw convertToWebApplicationException(r);
   }

   /**
    * inspired from CXF JAXRS AbstractClient.convertToWebApplicationException()
    * 400 => ClientErrorException
    * 500 => ServerErrorException
    * @param r
    * @return
    */
   protected WebApplicationException convertToWebApplicationException(Response r) {
       Class<?> exceptionClass = ExceptionUtils.getWebApplicationExceptionClass(r, 
                                      WebApplicationException.class);
       try {
           Constructor<?> ctr = exceptionClass.getConstructor(Response.class);
           return (WebApplicationException)ctr.newInstance(r);
       } catch (Throwable ex2) {
           return new WebApplicationException(r);
       }
   }

}
