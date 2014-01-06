package org.oasis.datacore.rest.server.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;


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
    * @param dcFieldTypeEnum
    * @param queryValue
    * @return
    * @throws ResourceParsingException
    */
   public Object parseValue(DCFieldTypeEnum dcFieldTypeEnum, String value) throws ResourceParsingException {
      if (value == null) {
         return null;
      }
      
      Class<?> readAsClass = (dcFieldTypeEnum != null) ? dcFieldTypeEnum.getToClass() : Object.class;
      try {
         ObjectReader reader = mapper.reader(readAsClass);
         return reader.readValue(value);
         
      } catch (IOException ioex) {
         // not JSON !
         // so first attempt to still make something of the value as a string :
         if (value.length() == 0) {
            return null; // guessing it means "no value"
         }
         switch (dcFieldTypeEnum) {
            case STRING :
            case RESOURCE : // reference (root & embedded done above by reader)
               return value;
            case LIST :
               // attempt to read as single value array
               List<Object> singleValueList = new ArrayList<Object>(1);
               try {
                  singleValueList.add(mapper.reader(Object.class).readValue(value));
               } catch (Exception e) {
                  singleValueList.add(value); // defaulting to string value
               }
               return singleValueList;
            default :
         }
         // else error
         throw new ResourceParsingException("IO error while reading " + readAsClass
               + "-formatted string : " + value, ioex);
         
      } catch (Exception ex) {
         throw new ResourceParsingException("Field value is not a " + dcFieldTypeEnum.getType()
               + "-formatted string : " + value, ex);
      }
   }
   
}
