package org.oasis.datacore.rest.api.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;


/**
 * Some Resource field value types can't be parsed from JSON by Jackson
 * in the current state of the Jackson-annotated API. These are
 * non-native (so not string, boolean, object / map, array / list, float-derived so also
 * int, short & double), non-string (so not Resource references) Resource field value types :
 * * date => org.joda.time.DateTime
 * * long => java.lang.Long
 * 
 * So this class provides helpers to parse their String values to their
 * Object counterpart.
 * 
 * NB. there is no problem in the other way i.e. serialization / formatting,
 * as long as they are provided in their canonical Object type, whose
 * toString() is used to do the job.
 * 
 * NB. Resource modelization could be improved so that it would be possible
 * provided that its Model is available (which it is not on client side for now).
 * 
 * @author mdutoo
 *
 */
public class ResourceParsingHelper {
   
   /* to parse values */
   //public ObjectMapper mapper = new DatacoreObjectMapper();

   /**
    * Parses a Datacore field (property) value of type date (using UTC timezone)
    * @param stringValue
    * @return null if no value (null or empty)
    * @throws IllegalArgumentException
    */
   public static DateTime parseDate(String stringValue) throws IllegalArgumentException {
      if (stringValue == null || stringValue.isEmpty()) {
         return null; // no value
      }
      /*if (stringValue.charAt(0) == '\"') {
         throw new ResourceParsingException("date Field value is attempted to be parsed  "
               + "from unquoted string but is quoted (maybe doubly ?) : " + stringValue);
      }*/
      //return getDateFormat().parse(stringValue);
      return new DateTime(stringValue, DateTimeZone.UTC); // like DateTimeDeserializer
      /*} catch (IllegalArgumentException e) {
         throw new ResourceParsingException("date Field value is not "
               + "an ISO 8601 Date-formatted string : " + stringValue, e);
      }*/
   }

   /**
    * Parses a Datacore field (property) value of type long.
    * @param stringValue
    * @return null if no value (null or empty)
    * @throws NumberFormatException
    */
   public static Long parseLong(String stringValue) throws NumberFormatException {
      if (stringValue == null || stringValue.isEmpty()) {
         return null; // no value
      }
      /*if (stringValue.charAt(0) == '\"') {
         throw new ResourceParsingException("date Field value is attempted to be parsed  "
               + "from unquoted string but is quoted (maybe doubly ?) : " + stringValue);
      }*/
      return Long.valueOf(stringValue);
      /*} catch (NumberFormatException e) {
         throw new ResourceParsingException("date Field value is not "
               + "an ISO 8601 Date-formatted string : " + stringValue, e);
      }*/
   }


   /*
   private ThreadLocal<DateFormat> dateFormatThreadLocal = new ThreadLocal<DateFormat>();
   private DateFormat getDateFormat() {
      DateFormat dateFormat = dateFormatThreadLocal.get();
      if (dateFormat == null) {
         dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"); // like in StdDateFormat
         dateFormatThreadLocal.set(dateFormat);
      }
      return dateFormat;
   }
   */
   
}
