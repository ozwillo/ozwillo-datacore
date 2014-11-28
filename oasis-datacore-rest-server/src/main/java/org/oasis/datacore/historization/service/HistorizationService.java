package org.oasis.datacore.historization.service;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.historization.exception.HistorizationException;

/**
 * Historization service.
 * 
 * Security of Historized data is (at least) the same as security of its latest version
 * (LATER OPT it could have additional business / Model-level permission to check).
 * Therefore it is not secured by itself, but by calling EntityService, which must therefore
 * be called (at least) once in each secured operation.
 * 
 * @author agiraudon
 *
 */
public interface HistorizationService {

	/**
	 * Historize an entity of a defined model.
	 * Secured, by being called before entityService.create/update().
    * Must be called BEFORE actual resource update so that it historizes PREVIOUS version
    * and not called at resource update, otherwise failures may not leave a consistent state.
    * TODO LATER could be on DCResource than DCEntity IF DCResource.cachedEntity !
	 * @param entity (impl should clone it else updates version and
	 * OptimisticLockingException at actual createOrUpdate)
	 * @param model
	 * @return
	 */
	public void historize(DCEntity entity, DCModelBase model) throws HistorizationException;
	
	/**
	 * Get the model where originalModel historization is stored
	 * @param originalModel
	 * @return
	 */
	public DCModelBase getOrCreateHistorizationModel(DCModelBase originalModel) throws HistorizationException;

   public DCModelBase getHistorizationModel(DCModelBase originalModel) throws HistorizationException;
	
	/**
	 * Create an historization model from an original model (it's a full copy of original model with only the name that differs : prefix "_h_" + name())
	 * @param originalModel
	 * @return
	 * @throws HistorizationException
	 */
	public DCModelBase createHistorizationModel(DCModelBase originalModel) throws HistorizationException;
	
	/**
	 * 
	 * @param model
	 * @return
	 * @throws HistorizationException
	 */
	public boolean isHistorizable(DCModelBase model) throws HistorizationException;
	
	/**
	 * Returns an historized entity matching uri AND version.
	 * We are also checking that the original entity exists with this latest version, if not
	 * we do not retrieve the historized one EVEN if it exists, because it would mean
	 * that the historized one has been created before a failed create/update operation !
	 * 
	 * Secured, by calling EntityService for this exact purpose.
	 * 
	 * @param uri
	 * @param version
	 * @param originalModel
	 * @return
	 * @throws HistorizationException
	 */
	public DCEntity getHistorizedEntity(String uri, int version, DCModelBase originalModel) throws HistorizationException;
	
	/**
	 * Return the mongo collection name of the historized model (if original model is historizable)
	 * @param originalModel
	 * @return
	 */
	public String getHistorizedCollectionNameFromOriginalModel(DCModelBase originalModel) throws HistorizationException;
	
}
