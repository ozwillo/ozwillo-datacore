package org.oasis.datacore.core.meta.model;

import java.util.Collection;

public interface DCModelService {

   /**
    * reified model types
    * @param modelType
    * @return
    */
   DCModel getModel(String modelType);
   /**
    * shared mixin types ; includes models (?)
    * @param type
    * @return
    */
   DCModelBase getMixin(String type);

   /**
    * reified model types
    * @return
    */
   Collection<DCModel> getModels();
   /**
    * shared mixin types ; includes models (?)
    * @return
    */
   Collection<DCModelBase> getMixins();
   
   /** TODO not used for now */
   DCField getFieldByPath(DCModel dcModel, String fieldPath);

}
