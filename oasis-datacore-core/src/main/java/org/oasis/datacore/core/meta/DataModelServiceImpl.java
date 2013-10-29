package org.oasis.datacore.core.meta;

import java.util.HashMap;
import java.util.Map;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.springframework.stereotype.Component;

/**
 * TODO readonly : get & set map only for tests, LATER admin using another (inheriting) model ?!?
 * => LATER extract update methods in another admin interface
 * @author mdutoo
 *
 */
@Component
public class DataModelServiceImpl implements DCModelService {
   
   private Map<String,DCModel> modelMap = new HashMap<String, DCModel>();

   @Override
   public DCModel getModel(String type) {
      return modelMap.get(type);
   }

   @Override
   public DCField getFieldByPath(DCModel dcModel, String fieldPath) {
      // see impl in DatacoreApiImpl BUT pb providing also lastHighestListField
      throw new UnsupportedOperationException();
   }

   
   ///////////////////////////////////////
   // admin / update methods

   public void addModel(DCModel dcModel) {
      modelMap.put(dcModel.getName(), dcModel);
   }
   
   public Map<String, DCModel> getModelMap() {
      return this.modelMap;
   }

   public void setModelMap(Map<String, DCModel> modelMap) {
      this.modelMap = modelMap;
   }

}
