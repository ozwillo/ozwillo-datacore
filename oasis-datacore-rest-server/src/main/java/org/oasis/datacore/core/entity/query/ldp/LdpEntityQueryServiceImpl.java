package org.oasis.datacore.core.entity.query.ldp;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.query.QueryException;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.core.meta.model.DCMapField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.rest.server.DatacoreApiImpl;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.parsing.model.DCQueryParsingContext;
import org.oasis.datacore.rest.server.parsing.model.DCResourceParsingContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;


/**
 * Provides W3C LDP (Linked Data Platform)-like query on top of Datacore MongoDB storage.
 * TODO LATER move to -core : by removing parsing (& dependency to queryParsingContext)
 * & temporary dependency to DatacoreApiImpl, or by making -core depend on -server...
 * 
 * @author mdutoo
 *
 */
@Component
public class LdpEntityQueryServiceImpl implements LdpEntityQueryService {
   
   private static Set<String> findConfParams = new HashSet<String>();
   static {
      // TODO rather using Enum, see BSON$RegexFlag
      findConfParams.add("start");
      findConfParams.add("limit");
   }

   //@Autowired
   //private DCDataEntityRepository dataRepo; // NO rather for (meta)model, for data can't be used because can't specify collection
   @Autowired
   private MongoOperations mgo; // TODO remove it by hiding it in services
   
   @Autowired
   private DatacoreApiImpl datacoreApiImpl; // temporary, to call stuff to be moved elesewhere

