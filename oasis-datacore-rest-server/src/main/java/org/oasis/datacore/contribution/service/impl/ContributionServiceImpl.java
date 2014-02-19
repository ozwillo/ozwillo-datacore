package org.oasis.datacore.contribution.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.oasis.datacore.contribution.ContributionUtils;
import org.oasis.datacore.contribution.exception.ContributionDatacoreCheckFailException;
import org.oasis.datacore.contribution.exception.ContributionWithNoModelException;
import org.oasis.datacore.contribution.exception.ContributionWithoutResourcesException;
import org.oasis.datacore.contribution.rest.api.DCContribution;
import org.oasis.datacore.contribution.service.ContributionService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.query.QueryException;
import org.oasis.datacore.core.entity.query.ldp.LdpEntityQueryService;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.contribution.DCContributionMixin;
import org.oasis.datacore.core.meta.model.contribution.DCContributionModel;
import org.oasis.datacore.core.security.service.DatacoreSecurityService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.server.BadUriException;
import org.oasis.datacore.rest.server.resource.ResourceEntityMapperService;
import org.oasis.datacore.rest.server.resource.ResourceException;
import org.oasis.datacore.rest.server.resource.ResourceService;
import org.oasis.datacore.rest.server.resource.ResourceTypeNotFoundException;
import org.oasis.datacore.rest.server.resource.UriService;
import org.springframework.beans.factory.annotation.Autowired;

public class ContributionServiceImpl implements ContributionService {
	
	@Autowired
	private ResourceService resourceService;

	@Autowired
	private DataModelServiceImpl dataModelServiceImpl;

	@Autowired
	private UriService uriService;
	
	@Autowired
	private DatacoreSecurityService datacoreSecurityService;
	
	@Autowired
	private LdpEntityQueryService ldpEntityQueryService;
	
	@Autowired
	private ResourceEntityMapperService resourceEntityMapperService;
	
	// Number of max resources you can put in a contribution
	private static int MAX_CONTRIBUTION_RESOURCES_LIMIT = 200;
	
	@Override
	public DCContribution create(DCContribution dcContribution) throws ContributionWithoutResourcesException, ContributionWithNoModelException, ContributionDatacoreCheckFailException, BadUriException, ResourceException {
		
		checkContribution(dcContribution);		
		
		String contributionId = ContributionUtils.generateContributionId();
		dcContribution.setId(contributionId);
		dcContribution.setUserId(datacoreSecurityService.getCurrentUserId());
		
		DCModel originalModel = dataModelServiceImpl.getModel(dcContribution.getModelType());
		DCContributionModel contributionModel = new DCContributionModel(originalModel);
				
		// we add the model to the model list but with a different name (getCollectionName)
		dataModelServiceImpl.addModel(contributionModel, contributionModel.getCollectionName());
		
		List<DCResource> listAddedResources = new ArrayList<>();
		for(DCResource resource : dcContribution.getListResources()) {
			if(resource != null) {
				// we add the mixin properties to each property
				resource.getProperties().putAll(ContributionUtils.getContributionPropertiesMap(dcContribution));
				listAddedResources.add(resourceService.createOrUpdate(resource, contributionModel.getCollectionName(), true, false, false));
			}
		}
		
		// we remove the contribution model
		dataModelServiceImpl.removeModel(contributionModel.getCollectionName());
		
		return dcContribution;		
	}
		
