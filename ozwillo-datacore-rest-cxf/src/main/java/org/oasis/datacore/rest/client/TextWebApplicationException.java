package org.oasis.datacore.rest.client;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class TextWebApplicationException extends WebApplicationException {
   private static final long serialVersionUID = -2312672794839211181L;
   
   private String textMessage;

   public TextWebApplicationException(String message, Response r) {
      super(r);
      this.textMessage = message;
   }
   
   @Override
   public String getMessage() {
      return this.textMessage;
   }

}
