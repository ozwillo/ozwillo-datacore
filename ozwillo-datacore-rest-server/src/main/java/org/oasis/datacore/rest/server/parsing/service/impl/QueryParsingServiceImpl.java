package org.oasis.datacore.rest.server.parsing.service.impl;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.oasis.datacore.core.entity.query.fulltext.Tokenizer;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
import org.oasis.datacore.core.meta.model.DCI18nField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.parsing.model.DCQueryParsingContext;
import org.oasis.datacore.rest.server.parsing.model.OperatorValue;
import org.oasis.datacore.rest.server.parsing.model.QueryOperatorsEnum;
import org.oasis.datacore.rest.server.parsing.service.QueryParsingService;
import org.oasis.datacore.rest.server.resource.ValueParsingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@Component("datacore.queryParsingService") // else can't autowire Qualified ; NOT @Service (permissions rather around EntityService)
public class QueryParsingServiceImpl implements QueryParsingService {

   @Autowired
   public ValueParsingService valueParsingService;

   /** for fulltext */
   @Autowired
   public Tokenizer tokenizer;


   @Override
   public void parseQueryParameter(String entityFieldPath, DCField dcField,
         String operatorAndValue, DCQueryParsingContext queryParsingContext) 
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
      
      // shortcuts :
      if ("i18n".equals(dcField.getType())) {
         if (!operatorEnum.isListSpecificOperator() && !operatorEnum.equals(QueryOperatorsEnum.EXISTS)) {
            String fieldPathElement = DCI18nField.KEY_VALUE;
            entityFieldPath = entityFieldPath + '.' + fieldPathElement;
            // first setting as i18n to still allow global language :
            queryParsingContext.setEntityFieldPathAsI18nValue(entityFieldPath, null); // ex. _p.i18n:name.v
            // i18n shortcut - if no language specified, by default lookup by value (and not the language) :
            dcField = ((DCListField) dcField).getListElementField();
            dcField = ((DCMapField) dcField).getMapFields().get(fieldPathElement);
         } // otherwise ex. operators on array
      }
      
      if (dcField.isFulltext() && operatorEnum == QueryOperatorsEnum.FULLTEXT) {
         String entityPathPrefixAboveFulltext = queryParsingContext.isI18nValueFieldPath(entityFieldPath) ?
               entityFieldPath.replaceAll("[^\\.]+\\." + DCI18nField.KEY_VALUE + "$", "")
               : entityFieldPath.replaceAll("[^\\.]+$", "");
         String fulltextFieldName = entityPathPrefixAboveFulltext + DCField.FULLTEXT_FIELD_NAME
               + "." + DCI18nField.KEY_VALUE;
         queryParsingContext.getFieldOperatorStorageNameMap().put(entityFieldPath, fulltextFieldName);
      }

      // We check if the selected operator is suitable for the type of DCField
      isQueryOperatorSuitableForField(dcField, operatorEnum);

      // If the operator is a sort (e.g. name=-) there is no value !
      // So we don't need to substring to remove the operator from the value as there is no value
      String queryValue = null;
      QueryOperatorsEnum supplSortEnum = null;
      Object parsedData = null;
      int operatorSize = operatorEntry.getValue();
      if (!QueryOperatorsEnum.sortOperators.contains(operatorEnum)) {
         // We get the value of the selected operator
         queryValue = operatorAndValue.substring(operatorSize);
         // We need to know if we need to sort the value
         supplSortEnum = isSortNeeded(queryValue);
         // If field is sorted we need to remove the last char (+ or -)
         if (supplSortEnum != null) {
            queryParsingContext.addOperatorValue(entityFieldPath, dcField, supplSortEnum, null);
            queryValue = queryValue.substring(0, queryValue.length()-1);
         }
         // Then we parse the data
         parsedData = parseQueryValue(operatorEnum, dcField, queryValue);
      }

      queryParsingContext.addOperatorValue(entityFieldPath, dcField, operatorEnum, parsedData);
      
