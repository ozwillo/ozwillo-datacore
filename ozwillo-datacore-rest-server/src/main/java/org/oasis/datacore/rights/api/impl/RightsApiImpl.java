package org.oasis.datacore.rights.api.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.oasis.datacore.core.entity.EntityService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.SimpleUriService;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.security.DCUserImpl;
import org.oasis.datacore.core.security.EntityPermissionService;
import org.oasis.datacore.core.security.service.DatacoreSecurityService;
import org.oasis.datacore.rights.enumeration.RightsActionType;
import org.oasis.datacore.rights.rest.api.DCRights;
import org.oasis.datacore.rights.rest.api.RightsApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Path("dc/r")
@Component("datacore.rights.apiImpl") // else can't autowire Qualified ; NOT @Service (permissions rather around EntityService)
public class RightsApiImpl implements RightsApi {

	private static final Logger logger = LoggerFactory.getLogger(RightsApiImpl.class);

	private static final String TOKEN_CACHE_NAME = "org.springframework.security.oauth2.provider.OAuth2Authentication";

	@Autowired
	private EntityPermissionService entityPermissionService;

	@Autowired
	private DCModelService modelService;

	@Autowired
	private EntityService entityService;
   
	@Autowired
	private DatacoreSecurityService datacoreSecurityService;

	@Autowired
	private CacheManager cacheManager;

	public void addRightsOnResource(String modelType, String iri, long version, DCRights dcRights) {

		preMergeVerification(RightsActionType.ADD, modelType, iri, version, dcRights.getWriters(), dcRights.getReaders(), dcRights.getOwners());

		DCModelBase model = modelService.getModelBase(modelType);

		if (model == null) {
			throw new NotFoundException(Response.status(Response.Status.NOT_FOUND).entity("Model " + modelType + " was not found").type(MediaType.TEXT_PLAIN).build());
		}

		String uri = SimpleUriService.buildUri(modelType, iri);
		DCEntity entity = entityService.getByUriUnsecured(uri, model);

		if (entity == null) {
			throw new NotFoundException(Response.status(Response.Status.NOT_FOUND).entity("Entity " + uri + " was not found").type(MediaType.TEXT_PLAIN).build());
		}

		mergeRights(RightsActionType.ADD, entity, dcRights.getOwners(), dcRights.getReaders(), dcRights.getWriters());
		
		entityService.changeRights(entity);

		logger.debug("Rights have been changed on resource {}, clearing token cache", iri);
		// slightly hacky/heavy-handed: if rights have been changed, we need to flush the token cache
		cacheManager.getCache(TOKEN_CACHE_NAME).clear();

		
		throw new WebApplicationException(Response.status(Response.Status.OK).entity("Rights have been added on the resource " + uri).type(MediaType.TEXT_PLAIN).build());

	}

	public void removeRightsOnResource(String modelType, String iri, long version, DCRights dcRights) {

		preMergeVerification(RightsActionType.REMOVE, modelType, iri, version, dcRights.getWriters(), dcRights.getReaders(), dcRights.getOwners());

		DCModelBase model = modelService.getModelBase(modelType);

		if (model == null) {
			throw new NotFoundException(Response.status(Response.Status.NOT_FOUND).entity("Model " + modelType + " was not found").type(MediaType.TEXT_PLAIN).build());
		}

		String uri = SimpleUriService.buildUri(modelType, iri);
		DCEntity entity = entityService.getByUriUnsecured(uri, model);

		if (entity == null) {
			throw new NotFoundException(Response.status(Response.Status.NOT_FOUND).entity("Entity " + uri + " was not found").type(MediaType.TEXT_PLAIN).build());
		}

		mergeRights(RightsActionType.REMOVE, entity, dcRights.getOwners(), dcRights.getReaders(), dcRights.getWriters());
		
		if(entity.getOwners() != null && entity.getOwners().isEmpty()) {
			throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity("There must be at least one owner on the resource").type(MediaType.TEXT_PLAIN).build());
		}
		
