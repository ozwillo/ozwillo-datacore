package org.oasis.datacore.rest.server.parsing.service.impl;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.parsing.model.DCQueryParsingContext;
import org.oasis.datacore.rest.server.parsing.model.QueryOperatorsEnum;
import org.oasis.datacore.rest.server.parsing.service.QueryParsingService;
import org.oasis.datacore.rest.server.resource.ValueParsingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

@Service
public class QueryParsingServiceImpl implements QueryParsingService {

   @Autowired
   public ValueParsingService valueParsingService;

   public void parseCriteriaFromQueryParameter(String operatorAndValue,
         DCField dcField, DCQueryParsingContext queryParsingContext) 
               throws ResourceParsingException {
      if (operatorAndValue == null) {
         // should not happen, at worse ""
         throw new ResourceParsingException("Missing value");
      }

      // QueryOperatorEnum = operator name
      // Integer = operator size
      SimpleEntry<QueryOperatorsEnum, Integer> operatorEntry = QueryOperatorsEnum.getEnumFromOperator(operatorAndValue);
      QueryOperatorsEnum operatorEnum = operatorEntry.getKey();
      if (operatorEnum == null) {
         throw new ResourceParsingException("Query operator wasn't identified for query parameter "
               + operatorAndValue);
      }

      // We check if the selected operator is suitable for the type of DCField
      isQueryOperatorSuitableForField(dcField, operatorEnum);

      // If the operator is a sort (e.g. name=-) there is no value !
      // So we don't need to substring to remove the operator from the value as there is no value
      String queryValue = null;
      QueryOperatorsEnum sortEnum = null;
      Object parsedData = null;
      int operatorSize = operatorEntry.getValue();
      if (!QueryOperatorsEnum.noValueOperators.contains(operatorEnum)) {
         // We get the value of the selected operator
         queryValue = operatorAndValue.substring(operatorSize);
         // We need to know if we need to sort the value
         sortEnum = isSortNeeded(queryValue);
         // If field is sorted we need to remove the last char (+ or -)
         if(sortEnum != null) {
            queryValue = queryValue.substring(0, queryValue.length()-1);
         }
         // Then we parse the data
         parsedData = parseQueryValue(operatorEnum, dcField, queryValue);
      }
      
      // update queryParsingContext aggregated info :
      // queryLimit :
      // (TODO LATER maybe per operator algo...)
      if (dcField.getQueryLimit() > queryParsingContext.getAggregatedQueryLimit()) {
         queryParsingContext.setAggregatedQueryLimit(dcField.getQueryLimit());
      }
      // hasNoIndexedField :
      queryParsingContext.setHasNoIndexedField(
            queryParsingContext.isHasNoIndexedField() || dcField.getQueryLimit() == 0);

      DCFieldTypeEnum dcFieldTypeEnum = DCFieldTypeEnum.getEnumFromStringType(dcField.getType()); // TODO LATER enum type
      
      switch(operatorEnum) {

      case ALL: 
         // TODO LATER $all with $elemMatch
         // TODO same fieldPath for mongodb ??
         if(parsedData instanceof ArrayList<?>) {
            queryParsingContext.addCriteria().all((Collection<?>)parsedData);
         }
         break;

      case ELEM_MATCH:
         // parsing using the latest upmost list field :
         // TODO WARNING the first element in the array must be selective, because all documents
         // containing it are scanned
         if (!(parsedData instanceof LinkedHashMap<?, ?>)) {
            // should be OK since has been checked to be of MAP parsing type and has been parsed by Jackson
            throw new ResourceParsingException("$elemMatch criteria value should be JSON parsed as LinkedHashMap !");
         }
         DCField listElementField = ((DCListField) dcField).getListElementField(); // has been checked before
         if (!(DCFieldTypeEnum.MAP.equals(DCFieldTypeEnum.getEnumFromStringType(listElementField.getType())))) {
            throw new ResourceParsingException("$elemMatch criteria value should be on list whose elements are maps !");
         }
         Map<String, DCField> listElementMapFields = ((DCMapField) listElementField).getMapFields(); // has been checked before
         @SuppressWarnings("unchecked")
         LinkedHashMap<String,Object> elemMatchCriteriaMap = (LinkedHashMap<String, Object>) parsedData;
         if (elemMatchCriteriaMap.isEmpty()) {
            break; // nothing to do
         }
         Iterator<Entry<String, Object>> entryIt = elemMatchCriteriaMap.entrySet().iterator();
         Entry<String, Object> entry = entryIt.next();
         Criteria elemMatchCriteria = new Criteria(entry.getKey()).is(entry.getValue());
         // TODO LATER support more than "equals", by moving out of Spring Data or reusing existing parsing algo
         for (; entryIt.hasNext();) {entry = entryIt.next();
            String subFieldName = entry.getKey();
            if (!listElementMapFields.containsKey(subFieldName)) {
               throw new ResourceParsingException("Trying to build an $elemMatch criteria on unknown field "
                     + subFieldName + " of map (in list) " + queryParsingContext.peekResourceValue().getFullValuedPath()
                      + " with fields " + listElementMapFields.keySet());
            }
            // TODO LATER support more than "equals", by moving out of Spring Data or reusing existing parsing algo
            elemMatchCriteria = elemMatchCriteria.and(subFieldName).is(entry.getValue());
         }
         queryParsingContext.addCriteria().elemMatch(elemMatchCriteria);

         // TODO which (other) syntax ??
         // alt 1 : don't support it as a different operator but auto use it on list fields
         // (save if ex. boolean mode...) ; and add OR syntax here using ex. &join=OR syntax NO WRONG 
         // alt 2 : support it explicitly, with pure mongo syntax NO NOT IN HTTP GET QUERY
         // alt 3 : support it explicitly, with Datacore query syntax MAYBE ex. :

         // (((TODO rather using another syntax ?? NOT FOR NOW rather should first add OR syntax
         // (i.e. autojoins ?!) to OR-like 'equals' on list elements, or prevent them)))
         // parsing using the mongodb syntax (so no sort) :
         
         // TODO arrays of arrays using {$elemMatch:{$elemMatch:
         // http://stackoverflow.com/questions/12629692/querying-an-array-of-arrays-in-mongodb
         break;

      case EQUALS:
         // TODO if null parsedData, AND / OR !$exists
         queryParsingContext.addCriteria().is(parsedData);
         break;

      case EXISTS:
         // NB. checks existence of value AND NOT of field in model / mixin type
         // (which has rather to be done using )
         // TODO AND / OR field value == null
         // TODO sparse index ?????????
         // TODO TODO can't return false because already failed to find field
         queryParsingContext.addCriteria().exists(true);
         break;

      case GREATER_OR_EQUAL:
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
         queryParsingContext.addSort(sortEnum);
         queryParsingContext.addCriteria().gte(parsedData);
         break;

      case GREATER_THAN:
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
         queryParsingContext.addSort(sortEnum);
         queryParsingContext.addCriteria().gt(parsedData);
         break;

      case IN:
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
         queryParsingContext.addSort(sortEnum);
         queryParsingContext.addCriteria().in((Collection<?>) parsedData); // BEWARE else taken as an object (array of array)
         break;

      case LOWER_OR_EQUAL:
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
         queryParsingContext.addSort(sortEnum);
         queryParsingContext.addCriteria().lte(parsedData);
         break;

      case LOWER_THAN:
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
         queryParsingContext.addSort(sortEnum);
         queryParsingContext.addCriteria().lt(parsedData);
         break;

      case NOT_EQUALS:
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
         queryParsingContext.addSort(sortEnum);
         queryParsingContext.addCriteria().ne(parsedData);
         break;

      case NOT_IN:
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
         queryParsingContext.addSort(sortEnum);
         queryParsingContext.addCriteria().nin((Collection<?>) parsedData); // BEWARE else taken as an object (array of array)
         break;

      case REGEX:
         String regexValue = (String)parsedData;
         String options = null;
         if (regexValue.length() != 0 && regexValue.charAt(0) == '/') {
            int lastSlashIndex = regexValue.lastIndexOf('/');
            if (lastSlashIndex != 0) {
               options = regexValue.substring(lastSlashIndex + 1);
            }
         }
         // TODO prevent or warn if first character(s) not provided in regex (making it much less efficient)
         if (options == null) {
            queryParsingContext.addCriteria().regex(regexValue);
         } else {
            queryParsingContext.addCriteria().regex(regexValue, options);
         }
         break;

      case SIZE:
         // TODO (mongo)operator for error & in parse ?
         // parsing using the latest upmost list field :
         // NB. mongo arrays with millions of items are supported, but let's not go in the Long area
         queryParsingContext.addCriteria().size((int) parsedData);
         break;

      case SORT_ASC:		// TODO (mongo)operator for error & in parse ?

         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that not i18n (which is map ! ; or allow fallback, order for locale) ???
         // TODO check that indexed (or set low limit) ??
         queryParsingContext.addSort(QueryOperatorsEnum.SORT_ASC);
         break;

      case SORT_DESC:
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ?!??
         queryParsingContext.addSort(QueryOperatorsEnum.SORT_DESC);
         break;

      default:
         // defaults to "equals"
         // TODO check that indexed ??
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // NB. can't sort a single value
         queryParsingContext.addCriteria().is(parsedData);
         break;

      }

   }

