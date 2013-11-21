package org.oasis.datacore.rest.server.parsing.service;

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

	public void parseCriteriaFromQueryParameter(String fieldPath, String operatorAndValue, DCField dcField, DCListField dcListField, DCQueryParsingContext queryParsingContext)
			throws ResourceParsingException;

}
