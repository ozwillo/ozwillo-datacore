package org.oasis.datacore.core.init;

import javax.annotation.PostConstruct;

import org.oasis.datacore.core.security.mock.LocalAuthenticationService;
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

   @Autowired
   protected LocalAuthenticationService localAuthenticationService;
   
   @PostConstruct
   public void register() {
      initService.register(this);
   }

   @Override
   public final void init() {
      String loginAsUsername = getLoginAsUserName();
      logger.info("Datacore - initing {} as {}...", this.getClass().getName(), loginAsUsername);
      if (loginAsUsername != null) {
         localAuthenticationService.loginAs(loginAsUsername);
      }
      try {
         doInit();
      } catch (Throwable t) {
         logger.error("Datacore - unknown error initing " + this.getClass().getName(), t);
      } finally {
         if (loginAsUsername != null) {
            localAuthenticationService.logout();
         }  
      }
   }

   /** override it to disable login or change user name */
   protected String getLoginAsUserName() {
      return LocalAuthenticationService.ADMIN_USER;
   }

   protected abstract void doInit();
   
}
