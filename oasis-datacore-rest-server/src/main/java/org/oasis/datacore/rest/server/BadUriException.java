package org.oasis.datacore.rest.server;

public class BadUriException extends Exception {
   private static final long serialVersionUID = -6207977393297305406L;

   private String ownMessage;
   private String uri;

   /**
    * Creates a "Missing uri" exception
    */
   public BadUriException() {
      super("Missing URI");
   }

   public BadUriException(String ownMessage, String uri, Throwable cause) {
      super(buildMessage(ownMessage, uri), cause);
      this.ownMessage = ownMessage;
      this.uri = uri;
   }

   public BadUriException(String ownMessage, String uri) {
      super(buildMessage(ownMessage, uri));
      this.ownMessage = ownMessage;
      this.uri = uri;
   }

   private static String buildMessage(String ownMessage, String uri) {
      StringBuilder sb;
      if (uri == null || uri.length() == 0) {
         sb = new StringBuilder("Missing uri");
      } else {
         sb = new StringBuilder(uri);
         sb.append(" is a bad URI");
      }
      if (ownMessage != null && ownMessage.length() != 0) {
         sb.append(": ");
         sb.append(ownMessage);
      }
      return sb.toString();
   }

   public String getUri() {
      return uri;
   }

   public String getOwnMessage() {
      return this.ownMessage;
   }

}
