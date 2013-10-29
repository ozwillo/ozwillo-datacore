package org.oasis.datacore.core.meta.model;

public interface DCModelService {

   DCModel getModel(String type);

   /** TODO not used for now */
   DCField getFieldByPath(DCModel dcModel, String fieldPath);

}
