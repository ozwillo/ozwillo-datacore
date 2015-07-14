package org.oasis.datacore.historization.service.impl;

import org.apache.commons.lang.StringUtils;
import org.oasis.datacore.core.entity.EntityModelService;
import org.oasis.datacore.core.entity.EntityService;
import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.model.DCEntityBase;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.historization.exception.HistorizationException;
import org.oasis.datacore.historization.service.HistorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;


/**
 * Provides resource history features
 *
 */
@Component("datacore.historizationService") // else can't autowire Qualified ; NOT @Service (permissions rather around EntityService)
public class HistorizationServiceImpl implements HistorizationService {

	@Autowired
	private DCModelService dcModelService;
   /** to get storage model collection name & fillDataEntityCaches */
   @Autowired
   private EntityModelService entityModelService;
   /** to 1. remove (in case previous insert failed)
    * and 2. rights of GET history (which are those of historized entity, which must therefore be retrieved)
    * TODO still avoid showing history of a deleted resource without checking its rights
    * TODO Q also alternative policy to accept rights at time of historizing ? */
   @Autowired
   private EntityService entityService;
	
	@Autowired
	private MongoOperations mongoOperations;
	
	@Autowired
	private DataModelServiceImpl dataModelServiceImpl;
	
	public final static String HISTORIZATION_COLLECTION_SUFFIX = ".h";
	
	
	/**
	 * Must be called BEFORE actual resource update so that it historizes PREVIOUS version
	 * and not called at resource update, otherwise failures may not leave a consistent state.
	 * Clones entity else updates version and OptimisticLockingException at actual createOrUpdate !
	 * TODO LATER could be on DCResource than DCEntity IF DCResource.cachedEntity !
	 */
	@Override
	public void historize(DCEntity entity, DCModelBase model) throws HistorizationException {
		
		if(isHistorizable(model) && entity != null) {
		   DCModelBase historizationModel = getOrCreateHistorizationModel(model);
			
			// Before inserting we search if the entity already exist in the historized model
			// We check by URI
         // If it already exists we delete the existant entity
         HistorizedEntity historizedEntity = createHistorizedEntity(entity);
			Query query = new Query(Criteria.where(DCEntity.KEY_URI).is(historizedEntity.getUri())
			      .and(DCEntity.KEY_V).is(historizedEntity.getVersion()));
			mongoOperations.remove(query, HistorizedEntity.class, historizationModel.getCollectionName());
			
			// Then we insert the new one
			// NB. cloning else updates version and OptimisticLockingException at actual createOrUpdate !!
			mongoOperations.insert(historizedEntity, getHistorizedCollectionNameFromOriginalModel(model));
			entityModelService.fillDataEntityCaches(historizedEntity, historizationModel,
			      historizationModel, null); // in case accessed later outside ; TODO def
		}
		
	}

	/**
	 * 
	 * @param entity
	 * @return with next version of entity
	 */
	private HistorizedEntity createHistorizedEntity(DCEntity entity) {
      // TODO Auto-generated method stub
	   HistorizedEntity historizedEntity = new HistorizedEntity(entity);
      long existingEntityVersion = entity.getVersion() == null ? 0 : entity.getVersion() + 1;
      historizedEntity.setVersion(existingEntityVersion);
      historizedEntity.setId(null);
	   return historizedEntity;
   }

   @Override
	public DCModelBase getOrCreateHistorizationModel(DCModelBase originalModel) throws HistorizationException {
		if(originalModel != null) {
		   DCModelBase historizationModel = getHistorizationModel(originalModel);
			if(historizationModel != null) {
				return historizationModel;
			} else {
		      return createHistorizationModel(originalModel);
			}
		} else {
			throw new HistorizationException("Original model cannot be null. "
               + "Maybe it has been deleted since (test only).");
		}
		
	}

   @Override
   public DCModelBase getHistorizationModel(DCModelBase originalModel) throws HistorizationException {
      if(originalModel == null) {
         throw new HistorizationException("Original model cannot be null. "
               + "Maybe it has been deleted since (test only).");
      }
      return dcModelService.getModelBase(originalModel.getName() + HISTORIZATION_COLLECTION_SUFFIX);
   }
	
	
	// TODO persist to resource
	public DCModelBase createHistorizationModel(DCModelBase originalModel) throws HistorizationException {
				
		if(originalModel != null) {
			String historizationModelName = originalModel.getName() + HISTORIZATION_COLLECTION_SUFFIX;
			DCModelBase historizationModel = new DCModel(historizationModelName,
			      originalModel.getPointOfViewAbsoluteName());
			historizationModel.addMixin(originalModel);
         //historizationModel.setStorage(true); // default ; & inherited model must have "a" storage
         //historizationModel.setInstanciable(true); // default
         historizationModel.setDefinition(false); // does not change inherited definition
			//historizationModel.setHistorizable(false); // TODO prevent changes
			dataModelServiceImpl.addModel(historizationModel);
			return historizationModel;
		} else {
			throw new HistorizationException("Can't create historization model because original model is null. "
               + "Maybe it has been deleted since (test only).");
		}
		
	}
	
	public boolean isHistorizable(DCModelBase model) throws HistorizationException {
		
		if(model != null) {
			return model.isHistorizable();
		} else {
			throw new HistorizationException("Can't know if model is historizable because model is null. "
               + "Maybe it has been deleted since (test only).");
		}
		
	}

	@Override
	public DCEntityBase getHistorizedEntity(String uri, int version, DCModelBase originalModel) throws HistorizationException {
		
		if(StringUtils.isEmpty(uri)) {
			throw new HistorizationException("URI must not be null or empty");
		}
		
		if(originalModel == null) {
			throw new HistorizationException("Original model must not be null");
		}
      
      if (!originalModel.isHistorizable()) {
         throw new HistorizationException("Original model must be historizable");
      }
      
      if (version < 0) {
         throw new HistorizationException("Version must not be negative");
      }

      // check existence (not deleted) and rights :
		DCEntity originalDataEntity = entityService.getByUri(uri, originalModel) ;		
		if (originalDataEntity == null) {
			return null;
		}
		
		DCModelBase historizationModel = getOrCreateHistorizationModel(originalModel);
		if(historizationModel == null) {
			throw new HistorizationException("The historization model of : " + originalModel.getName() + " dont exist, you must activate historization on your model before requesting historized resources");
		}
		HistorizedEntity dataEntity = mongoOperations.findOne(new Query(new Criteria(DCEntity.KEY_URI).is(uri).and(DCEntity.KEY_V).is(version)),
		      HistorizedEntity.class, getHistorizedCollectionNameFromOriginalModel(originalModel));
		if (dataEntity != null) {
	      entityModelService.fillDataEntityCaches(dataEntity, historizationModel, historizationModel, null);
		} // else ex. version not (yet) historized
		return dataEntity;
	    
	}

	@Override
	public String getHistorizedCollectionNameFromOriginalModel(DCModelBase originalModel) throws HistorizationException {
		
		if(originalModel != null && originalModel.isHistorizable()) {
			return entityModelService.getCollectionName(originalModel) + HISTORIZATION_COLLECTION_SUFFIX;
		} else {
			throw new HistorizationException("Original model cannot be null or non-historizable");
		}
		
	}
	

}
