package org.oasis.datacore.rest.server;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Resource that returns 200 if Datacore is operational, else 503 if HAProxy
 * should redirect traffic to other (failover) instances ; LATER will return
 * details about Datacore state of operation (availability of MongoDB, Kernel...).
 * 
 * @author mdutoo
 *
 */
@Path("/dc/status")
@Component("datacore.server.statusResource") // else can't autowire Qualified
public class StatusResource {
   
   private ObjectMapper jsonNodeMapper = new ObjectMapper();

   /**
    * TODO fill and return lazily cached Status
    * @return
    */
   @GET
   public Response getStatus() {
      return Response.ok().build(); // TODO LATER .entity(jsonNodeMapper.map(new State(...)))
   }
   
}
