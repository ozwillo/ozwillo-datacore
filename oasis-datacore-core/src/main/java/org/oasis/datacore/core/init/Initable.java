package org.oasis.datacore.core.init;

public interface Initable {

   // TODO LATER dependency order on models & data, & check that does not conflict on order
   public void init();
   public int getOrder();
   
}
