package org.oasis.datacore.core.entity;

import org.oasis.datacore.core.meta.model.DCModelBase;


/**
 * Creates indexes
 * @author mdutoo
 *
 */
public interface DatabaseSetupService {

   /**
    * Computes and applies (create / drop) indexes on its storage model
    * @param model applied on its storage model
    */
   boolean ensureCollectionAndIndices(DCModelBase model, boolean deleteCollectionsFirst);

   /**
    * 
    * Consequences of removing model on its resource storage : if storage drops collection,
    * else cleans data (TODO and indexes specific to it only)
    *
    * @return false if has no storage model
    */
   boolean cleanModel(DCModelBase modelOrMixin);

   /**
    * Cleans, in this model's storage model's collection, data resources with its type
    *
    * @return false if has no storage model
    */
   boolean cleanDataOfCreatedModel(DCModelBase model) throws RuntimeException;
}
