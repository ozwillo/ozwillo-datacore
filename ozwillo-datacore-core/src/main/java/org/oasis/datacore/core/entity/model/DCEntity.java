package org.oasis.datacore.core.entity.model;

import org.springframework.data.annotation.Version;


/**
 * Same as DCEntityBase, but with @Version-based optimistic locking enabled
 * (no better way to have it disabled in HistorizedEntity)
 * 
 * @author mdutoo
 *
 */
public class DCEntity extends DCEntityBase {
   private static final long serialVersionUID = 7506137398942293777L;

   public DCEntity() {
      
   }

   public DCEntity(DCEntityBase dcEntity) {
      super(dcEntity);
   }

   /** enable @Version-based optimistic locking */
   @Version
   public Long getVersion() {
      return super.getVersion();
   }
   
}
