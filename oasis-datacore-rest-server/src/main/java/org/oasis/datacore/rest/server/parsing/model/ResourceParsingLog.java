package org.oasis.datacore.rest.server.parsing.model;

public class ResourceParsingLog {

   private String fieldFullPath; // TODO or (also because ...) field,
   // to be able to display details (its type, conf...) ?!?
   // TODO add stack
   private String message;
   private Exception exception;
   
   public ResourceParsingLog(String fieldFullPath, String message) {
      this(fieldFullPath, message, null);
   }
   
   public ResourceParsingLog(String fieldFullPath, String message, Exception exception) {
      this.fieldFullPath = fieldFullPath;
      this.message = message;
      this.exception = exception;
   }

   public String getFieldFullPath() {
      return fieldFullPath;
   }

   public void setFieldFullPath(String fieldFullPath) {
      this.fieldFullPath = fieldFullPath;
   }

   public String getMessage() {
      return message;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   public Exception getException() {
      return exception;
   }

   public void setException(Exception exception) {
      this.exception = exception;
   }
   
}
