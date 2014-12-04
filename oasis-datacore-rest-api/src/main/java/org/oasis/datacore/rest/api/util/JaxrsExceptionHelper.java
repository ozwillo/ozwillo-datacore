package org.oasis.datacore.rest.api.util;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class JaxrsExceptionHelper {

   /**
    * To be used inside BadRequestException, like this :
    * throw new BadRequestException(JaxrsExceptionHelper.toBadRequestResponse(e))
    * @param t
    * @return
    */
   public static Response toBadRequestResponse(Throwable t) {
      return Response.status(Response.Status.BAD_REQUEST)
            .entity(toString(t)).type(MediaType.TEXT_PLAIN).build();
   }
   /**
    * To be used inside NotFoundException, like this :
    * throw new NotFoundException(JaxrsExceptionHelper.toNotFoundResponse(e))
    * @param t
    * @return
    */
   public static Response toNotFoundResponse(Throwable t) {
      return Response.status(Response.Status.NOT_FOUND)
            .entity(toString(t)).type(MediaType.TEXT_PLAIN).build();
   }
   /**
    * To be used inside ClientErrorException, like this :
    * throw new ClientErrorException(JaxrsExceptionHelper.toClientErrorResponse(e))
    * @param t
    * @return
    */
   public static Response toClientErrorResponse(Throwable t) {
      return Response.status(Response.Status.CONFLICT)
            .entity(toString(t)).type(MediaType.TEXT_PLAIN).build();
   }
   /**
    * To be used inside InternalServerErrorException, like this :
    * throw new InternalServerErrorException(JaxrsExceptionHelper.toInternalServerErrorResponse(e))
    * @param t
    * @return
    */
   public static Response toInternalServerErrorResponse(Throwable t) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(toString(t)).type(MediaType.TEXT_PLAIN).build();
   }
   /**
    * To be used inside InternalServerErrorException, like this :
    * throw new InternalServerErrorException(JaxrsExceptionHelper.toNotImplementedResponse());
    * @return
    */
   public static Response toNotImplementedResponse() {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity("Feature not implemented").type(MediaType.TEXT_PLAIN).build();
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
