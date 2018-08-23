package org.oasis.datacore.core.meta.pov;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.model.DCModelBase;

/**
 * TODO rather UseCasePointOfViews ?!?
 * @author mdutoo
 *
 */
public interface DCUseCasePointOfView extends DCPointOfView {
   
   /** checks whether this POV (found by name) applies to given entity and then returns (one of) the POV's model(s) */
   DCModelBase getModel(DCEntity dataEntity);
}