   @Override
   public List<DCEntity> findDataInType(DCModel dcModel, Map<String, List<String>> params,
         Integer start, Integer limit) throws QueryException {
      boolean detailedErrorsMode = true; // TODO
      
      String modelType = dcModel.getName(); // for error logging

      // parsing query parameters criteria according to model :
      DCQueryParsingContext queryParsingContext = new DCQueryParsingContext(dcModel, null);
      
      parameterLoop : for (String fieldPath : params.keySet()) {
         if (findConfParams.contains(fieldPath)) {
            // skip find conf params
            continue;
         }
         
         //DCField dcField = dcModelService.getFieldByPath(dcModel, fieldPath); // TODO TODOOOOOOOOOOOOO
         // TODO move impl :
         String[] fieldPathElements = fieldPath.split("\\."); // (escaping regex) mongodb field path syntax
         // TODO LATER also XML fieldPath.split("/") ??
         if (fieldPathElements.length == 0) {
            continue; // should not happen
         }
         DCField dcField = dcModel.getField(fieldPathElements[0]);
         if (dcField == null) {
            queryParsingContext.addError("In type " + modelType + ", can't find field with path elements "
                  + Arrays.asList(fieldPathElements) + " : can't find field for first path element "
                  + fieldPathElements[0]);
            continue;
         }
         
         // finding the leaf field
         
         // finding the latest higher list field :
         DCListField dcListField = null;
         if ("list".equals(dcField.getType())) {
            dcListField = (DCListField) dcField;
            do {
               dcField = ((DCListField) dcField).getListElementField();
            } while ("list".equals(dcField.getType()));
         }
         
         // loop on path elements for finding the leaf field :
         for (int i = 1; i < fieldPathElements.length; i++) {
            String fieldPathElement = fieldPathElements[i];
            if ("map".equals(dcField.getType())) {
               dcField = ((DCMapField) dcField).getMapFields().get(fieldPathElement);
               if (dcField == null) {
                  queryParsingContext.addError("In type " + modelType + ", can't find field with path elements"
                        + Arrays.asList(fieldPathElements) + ": can't go below " + i + "th path element "
                        + fieldPathElement + ", because field is unkown");
                  continue parameterLoop;
               }
            } else if ("resource".equals(dcField.getType())) {
               queryParsingContext.addError("Found criteria requiring join : in type " + modelType + ", field "
                     + fieldPath + " (" + i + "th in field path elements " + Arrays.asList(fieldPathElements)
                     + ") can't be done in findDataInType, do it rather on client side");
               continue parameterLoop; // TODO boum
            } else {
               queryParsingContext.addError("In type " + modelType + ", can't find field with path elements"
                     + Arrays.asList(fieldPathElements) + ": can't go below " + i + "th element " 
                     + fieldPathElement + ", because field is neither map nor list but " + dcField.getType());
               continue parameterLoop; // TODO boum
            }

            if ("list".equals(dcField.getType())) {
               // finding the latest higher list field :
               dcListField = (DCListField) dcField;
               do {
                  dcField = ((DCListField) dcField).getListElementField();
                  // TODO TODO check that indexed (or set low limit) ??
               } while ("list".equals(dcField.getType()));
            } else {
               dcListField = null;
            }
         }
         
         List<String> values = params.get(fieldPath);
         if (values == null || values.size() == 0) {
            queryParsingContext.addError("Missing value for parameter " + fieldPath);
            continue;
         } // should not happen
         String operatorAndValue = values.get(0);
         if (operatorAndValue == null) {
            queryParsingContext.addError("Missing value for parameter " + fieldPath);
            continue; // should not happen
         }
         
         // parsing query parameter criteria according to model field :
         // TODO LATER using ANTLR ?!?
         // recognizes MongoDB criteria (operators & values), see http://docs.mongodb.org/manual/reference/operator/query/
         // and fills Spring Criteria with them
         
         try {
            parseCriteriaFromQueryParameter(fieldPath, operatorAndValue,
                  dcField, dcListField, queryParsingContext);
         } catch (Exception ex) {
            queryParsingContext.addError("Error while parsing query criteria " + fieldPath
                  + operatorAndValue, ex);
         }
      }
      

      if (queryParsingContext.hasErrors()) {
         String msg = DCResourceParsingContext.formatParsingErrorsMessage(queryParsingContext, detailedErrorsMode);
         throw new QueryException(msg);
      } // else TODO if warnings return them as response header ?? or only if failIfWarningsMode ??
      
      
      // adding paging & sorting :
      if (start > 500) {
         start = 500; // max (conf'ble in model ?), else prefer ranged query ; TODO or error message ?
      }
      if (limit > 50) {
         limit = 50; // max (conf'ble in model ?), else prefer ranged query ; TODO or error message ?
      }
      Sort sort = queryParsingContext.getSort();
      if (sort == null) {
         // TODO sort by default : configured in model (uri, last modified date, iri?, other fields...)
         sort = new Sort(Direction.ASC, "_uri");
      }
      Query springMongoQuery = new Query(queryParsingContext.getCriteria())
         .with(sort).skip(start).limit(limit); // TODO rather range query, if possible on sort field
         
      // executing the mongo query :
      String collectionName = dcModel.getCollectionName(); // TODO getType() or getCollectionName(); for weird type names ??
      List<DCEntity> foundEntities = mgo.find(springMongoQuery, DCEntity.class, collectionName);
      
      return foundEntities;
   }

   


