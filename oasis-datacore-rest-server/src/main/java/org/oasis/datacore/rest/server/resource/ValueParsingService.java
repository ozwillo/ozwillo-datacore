package org.oasis.datacore.rest.server.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
import org.oasis.datacore.rest.api.util.ResourceParsingHelper;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;


/**
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
            // first, tried JSON case
            return value.substring(1, valueLength - 1);
         }
         if (DCFieldTypeEnum.RESOURCE.equals(fieldTypeEnum)) {
            //value = quote(value);
            return parseStringPrimitiveValueFromString(value); //return value;
         } // else try to parse embedded or root resource
         
      } else if (DCFieldTypeEnum.stringSerializedPrimitiveTypes.contains(fieldTypeEnum)) {
         // they MUST be parsed AS JSON (i.e. quoted string)
         int valueLength = value.length();
         if (valueLength <= 1 || value.charAt(0) != '\"' || value.charAt(valueLength -1) != '\"') {
            value = quote(value);
         } // else assuming JSON case
         return parseStringSerializedPrimitiveValueFromString(fieldTypeEnum, value);
      }
      
      try {
         // then try to parse as json :
         return parseValueFromJSONInternal(fieldTypeEnum, value);
         
      } catch (IOException ioex) {
         // not JSON !
         
         // so first attempt to still make something of the value as a string :
         if (value.length() == 0) {
            return null; // guessing it means "no value"
         }
         switch (fieldTypeEnum) {
         case STRING:
            // notably unquoted regex query criteria case such as $regex.*Bord.*
            return value;
         case LIST:
            // attempt to read as single value array
            List<Object> singleValueList = new ArrayList<Object>(1);
            try {
               singleValueList.add(mapper.reader(Object.class).readValue(value)); // TODO list field
            } catch (Exception e) {
               singleValueList.add(value); // defaulting to string value
            }
            return singleValueList;
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
    * @param value
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
      case DATE:
         return parseDateFromString(stringValue);
      case LONG:
         return parseLongFromString(stringValue);
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
   
   public Object parseStringPrimitiveValueFromString(String stringValue) {
      return stringValue;
   }
   
   public Object parseStringSerializedPrimitiveValueFromString(
         DCFieldTypeEnum fieldTypeEnum, String stringValue) throws ResourceParsingException {
      try {
         return parseValueFromJSONInternal(fieldTypeEnum, quote(stringValue));
         
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
         throw new ResourceParsingException("date Field value is not "
               + "an ISO 8601 Date-formatted string : " + stringValue, e);
      }
   }

   private String quote(String stringValue) {
      //return "\"" + stringValue + "\"";
      StringBuilder sb = new StringBuilder('\"');
      sb.append(stringValue);
      sb.append('\"');
      return sb.toString();
   }
   
}
