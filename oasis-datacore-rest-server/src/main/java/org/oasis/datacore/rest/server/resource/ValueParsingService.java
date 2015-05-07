package org.oasis.datacore.rest.server.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.ClassUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
import org.oasis.datacore.rest.api.util.ResourceParsingHelper;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.parsing.model.DCResourceParsingContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;


/**
 * TODO rather ValueService
 * 
 * This class provides Datacore field value parsing for 2 purpose :
 * * eager parsing of query criteria values
 * * parsing Resource field (property) values that are non-native
 * and serialized as string, i.e. bringing -api's ResourceParsingHelper
 * to server side.
 * 
 * @author mdutoo
 *
 */
@Component
public class ValueParsingService {

   /** to parse values */
   @Autowired
   @Qualifier("datacoreApiServer.objectMapper")
   public ObjectMapper mapper;

   
   ////////////////////////////////////////////////////////////////
   // parsing from string :

   /**
    * Parses value according to given field type.
    * 
    * A null return means no value, and is specified by a "null" JSON queryValue
    * or IF POSSIBLE an empty string queryValue.
    * 
    * WARNING for dates : Requires ALLOW_NUMERIC_LEADING_ZEROS enabled
    * on JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS
    * else fails when years serialized with wrong leading zeroes ex. 0300 :
    * JsonParseException: Invalid numeric value: Leading zeroes not allowed
    * in ReaderBasedJsonParser._verifyNoLeadingZeroes()
    * 
    * @param fieldTypeEnum
    * @param queryValue
    * @return
    * @throws ResourceParsingException
    */
   public Object parseValueFromJSON(DCFieldTypeEnum fieldTypeEnum, String value) throws ResourceParsingException {
      if (value == null) {
         return null;
      }
      
      try {
         return parseValueFromJSONInternal(fieldTypeEnum, value);
         
      } catch (IOException ioex) {
         throw new ResourceParsingException("IO error while reading " + fieldTypeEnum.getType()
               + "-formatted string : " + value, ioex);
         
      } catch (Exception ex) {
         throw new ResourceParsingException("Field value is not a " + fieldTypeEnum.getType()
               + "-formatted string : " + value, ex);
      }
   }
   
   private Object parseValueFromJSONInternal(DCFieldTypeEnum fieldTypeEnum, String value) throws IOException {
      ObjectReader reader = mapper.reader(fieldTypeEnum.getToClass());
      return reader.readValue(value);
   }
   
