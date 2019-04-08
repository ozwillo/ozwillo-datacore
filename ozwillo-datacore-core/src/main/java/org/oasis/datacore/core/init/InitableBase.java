package org.oasis.datacore.core.init;

import javax.annotation.PostConstruct;

import org.oasis.datacore.common.context.DCRequestContextProvider;
import org.oasis.datacore.common.context.SimpleRequestContextProvider;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.core.security.mock.LocalAuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableMap;


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
   /** to get (or create) project */
   @Autowired
   protected DataModelServiceImpl modelAdminService;
   
   @PostConstruct
   public void register() {
      initService.register(this);
   }

   @Override
   public final void init() {
      login();
      try {
         DCProject project = getProject();
         logger.info("Datacore - initing {} as {} in {}...", this.getClass().getName(), getLoginAsUserName(), project);
         if (project != null) {
            new SimpleRequestContextProvider<Object>() { // set context project beforehands :
               protected Object executeInternal() throws Exception {
                  doInit();
                  return null;
               }
            }.execInContext(new ImmutableMap.Builder<String, Object>()
                  .put(DCRequestContextProvider.PROJECT, project.getName()).build());
         } else {
            doInit();
         }
      } catch (Throwable t) {
         logger.error("Datacore - unknown error initing " + this.getClass().getName(), t); // TODO unwrap ?
      } finally {
         logout(); 
      }
   }

   protected void login() {
      String loginAsUsername = getLoginAsUserName();
      if (loginAsUsername != null) {
         localAuthenticationService.loginAs(loginAsUsername);
      }
   }
   protected void logout() {
      String loginAsUsername = getLoginAsUserName();
      if (loginAsUsername != null) {
         localAuthenticationService.logout();
      }
   }

   /** override it to disable login or change user name */
   private String getLoginAsUserName() {
      return LocalAuthenticationService.ADMIN_USER;
   }

   /** override it else none, null means must be set explicitly in
    * buildModels (or initModels), cleanDataOfCreatedModels, fillData */
   protected DCProject getProject() {
      return null;
   }

   /** Exceptions are caught and logged */
   protected abstract void doInit() throws Exception;
   
}
