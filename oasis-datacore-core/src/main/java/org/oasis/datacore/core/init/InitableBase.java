package org.oasis.datacore.core.init;

import java.util.LinkedHashSet;

import javax.annotation.PostConstruct;

import org.oasis.datacore.common.context.DCRequestContextProvider;
import org.oasis.datacore.common.context.SimpleRequestContextProvider;
import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCSecurity;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.core.security.mock.LocalAuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;


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
   protected String getLoginAsUserName() {
      return LocalAuthenticationService.ADMIN_USER;
   }


   /** override it if not main.sample, null means must be set explicitly in
    * buildModels (or initModels), cleanDataOfCreatedModels, fillData */
   protected DCProject getProject() {
      return getMetamodelProject();
   }

   /** Exceptions are caught and logged */
   protected abstract void doInit() throws Exception;
   

   /////////////////////////
   // (META) PROJECT INIT

   public static LinkedHashSet<String> ozwilloDefaultModelAdmins = new LinkedHashSet<String>(
         new ImmutableSet.Builder<String>().add("ozwillo-model-admins").build()); // TODO take it from per env props
   public static LinkedHashSet<String> ozwilloMetaAdmins = new LinkedHashSet<String>(ozwilloDefaultModelAdmins); // "ozwillo-meta-admins" i.e. mainly project admins
   public static LinkedHashSet<String> ozwilloMainAdmins = new LinkedHashSet<String>(ozwilloDefaultModelAdmins); // "ozwillo-main-admins"
   public static LinkedHashSet<String> ozwilloSandboxAdmins = new LinkedHashSet<String>(ozwilloMainAdmins); // main's
   public static LinkedHashSet<String> ozwilloSampleAdmins = new LinkedHashSet<String>(ozwilloDefaultModelAdmins);
   public static LinkedHashSet<String> ozwilloGeoAdmins = new LinkedHashSet<String>(ozwilloDefaultModelAdmins); // "ozwillo-geo-admins"
   public static LinkedHashSet<String> ozwilloOrgAdmins = new LinkedHashSet<String>(ozwilloDefaultModelAdmins); // "ozwillo-org-admins"
   public static LinkedHashSet<String> ozwilloCitizenkinAdmins = new LinkedHashSet<String>(ozwilloDefaultModelAdmins); // "ozwillo-citizenkin-admins"
   
   protected final DCProject getMetamodelProject() {
      DCProject project = modelAdminService.getProject(DCProject.OASIS_META);
      if (project == null) {
         project = new DCProject(DCProject.OASIS_META);
         modelAdminService.addProject(project);
      }

      // override rights policy (though projects not restored from persistence for now) :
      setDefaultGlobalPublicSecurity(project, ozwilloMetaAdmins);
      
      return project;
   }
   
   /**
    * public, only project-wide owners, can't write outside
    * @param org1Project
    * @param owners
    */
   public void setDefaultGlobalPublicSecurity(DCProject org1Project, LinkedHashSet<String> owners) {
      // override rights policy (though projects not restored from persistence for now) :
      // buildReadNoOutsideWriteSimpleProjectSecurity()
      org1Project.setModelLevelSecurityEnabled(false); // default
      setDefaultPublicSecurity(org1Project);
      setDefaultGlobalOwnersSecurity(org1Project, owners);
      setWriteInProjectVisibleSecurityConstraints(org1Project); // projects seen this one can't write in it
      // no other security shortcut
   }

   public void setDefaultPublicSecurity(DCProject project) {
      project.setSecurityDefaults(new DCSecurity());
      project.getSecurityDefaults().setAuthentifiedReadable(true); // read shortcut
      project.getSecurityDefaults().setAuthentifiedWritable(false); // default
      project.getSecurityDefaults().setAuthentifiedCreatable(false); // default
   }
   public void setWriteInProjectVisibleSecurityConstraints(DCProject project) {
      project.setVisibleSecurityConstraints(new DCSecurity());
      project.getVisibleSecurityConstraints().setAuthentifiedReadable(true); // don't constrain (otherwise could mean storage fork) : can read outside project
      project.getVisibleSecurityConstraints().setAuthentifiedWritable(false); // constrain (default) (otherwise transparent fork) : can't write outside project
   }
   public void setDefaultGlobalOwnersSecurity(DCProject orgProject, LinkedHashSet<String> owners) {
      orgProject.getSecurityDefaults().setResourceOwners(
            new LinkedHashSet<String>(owners)); // owner (& write, create) shortcut
      // OR / AND (so that if we were to change rights policy it would still be OK) :
      orgProject.getSecurityDefaults().setResourceCreators(new LinkedHashSet<String>(
            orgProject.getSecurityDefaults().getResourceOwners())); // TODO Q or none, create rather only in org_1
      orgProject.getSecurityDefaults().setResourceCreationOwners(new LinkedHashSet<String>(
            orgProject.getSecurityDefaults().getResourceCreators())); // not required at first, only if resourceOwners change, to keep current owners owning their created resources
   }
   
}
