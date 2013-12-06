package org.oasis.datacore.core.meta.model;

import java.util.Collection;

public interface DCModelService {

   /** reified model types */
   DCModel getModel(String modelType);
   /** shared mixin types */
   DCMixin getMixin(String type);

   Collection<DCModel> getModels();
   Collection<DCMixin> getMixins();
   
   /** TODO not used for now */
   DCField getFieldByPath(DCModel dcModel, String fieldPath);

}
