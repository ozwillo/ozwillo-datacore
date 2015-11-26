package org.oasis.datacore.core.entity;

/**
 * Entity pb that should be thrown all the way up & solved at resource level,
 * ex. when implicit only fork
 * Not runtime because should always be caught & converted to ResourceException.
 * TODO LATER make NonTransientDataAccessException one.
 * @author mdutoo
 */
public class EntityException extends RuntimeException {
   private static final long serialVersionUID = -3109026477617015473L;

   public EntityException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
   }

   public EntityException(String message, Throwable cause) {
      super(message, cause);
   }

   public EntityException(String message) {
      super(message);
   }

}
