package org.oasis.datacore.rest.api.util;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class JaxrsExceptionHelper {

   /**
    * To be used inside ClientErrorException, like this :
    * throw new ClientErrorException(JaxrsExceptionHelper.toConflictResponse(e))
    * @param t
    * @return
    */
   public static Response toConflictResponse(Throwable t) {
      return Response.status(Response.Status.CONFLICT)
            .entity(toString(t)).type(MediaType.TEXT_PLAIN).build();
   }
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
    * To be used inside BadRequestException, like this :
    * throw new BadRequestException(JaxrsExceptionHelper.toConflictJsonResponse(e))
    * @param t
    * @return
    */
   public static Response toConflictJsonResponse(Throwable t) {
      return Response.status(Response.Status.CONFLICT)
            .entity(toJson(t)).type(MediaType.APPLICATION_JSON).build();
   }
   /**
    * To be used inside BadRequestException, like this :
    * throw new BadRequestException(JaxrsExceptionHelper.toBadRequestJsonResponse(e))
    * @param t
    * @return
    */
   public static Response toBadRequestJsonResponse(Throwable t) {
      return Response.status(Response.Status.BAD_REQUEST)
            .entity(toJson(t)).type(MediaType.APPLICATION_JSON).build();
   }
   /**
    * To be used inside NotFoundException, like this :
    * throw new NotFoundException(JaxrsExceptionHelper.toNotFoundJsonResponse(e))
    * @param t
    * @return
    */
   public static Response toNotFoundJsonResponse(Throwable t) {
      return Response.status(Response.Status.NOT_FOUND)
            .entity(toJson(t)).type(MediaType.APPLICATION_JSON).build();
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
      t = t.getCause(); // not InvocationTargetException at top level
      while (t != null) { // there is often at least one ex. for Resource(Parsing)Exception
         sb.append("\n   Cause :\n");
         toShortString(t, sb);
         if (t.getCause() != null) {
            t = t.getCause();
         } else if (t instanceof InvocationTargetException) {
            t = ((InvocationTargetException) t).getTargetException();
         } else {
            break;
         }
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

   /**
    * 
    * @param <V>
    * @param t
    * @return mutable, to enrich it specifically
    */
   public static <V> Map<String,Object> toJson(Throwable t) {
      HashMap<String,Object> exMap = new HashMap<String, Object>();
      exMap.put("code", t.getClass().getName());
      exMap.put("message", t.getMessage());
      exMap.put("location", t.getStackTrace()[0].toString());
      ImmutableList.Builder<Object> causeListBuilder = new ImmutableList.Builder<Object>();
      t = t.getCause(); // not InvocationTargetException at top level
      while (t != null) { // there is often at least one ex. for Resource(Parsing)Exception
         ImmutableMap.Builder<String, String> causeBuilder = new ImmutableMap.Builder<String, String>();
         causeBuilder.put("code", t.getClass().getName());
         causeBuilder.put("message", t.getMessage() == null ? "" : t.getMessage());
         causeBuilder.put("location", t.getStackTrace()[0].toString());
         causeListBuilder.add(causeBuilder.build());
         if (t.getCause() != null) {
            t = t.getCause();
         } else if (t instanceof InvocationTargetException) {
            t = ((InvocationTargetException) t).getTargetException();
         } else {
            break;
         }
      }
      exMap.put("causes", causeListBuilder.build());
      return exMap;
      
   }
   
}
