package org.oasis.datacore.historization.service.impl;

import org.apache.commons.lang.StringUtils;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.historization.exception.HistorizationException;
import org.oasis.datacore.historization.service.HistorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;


/**
 * Provides resource history features
 *
 */
@Service
public class HistorizationServiceImpl implements HistorizationService {

	@Autowired
	private DCModelService dcModelService;
	
	@Autowired
	private MongoOperations mongoOperations;
	
	@Autowired
	private DataModelServiceImpl dataModelServiceImpl;
	
	private final static String HISTORIZATION_COLLECTION_SUFFIX = ".h";
	
	
	/**
	 * Must be called BEFORE actual resource update so that it historizes PREVIOUS version
	 * and not called at resource update, otherwise failures may not leave a consistent state.
	 * Clones entity else updates version and OptimisticLockingException at actual createOrUpdate !
	 * TODO LATER could be on DCResource than DCEntity IF DCResource.cachedEntity !
	 */
	@Override
	public void historize(DCEntity entity, DCModel model) throws HistorizationException {
		
		if(isHistorizable(model) && entity != null) {
			DCModel historizationModel = getHistorizationModel(model);
			
			// Before inserting we search if the entity already exist in the historized model
			// We check by URI
			Query query = new Query();
			query.addCriteria(Criteria.where("_uri").is(entity.getUri()));
			DCEntity existantEntity = mongoOperations.findOne(query, DCEntity.class, historizationModel.getCollectionName());
			
			// TODO TODOOOO better in a single operation !!?
			
			// If it already exists we delete the existant entity
			if(existantEntity != null) {
				mongoOperations.remove(existantEntity, historizationModel.getCollectionName());
			}
			
			// Then we insert the new one
			DCEntity historyEntity = new DCEntity(entity);
			// NB. cloning else updates version and OptimisticLockingException at actual createOrUpdate !!
			mongoOperations.insert(historyEntity, historizationModel.getCollectionName());
		}
		
	}

	@Override
	public DCModel getHistorizationModel(DCModel originalModel) throws HistorizationException {
		if(originalModel != null) {
			DCModel historizationModel = dcModelService.getModel(originalModel.getName() + HISTORIZATION_COLLECTION_SUFFIX);
			if(historizationModel != null) {
				return historizationModel;
			} else {
		      return createHistorizationModel(originalModel);
			}
		} else {
			throw new HistorizationException("Original model cannot be null");
		}
		
	}
	
	
	public DCModel createHistorizationModel(DCModel originalModel) throws HistorizationException {
				
		if(originalModel != null) {
			String historizationModelName = originalModel.getName() + HISTORIZATION_COLLECTION_SUFFIX;
			DCModel historizationModel = new DCModel(historizationModelName);
			for(DCField originalField : originalModel.getFieldMap().values()) {
				historizationModel.addField(originalField);
			}
			dataModelServiceImpl.addModel(historizationModel);
			return historizationModel;
		} else {
			throw new HistorizationException("Can't create historization model because original model is null");
		}
		
	}
	
	public boolean isHistorizable(DCModel model) throws HistorizationException {
		
		if(model != null) {
			return model.isHistorizable();
		} else {
			throw new HistorizationException("Can't know if model is historizable because model is null");
		}
		
	}

	@Override
	public DCEntity getHistorizedEntity(String uri, int version, DCModel originalModel) throws HistorizationException {
		
		if(StringUtils.isEmpty(uri)) {
			throw new HistorizationException("URI must not be null or empty");
		}
		
		if(originalModel == null) {
			throw new HistorizationException("Original model must not be null");
		}

		DCEntity originalDataEntity = mongoOperations.findOne(new Query(new Criteria("_uri").is(uri).and("_v").is(version)), DCEntity.class, originalModel.getCollectionName());
		
		if(originalDataEntity == null) {
			return null;
		}
		
		DCModel historizationModel = getHistorizationModel(originalModel);
		if(historizationModel == null) {
			throw new HistorizationException("The historization model of : " + originalModel.getName() + " dont exist, you must activate historization on your model before requesting historized resources");
		}
		DCEntity dataEntity = mongoOperations.findOne(new Query(new Criteria("_uri").is(uri).and("_v").is(version)), DCEntity.class, historizationModel.getCollectionName());
	    
		return dataEntity;
	    
	}

	@Override
	public String getHistorizedCollectionNameFromOriginalModel(DCModel originalModel) throws HistorizationException {
		
		if(originalModel != null && originalModel.isHistorizable()) {
			return originalModel.getName() + HISTORIZATION_COLLECTION_SUFFIX;
		} else {
			throw new HistorizationException("Original model cannot be null or non-historizable");
		}
		
	}
	

}
