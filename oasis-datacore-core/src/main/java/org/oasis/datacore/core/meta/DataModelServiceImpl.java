package org.oasis.datacore.core.meta;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCMixin;
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
   private Map<String,DCMixin> mixinMap = new HashMap<String, DCMixin>();

   @Override
   public DCModel getModel(String type) {
      return modelMap.get(type);
   }
   @Override
   public DCMixin getMixin(String type) {
      return mixinMap.get(type);
   }

   @Override
   public DCField getFieldByPath(DCModel dcModel, String fieldPath) {
      // see impl in DatacoreApiImpl BUT pb providing also lastHighestListField
      throw new UnsupportedOperationException();
   }

   @Override
   public Collection<DCModel> getModels() {
      return this.modelMap.values();
   }

   @Override
   public Collection<DCMixin> getMixins() {
      return this.mixinMap.values();
   }
   
   
   ///////////////////////////////////////
   // admin / update methods

   public void addModel(DCModel dcModel) {
	  modelMap.put(dcModel.getName(), dcModel);  
   }
   
   public void addModel(DCModel dcModel, String name) {
	   modelMap.put(name, dcModel);
   }
   
   public void removeModel(String name) {
	   modelMap.remove(name);
   }
   
   public Map<String, DCModel> getModelMap() {
      return this.modelMap;
   }

   public void setModelMap(Map<String, DCModel> modelMap) {
      this.modelMap = modelMap;
   }

   public void addMixin(DCMixin mixin) {
      mixinMap.put(mixin.getName(), mixin);
   }
   
   public Map<String, DCMixin> getMixinMap() {
      return this.mixinMap;
   }

   public void setMixinMap(Map<String, DCMixin> mixinMap) {
      this.mixinMap = mixinMap;
   }

}
