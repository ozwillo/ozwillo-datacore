package org.oasis.datacore.rest.server.parsing.service;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.parsing.model.DCQueryParsingContext;
import org.oasis.datacore.rest.server.parsing.model.QueryOperatorsEnum;

/**
 * 
 * @author agiraudon
 * 
 */

public interface QueryParsingService {

	public void parseCriteriaFromQueryParameter(String fieldPath, String operatorAndValue, DCField dcField, DCQueryParsingContext queryParsingContext) throws ResourceParsingException;

	public void addSort(String fieldPath, QueryOperatorsEnum sortEnum, DCQueryParsingContext queryParsingContext);
	
	/**
	 * Parses using the operatorEnum.parsingType if any, else the given dcField type
	 * @param operatorEnum
	 * @param dcField
	 * @param value
	 * @return
	 * @throws ResourceParsingException
	 */
	Object parseQueryValue(QueryOperatorsEnum operatorEnum, DCField dcField,
         String value) throws ResourceParsingException;
	
}