   /**
    * Eager parsing, notably to be used to parse query criteria.
    * Parses either from JSON (ex. strings or quoted) or mere string (ex. string are unquoted)
    * 
    * If fieldTypeEnum (operator's if any, else field's) is a string primitive (ex. string, resource),
    * tries to unquote and returns string ;
    * else if it's a serialized string primitive (ex. date, long or not ex. string, resource),
    * quotes and parses as JSON ;
    * else tries to parse as JSON ; if fails : if empty returns null, else if list attempts to read as Object
    * to return single value list. 
    * 
    * @param fieldTypeEnum 
    * @param value
    * @return
    * @throws ResourceParsingException
    */
   public Object parseValueFromJSONOrString(DCFieldTypeEnum fieldTypeEnum, String value) throws ResourceParsingException {
      if (value == null) {
         return null;
      }
      if (fieldTypeEnum == null) {
         // guessing is too hard / faulty with Datacore types (and can't switch on null)
         throw new ResourceParsingException("Field type must be provided");
      }
      
      // first try shortcuts :
      if (DCFieldTypeEnum.stringPrimitiveTypes.contains(fieldTypeEnum)) {
         // they can be returned as is
         int valueLength = value.length();
         if (valueLength > 1 && value.charAt(0) == '\"' && value.charAt(valueLength -1) == '\"') {
            // first, trying JSON case (to get a string enclosed in quotes, double quote it)
            value = value.substring(1, valueLength - 1);
         }
         // else no JSON : return string value as is
         return parseStringPrimitiveValueFromString(value); //return value;
         // TODO LATER try to parse embedded or root resource
         
      } else if (DCFieldTypeEnum.stringSerializedPrimitiveTypes.contains(fieldTypeEnum)) {
         // they MUST be parsed AS JSON (i.e. quoted string)
         int valueLength = value.length();
         if (valueLength <= 1 || value.charAt(0) != '\"' || value.charAt(valueLength -1) != '\"') {
            value = quote(value); // quote if not yet
         } // else assuming JSON case
         return parseStringSerializedPrimitiveValueFromJSON(fieldTypeEnum, value);
      }
      
      try {
         // else try to parse as json :
         Object res = parseValueFromJSONInternal(fieldTypeEnum, value);
         if (fieldTypeEnum == DCFieldTypeEnum.I18N && res instanceof String) {
            throw new ResourceParsingException("Wrong use of this method, can't read "
                  + value + " string as i18n because here default language can't be known");
         }
         return res;
         
      } catch (IOException ioex) {
         // not JSON !
         
         // so first attempt to still make something of the value as a string :
         if (value.length() == 0) {
            return null; // guessing it means "no value"
         }
         switch (fieldTypeEnum) {
         case LIST:
            // attempt to read as single value array
            List<Object> singleValueList = new ArrayList<Object>(1);
            try {
               singleValueList.add(mapper.reader(Object.class).readValue(value)); // TODO list field
            } catch (Exception e) {
               singleValueList.add(value); // defaulting to string value
            }
            return singleValueList;
         case I18N:
            throw new ResourceParsingException("Wrong use of this method, can't read "
                  + value + " string as i18n because here default language can't be known", ioex);
         default:
         }
         
         // else error
         throw new ResourceParsingException("IO error while reading " + fieldTypeEnum.getType()
               + "-formatted string : " + value, ioex);
         
      } catch (Exception ex) {
         throw new ResourceParsingException("Field value is not a " + fieldTypeEnum.getType()
               + "-formatted string : " + value, ex);
      }
   }

   /**
    * 
    * @param dcFieldTypeEnum
    * @param stringValue
    * @return
    * @throws ResourceParsingException
    */
   public Object parseValueFromString(DCFieldTypeEnum fieldTypeEnum, String stringValue) throws ResourceParsingException {
      if (stringValue == null) {
         return null;
      }
      if (fieldTypeEnum == null) {
         // guessing is too hard / faulty with Datacore types (and can't switch on null)
         throw new ResourceParsingException("Field type must be provided");
      }

      // first try shortcuts :
      switch (fieldTypeEnum) {
      // string primitives :
      //if (DCFieldTypeEnum.stringPrimitiveTypes.contains(fieldTypeEnum)) {
      case STRING:
      case RESOURCE:
         // they can be returned as is
         //stringValue = quote(stringValue);
         return parseStringPrimitiveValueFromString(stringValue); //return stringValue;
      //if (DCFieldTypeEnum.stringSerializedPrimitiveTypes.contains(fieldTypeEnum)) {
         // they MUST be parsed AS JSON (i.e. quoted string)
         /*if (stringValue.charAt(0) == '\"') {
            throw new ResourceParsingException("date Field value is attempted to be parsed  "
                  + "from unquoted string but is quoted (maybe doubly ?) : " + stringValue);
         }*/
         //stringValue = quote(stringValue);
      // string serialized primitives :
      //if (DCFieldTypeEnum.stringSerializedPrimitiveTypes.contains(fieldTypeEnum)) {
      case DATE:
         return parseDateFromString(stringValue);
      case LONG:
         return parseLongFromString(stringValue);
      case DOUBLE:
         return parseDoubleFromString(stringValue);
      default:
      }
      
      try {
         return parseValueFromJSONInternal(fieldTypeEnum, stringValue);
         
      } catch (IOException ioex) {
         // else error
         throw new ResourceParsingException("IO error while reading " + fieldTypeEnum.getType()
               + "-formatted string : " + stringValue, ioex);
         
      } catch (Exception ex) {
         throw new ResourceParsingException("Field value is not a " + fieldTypeEnum.getType()
               + "-formatted string : " + stringValue, ex);
      }
   }
   
   /**
    * @param stringValue can be returned "as is"
    * @return
    */
   public Object parseStringPrimitiveValueFromString(String stringValue) {
      return stringValue;
   }
   
