package org.oasis.datacore.rest.server.parsing.model;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.rest.api.binding.DatacoreObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OperatorValue {

   private String entityFieldPath;
   private DCField field;
   private QueryOperatorsEnum operatorEnum;
   private Object parsedData;

   /**
    * 
    * @param entityFieldPath
    * @param field
    * @param operatorEnum operator or sort
    * @param parsedData
    */
   public OperatorValue(String entityFieldPath, DCField field,
         QueryOperatorsEnum operatorEnum, Object parsedData) {
      this.entityFieldPath = entityFieldPath;
      this.field = field;
      this.operatorEnum = operatorEnum;
      this.parsedData = parsedData;
   }

   public String getEntityFieldPath() {
      return entityFieldPath;
   }

   public DCField getField() {
      return field;
   }

   public QueryOperatorsEnum getOperatorEnum() {
      return operatorEnum;
   }

   public Object getParsedData() {
      return parsedData;
   }

   private static ObjectMapper objectMapper = new DatacoreObjectMapper();
   public String toString() {
      try {
         return objectMapper.writeValueAsString(this);
      } catch (JsonProcessingException e) {
         return this.getClass().getName() + "[" + this.entityFieldPath + " , bad json]";
      }
   }
   
}
