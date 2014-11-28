package org.oasis.datacore.core.meta.pov;

import org.oasis.datacore.common.context.DCRequestContextProviderFactory;
import org.oasis.datacore.core.entity.model.DCEntity;

public class QueryPointOfView extends UseCasePointOfViewElement {
   
   @Override
   public String getName() {
      return "query";
   }

   @Override
   protected boolean appliesFor(DCEntity dataEntity) {
      if ("GET".equals(DCRequestContextProviderFactory.getProvider().get("org.apache.cxf.request.method"))) {
         return true;
      }
      return false;
   }

}
