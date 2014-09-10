package org.oasis.datacore.contribution.service;

import java.util.List;

import javax.ws.rs.BadRequestException;

import org.oasis.datacore.contribution.exception.ContributionDatacoreCheckFailException;
import org.oasis.datacore.contribution.exception.ContributionWithNoModelException;
import org.oasis.datacore.contribution.exception.ContributionWithoutResourcesException;
import org.oasis.datacore.contribution.rest.api.DCContribution;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.server.BadUriException;
import org.oasis.datacore.rest.server.resource.ResourceException;
import org.oasis.datacore.rest.server.resource.ResourceTypeNotFoundException;

public interface ContributionService {

	public DCContribution create(DCContribution dcContribution) throws ContributionWithoutResourcesException, ContributionWithNoModelException, ContributionDatacoreCheckFailException, BadUriException, ResourceException;

	public List<DCContribution> get(String modelType, String userId, String contributionId, int start, int limit) throws RuntimeException, BadRequestException;
	
	public List<DCResource> get(String modelType, String contributionId);
	
	/**
	 * 
	 * @param modelType
	 * @param contributionId
	 * @return
	 * @throws ResourceTypeNotFoundException
    * @throws ResourceException if asked to abort from within triggered event
	 */
	public boolean remove(String modelType, String contributionId) throws ResourceTypeNotFoundException, ResourceException;
	
}