   private void parseCriteriaFromQueryParameter(String fieldPath,
         String operatorAndValue, DCField dcField, DCListField dcListField,
         DCQueryParsingContext queryParsingContext) throws ResourceParsingException {
      // TODO (mongo)operator for error & in parse ?
      String entityFieldPath = "_p." + fieldPath;
      
      if (operatorAndValue.startsWith("=")
            || operatorAndValue.startsWith("==")) { // java-like
         // TODO check that indexed ??
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // NB. can't sort a single value
         String stringValue = operatorAndValue.substring(1); // TODO more than mongodb
         Object value = checkAndParseFieldValue(dcField, stringValue);
         queryParsingContext.getCriteria().and(entityFieldPath).is(value); // TODO same fieldPath for mongodb ??
         
      } else if (operatorAndValue.equals("+")) {
         checkComparable(dcField, fieldPath + operatorAndValue);
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ?!??
         queryParsingContext.addSort(new Sort(Direction.ASC, entityFieldPath));

      } else if (operatorAndValue.equals("-")) {
         checkComparable(dcField, fieldPath + operatorAndValue);
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that not i18n (which is map ! ; or allow fallback, order for locale) ???
         // TODO check that indexed (or set low limit) ??
         queryParsingContext.addSort(new Sort(Direction.ASC, entityFieldPath));
         
      } else if (operatorAndValue.startsWith(">")
            || operatorAndValue.startsWith("&gt;") // xml // TODO ; ?
            || operatorAndValue.startsWith("$gt")) { // mongodb
         checkComparable(dcField, fieldPath + operatorAndValue);
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
         int sortIndex = addSort(entityFieldPath, operatorAndValue, queryParsingContext);
         String stringValue = operatorAndValue.substring(3, sortIndex); // TODO more than mongodb
         Object value = parseFieldValue(dcField, stringValue);
         queryParsingContext.getCriteria().and(entityFieldPath).gt(value); // TODO same fieldPath for mongodb ??
         
      } else if (operatorAndValue.startsWith("<")
            || operatorAndValue.startsWith("&lt;") // xml // TODO ; ?
            || operatorAndValue.startsWith("$lt")) { // mongodb
         checkComparable(dcField, fieldPath + operatorAndValue);
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
         int sortIndex = addSort(entityFieldPath, operatorAndValue, queryParsingContext);
         String stringValue = operatorAndValue.substring(3, sortIndex); // TODO more than mongodb
         Object value = parseFieldValue(dcField, stringValue);
         queryParsingContext.getCriteria().and(entityFieldPath).lt(value); // TODO same fieldPath for mongodb ??
         
      } else if (operatorAndValue.startsWith(">=")
            || operatorAndValue.startsWith("&gt;=") // xml // TODO ; ?
            || operatorAndValue.startsWith("$gte")) { // mongodb
         checkComparable(dcField, fieldPath + operatorAndValue);
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
         int sortIndex = addSort(entityFieldPath, operatorAndValue, queryParsingContext);
         String stringValue = operatorAndValue.substring(4, sortIndex); // TODO more than mongodb
         Object value = parseFieldValue(dcField, stringValue);
         queryParsingContext.getCriteria().and(entityFieldPath).gte(value); // TODO same fieldPath for mongodb ??
         
      } else if (operatorAndValue.startsWith("<=")
            || operatorAndValue.startsWith("&lt;=") // xml // TODO ; ?
            || operatorAndValue.startsWith("$lte")) { // mongodb
         checkComparable(dcField, fieldPath + operatorAndValue);
         // TODO LATER allow (joined) resource and order per its default order field ??
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
         int sortIndex = addSort(entityFieldPath, operatorAndValue, queryParsingContext);
         String stringValue = operatorAndValue.substring(4, sortIndex); // TODO more than mongodb
         Object value = parseFieldValue(dcField, stringValue);
         queryParsingContext.getCriteria().and(entityFieldPath).lte(value); // TODO same fieldPath for mongodb ??

      } else if (operatorAndValue.startsWith("<>")
            || operatorAndValue.startsWith("&lt;&gt;") // xml // TODO may happen, ';' ??
            || operatorAndValue.startsWith("$ne") // mongodb
            || operatorAndValue.startsWith("!=")) { // java-like
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
         int sortIndex = addSort(entityFieldPath, operatorAndValue, queryParsingContext); // TODO ???????
         String stringValue = operatorAndValue.substring(3, sortIndex); // TODO more than mongodb
         Object value = checkAndParseFieldValue(dcField, stringValue);
         queryParsingContext.getCriteria().and(entityFieldPath).ne(value); // TODO same fieldPath for mongodb ??
         
      } else if (operatorAndValue.startsWith("$in")) { // mongodb
         if ("map".equals(dcField.getType()) || "list".equals(dcField.getType())) {
            throw new ResourceParsingException("$in can't be applied to a " + dcField.getType() + " Field");
         }
         // TODO check that not date ???
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
         int sortIndex = addSort(entityFieldPath, operatorAndValue, queryParsingContext); // TODO ???
         String stringValue = operatorAndValue.substring(3, sortIndex); // TODO more than mongodb
         List<Object> value = parseFieldListValue(dcField, stringValue);
         queryParsingContext.getCriteria().and(entityFieldPath).in(value); // TODO same fieldPath for mongodb ??
         
      } else if (operatorAndValue.startsWith("$nin")) { // mongodb
         if ("map".equals(dcField.getType()) || "list".equals(dcField.getType())) {
            throw new ResourceParsingException("$nin can't be applied to a " + dcField.getType() + " Field");
         }
         // TODO check that not date ???
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // TODO check that indexed (or set low limit) ??
         int sortIndex = addSort(entityFieldPath, operatorAndValue, queryParsingContext); // TODO ???
         String stringValue = operatorAndValue.substring(4, sortIndex); // TODO more than mongodb
         List<Object> value = parseFieldListValue(dcField, stringValue);
         queryParsingContext.getCriteria().and(entityFieldPath).nin(value); // TODO same fieldPath for mongodb ??
         
      } else if (operatorAndValue.startsWith("$regex")) { // mongodb
         if (!"string".equals(dcField.getType())) {
            throw new ResourceParsingException("$regex can only be applied to a string but found "
                  + dcField.getType() + " Field");
         }
         int sortIndex = addSort(entityFieldPath, operatorAndValue, queryParsingContext); // TODO ????
         String value = operatorAndValue.substring(6, sortIndex); // TODO more than mongodb
         String options = null;
         if (value.length() != 0 && value.charAt(0) == '/') {
            int lastSlashIndex = value.lastIndexOf('/');
            if (lastSlashIndex != 0) {
               options = value.substring(lastSlashIndex + 1);
               value = value.substring(1, lastSlashIndex);
            }
         }
         // TODO prevent or warn if first character(s) not provided in regex (making it much less efficient)
         if (options == null) {
            queryParsingContext.getCriteria().and(entityFieldPath).regex(value); // TODO same fieldPath for mongodb ??
         } else {
            queryParsingContext.getCriteria().and(entityFieldPath).regex(value, options); // TODO same fieldPath for mongodb ??
         }
         
      } else if (operatorAndValue.startsWith("$exists")) { // mongodb field
         // TODO TODO rather hasAspect / mixin / type !!!!!!!!!!!!!!!
         // TODO AND / OR field value == null
         // TODO sparse index ?????????
         Boolean value = datacoreApiImpl.parseBoolean(operatorAndValue.substring(7));
         // TODO TODO can't return false because already failed to find field
         queryParsingContext.getCriteria().and(entityFieldPath).exists(value); // TODO same fieldPath for mongodb ??
         
      //////////////////////////////////
      // list (array) operators :
         
      // NB. all items are of the same type
         
      } else if (operatorAndValue.startsWith("$all")) { // mongodb array
         // parsing using the latest upmost list field :
         List<Object> value = checkAndParseFieldPrimitiveListValue(dcListField, dcListField.getListElementField(),
               operatorAndValue.substring(4)); // TODO more than mongodb
         // TODO LATER $all with $elemMatch
         queryParsingContext.getCriteria().and(entityFieldPath).all(value); // TODO same fieldPath for mongodb ??
         
      } else if (operatorAndValue.startsWith("$elemMatch")) { // mongodb array
         if (!"list".equals(dcField.getType())) {
            throw new ResourceParsingException("$elemMatch can only be applied to a list but found "
                  + dcField.getType() + " Field");
         }
         // parsing using the latest upmost list field :
         // WARNING the first element in the array must be selective, because all documents
         // containing it are scanned
         
         // TODO which syntax ??
         // TODO alt 1 : don't support it as a different operator but auto use it on list fields
         // (save if ex. boolean mode...) ; and add OR syntax here using ex. &join=OR syntax NO WRONG 
         // alt 2 : support it explicitly, with pure mongo syntax NO NOT IN HTTP GET QUERY
         // alt 3 : support it explicitly, with Datacore query syntax MAYBE ex. :
         
         // (((TODO rather using another syntax ?? NOT FOR NOW rather should first add OR syntax
         // (i.e. autojoins ?!) to OR-like 'equals' on list elements, or prevent them)))
         // parsing using the mongodb syntax (so no sort) :
         String stringValue = operatorAndValue.substring(10);
         /*
         Criteria value = parseCriteria(dcListField.getListElementField(),
               operatorAndValue.substring(10)); // TODO more than mongodb
         ///Map<String,Object> value = parseMapValue(listField, stringValue);
         Map<String,Object> mapValue;
         try {
            mapValue = mapper.readValue((String) stringValue, Map.class);
         } catch (IOException ioex) {
            throw new ResourceParsingException("IO error while Object-formatted string : "
                  + stringValue, ioex);
         } catch (Exception e) {
            throw new ResourceParsingException("$elemMatch operator value is not "
                  + "an Object-formatted string : " + stringValue, e);
         }
         for (String criteriaKey : mapValue.keySet()) {
            Object criteriaValue = mapValue.get(criteriaKey);
            DCField criteriaField = dcListField.getListElementField();
            parseCriteriaFromQueryParameter(fieldPath + '.' + criteriaKey,
                  criteriaValue, criteriaField, null, queryParsingContext); // or next highest dcListField rather than null ??
         }
         queryParsingContext.getCriteria().and(fieldPath).elemMatch(value); // TODO same fieldPath for mongodb ??
         */
         // TODO more than mongodb
         
      } else if (operatorAndValue.startsWith("$size")) { // mongodb array
         // TODO (mongo)operator for error & in parse ?
         if (!"list".equals(dcField.getType())) {
            throw new ResourceParsingException("$size can only be applied to a list but found "
                  + dcField.getType() + " Field");
         }
         // parsing using the latest upmost list field :
         // NB. mongo arrays with millions of items are supported, but let's not go in the Long area
         Integer value = datacoreApiImpl.parseInteger(operatorAndValue.substring(5)); 
         queryParsingContext.getCriteria().and(entityFieldPath).size(value); // TODO same fieldPath for mongodb ??
         
      } else {
         // defaults to "equals"
         // TODO check that indexed ??
         // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
         // NB. can't sort a single value
         Object value = checkAndParseFieldValue(dcField, operatorAndValue);
         queryParsingContext.getCriteria().and(entityFieldPath).is(value); // TODO same fieldPath for mongodb ??
      }
   }

