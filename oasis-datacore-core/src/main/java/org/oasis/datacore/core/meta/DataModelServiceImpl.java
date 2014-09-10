package org.oasis.datacore.core.meta;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
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
   private Map<String,DCModelBase> mixinMap = new HashMap<String, DCModelBase>();

   @Override
   public DCModel getModel(String type) {
      return modelMap.get(type);
   }
   // includes models (?)
   @Override
   public DCModelBase getMixin(String type) {
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

   // includes models (?)
   @Override
   public Collection<DCModelBase> getMixins() {
      return this.mixinMap.values();
   }
   
   
   ///////////////////////////////////////
   // admin / update methods

   public void addModel(DCModel dcModel) {
	   modelMap.put(dcModel.getName(), dcModel);
	   addMixin(dcModel);
   }
   
   /** ONLY TO CREATE DERIVED MODELS ex. Contribution, TODO LATER rather change their name ?!? */
   public void addModel(DCModel dcModel, String name) {
	   modelMap.put(name, dcModel);
   }
   
   public void removeModel(String name) {
	   modelMap.remove(name);
      removeMixin(name);
   }
   
   public Map<String, DCModel> getModelMap() {
      return this.modelMap;
   }

   public void setModelMap(Map<String, DCModel> modelMap) {
      this.modelMap = modelMap;
   }

   public void addMixin(DCModelBase mixin) {
      mixinMap.put(mixin.getName(), mixin);
   }
   
   public void removeMixin(String name) {
      mixinMap.remove(name);
   }
   
   public Map<String, DCModelBase> getMixinMap() {
      return this.mixinMap;
   }

   public void setMixinMap(Map<String, DCModelBase> mixinMap) {
      this.mixinMap = mixinMap;
   }

}
