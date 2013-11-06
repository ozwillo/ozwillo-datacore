package org.oasis.datacore.model.rest.server;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.model.rest.api.DataModelApi;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@Path("dc/model") // relative path among other OASIS services
@Consumes(MediaType.APPLICATION_JSON) // TODO finer media type ex. +oasis-datacore ??
@Produces(MediaType.APPLICATION_JSON)
///@Component("datacore.model.apiImpl") // else can't autowire Qualified ; TODO @Service ?
public class DataModelApiImpl implements DataModelApi {

   @Autowired
   private DataModelServiceImpl modelQueryService;
   
   public ObjectMapper mapper = new ObjectMapper(); // or per-request ?? TODO remove it once DCModel is Jacksonized
   
   @Override
   public String findModel() {
      Map<String, DCModel> models = this.modelQueryService.getModelMap();
      
      // using custom writer for pretty print :
      ObjectWriter writer = this.mapper.writer().withDefaultPrettyPrinter(); // TODO or global ?
      // TODO rather as JAXRS provider ? see Swagger's : com.wordnik.swagger.jaxrs.listing.ResourceListingProvider
      
      try {
         return writer.writeValueAsString(models);
      } catch (JsonProcessingException jpex) {
         throw new WebApplicationException(jpex, Response.status(Response.Status.INTERNAL_SERVER_ERROR)
               .entity("Error serializing models as JSON : " + jpex.getMessage())
               .type(MediaType.TEXT_PLAIN).build());
      }
   }

}
