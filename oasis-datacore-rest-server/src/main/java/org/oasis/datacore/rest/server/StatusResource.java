package org.oasis.datacore.rest.server;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
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
   @Path("")
   @GET
   public Response getStatus() {
      Map<String,Object> statusMap = new HashMap<String,Object>();
      //statusMap.put("mongodb", true); // TOOD LATER compute & cache
      //statusMap.put("kernel", true);
      try {
         return Response.ok().entity(jsonNodeMapper.writer().writeValueAsString(statusMap)).build();
      } catch (JsonProcessingException jpex) {
         throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
               .entity("Error writing status as JSON ("
                     + statusMap + ") : " + jpex.getMessage())
                     .type(MediaType.TEXT_PLAIN).build());
      }
   }
   
}