   private void checkComparable(DCField dcField, String stringCriteria)
         throws ResourceParsingException {
      if ("map".equals(dcField.getType())
            || "list".equals(dcField.getType())
         // NB. though could be applied on array, since in mongo, sort of an array is based
         // on the min or max value as said at https://jira.mongodb.org/browse/SERVER-5596
            || "resource".equals(dcField.getType())) {
         // TODO LATER allow (joined) resource and order per its default order field ??
         throw new ResourceParsingException("Field of type " + dcField.getType() + " is not comparable");
      }
      // NB. date allowed for range check, see http://cookbook.mongodb.org/patterns/date_range/
      // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
      // TODO check that indexed (or set low limit) ?!??
   }

   /**
    * 
    * @param fieldPath TODO or better DCField ?
    * @param operatorAndValue
    * @param queryParsingContext
    * @return sortIndex (ex. operatorAndValue length if no sort suffix)
    */
   private int addSort(String fieldPath, String operatorAndValue,
         DCQueryParsingContext queryParsingContext) {
      int operatorAndValueLength = operatorAndValue.length();
      char lastChar = operatorAndValue.charAt(operatorAndValueLength - 1);
      switch (lastChar) {
      case '+' :
         queryParsingContext.addSort(new Sort(Direction.ASC, fieldPath));
         break;
      case '-' :
         queryParsingContext.addSort(new Sort(Direction.DESC, fieldPath));
         break;
      default :
         return operatorAndValueLength;
      }
      return operatorAndValueLength - 1;
   }
   
   
   ////////////////////////////////////////
   // Parsing
   // TODO move to dedicated ResourceQueryParsingService/Helper