	private void checkContribution(DCContribution dcContribution) throws ContributionWithoutResourcesException, ContributionWithNoModelException, ContributionDatacoreCheckFailException {
		
		if(dcContribution == null) {
			throw new RuntimeException("Contribution is null, cannot be created");
		}
				
		if(dcContribution.getListResources() == null) {
			throw new ContributionWithoutResourcesException();
		}
			
		if(dcContribution.getListResources().isEmpty()) {
			throw new ContributionWithoutResourcesException();
		}
		
		if(dcContribution.getModelType() == null || "".equals(dcContribution.getModelType())) {
			throw new ContributionWithNoModelException();
		}
		
		if(dcContribution.getListResources().size() > MAX_CONTRIBUTION_RESOURCES_LIMIT) {
			throw new RuntimeException("");
		}
		
		List<DCResource> listResource = dcContribution.getListResources();
		DCModel dcModel = dataModelServiceImpl.getModel(dcContribution.getModelType());
		
		if(dcModel == null) {
			throw new RuntimeException("Model cannot be found");
		}
		
		Collection<String> listExceptionMessage = new ArrayList<>();
		
		for(DCResource tmpResource : listResource) {
			
			if(tmpResource != null) {
				
				if(!dcModel.getName().equals(tmpResource.getModelType())) {
					listExceptionMessage.add(" - Resource " + tmpResource.getUri() + " has not the same model than the contribution");
				}
				
				if(tmpResource.getProperties().size() != dcModel.getFieldMap().size()) {
					listExceptionMessage.add(" - Resource " + tmpResource.getUri() + " has not the same number of fields than the model");
				} else {
					for(String key : tmpResource.getProperties().keySet()) {
						if(key != null && !"".equals(key)) {
							if(dcModel.getField(key) == null) {
								listExceptionMessage.add("Resource#Field : " + tmpResource.getUri() + "#" + key + " is not in the destination model");
							}
						}
					}
				}
			}
			
		}
		
		if(!listExceptionMessage.isEmpty()) {
			String finalMessage = "";
			for(String message : listExceptionMessage) {
				finalMessage += message + System.getProperty("line.separator");
			}
			throw new ContributionDatacoreCheckFailException(finalMessage);
		}
		
	}

	@Override
	public List<DCContribution> get(String modelType, String userId, String contributionId, int start, int limit) throws RuntimeException, BadRequestException {

		DCModel model = null;

		if (modelType != null) {
			model = dataModelServiceImpl.getModel(modelType);
		}

		if (model == null) {
			throw new RuntimeException("Model not found, you cannot get associated contributions");
		}

		DCContributionModel contributionModel = new DCContributionModel(model);

		MultivaluedMap<String, String> params = new MultivaluedHashMap<>();

		if (userId != null && !userId.isEmpty()) {
			params.add(DCContributionMixin.getFieldName(DCContributionMixin.USER_ID), userId);
		}

		if (contributionId != null && !contributionId.isEmpty()) {
			params.add(DCContributionMixin.getFieldName(DCContributionMixin.ID), contributionId);
		}

		List<DCEntity> listEntities;
		try {
			listEntities = ldpEntityQueryService.findDataInType(contributionModel, params, start, limit);
		} catch (QueryException qex) {
			throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(qex.getMessage()).type(MediaType.TEXT_PLAIN).build());
		}

		List<DCResource> listResources = resourceEntityMapperService.entitiesToResources(listEntities);
		
		List<DCContribution> listContribution = resourcesToDistinctContribution(listResources);
		