   /**
    * 
    * @param fieldTypeEnum
    * @param stringValue
    * @return
    * @throws ResourceParsingException
    */
   public Object parseStringSerializedPrimitiveValueFromJSON(
         DCFieldTypeEnum fieldTypeEnum, String stringValue) throws ResourceParsingException {
      try {
         return parseValueFromJSONInternal(fieldTypeEnum, stringValue);
         
      } catch (IOException ioex) {
         // else error
         throw new ResourceParsingException("IO error while reading " + fieldTypeEnum.getType()
               + "-formatted string : " + stringValue, ioex);
         
      } catch (Exception ex) {
         throw new ResourceParsingException("Field value is not a " + fieldTypeEnum.getType()
               + "-formatted string : " + stringValue, ex);
      }
   }

   /**
    * Optimized using Joda DateTime
    * (rather than threaded DateFormat inited from Jackson's, or Jackson mapper)
    * @param stringValue
    * @return
    * @throws ResourceParsingException
    */
   public DateTime parseDateFromString(String stringValue) throws ResourceParsingException {
      /*if (stringValue == null || stringValue.isEmpty()) {
         return null; // no value
      }
      if (stringValue.charAt(0) == '\"') {
         throw new ResourceParsingException("date Field value is attempted to be parsed  "
               + "from unquoted string but is quoted (maybe doubly ?) : " + stringValue);
      }*/
      try {
         //return getDateFormat().parse(stringValue);
         //return new DateTime(stringValue, DateTimeZone.UTC); // like DateTimeDeserializer
         return ResourceParsingHelper.parseDate(stringValue);
      /*} catch (IOException ioex) {
         throw new ResourceParsingException("IO error while reading ISO 8601 Date-formatted string : "
               + stringValue, ioex);*/
      } catch (IllegalArgumentException e) {
         throw new ResourceParsingException("date Field value is not "
               + "an ISO 8601 Date-formatted string : " + stringValue, e);
      }
   }


   public Long parseLong(Object resourceValue, DCResourceParsingContext resourceParsingContext)
         throws ResourceParsingException {
      if (!(resourceValue instanceof String)) {
         if (resourceValue instanceof Long) {
            return (Long) resourceValue; // can only happen if called locally, NOT remotely through jackson
         } else if (resourceValue instanceof Integer) {
            if (resourceParsingContext != null) {
               resourceParsingContext.addWarning("long Field value is a JSON integer : " + resourceValue
                     + ", which allowed as fallback but should rather be a JSON long");
            }
            return new Long((Integer) resourceValue);
         } else {
            // other types ex. Double, float are wrong
            throw new ResourceParsingException("long Field value is not a string : " + resourceValue);
         }
      } else {
         return this.parseLongFromString((String) resourceValue);
      }
   }

   /**
    * Optimized using Long.valueOf() (rather than Jackson mapper)
    * @param stringValue
    * @return null if no value (null or empty)
    * @throws ResourceParsingException
    */
   public Long parseLongFromString(String stringValue) throws ResourceParsingException {
      if (stringValue == null || stringValue.isEmpty()) {
         return null; // no value
      }
      /*if (stringValue.charAt(0) == '\"') {
         throw new ResourceParsingException("date Field value is attempted to be parsed  "
               + "from unquoted string but is quoted (maybe doubly ?) : " + stringValue);
      }*/
      try {
         return Long.valueOf(stringValue);
      } catch (NumberFormatException e) {
         throw new ResourceParsingException("long Field value is not "
               + "a compatible string : " + stringValue, e);
      }
   }
   