   // TODO move to ResourceQueryParsingService/Helper
   private Object checkAndParseFieldValue(DCField dcField, String stringValue) throws ResourceParsingException {
      if ("map".equals(dcField.getType()) || "list".equals(dcField.getType())) {
         throw new ResourceParsingException("operator can't be applied to a " + dcField.getType() + " Field");
      }
      return parseFieldValue(dcField, stringValue);
   }
   // TODO move to ResourceQueryParsingService/Helper
   private Object parseFieldValue(DCField dcField, String stringValue) throws ResourceParsingException {
      try {
         if ("string".equals(dcField.getType())) {
            return stringValue;
            
         } else if ("boolean".equals(dcField.getType())) {
            return datacoreApiImpl.parseBoolean(stringValue);
            
         } else if ("int".equals(dcField.getType())) {
            return datacoreApiImpl.parseInteger(stringValue);
            
         } else if ("float".equals(dcField.getType())) {
            return datacoreApiImpl.mapper.readValue(stringValue, Float.class);
            
         } else if ("long".equals(dcField.getType())) {
            return datacoreApiImpl.mapper.readValue(stringValue, Long.class);
            
         } else if ("double".equals(dcField.getType())) {
            return datacoreApiImpl.mapper.readValue(stringValue, Double.class);
            
         } else if ("date".equals(dcField.getType())) {
            return datacoreApiImpl.parseDate(stringValue);
            
         } else if ("resource".equals(dcField.getType())) {
            // TODO resource better ex. allow auto joins ?!?
            return stringValue;
            
         /*} else if ("i18n".equals(dcField.getType())) { // TODO i18n better
            entityValue = (HashMap<?,?>) resourceValue; // TODO NOOOO _i18n
            // TODO locale & fallback
            
         } else if ("wkt".equals(dcField.getType())) { // TODO LATER2 ??
            entityValue = (String) resourceValue;*/
         }
         
      } catch (ResourceParsingException rpex) {
         throw rpex;
      } catch (IOException ioex) {
         throw new ResourceParsingException("IO error while reading integer-formatted string : "
               + stringValue, ioex);
      } catch (Exception ex) {
         throw new ResourceParsingException("Not an integer-formatted string : "
               + stringValue, ex);
      }
      
      throw new ResourceParsingException("Unsupported field type " + dcField.getType());
   }


   // TODO move in ResourceQueryParsingService/Helper
   private List<Object> checkAndParseFieldPrimitiveListValue(DCField dcField, DCField listElementField,
         String stringListValue) throws ResourceParsingException {
      if (!"list".equals(dcField.getType())) {
         throw new ResourceParsingException("list operator can only be applied to a list but found "
               + dcField.getType() + " Field");
      }
      if ("map".equals(listElementField.getType()) || "list".equals(listElementField.getType())) {
         throw new ResourceParsingException("list operator can't be applied to a "
               + listElementField.getType() + " list element Field");
      }
      return parseFieldListValue(listElementField, stringListValue);
   }

   // TODO move in ResourceQueryParsingService/Helper
   @SuppressWarnings("unchecked")
   private List<Object> parseFieldListValue(DCField listElementField,
         String stringListValue) throws ResourceParsingException {
      try {
         return datacoreApiImpl.mapper.readValue(stringListValue, List.class);
      } catch (IOException e) {
         throw new ResourceParsingException("IO error while reading list-formatted string : "
               + stringListValue, e);
      } catch (Exception e) {
         throw new ResourceParsingException("Not a list-formatted string : "
               + stringListValue, e);
      }
   }
}
