package org.oasis.datacore.rest.server.cxf;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.cxf.logging.FaultListener;
import org.apache.cxf.message.Message;


/**
 * Only logs 400 (Client error) & 500 (Server error) WebApplicationException,
 * as well as non-WebApplicationExceptions.
 * Prevents WebApplicationExceptionMapper.toResponse() from logging 304 Not Modified
 * (used for client-side cache) when called by JAXRSInvoker.handleFault()
 * & JARSUtils.convertFaultToResponse().
 * To use it, configure it on CXF Bus with key org.apache.cxf.logging.FaultListener.
 * 
 * WARNING returns true won't log, and false will WHICH IS THE OPPOSITE OF THE DOC
 * 
 * @author mdutoo
 *
 */
public class ClientServerErrorFaultListener implements FaultListener {

   @Override
   public boolean faultOccurred(Exception exception, String description,
         Message message) {
      if (exception instanceof WebApplicationException) {
         WebApplicationException waex = (WebApplicationException) exception;
         Response r = waex.getResponse();
         if (r != null && r.getStatus() < 400) {
            // ex. 200, 201 Created, 304 Not Modified...
            return true; // is a regular Fault but no error, don't log
         }
      }
      return false; // not a regular Fault but an error, log it
   }

}
