package org.oasis.datacore.core.meta.pov;

import java.util.Collection;
import java.util.List;

import org.oasis.datacore.core.meta.model.DCModelBase;

public interface DCPointOfView {
   
   /** the name by which to find this point of view among its parent's DCPointOfViews */
   public String getName();
   String getParentAbsoluteName();
   String getAbsoluteName();
   List<String> getParentPointOfViewNames();
   
   /**
    * @param type
    * @return the corresponding model, in this and sub PointOfViews 
    */
   public DCModelBase getModel(String type);
   
   /** allows to iterate recursively on DCPointOfViews (and from there on models, and resources...) */
   public Collection<? extends DCPointOfView> getPointOfViews();
   ///public Map<String,? extends DCPointOfView> getPointOfViewMap();
   
   // TODO rm ; only to provider keys & values
   ///public Map<String,DCModelBase> getLocalModelMap();
   ///public Collection<String> getLocalModelNames();
   public Collection<DCModelBase> getLocalModels();
   DCModelBase getLocalModel(String type);
   void addLocalModel(DCModelBase model);
   /** TODO LATER check version */
   void removeLocalModel(String type);
   
}