      // update queryParsingContext aggregated info :
      // queryLimit :
      // (TODO LATER maybe per operator algo...)
      if (dcField.getQueryLimit() > queryParsingContext.getAggregatedQueryLimit()) {
         queryParsingContext.setAggregatedQueryLimit(dcField.getQueryLimit());
      }
      // hasNoIndexedField :
      queryParsingContext.setHasNoIndexedField(
            queryParsingContext.isHasNoIndexedField() || dcField.getQueryLimit() == 0);
   }

   @Override
   public void buildCriteria(DCQueryParsingContext queryParsingContext) {
      for (List<OperatorValue> fieldOperatorValues : queryParsingContext.getOperatorValues()) {
         String entityFieldPath = fieldOperatorValues.get(0).getEntityFieldPath(); // (never empty)
         queryParsingContext.enterCriteria(entityFieldPath);
         
         try  {
            // parsing multiple values (of a field that is mentioned several times) :
            // (such as {limit=[10], founded=[>"-0143-04-01T00:00:00.000Z", <"-0043-04-02T00:00:00.000Z"]})
            // NB. can't be done by merely chaining .and(...)'s because of mongo BasicDBObject limitations, see
            // http://www.mkyong.com/java/due-to-limitations-of-the-basicdbobject-you-cant-add-a-second-and/
            for (OperatorValue operatorValue : fieldOperatorValues) {
               try {
                  this.buildCriteria(operatorValue, queryParsingContext);
                  
               } catch (Exception ex) {
                  queryParsingContext.addError("Error while parsing query criteria "
                        + operatorValue, ex); // even RuntimeException, but this allows to be more lenient than 503
               }
            }
            
         } finally {
            queryParsingContext.exitCriteria();
         }
      }
   }
   
   private void buildCriteria(OperatorValue operatorValue, DCQueryParsingContext queryParsingContext) 
               throws ResourceParsingException {
      ///DCFieldTypeEnum dcFieldTypeEnum = DCFieldTypeEnum.getEnumFromStringType(dcField.getType()); // TODO LATER enum type
      DCField dcField = operatorValue.getField();
      QueryOperatorsEnum operatorEnum = operatorValue.getOperatorEnum();
      Object parsedData = operatorValue.getParsedData();
      
      switch(operatorEnum) {

      case ALL: 
         // TODO LATER $all with $elemMatch
         // TODO same fieldPath for mongodb ??
         if(parsedData instanceof ArrayList<?>) {
            queryParsingContext.addCriteria().all((Collection<?>)parsedData);
         }
         break;

      case ELEM_MATCH:
         // (for now tested in the case of i18n)
         // parsing using the latest upmost list field :
         // TODO WARNING the first element in the array must be selective, because all documents
         // containing it are scanned
         if (!(parsedData instanceof LinkedHashMap<?, ?>)) {
            // should be OK since has been checked to be of MAP parsing type and has been parsed by Jackson
            throw new ResourceParsingException("$elemMatch criteria value should be JSON parsed as LinkedHashMap !");
         }
         DCField listElementField = ((DCListField) dcField).getListElementField(); // has been checked before
         //if (!(DCFieldTypeEnum.MAP.equals(DCFieldTypeEnum.getEnumFromStringType(listElementField.getType())))) {
         if (!(listElementField instanceof DCMapField)) { // allows i18n also
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
         // TODO HACK CUSTOM first handling custom serialization fields (LATER pluggable) :
         if (parsedData != null && dcField.getName().equals(DCResource.KEY_DCCREATED)) {
            ObjectId createdDateObjectId = (ObjectId) parsedData;
            // ObjectId timestamp in seconds provides dc:created, so look between this second and the next :
            // NB. done as multi criteria, else error :
            // "Due to limitations of the com.mongodb.BasicDBObject, you can't add a second '_id' expression ..."
            queryParsingContext.addCriteria().gte(createdDateObjectId);
            // NB. ObjectId starts by createdDate.getMillis() / 1000 http://steveridout.github.io/mongo-object-time/
            DateTime nextSecondDate = new DateTime(createdDateObjectId.getDate()).plusSeconds(1);
            queryParsingContext.addCriteria().lt(new ObjectId(nextSecondDate.toDate()));
            break;
         }
         // TODO if null parsedData, AND / OR !$exists
         queryParsingContext.addCriteria().is(parsedData);
         break;

      case EXISTS:
         // NB. checks existence of value AND NOT of field in model / mixin type
         // (which has rather to be done using )
         // TODO AND / OR field value == null
         // TODO sparse index ?????????
         // TODO TODO can't return false because already failed to find field
         if (parsedData == null) {
            parsedData = true; // default
         } else if (!(parsedData instanceof Boolean)) {
            throw new ResourceParsingException(operatorEnum + "'s only parameter should be a boolean");
         }
         queryParsingContext.addCriteria().exists((Boolean) parsedData);
         break;

      case GREATER_OR_EQUAL:
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that indexed (or set low limit) ??
         queryParsingContext.addCriteria().gte(parsedData);
         break;

      case GREATER_THAN:
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that indexed (or set low limit) ??
         queryParsingContext.addCriteria().gt(parsedData);
         break;

      case IN:
         // TODO HACK CUSTOM first handling custom serialization fields :
         if (dcField.getName().equals(DCResource.KEY_DCCREATED)) {
            throw new ResourceParsingException(operatorEnum + " not yet implemented for "
                  + DCResource.KEY_DCCREATED + ", rather use betweens"); // TODO LATER auto, see equals
         }
         // TODO check that indexed (or set low limit) ??
         queryParsingContext.addCriteria().in((Collection<?>) parsedData); // BEWARE else taken as an object (array of array)
         break;

      case LOWER_OR_EQUAL:
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that indexed (or set low limit) ??
         queryParsingContext.addCriteria().lte(parsedData);
         break;

      case LOWER_THAN:
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that indexed (or set low limit) ??
         queryParsingContext.addCriteria().lt(parsedData);
         break;

      case NOT_EQUALS:
         // TODO HACK CUSTOM first handling custom serialization fields :
         if (dcField.getName().equals(DCResource.KEY_DCCREATED)) {
            throw new ResourceParsingException(operatorEnum + " not yet implemented for "
                  + DCResource.KEY_DCCREATED + ", rather use betweens"); // TODO LATER auto, see equals
         }
         // TODO check that indexed (or set low limit) ??
         queryParsingContext.addCriteria().ne(parsedData);
         break;

      case NOT_IN:
         // TODO HACK CUSTOM first handling custom serialization fields :
         if (dcField.getName().equals(DCResource.KEY_DCCREATED)) {
            throw new ResourceParsingException(operatorEnum + " not yet implemented for "
                  + DCResource.KEY_DCCREATED + ", rather use betweens"); // TODO LATER auto, see equals
         }
         // TODO check that not i18n (which is map ! or use value or (context) default language or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
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

      case FULLTEXT:
         if (!dcField.isFulltext()) {
            // NB. in case of i18n, has been copied on value field from containing i18n field
            throw new ResourceParsingException("Attempting a fulltext search on a field that is not configured so");
         }
         
         String tokenString = (String) parsedData;
         List<String> tokens = tokenizer.tokenize(tokenString).stream().collect(Collectors.toList()); // TODO check that trimmed & no empty
         for (String token : tokens) {
            queryParsingContext.addCriteria().regex('^' // only checking start of the token, else not efficient
                  + token + queryParsingContext.getFulltextRegexSuffix()); // TODO ???? bonus : value could be regex (without implicit "start" symbol)
            // TODO support subresources & query on l
         }
         // NB. if none, no mongo criteria will have been added at all
         break;

      case SIZE:
         // TODO (mongo)operator for error & in parse ?
         // parsing using the latest upmost list field :
         // NB. mongo arrays with millions of items are supported, but let's not go in the Long area
         queryParsingContext.addCriteria().size((int) parsedData);
         break;
         
      case SORT_ASC:
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that indexed (or set low limit) ??
         queryParsingContext.addSort(QueryOperatorsEnum.SORT_ASC);
         break;

      case SORT_DESC:
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that indexed (or set low limit) ?!??
         queryParsingContext.addSort(QueryOperatorsEnum.SORT_DESC);
         break;

      default:
         throw new ResourceParsingException("Unknown query operator " + operatorEnum);
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
         //if (!DCFieldTypeEnum.LIST.equals(dcFieldTypeEnum)) {
         if (!(dcField instanceof DCListField)) { // allows i18n also
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
         if(!queryOperatorsEnum.getCompatibleTypes().contains(dcFieldTypeEnum)) {
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
      // TODO HACK CUSTOM handling custom serialization fields (LATER pluggable) :
      if (parsedValue != null && dcField.getName().equals(DCResource.KEY_DCCREATED)) {
         DateTime createdDate = (DateTime) parsedValue;
         // ObjectId timestamp in seconds provides dc:created :
         // NB. ObjectId starts by createdDate.getMillis() / 1000 http://steveridout.github.io/mongo-object-time/
         parsedValue = new ObjectId(createdDate.toDate());
      }
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
