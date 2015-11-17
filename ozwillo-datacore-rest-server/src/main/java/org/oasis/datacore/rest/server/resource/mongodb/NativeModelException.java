package org.oasis.datacore.rest.server.resource.mongodb;

public class NativeModelException extends Exception {
   private static final long serialVersionUID = 8452935628902505405L;

   public NativeModelException(String message) {
      super(message);
   }

   public NativeModelException(String message, Throwable cause) {
      super(message, cause);
   }

   public NativeModelException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
   }

}
