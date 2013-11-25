package org.oasis.datacore.rest.server.parsing.service;

import java.util.List;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.parsing.model.DCQueryParsingContext;

/**
 * 
 * @author agiraudon
 * 
 */

public interface QueryParsingService {

	public void parseCriteriaFromQueryParameter(String fieldPath, String operatorAndValue, DCField dcField, DCQueryParsingContext queryParsingContext) throws ResourceParsingException;

	public void checkComparable(DCField dcField, String stringCriteria) throws ResourceParsingException;

	public int addSort(String fieldPath, String operatorAndValue, DCQueryParsingContext queryParsingContext);
	
	public Object checkAndParseFieldValue(DCField dcField, String stringValue) throws ResourceParsingException;
	
	public Object parseFieldValue(DCField dcField, String stringValue) throws ResourceParsingException;
	
	public List<Object> checkAndParseFieldPrimitiveListValue(DCField dcField, DCField listElementField, String stringListValue) throws ResourceParsingException;
	
	public List<Object> parseFieldListValue(DCField listElementField, String stringListValue) throws ResourceParsingException;
	
}
