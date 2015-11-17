package org.oasis.datacore.core.meta.pov;

import java.util.LinkedHashMap;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.model.DCModelBase;

public abstract class DCUseCasePointOfViewBase implements DCUseCasePointOfView {

   private String name;
   private LinkedHashMap<String,UseCasePointOfViewElement> useCaseElementMap = new LinkedHashMap<String,UseCasePointOfViewElement>();

   /** for unmarshalling only */
   public DCUseCasePointOfViewBase() {
      
   }
   public DCUseCasePointOfViewBase(String name) {
      this.setName(name);
   }

   @Override
   public String getName() {
      return name;
   }
   
   @Override
   public DCModelBase getModel(DCEntity dataEntity) {
      for (UseCasePointOfViewElement useCaseElement : useCaseElementMap.values()) {
         if (useCaseElement.appliesFor(dataEntity)) {
            return useCaseElement.getModel();
         }
      }
      return null;
   }
   
   public void setName(String name) {
      this.name = name;
   }

}