   public Double parseDouble(Object resourceValue, DCResourceParsingContext resourceParsingContext)
         throws ResourceParsingException {

      // TODOOOOOOOOOOOOOOOOOOOO Javascript has no double : http://javascript.about.com/od/reference/g/rlong.htm
      // so supported through String instead (or Float as fallback)
      /*if (!(resourceValue instanceof Double)) { // TODO
         if (resourceValue instanceof Float) {
            entityValue = new Double((Float) resourceValue);
         } else if (resourceValue instanceof Integer) {
            entityValue = new Float((Integer) resourceValue);
            resourceParsingContext.addWarning("float Field value is a JSON integer : " + resourceValue
                  + ", which allowed as fallback but should rather be a JSON float");
         } else if (resourceValue instanceof String) {
            
         } else {
            throw new ResourceParsingException("double Field value is not a JSON double nor a float nor a string : " + resourceValue);
         }
      }
      entityValue = (Double) resourceValue; // TODO LATER also allow String  ??*/
      
      if (!(resourceValue instanceof String)) {
         if (resourceValue instanceof Float) {
            return new Double((Float) resourceValue);
         } else if (resourceValue instanceof Double) {
            return (Double) resourceValue; // can only happen if called locally, NOT remotely through jackson
         } else if (resourceValue instanceof Integer) {
            if (resourceParsingContext != null) {
               resourceParsingContext.addWarning("double Field value is a JSON integer : " + resourceValue
                     + ", which allowed as fallback but should rather be a JSON long");
            }
            return new Double((Integer) resourceValue);
         } else {
            // other types ex. Double, float are wrong
            throw new ResourceParsingException("double Field value is not a string or a float : " + resourceValue);
         }
      } else {
         return this.parseDoubleFromString((String) resourceValue);
      }
   }

   /**
    * Optimized using Double.valueOf() (rather than Jackson mapper)
    * @param stringValue
    * @return null if no value (null or empty)
    * @throws ResourceParsingException
    */
   public Double parseDoubleFromString(String stringValue) throws ResourceParsingException {
      if (stringValue == null || stringValue.isEmpty()) {
         return null; // no value
      }
      /*if (stringValue.charAt(0) == '\"') {
         throw new ResourceParsingException("date Field value is attempted to be parsed  "
               + "from unquoted string but is quoted (maybe doubly ?) : " + stringValue);
      }*/
      try {
         return Double.valueOf(stringValue);
      } catch (NumberFormatException e) {
         throw new ResourceParsingException("double Field value is not "
               + "a compatible string : " + stringValue, e);
      }
   }

   
   private String quote(String stringValue) {
      //return "\"" + stringValue + "\"";
      StringBuilder sb = new StringBuilder(stringValue.length() + 2);
      sb.append('\"');
      sb.append(stringValue);
      sb.append('\"');
      return sb.toString();
   }
   
   
   ////////////////////////////////////////////////////////////////
   // serializing to string :
   
   /**
    * Used for model serialization for now only
    * @param value
    * @param fieldTypeEnum
    * @return
    * @throws ResourceParsingException TODO rather ConversionException ?!?
    */
   public String valueToString(Object value/*, DCFieldTypeEnum fieldTypeEnum*/) throws ResourceParsingException {
      if (value == null) {
         return null;
      }
      if (value instanceof Date) {
         if (!(value instanceof DateTime)) {
            // TODO better in mongo persistence
            return new DateTime((Date) value, DateTimeZone.UTC).toString();
            // NB. if not UTC, default timezone has a non-integer time shift
         }
         return value.toString();
      } else {
         Class<? extends Object> valueClass = value.getClass();
         if (valueClass.isPrimitive() || ClassUtils.wrapperToPrimitive(valueClass) != null) {
            // see http://stackoverflow.com/questions/709961/determining-if-an-object-is-of-primitive-type
            return value.toString(); // TODO or quoted for string-likes ? including Long & Double ?
         }
      }
      /*switch (fieldTypeEnum) {
      case STRING:
      case RESOURCE:
         return (String) value; // or quoted ?
      case BOOLEAN:
      case INTEGER:
      case FLOAT:
         return value.toString();
      case DOUBLE:
      case LONG:
         return value.toString(); // or quoted ?
      case DATE:
         return value.toString(); // TODO Date case ? (TimeZone ??) or quoted ?
      default:
      }*/
      // list (including i18n) or map :
      try {
         return mapper.writer().writeValueAsString(value);
      } catch (JsonProcessingException jpex) {
         throw new ResourceParsingException("JSON error while writing value to string : " + value, jpex);
      }
   }
   
}
