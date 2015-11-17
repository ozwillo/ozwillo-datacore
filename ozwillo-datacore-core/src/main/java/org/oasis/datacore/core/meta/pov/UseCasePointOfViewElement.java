package org.oasis.datacore.core.meta.pov;

import java.util.Collection;
import java.util.Map;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public abstract class UseCasePointOfViewElement extends DCPointOfViewBase implements DCPointOfView {
   
   public static final Collection<DCPointOfView> EMPTY_POINTOFVIEW_LIST =
         new ImmutableList.Builder<DCPointOfView>().build();
   public static final Map<String,DCPointOfView> EMPTY_POINTOFVIEW_MAP =
         new ImmutableMap.Builder<String, DCPointOfView>().build();
   
   private DCModel model = null;

   public UseCasePointOfViewElement() {
      
   }

   public DCModelBase getModel() {
      return model;
   }
   
   protected abstract boolean appliesFor(DCEntity dataEntity);

   @Override
   public Collection<? extends DCPointOfView> getPointOfViews() {
      return EMPTY_POINTOFVIEW_LIST;
   }
   ///@Override
   public Map<String,? extends DCPointOfView> getPointOfViewMap() {
      return EMPTY_POINTOFVIEW_MAP;
   }
   
   /* impl-dependent, to find it among other same-named POVs. May contain (one of) the POV's model(s) name */
   ///public String getId();
   
   public void setLocalModel(DCModel model) {
      this.model = model;
      this.modelMap.clear();
      this.modelMap.put(model.getName(), model);
   }
   
   @Override
   protected void buildAbsoluteNames() {
      if (getParentPointOfViewNames() == null || getParentPointOfViewNames().isEmpty()) {
         throw new RuntimeException("There must be at least one pointOfView level (ex. project) "
               + "when creating new UCPOV " + getName());
      }
      super.buildAbsoluteNames();
   }
   
}