   private void isQueryOperatorSuitableForField(DCField dcField, QueryOperatorsEnum queryOperatorsEnum) throws ResourceParsingException {

      if(dcField == null || queryOperatorsEnum == null) {
         throw new ResourceParsingException("Field or query operator can't be null");
      }
      
      DCFieldTypeEnum dcFieldTypeEnum = DCFieldTypeEnum.getEnumFromStringType(dcField.getType());
      if(DCFieldTypeEnum.NOT_FOUND.name().equals(dcFieldTypeEnum.name())) {
         return; // TODO can that even happen ??
      }
      
      if (queryOperatorsEnum.isListSpecificOperator()) {
         // if list-specific operator ex. $all, field must be a list
         // (and its element field compatible with query value, but that is checked later)
         if (!DCFieldTypeEnum.LIST.equals(dcFieldTypeEnum)) {
            throw new ResourceParsingException("Field of type " + dcField.getType()
                  + " is not compatible with operator " + queryOperatorsEnum.name()
                  + " : list-specific operators only apply to list fields !");
         }
      } else {
         while (DCFieldTypeEnum.LIST.equals(dcFieldTypeEnum)) {
            // if list (and not list-specific operator ex. $all), it is its inner field's parsingType
            dcField = ((DCListField) dcField).getListElementField();
            dcFieldTypeEnum = DCFieldTypeEnum.getEnumFromStringType(dcField.getType());
         }
         if(!queryOperatorsEnum.getListCompatibleTypes().contains(dcFieldTypeEnum)) {
            throw new ResourceParsingException("Field of type " + dcField.getType() + " is not compatible with query operator " + queryOperatorsEnum.name());
         }
      }

   }


