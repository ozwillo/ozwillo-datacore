package org.oasis.datacore.rest.server.parsing;

public class ResourceParsingException extends Exception {
   private static final long serialVersionUID = -8317559306115477791L;

   public ResourceParsingException(String message, Throwable cause) {
      super(message, cause);
   }

   public ResourceParsingException(String message) {
      super(message);
   }

}
