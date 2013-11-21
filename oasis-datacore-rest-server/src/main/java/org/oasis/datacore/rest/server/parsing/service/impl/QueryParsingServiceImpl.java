package org.oasis.datacore.rest.server.parsing.service.impl;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCListField;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;
import org.oasis.datacore.rest.server.parsing.model.DCQueryParsingContext;
import org.oasis.datacore.rest.server.parsing.service.QueryParsingService;
import org.springframework.stereotype.Service;

@Service
public class QueryParsingServiceImpl implements QueryParsingService {

	@Override
	public void parseCriteriaFromQueryParameter(String fieldPath, String operatorAndValue, DCField dcField, DCListField dcListField, DCQueryParsingContext queryParsingContext)
			throws ResourceParsingException {
		// TODO Auto-generated method stub
		
	}

	
	
}
