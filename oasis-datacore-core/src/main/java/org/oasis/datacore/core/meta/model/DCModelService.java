package org.oasis.datacore.core.meta.model;

public interface DCModelService {

   /** reified model types */
   DCModel getModel(String modelType);
   // shared mixin types
   //DCModel getMixin(String mixinType); // TODO

   /** TODO not used for now */
   DCField getFieldByPath(DCModel dcModel, String fieldPath);

}
