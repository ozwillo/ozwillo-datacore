package org.oasis.datacore.model.resource;

import java.util.List;

import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.sample.ResourceModelIniter;
import org.springframework.stereotype.Component;

/**
 * Adds (and persists) metamodel first thing, which is required by all other
 * subsequent (Datacore)Sample(Base)s, while extended ResourceModelIniter
 * will only fill it with data i.e. model resources using said DatacoreSamplebases.
 * @author mdutoo
 *
 */
@Component
public class ResourceMetamodelIniter extends ResourceModelIniter {

   @Override
   public int getOrder() {
      return 0; // first one
   }

   @Override
   public void buildModels(List<DCModelBase> modelsToCreate) {
      internalBuildModels(modelsToCreate); // enabled
   }

   @Override
   public void fillData() {
      //super.fillData(); // disabled
   }

}
