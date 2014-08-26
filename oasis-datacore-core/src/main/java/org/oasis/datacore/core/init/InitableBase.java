package org.oasis.datacore.core.init;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Eases up creating impls of Initable
 * @author mdutoo
 *
 */
public abstract class InitableBase implements Initable {

   protected final Logger logger = LoggerFactory.getLogger(getClass());
   
   @Autowired
   private InitService initService;
   
   @PostConstruct
   public void register() {
      initService.register(this);
   }

   @Override
   public final void init() {
      logger.info("Datacore - initing " + this.getClass().getName() + "...");
      doInit();
   }

   protected abstract void doInit();
   
}
