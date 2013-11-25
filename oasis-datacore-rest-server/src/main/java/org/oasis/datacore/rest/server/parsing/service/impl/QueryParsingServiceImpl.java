package org.oasis.datacore.rest.server.parsing.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.AbstractMap.SimpleEntry;

import org.joda.time.format.ISOPeriodFormat;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.parsing.model.DCQueryParsingContext;
import org.oasis.datacore.rest.server.parsing.model.QueryOperatorsEnum;
import org.oasis.datacore.rest.server.parsing.service.QueryParsingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class QueryParsingServiceImpl implements QueryParsingService {

	@Autowired
	@Qualifier("datacoreApiServer.objectMapper")
	public ObjectMapper mapper = new ObjectMapper();
	
	public void parseCriteriaFromQueryParameter(String fieldPath, String operatorAndValue, 
												DCField dcField, DCQueryParsingContext queryParsingContext) 
												throws ResourceParsingException {
		// TODO (mongo)operator for error & in parse ?
		String entityFieldPath = "_p." + fieldPath;
				
		// QueryOperatorEnum = operator name
		// Integer = operator size
		SimpleEntry<QueryOperatorsEnum, Integer> operatorEntry = QueryOperatorsEnum.getEnumFromOperator(operatorAndValue);
		QueryOperatorsEnum operatorEnum = operatorEntry.getKey();
		int operatorSize = operatorEntry.getValue();
		// We check if the selected operator is suitable for the type of DCField
		isQueryOperatorSuitableForField(dcField, operatorEnum);
		// We get the value of the selected operator
		String queryValue = operatorAndValue.substring(operatorSize);
		// We get the DCFieldType enum according to the type of the field
		DCFieldTypeEnum dcFieldTypeEnum = DCFieldTypeEnum.getEnumFromStringType(dcField.getType());
		Class<T> toClass = dcFieldTypeEnum.getToClass();
		// Then we parse the data
		parseValue(dcField, dcFieldTypeEnum, queryValue);
		
		switch(operatorEnum) {
		
			case ALL: 
				// TODO LATER $all with $elemMatch
				// TODO same fieldPath for mongodb ??
				List<Object> listAll = checkAndParseFieldPrimitiveListValue(dcListField, dcListField.getListElementField(), operatorAndValue.substring(operatorSize));
				queryParsingContext.getCriteria().and(entityFieldPath).all(listAll);
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
				String equalsValue = operatorAndValue.substring(operatorSize);
				queryParsingContext.getCriteria().and(entityFieldPath).is(checkAndParseFieldValue(dcField, equalsValue));
				break;
			
			case EXISTS:
				// TODO TODO rather hasAspect / mixin / type !!!!!!!!!!!!!!!
				// TODO AND / OR field value == null
				// TODO sparse index ?????????
				// TODO TODO can't return false because already failed to find field
				boolean exist = mapper.readValue(operatorAndValue.substring(operatorSize), Boolean.class);
				queryParsingContext.getCriteria().and(entityFieldPath).exists(exist); // TODO same fieldPath for mongodb ??
				break;
			
			case GREATER_OR_EQUAL:
				// TODO LATER allow (joined) resource and order per its default order field ??
				// TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
				// TODO check that indexed (or set low limit) ??
				checkComparable(dcField, fieldPath + operatorAndValue);
				int sortIndexGtoe = addSort(entityFieldPath, operatorAndValue, queryParsingContext);
				String gtoeValue = operatorAndValue.substring(operatorSize, sortIndexGtoe);
				queryParsingContext.getCriteria().and(entityFieldPath).gte(parseFieldValue(dcField, gtoeValue));
				break;
			
			case GREATER_THAN:
				// TODO LATER allow (joined) resource and order per its default order field ??
				// TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
				// TODO check that indexed (or set low limit) ??
				checkComparable(dcField, fieldPath + operatorAndValue);
				int sortIndexGt = addSort(entityFieldPath, operatorAndValue, queryParsingContext);
				String gtValue = operatorAndValue.substring(operatorSize, sortIndexGt);
				queryParsingContext.getCriteria().and(entityFieldPath).gt(parseFieldValue(dcField, gtValue)); // TODO same fieldPath for mongodb ??
				break;
			
			case IN:
				// TODO check that not date ???
				// TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
				// TODO check that indexed (or set low limit) ??
				if ("map".equals(dcField.getType()) || "list".equals(dcField.getType())) {
					throw new ResourceParsingException("$in can't be applied to a " + dcField.getType() + " Field");
				}
				int sortIndexIn = addSort(entityFieldPath, operatorAndValue, queryParsingContext);
				String inValue = operatorAndValue.substring(operatorSize, sortIndexIn);
				queryParsingContext.getCriteria().and(entityFieldPath).in(parseFieldListValue(dcField, inValue)); // TODO same fieldPath for mongodb ??
				break;
		
			case LOWER_OR_EQUAL:
				// TODO LATER allow (joined) resource and order per its default order field ??
				// TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
				// TODO check that indexed (or set low limit) ??
				checkComparable(dcField, fieldPath + operatorAndValue);
				int sortIndexLoe = addSort(entityFieldPath, operatorAndValue, queryParsingContext);
				String loeValue = operatorAndValue.substring(operatorSize, sortIndexLoe); // TODO more than mongodb
				queryParsingContext.getCriteria().and(entityFieldPath).lte(parseFieldValue(dcField, loeValue)); // TODO same fieldPath for mongodb ??
				break;
				
			case LOWER_THAN:
				// TODO LATER allow (joined) resource and order per its default order field ??
				// TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
				// TODO check that indexed (or set low limit) ??
				checkComparable(dcField, fieldPath + operatorAndValue);
				int sortIndex = addSort(entityFieldPath, operatorAndValue, queryParsingContext);
				String stringValue = operatorAndValue.substring(operatorSize, sortIndex); // TODO more than mongodb
				Object value = parseFieldValue(dcField, stringValue);
				queryParsingContext.getCriteria().and(entityFieldPath).lt(value); // TODO same fieldPath for mongodb ??
				break;
				
			case NOT_EQUALS:
				// TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
				// TODO check that indexed (or set low limit) ??
				int sortIndexNe = addSort(entityFieldPath, operatorAndValue, queryParsingContext);
				String neValue = operatorAndValue.substring(operatorSize, sortIndexNe);
				queryParsingContext.getCriteria().and(entityFieldPath).ne(checkAndParseFieldValue(dcField, neValue));
				break;
				
			case NOT_FOUND:
				break;
				
			case NOT_IN:
				// TODO check that not date ???
			    // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
			    // TODO check that indexed (or set low limit) ??
				if ("map".equals(dcField.getType()) || "list".equals(dcField.getType())) {
					throw new ResourceParsingException("$nin can't be applied to a " + dcField.getType() + " Field");
				}
			    int sortIndexNin = addSort(entityFieldPath, operatorAndValue, queryParsingContext);
			    String ninValue = operatorAndValue.substring(operatorSize, sortIndexNin);
			    queryParsingContext.getCriteria().and(entityFieldPath).nin(parseFieldListValue(dcField, ninValue));
				break;
				
			case REGEX:
				if (!"string".equals(dcField.getType())) {
					throw new ResourceParsingException("$regex can only be applied to a string but found " + dcField.getType() + " Field");
				}
			    int sortIndexRegex = addSort(entityFieldPath, operatorAndValue, queryParsingContext); // TODO ????
			    String regexValue = operatorAndValue.substring(operatorSize, sortIndexRegex); // TODO more than mongodb
			    String options = null;
			    if (regexValue.length() != 0 && regexValue.charAt(0) == '/') {
			    	int lastSlashIndex = regexValue.lastIndexOf('/');
			    	if (lastSlashIndex != 0) {
			    		options = regexValue.substring(lastSlashIndex + 1);
			            value = regexValue.substring(1, lastSlashIndex);
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
				if (!"list".equals(dcField.getType())) {
					throw new ResourceParsingException("$size can only be applied to a list but found " + dcField.getType() + " Field");
				}
			    Integer sizeValue = mapper.readValue(operatorAndValue.substring(operatorSize), Integer.class); 
			    queryParsingContext.getCriteria().and(entityFieldPath).size(sizeValue);
			
			case SORT_ASC:
				// TODO LATER allow (joined) resource and order per its default order field ??
				// TODO check that not i18n (which is map ! ; or allow fallback, order for locale) ???
				// TODO check that indexed (or set low limit) ??
				checkComparable(dcField, fieldPath + operatorAndValue);
				queryParsingContext.addSort(new Sort(Direction.ASC, entityFieldPath));
				break;
				
			case SORT_DESC:
				// TODO LATER allow (joined) resource and order per its default order field ??
				// TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
				// TODO check that indexed (or set low limit) ?!??
				checkComparable(dcField, fieldPath + operatorAndValue);
				queryParsingContext.addSort(new Sort(Direction.ASC, entityFieldPath));
				break;
				
			default:
				// defaults to "equals"
			    // TODO check that indexed ??
			    // TODO check that not i18n (which is map ! ; or use locale or allow fallback) ???
			    // NB. can't sort a single value
			    queryParsingContext.getCriteria().and(entityFieldPath).is(checkAndParseFieldValue(dcField, operatorAndValue));
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
	
	private Object parseValue(DCField dcField, DCFieldTypeEnum dcFieldTypeEnum, String queryValue) throws ResourceParsingException {
		
		if(dcField != null && queryValue != null && !"".equals(queryValue)) {
			if(dcFieldTypeEnum != null) {
				try {
					return mapper.readValue(queryValue, dcFieldTypeEnum.getToClass());
				} catch (Exception e) {
					throw new ResourceParsingException("error while reading " + dcFieldTypeEnum.getType() + "-formatted string : " + queryValue, e);
				}
			}
		}
		
		return null;
		
	}
	
	/**
    * 
    * @param fieldPath TODO or better DCField ?
    * @param operatorAndValue
    * @param queryParsingContext
    * @return sortIndex (ex. operatorAndValue length if no sort suffix)
    */
	public int addSort(String fieldPath, String operatorAndValue, DCQueryParsingContext queryParsingContext) {
		
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
	   

	public Object checkAndParseFieldValue(DCField dcField, String stringValue) throws ResourceParsingException {
		
		if ("map".equals(dcField.getType()) || "list".equals(dcField.getType())) {
			throw new ResourceParsingException("operator can't be applied to a " + dcField.getType() + " Field");
		}
		
		return parseFieldValue(dcField, stringValue);
	}
		
	public Object parseFieldValue(DCField dcField, String stringValue) throws ResourceParsingException {
		try {
			if ("string".equals(dcField.getType())) {
				return stringValue;
			} else if ("boolean".equals(dcField.getType())) {
				return mapper.parseBoolean(stringValue);
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
			}
			// TODO i18n+wkt
		} catch (ResourceParsingException rpex) {
			throw rpex;
		} catch (IOException ioex) {
			throw new ResourceParsingException("IO error while reading integer-formatted string : " + stringValue, ioex);
		} catch (Exception ex) {
			throw new ResourceParsingException("Not an integer-formatted string : " + stringValue, ex);
		}
		throw new ResourceParsingException("Unsupported field type " + dcField.getType());
	}

	public List<Object> checkAndParseFieldPrimitiveListValue(DCField dcField, DCField listElementField, String stringListValue) throws ResourceParsingException {
		
		if (!"list".equals(dcField.getType())) {
			throw new ResourceParsingException("list operator can only be applied to a list but found " + dcField.getType() + " Field");
		}
		if ("map".equals(listElementField.getType()) || "list".equals(listElementField.getType())) {
			throw new ResourceParsingException("list operator can't be applied to a " + listElementField.getType() + " list element Field");
		}

		return parseFieldListValue(listElementField, stringListValue);
	}

	public List<Object> parseFieldListValue(DCField listElementField, String stringListValue) throws ResourceParsingException {
		
		try {
			return datacoreApiImpl.mapper.readValue(listElementField, List.class);
		} catch (IOException e) {
			throw new ResourceParsingException("IO error while reading list-formatted string : " + stringListValue, e);
		} catch (Exception e) {
			throw new ResourceParsingException("Not a list-formatted string : " + stringListValue, e);
		}
	}

	   
	
}
