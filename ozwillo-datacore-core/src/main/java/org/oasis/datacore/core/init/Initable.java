package org.oasis.datacore.core.init;

public interface Initable {

   // TODO LATER dependency order on models & data, & check that does not conflict on order
   void init();

   /**
    * Smaller ones will be inited first.
    * Ex. 10 for dropdb, 1000 for default sample init, 100000 for ModelResourceIniter
    */
   int getOrder();
}
