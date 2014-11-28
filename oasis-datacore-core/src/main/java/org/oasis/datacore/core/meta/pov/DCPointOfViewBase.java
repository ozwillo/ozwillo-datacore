package org.oasis.datacore.core.meta.pov;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.oasis.datacore.core.meta.model.DCModelBase;

import com.google.common.collect.ImmutableList;

public abstract class DCPointOfViewBase implements DCPointOfView {
   
   private String name;
   private List<String> parentPointOfViewNames;
   private String parentAbsoluteName;
   private String absoluteName = null;
   ///protected LinkedHashMap<String,DCPointOfView> pointOfViewMap = new LinkedHashMap<String,DCPointOfView>();
   protected LinkedHashMap<String,DCModelBase> modelMap = new LinkedHashMap<String,DCModelBase>();

   /** for unmarshalling only */
   public DCPointOfViewBase() {
      
   }
   public DCPointOfViewBase(String name, String ... parentPointOfViewNames) {
      this.name = name;
      this.parentPointOfViewNames = ImmutableList.copyOf(parentPointOfViewNames);
      buildAbsoluteNames();
   }
   
   @Override
   public String getParentAbsoluteName() {
      return parentAbsoluteName;
   }
   
   @Override
   public String getName() {
      return name;
   }
   
   @Override
   public String getAbsoluteName() {
      return absoluteName;
   }
   

   /** TO BE OVERRIDE */
   @Override
   public DCModelBase getModel(String type) {
      return modelMap.get(type);
   }

   ///@Override
   public abstract Map<String, ? extends DCPointOfView> getPointOfViewMap();

   /*@Override
   public Map<String, DCModelBase> getLocalModelMap() {
      return modelMap;
   }*/

   @Override
   public Collection<DCModelBase> getLocalModels() {
      return modelMap.values();
   }

   @Override
   public DCModelBase getLocalModel(String type) {
      return modelMap.get(type);
   }

   @Override
   public void addLocalModel(DCModelBase model) {
      modelMap.put(model.getName(), model);
   }

   // TODO LATER check version
   @Override
   public void removeLocalModel(String type) {
      modelMap.remove(type);
   }

   public void setName(String name) {
      this.name = name;
   }
   
   public List<String> getParentPointOfViewNames() {
      return parentPointOfViewNames;
   }
   public void setParentPointOfViewNames(List<String> parentPointOfViewNames) {
      this.parentPointOfViewNames = parentPointOfViewNames;
      buildAbsoluteNames();
   }
   
   protected void buildAbsoluteNames() {
      if (name == null || name.length() == 0) {
         throw new RuntimeException("Empty name when creating new project " + name);
      }
      if (parentPointOfViewNames == null || parentPointOfViewNames.isEmpty()) {
         this.parentAbsoluteName = "";
         this.absoluteName = name;
         return;
      }
      this.parentAbsoluteName = this.parentPointOfViewNames.stream().collect(Collectors.joining("."));
      this.absoluteName = parentAbsoluteName + "." + name;
   }
   
}