		entityService.changeRights(entity);

		logger.debug("Rights have been changed on resource {}, clearing token cache", iri);
		// slightly hacky/heavy-handed: if rights have been changed, we need to flush the token cache
		cacheManager.getCache(TOKEN_CACHE_NAME).clear();


		throw new WebApplicationException(Response.status(Response.Status.OK).entity("Rights have been removed on the resource " + uri).type(MediaType.TEXT_PLAIN).build());

	}

	public void flushRightsOnResource(String modelType, String iri, long version) {

		preMergeVerification(RightsActionType.FLUSH, modelType, iri, version, null, null, null);
		
		DCModelBase model = modelService.getModelBase(modelType);
		
		if(model == null) {
			throw new NotFoundException(Response.status(Response.Status.NOT_FOUND).entity("Model " + modelType + " was not found").type(MediaType.TEXT_PLAIN).build());
		}
		
		String uri = SimpleUriService.buildUri(modelType, iri);
		DCEntity entity = entityService.getByUriUnsecured(uri, model);
		
		if(entity == null) {
			throw new NotFoundException(Response.status(Response.Status.NOT_FOUND).entity("Entity " + uri + " was not found").type(MediaType.TEXT_PLAIN).build());
		}
		
		mergeRights(RightsActionType.FLUSH, entity, null, null, null);
		
		entityService.changeRights(entity);

		logger.debug("Rights have been changed on resource {}, clearing token cache", iri);
		// slightly hacky/heavy-handed: if rights have been changed, we need to flush the token cache
		cacheManager.getCache(TOKEN_CACHE_NAME).clear();


		throw new WebApplicationException(Response.status(Response.Status.OK).entity("Rights have been flushed (readers,writers) on the resource " + uri).type(MediaType.TEXT_PLAIN).build());
		
	}
	
	public void replaceRightsOnResource(String modelType, String iri, long version, DCRights dcRights) {
		
		preMergeVerification(null, modelType, iri, version, null, null, null);
		
		DCModelBase model = modelService.getModelBase(modelType);
		
		if(model == null) {
			throw new NotFoundException(Response.status(Response.Status.NOT_FOUND).entity("Model " + modelType + " was not found").type(MediaType.TEXT_PLAIN).build());
		}
		
		String uri = SimpleUriService.buildUri(modelType, iri);
		DCEntity entity = entityService.getByUriUnsecured(uri, model);
		
		if(entity == null) {
			throw new NotFoundException(Response.status(Response.Status.NOT_FOUND).entity("Entity " + uri + " was not found").type(MediaType.TEXT_PLAIN).build());
		}
		
		if(dcRights == null) {
			throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity("DCRight object has to be defined if you want to replace rights").type(MediaType.TEXT_PLAIN).build());
		}
		
		entity.setOwners(new HashSet<String>());
		if(dcRights.getOwners() != null) {
			entity.getOwners().addAll(dcRights.getOwners());
		} else {
			// we put the current user (how might me owner)
			// if he is not the changeRights will not work and this will not affect the entity
			entity.setOwners(new HashSet<String>());
			entity.getOwners().add(DCUserImpl.USER_ORGANIZATION_PREFIX + datacoreSecurityService.getCurrentUserId()); 
		}
		
		entity.setReaders(new HashSet<String>());
		if(dcRights.getReaders() != null) {
			entity.getReaders().addAll(dcRights.getReaders());
		}
		
		entity.setWriters(new HashSet<String>());
		if(dcRights.getWriters() != null) {
			entity.getWriters().addAll(dcRights.getWriters());
		}
		
		entityPermissionService.recomputeAllReaders(entity);
		
		entityService.changeRights(entity);

		logger.debug("Rights have been changed on resource {}, clearing token cache", iri);
		// slightly hacky/heavy-handed: if rights have been changed, we need to flush the token cache
		cacheManager.getCache(TOKEN_CACHE_NAME).clear();


		throw new WebApplicationException(Response.status(Response.Status.OK).entity("Rights have been replaced on the resource " + uri).type(MediaType.TEXT_PLAIN).build());
		
	}

	private void preMergeVerification(RightsActionType rightsActionType, String modelType, String iri, long version, List<String> writers, List<String> readers, List<String> owners) {

		if (modelType == null) {
			throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity("Type must be defined").type(MediaType.TEXT_PLAIN).build());
		}

		if (iri == null) {
			throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity("IRI must be defined").type(MediaType.TEXT_PLAIN).build());
		}

		if (writers == null && readers == null && owners == null && rightsActionType != null & rightsActionType != RightsActionType.FLUSH) {
			throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity("One of the sets must be defined (writers,readers,owners)").type(MediaType.TEXT_PLAIN).build());
		}

	}

	@Override
	public DCRights getRightsOnResource(String modelType, String iri, long version) {
		
		if (modelType == null) {
			throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity("Type must be defined").type(MediaType.TEXT_PLAIN).build());
		}

		if (iri == null) {
			throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity("IRI must be defined").type(MediaType.TEXT_PLAIN).build());
		}
		
		DCModelBase model = modelService.getModelBase(modelType);
		
		if(model == null) {
			throw new NotFoundException(Response.status(Response.Status.NOT_FOUND).entity("Model " + modelType + " was not found").type(MediaType.TEXT_PLAIN).build());
		}
		
		String uri = SimpleUriService.buildUri(modelType, iri);
		DCEntity entity = entityService.getByUriUnsecured(uri, model);
		
		if(entity == null) {
			throw new NotFoundException(Response.status(Response.Status.NOT_FOUND).entity("Entity " + uri + " was not found").type(MediaType.TEXT_PLAIN).build());
		}
		
		// We only use that to check that we are owner on the resource
		entityService.getRights(entity);
		
		DCRights dcRights = new DCRights();
		
		dcRights.setReaders(new ArrayList<String>());
		dcRights.setWriters(new ArrayList<String>());
		
		if(entity.getOwners() != null) {
			dcRights.setOwners(new ArrayList<String>());
			dcRights.getOwners().addAll(entity.getOwners());
		}
		
		if(entity.getReaders() != null) {
			dcRights.setReaders(new ArrayList<String>());
			dcRights.getReaders().addAll(entity.getReaders());
		}
		
		if(entity.getWriters() != null) {
			dcRights.setWriters(new ArrayList<String>());
			dcRights.getWriters().addAll(entity.getWriters());
		}
		
		throw new WebApplicationException(Response.status(Response.Status.OK).entity(dcRights).type(MediaType.APPLICATION_JSON).build());
		
	}
	
	private void mergeRights(RightsActionType rightsActionType, DCEntity entity, List<String> owners, List<String> readers, List<String> writers) {

		if (rightsActionType != null && entity != null) {

			switch (rightsActionType) {

			case ADD:
				entity.setOwners(entityPermissionService.addRights(entity.getOwners(), owners));
				entity.setReaders(entityPermissionService.addRights(entity.getReaders(), readers));
				entity.setWriters(entityPermissionService.addRights(entity.getWriters(), writers));
				entityPermissionService.recomputeAllReaders(entity);
				break;

			case REMOVE:
				entity.setOwners(entityPermissionService.removeRights(entity.getOwners(), owners));
				entity.setReaders(entityPermissionService.removeRights(entity.getReaders(), readers));
				entity.setWriters(entityPermissionService.removeRights(entity.getWriters(), writers));
				entityPermissionService.recomputeAllReaders(entity);
				break;

			case FLUSH:
				entity.setReaders(new HashSet<String>());
				entity.setWriters(new HashSet<String>());
				entityPermissionService.recomputeAllReaders(entity);
			}

		}

	}

}
