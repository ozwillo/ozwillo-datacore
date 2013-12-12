package org.oasis.datacore.rest.server.parsing.service;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
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
    * Parses value according to given field type.
	 * @param dcFieldTypeEnum
	 * @param queryValue
	 * @return
	 * @throws ResourceParsingException
	 */
	public Object parseValue(DCFieldTypeEnum dcFieldTypeEnum, String queryValue) throws ResourceParsingException;
		
}
