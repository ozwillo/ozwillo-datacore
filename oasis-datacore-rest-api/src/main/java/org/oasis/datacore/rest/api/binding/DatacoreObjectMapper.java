package org.oasis.datacore.rest.api.binding;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;

/**
 * Jackson databinding mapper with configuration for Datacore format
 * 
 * @author mdutoo
 *
 */
public class DatacoreObjectMapper extends ObjectMapper {
   private static final long serialVersionUID = -337804821665272624L;

   public DatacoreObjectMapper() {
      super();
      
      // date databinding configuration :
      
      // model dates as Joda's DateTime,
      // else com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException: Unrecognized field "weekOfWeekyear" (class org.joda.time.DateTime)
      // or JsonMappingException: Can not instantiate value of type [simple type, class org.joda.time.DateTime]
      // from String value; no single-String constructor/factory method (through reference chain: org.oasis.datacore.rest.api.DCResource["created"])
      // see http://stackoverflow.com/questions/13700853/jackson2-json-iso-8601-date-from-jodatime-in-spring-3-2rc1
      this.registerModule(new JodaModule());
      
      // serialize as timestamp
      // NB. and not WRITE_DATE_KEYS_AS_TIMESTAMPS
      // see http://www.lorrin.org/blog/2013/06/28/custom-joda-time-dateformatter-in-jackson
      this.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
      
      // NO joda is used instead anyway (which parses using new DateTime(string, UTC))
      //this.setDateFormat(StdDateFormat.getBlueprintISO8601Format());
      // or SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ") 
      
      // allow leading zeroes
      // else fails on date with years serialized with wrong leading zeroes ex. 0300 :
      // JsonParseException: Invalid numeric value: Leading zeroes not allowed, in ReaderBasedJsonParser._verifyNoLeadingZeroes()
      this.configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true);
   }
   
}
