package org.oasis.datacore.core.entity;

import org.oasis.datacore.core.meta.model.DCModelBase;


/**
 * Creates indexes
 * @author mdutoo
 *
 */
public interface DatabaseSetupService {

   /**
    * @param model
    * @param deleteCollectionsFirst
    * @return
    */
   public boolean ensureCollectionAndIndices(DCModelBase model, boolean deleteCollectionsFirst);

   public void cleanModel(DCModelBase modelOrMixin);

   void cleanDataOfCreatedModel(DCModelBase storageModel) throws RuntimeException;
   
}
