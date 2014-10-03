package org.oasis.datacore.contribution.api.impl;

import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.oasis.datacore.contribution.exception.ContributionDatacoreCheckFailException;
import org.oasis.datacore.contribution.exception.ContributionWithNoModelException;
import org.oasis.datacore.contribution.exception.ContributionWithoutResourcesException;
import org.oasis.datacore.contribution.rest.api.ContributionApi;
import org.oasis.datacore.contribution.rest.api.DCContribution;
import org.oasis.datacore.contribution.service.ContributionService;
import org.oasis.datacore.core.security.service.DatacoreSecurityService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.server.resource.ResourceException;
import org.oasis.datacore.rest.server.resource.ResourceTypeNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Path("dc/c")
@Component("datacore.contribution.apiImpl") // else can't autowire Qualified ; NOT @Service (permissions rather around EntityService)
public class ContributionApiImpl implements ContributionApi {

	@Autowired
	private ContributionService contributionService;
	
	@Autowired
	private DatacoreSecurityService datacoreSecurityService;
	
	public DCContribution createContribution(DCContribution contribution) {
		
		DCContribution returnedContribution = null;
		
		try {
			returnedContribution = contributionService.create(contribution);
		} catch (ContributionWithoutResourcesException e) {
			throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build());
		} catch (ContributionWithNoModelException e) {
			throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build());
		} catch (ContributionDatacoreCheckFailException e) {
			throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build());
		} catch (Exception e) {
			throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build());
		}
				
		throw new WebApplicationException(Response.status(Response.Status.OK).entity(returnedContribution).type(MediaType.APPLICATION_JSON).build());
		
	}

	public List<DCContribution> getContribution(String modelType) {
		
		String userId = datacoreSecurityService.getCurrentUserId();
		
		if(userId == null) {
			throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity("You must be identified while doing this request").type(MediaType.TEXT_PLAIN).build()); 
		}
		
		List<DCContribution> listContribution = contributionService.get(modelType, userId, null, 0, 200);
		
		throw new WebApplicationException(Response.status(Response.Status.OK).entity(listContribution).type(MediaType.APPLICATION_JSON).build());
		
	}

	public List<DCContribution> getContribution(String modelType, String contributionId) {
				
		String userId = datacoreSecurityService.getCurrentUserId();
		
		if(userId == null) {
			throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity("You must be identified while doing this request").type(MediaType.TEXT_PLAIN).build()); 
		}
		
		List<DCContribution> listContribution = contributionService.get(modelType, userId, contributionId, 0, 200);
		
		throw new WebApplicationException(Response.status(Response.Status.OK).entity(listContribution).type(MediaType.APPLICATION_JSON).build());
		
	}

	public List<DCContribution> getContributionResources(String modelType, String contributionId) {
		
		List<DCResource> listResources = contributionService.get(modelType, contributionId);
		
		throw new WebApplicationException(Response.status(Response.Status.OK).entity(listResources).type(MediaType.APPLICATION_JSON).build());
		
	}

	public void deleteContribution(String modelType, String contributionId) {
		
		try {
			contributionService.remove(modelType, contributionId);
      } catch (ResourceTypeNotFoundException e) {
         throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Contribution wasn't found, does it exist ?").type(MediaType.TEXT_PLAIN).build());
		} catch (ResourceException e) { // asked to abort from within triggered event
			throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build());
		}
		
		throw new WebApplicationException(Response.status(Response.Status.OK).entity("Contribution has been removed").type(MediaType.TEXT_PLAIN).build());
		
	}

}
