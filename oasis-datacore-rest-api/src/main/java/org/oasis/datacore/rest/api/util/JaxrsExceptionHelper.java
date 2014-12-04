package org.oasis.datacore.rest.api.util;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class JaxrsExceptionHelper {

   public static BadRequestException toBadRequestException(Throwable t) {
      return new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
            .entity(toString(t)).type(MediaType.TEXT_PLAIN).build());
   }
   public static NotFoundException toNotFoundException(Throwable t) {
      return new NotFoundException(Response.status(Response.Status.NOT_FOUND)
            .entity(toString(t)).type(MediaType.TEXT_PLAIN).build());
   }
   public static ClientErrorException toClientErrorException(Throwable t) {
      return new ClientErrorException(Response.status(Response.Status.CONFLICT)
            .entity(toString(t)).type(MediaType.TEXT_PLAIN).build());
   }
   public static InternalServerErrorException toInternalServerErrorException(Throwable t) {
      throw new InternalServerErrorException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(toString(t)).type(MediaType.TEXT_PLAIN).build());
   }
   public static InternalServerErrorException toNotImplementedException() {
      throw new InternalServerErrorException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity("Feature not implemented").type(MediaType.TEXT_PLAIN).build());
   }
   
   /**
    * To be used to build JAXRS WebApplication Exceptions.
    */
   public static String toString(Throwable t) {
      StringBuilder sb = new StringBuilder();
      toShortString(t, sb);
      while ((t = t.getCause()) != null) { // there is often at least one ex. for Resource(Parsing)Exception
         sb.append("\n   Cause :\n");
         toShortString(t, sb);
      }
      return sb.toString();
   }
   
   private static void toShortString(Throwable t, StringBuilder sb) {
      sb.append(t.getMessage());
      sb.append("\n   ");
      sb.append(t.getClass().getSimpleName());
      sb.append(" at ");
      sb.append(t.getStackTrace()[0].toString());
   }
   
}
