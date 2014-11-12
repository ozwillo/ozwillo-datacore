package org.oasis.datacore.playground.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;


/**
 * Resource that provides playground configuration info to UI.
 * 
 * @author mdutoo
 *
 */
@Path("dc/playground/configuration")
@Component("datacore.playground.configurationResource") // else can't autowire Qualified
public class PlaygroundConfigurationResource extends PlaygroundResourceBase {
   
   @GET
   @Path("")
   public Response getConfiguration() {
      Map<String,Object> confMap = new HashMap<String,Object>();
      confMap.put("devmode", devmode);
      confMap.put("baseUrl", baseUrl);
      confMap.put("containerUrl", containerUrl);
      confMap.put("kernelBaseUrl", kernelBaseUrl);
      confMap.put("playgroundUiUrl", playgroundUiUrl);
      confMap.put("knownDatacoreContainerUrls", knownDatacoreContainerUrls);
      confMap.put("queryMaxScan", queryMaxScan);
      confMap.put("queryMaxStart", queryMaxStart);
      confMap.put("queryMaxLimit", queryMaxLimit);
      try {
         return Response.ok().entity(jsonNodeMapper.writer().writeValueAsString(confMap)).build();
      } catch (JsonProcessingException jpex) {
         throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
               .entity("Error writing configuration as JSON ("
                     + confMap + ") : " + jpex.getMessage())
                     .type(MediaType.TEXT_PLAIN).build());
      }
   }
   
}
