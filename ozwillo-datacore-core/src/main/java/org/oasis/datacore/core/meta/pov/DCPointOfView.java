package org.oasis.datacore.core.meta.pov;

import java.util.Collection;
import java.util.List;

import org.oasis.datacore.core.meta.model.DCModelBase;

public interface DCPointOfView {
   
   /** the name by which to find this point of view among its parent's DCPointOfViews */
   String getName();
   String getAbsoluteName();
   List<String> getParentPointOfViewNames();
   
   /**
    * @return the corresponding model, in this and sub PointOfViews
    */
   DCModelBase getModel(String type);
   
   /** allows to iterate recursively on DCPointOfViews (and from there on models, and resources...) */
   Collection<? extends DCPointOfView> getPointOfViews();

   Collection<DCModelBase> getLocalModels();
   DCModelBase getLocalModel(String type);
   void addLocalModel(DCModelBase model);
   /** TODO LATER check version */
   void removeLocalModel(String type);
}
