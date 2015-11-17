package org.oasis.datacore.core.entity.query;


/**
 * TODO add support of parsing errors, warnings, up to QueryParsingContext ?!?
 * TODO LATER use it to add support for "explain" switch and other non-error
 * additional behaviours of query engines
 * 
 * @author mdutoo
 *
 */
public class QueryException extends Exception {
   private static final long serialVersionUID = 2296337662914255799L;

   public QueryException(String message, Throwable cause) {
      super(message, cause);
   }

   public QueryException(String message) {
      super(message);
   }

}
