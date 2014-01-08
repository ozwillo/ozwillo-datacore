package org.oasis.datacore.rest.server.parsing.service.impl;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.parsing.model.DCQueryParsingContext;
import org.oasis.datacore.rest.server.parsing.model.QueryOperatorsEnum;
import org.oasis.datacore.rest.server.parsing.service.QueryParsingService;
import org.oasis.datacore.rest.server.resource.ValueParsingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

@Service
public class QueryParsingServiceImpl implements QueryParsingService {

	@Autowired
	public ValueParsingService valueParsingService;
	
	public void parseCriteriaFromQueryParameter(String fieldPath, String operatorAndValue, 
												DCField dcField, DCQueryParsingContext queryParsingContext) 
												throws ResourceParsingException {
		// TODO (mongo)operator for error & in parse ?
		String entityFieldPath = "_p." + fieldPath;
		
		// QueryOperatorEnum = operator name
		// Integer = operator size
		SimpleEntry<QueryOperatorsEnum, Integer> operatorEntry = QueryOperatorsEnum.getEnumFromOperator(operatorAndValue);
		QueryOperatorsEnum operatorEnum = operatorEntry.getKey();
      if (operatorEnum == null) {
         throw new ResourceParsingException("Query operator wasn't identified for query parameter " + operatorAndValue);
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
		
		switch(operatorEnum) {
		
			case ALL: 
				// TODO LATER $all with $elemMatch
				// TODO same fieldPath for mongodb ??
				if(parsedData instanceof ArrayList<?>) {
					queryParsingContext.getCriteria().and(entityFieldPath).all((Collection<?>)parsedData);
				}
				break;
				
			case ELEM_MATCH:
				if (!"list".equals(dcField.getType())) {
					throw new ResourceParsingException("$elemMatch can only be applied to a list but found " + dcField.getType() + " Field");
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
				break;
				
			case EQUALS:
            // TODO if null parsedData, AND / OR !$exists
				queryParsingContext.getCriteria().and(entityFieldPath).is(parsedData);
				break;
			
			case EXISTS:
				// NB. checks existence of value AND NOT of field in model / mixin type
			   // (which has rather to be done using )
				// TODO AND / OR field value == null
				// TODO sparse index ?????????
				// TODO TODO can't return false because already failed to find field
				queryParsingContext.getCriteria().and(entityFieldPath).exists(true); // TODO same fieldPath for mongodb ??
				break;
			
			case GREATER_OR_EQUAL:
				// TODO LATER allow (joined) resource and order per its default order field ??
				// TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
				// TODO check that indexed (or set low limit) ??
				addSort(entityFieldPath, sortEnum, queryParsingContext);
				queryParsingContext.getCriteria().and(entityFieldPath).gte(parsedData);
				break;
			
			case GREATER_THAN:
				// TODO LATER allow (joined) resource and order per its default order field ??
				// TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
				// TODO check that indexed (or set low limit) ??
				addSort(entityFieldPath, sortEnum, queryParsingContext);
				queryParsingContext.getCriteria().and(entityFieldPath).gt(parsedData); // TODO same fieldPath for mongodb ??
				break;
			
			case IN:
				// TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
				// TODO check that indexed (or set low limit) ??
				addSort(entityFieldPath, sortEnum, queryParsingContext);
				queryParsingContext.getCriteria().and(entityFieldPath) // TODO same fieldPath for mongodb ??
				   .in((Collection<?>) parsedData); // BEWARE else taken as an object (array of array)
				break;
		
			case LOWER_OR_EQUAL:
				// TODO LATER allow (joined) resource and order per its default order field ??
				// TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
				// TODO check that indexed (or set low limit) ??
				addSort(entityFieldPath, sortEnum, queryParsingContext);
				queryParsingContext.getCriteria().and(entityFieldPath).lte(parsedData); // TODO same fieldPath for mongodb ??
				break;
				
			case LOWER_THAN:
				// TODO LATER allow (joined) resource and order per its default order field ??
				// TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
				// TODO check that indexed (or set low limit) ??
				addSort(entityFieldPath, sortEnum, queryParsingContext);
				queryParsingContext.getCriteria().and(entityFieldPath).lt(parsedData); // TODO same fieldPath for mongodb ??
				break;
				
			case NOT_EQUALS:
				// TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
				// TODO check that indexed (or set low limit) ??
				addSort(entityFieldPath, sortEnum, queryParsingContext);
				queryParsingContext.getCriteria().and(entityFieldPath).ne(parsedData);
				break;
				
			case NOT_IN:
			    // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
			    // TODO check that indexed (or set low limit) ??
				addSort(entityFieldPath, sortEnum, queryParsingContext);
			    queryParsingContext.getCriteria().and(entityFieldPath) // TODO same fieldPath for mongodb ??
			       .nin((Collection<?>) parsedData); // BEWARE else taken as an object (array of array)
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
			    	queryParsingContext.getCriteria().and(entityFieldPath).regex(regexValue);
			    } else {
			    	queryParsingContext.getCriteria().and(entityFieldPath).regex(regexValue, options);
			    }
			    break;
			 
			case SIZE:
				// TODO (mongo)operator for error & in parse ?
				// parsing using the latest upmost list field :
			    // NB. mongo arrays with millions of items are supported, but let's not go in the Long area
				queryParsingContext.getCriteria().and(entityFieldPath).size((int)parsedData);
			
			case SORT_ASC:		// TODO (mongo)operator for error & in parse ?

				// TODO LATER allow (joined) resource and order per its default order field ??
				// TODO check that not i18n (which is map ! ; or allow fallback, order for locale) ???
				// TODO check that indexed (or set low limit) ??
				queryParsingContext.addSort(new Sort(Direction.ASC, entityFieldPath));
				break;
				
			case SORT_DESC:
				// TODO LATER allow (joined) resource and order per its default order field ??
				// TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
				// TODO check that indexed (or set low limit) ?!??
				queryParsingContext.addSort(new Sort(Direction.DESC, entityFieldPath));
				break;
				
			default:
				// defaults to "equals"
			    // TODO check that indexed ??
			    // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
			    // NB. can't sort a single value
			    queryParsingContext.getCriteria().and(entityFieldPath).is(parsedData);
				break;
				
		}
		
	}
	  
	private void isQueryOperatorSuitableForField(DCField dcField, QueryOperatorsEnum queryOperatorsEnum) throws ResourceParsingException {
		
		if(dcField != null && queryOperatorsEnum != null) {
			DCFieldTypeEnum dcFieldTypeEnum = DCFieldTypeEnum.getEnumFromStringType(dcField.getType());
			if(!"NOT_FOUND".equals(dcFieldTypeEnum.name())) {
				if(!queryOperatorsEnum.getListCompatibleTypes().contains(dcFieldTypeEnum)) {
					throw new ResourceParsingException("Field of type " + dcField.getType() + " is not compatible with query operator " + queryOperatorsEnum.name());
				} else {
					// If the field is a list we also check the element type
					if(dcField instanceof DCListField) {
						DCField listField = ((DCListField)dcField).getListElementField();
						if(listField != null) {
							if(DCFieldTypeEnum.LIST.getType().equals(listField.getType()) || DCFieldTypeEnum.MAP.getType().equals(listField.getType())) {
								throw new ResourceParsingException("Field of type " + dcField.getType() + " is not compatible with operator " + queryOperatorsEnum.name());
							}
						}
					}
				}
			}
		} else {
			throw new ResourceParsingException("Field or query operator can't be null");
		}
		
	}

	
   public Object parseQueryValue(QueryOperatorsEnum operatorEnum, DCField dcField,
         String value) throws ResourceParsingException {
      DCFieldTypeEnum parsingTypeEnum = operatorEnum.getParsingType();
      if (parsingTypeEnum == null) {
         // We get the DCFieldType enum according to the type of the field
         parsingTypeEnum = DCFieldTypeEnum.getEnumFromStringType(dcField.getType());
         if (parsingTypeEnum == null) {
            throw new ResourceParsingException("Can't find the type of field " + dcField.getName());
         }
      }
      
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
	
	
	public void addSort(String fieldPath, QueryOperatorsEnum sortEnum, DCQueryParsingContext queryParsingContext) {
		
		if (sortEnum != null) {
			switch (sortEnum) {
			case SORT_ASC:
				queryParsingContext.addSort(new Sort(Direction.ASC, fieldPath));
				break;
			case SORT_DESC:
				queryParsingContext.addSort(new Sort(Direction.DESC, fieldPath));
				break;
			default:
				break;
			}
		}
		
	}
	
	private QueryOperatorsEnum isSortNeeded(String queryValue) {
		
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
