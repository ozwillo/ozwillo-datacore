package org.oasis.datacore.rest.client.cxf;

import java.lang.reflect.Constructor;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.oasis.datacore.rest.client.TextWebApplicationException;

/**
 * Improves exception logging output & readability :
 * if HTTP response is text, fills exception message with it.
 * 
 * TODO or only of (jaxrs 2-only) InternalServerErrorException ?
 * @author mdutoo
 *
 */
public class TextResponseExceptionMapper
      implements org.apache.cxf.jaxrs.client.ResponseExceptionMapper<WebApplicationException> {

   @Override
   public WebApplicationException fromResponse(Response r) {
      // TODO Auto-generated method stub
      //r.getEntity() + ""
      if (r != null && (r.getMediaType() != null && r.getMediaType().isCompatible(MediaType.TEXT_PLAIN_TYPE))) {
         String message = String.valueOf(r.getEntity());
         return new TextWebApplicationException(message, r); // TODO or (jaxrs 2-only) InternalServerErrorException ?
      }
      throw convertToWebApplicationException(r); 
   }

   /**
    * inspired from CXF JAXRS AbstractClient.convertToWebApplicationException()
    * @param r
    * @return
    */
   protected WebApplicationException convertToWebApplicationException(Response r) {
       Class<?> exceptionClass = JAXRSUtils.getWebApplicationExceptionClass(r, 
                                      WebApplicationException.class);
       try {
           Constructor<?> ctr = exceptionClass.getConstructor(Response.class);
           return (WebApplicationException)ctr.newInstance(r);
       } catch (Throwable ex2) {
           return new WebApplicationException(r);
       }
   }

}
