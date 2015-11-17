package org.oasis.datacore.historization.service.impl;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.entity.model.DCEntityBase;

/**
 * Without @Version-based optimistic locking
 * 
 * @author mdutoo
 *
 */
public class HistorizedEntity extends DCEntityBase {
   private static final long serialVersionUID = 3029796925650421555L;
   
   public HistorizedEntity() {
      super();
   }

   public HistorizedEntity(DCEntity dcEntity) { 
      super(dcEntity);
   }

}