		return listContribution;
		
	}
	
	@Override
	public List<DCResource> get(String modelType, String contributionId) {
		
		DCModel model = null;

		if (modelType != null) {
			model = dataModelServiceImpl.getModel(modelType);
		}

		if (model == null) {
			throw new RuntimeException("Model not found, you cannot get resources associated to this contributionId");
		}
		
		if(contributionId == null) {
			throw new RuntimeException("Contribution id must not be null");
		}

		DCContributionModel contributionModel = new DCContributionModel(model);

		MultivaluedMap<String, String> params = new MultivaluedHashMap<>();

		if (!contributionId.isEmpty()) {
			params.add(DCContributionMixin.getFieldName(DCContributionMixin.ID), contributionId);
		} else {
			throw new RuntimeException("Contribution id must not be empty");
		}

		List<DCEntity> listEntities;
		try {
			// We must limit the request to a number of resources retrieved
			// As we limit the number of resources per contribution with a parameter, we use the same to retrieve
			listEntities = ldpEntityQueryService.findDataInType(contributionModel, params, 0, MAX_CONTRIBUTION_RESOURCES_LIMIT);
		} catch (QueryException qex) {
			throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(qex.getMessage()).type(MediaType.TEXT_PLAIN).build());
		}

		List<DCResource> listResources = resourceEntityMapperService.entitiesToResources(listEntities);
		
		// We remove the ContributionMixin from the resources
		removeContributionMixinFromResource(listResources);
		
		return listResources;
		
	}
	

	@Override
	public boolean remove(String modelType, String contributionId) throws ResourceTypeNotFoundException {
		
		DCModel model = null;

		if (modelType != null) {
			model = dataModelServiceImpl.getModel(modelType);
		}

		if (model == null) {
			throw new RuntimeException("Model not found, you cannot get resources associated to this contributionId");
		}
		
		if(contributionId == null) {
			throw new RuntimeException("Contribution id must not be null");
		}

		DCContributionModel contributionModel = new DCContributionModel(model);
				
		// we add the model to the model list but with a different name (getCollectionName)
		dataModelServiceImpl.addModel(contributionModel, contributionModel.getCollectionName());

		List<DCResource> listResource = this.get(modelType, contributionId);
		
		for(DCResource resource : listResource) {
			if(resource != null) {
				resourceService.delete(resource.getUri(), contributionModel.getCollectionName(), resource.getVersion());
			}
		}
		
		return true;
		
	}
	
	private List<DCContribution> resourcesToDistinctContribution(List<DCResource> listResources) {
		
		if(listResources == null) {
			throw new RuntimeException("Cannot convert resources into contribution if resources list is null");
		}
		
		List<DCContribution> listContribution = new ArrayList<>();
		Map<String, DCContribution> mapContribution = new HashMap<String, DCContribution>();
		
		for(DCResource tmpResource : listResources) {
			if(tmpResource != null) {
				String contributionId = getMixinValueFromResource(tmpResource, DCContributionMixin.ID);
				if(contributionId != null && !contributionId.isEmpty()) {
					DCContribution contribution = mapContribution.get(contributionId);
					if(contribution != null) {
						removeContributionMixinFromResource(tmpResource);
						contribution.getListResources().add(tmpResource);
					} else {
						contribution = resourceToContribution(tmpResource);
						mapContribution.put(contributionId, contribution);
					}
				}
			}
		}
		
		
		
		listContribution.addAll(mapContribution.values());
		
		return listContribution;
		
	}

	private DCContribution resourceToContribution(DCResource resource) {

		if (resource == null) {
			throw new RuntimeException("Cannot convert empty resource to contribution");
		}

		if (resource.getTypes().contains(DCContributionMixin.MIXIN_NAME)) {
			DCContribution contribution = new DCContribution();
			contribution.setComment(getMixinValueFromResource(resource, DCContributionMixin.COMMENT));
			contribution.setId(getMixinValueFromResource(resource, DCContributionMixin.ID));
			contribution.setListResources(new ArrayList<DCResource>());
			contribution.getListResources().add(resource);
			contribution.setModelType(getMixinValueFromResource(resource, DCContributionMixin.MODEL_TYPE));
			contribution.setStatus(getMixinValueFromResource(resource, DCContributionMixin.STATUS));
			contribution.setTitle(getMixinValueFromResource(resource, DCContributionMixin.TITLE));
			contribution.setUserId(getMixinValueFromResource(resource, DCContributionMixin.USER_ID));
			contribution.setValidationComment(getMixinValueFromResource(resource, DCContributionMixin.VALIDATION_COMMENT));
			return contribution;
		} else {
			return null;
		}
		
	}
	
	private void removeContributionMixinFromResource(List<DCResource> listResources) {
		
		for(DCResource tmpResource : listResources) {
			removeContributionMixinFromResource(tmpResource);
		}
		
	}
	
	private void removeContributionMixinFromResource(DCResource resource) {
		
		List<String> listFieldName = ContributionUtils.getContributionFieldNameList(); 
		
		if(resource != null) {
			for(String tmpField : listFieldName) {
				if(resource.getProperties() != null && resource.getProperties().containsKey(tmpField)) {
					resource.getProperties().remove(tmpField);
				}
			}
			resource.getTypes().remove(DCContributionMixin.MIXIN_NAME);
		}
		
	}
	
	private String getMixinValueFromResource(DCResource resource, String fieldName) {
		
		if(resource != null && resource.getProperties() != null) {
			Object value = resource.get(DCContributionMixin.getFieldName(fieldName));
			if(value != null && value instanceof String) {
				return (String)value;
			}
		}
		
		return null;
		
	}
	
}