   public Object parseQueryValue(QueryOperatorsEnum operatorEnum, DCField dcField,
         String value) throws ResourceParsingException {
      DCFieldTypeEnum parsingTypeEnum = operatorEnum.getParsingType();
      if (parsingTypeEnum == null) {
         // We get the DCFieldType enum according to the type of the field
         parsingTypeEnum = DCFieldTypeEnum.getEnumFromStringType(dcField.getType());
         if (DCFieldTypeEnum.LIST.equals(parsingTypeEnum)) {
            // if list (and not list-specific operator ex. $all), it is its inner field's parsingType
            parsingTypeEnum = DCFieldTypeEnum.getEnumFromStringType(
                  ((DCListField) dcField).getListElementField().getType());
         }

         if (parsingTypeEnum == null) {
            throw new ResourceParsingException("Can't find the type of field " + dcField.getName());
         }
      } // else operator with specific type, ex. $all with list type

      // special list case
      /*switch(operatorEnum) {
      case IN:
      case NOT_IN:
         // TODO if non-JSON, unquoted string, attempt to parse it as dcField.type and wrap it in collection
      default:
      }*/

      // other cases
      Object parsedValue = valueParsingService.parseValueFromJSONOrString(parsingTypeEnum, value);
      /*if (parsedValue == null) {
         throw new ResourceParsingException("Field " + dcField.getName() + " cannot be parsed in "
               + parsingTypeEnum.getType() + " format");
      }*/
      return parsedValue;
   }


   private QueryOperatorsEnum isSortNeeded(String queryValue) {
      if (queryValue == null || queryValue.isEmpty()) {
         return null;
      }

      char lastChar = queryValue.charAt(queryValue.length() - 1);
      switch (lastChar) {
      case '+' :
         return QueryOperatorsEnum.SORT_ASC;
      case '-' :
         return QueryOperatorsEnum.SORT_DESC;
      }

      return null;
   }

}
