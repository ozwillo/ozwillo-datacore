package org.oasis.datacore.sample.meta;

import java.util.LinkedHashSet;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.oasis.datacore.core.meta.DataModelServiceImpl;
import org.oasis.datacore.core.meta.model.DCSecurity;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * TODO LATER move to ex. org.oasis.datacore.server.init.meta
 * Provides helpers to create projects in java
 * (most project methods are rather on ModelService)
 * @author mdutoo
 *
 */
@Component
public class ProjectInitService {

   /** to get (or create) project */
   @Autowired
   private DataModelServiceImpl modelAdminService;
   
   /** default project owners / admins (comma-separated) */
   @Resource(name="datacore.security.projectOwners")
   private Properties projectOwners;
   /*
   public static LinkedHashSet<String> ozwilloDefaultModelAdmins = new LinkedHashSet<String>(
         new ImmutableSet.Builder<String>().add("ozwillo-model-admins").build()); // TODO take it from per env props
   public static LinkedHashSet<String> ozwilloMetaAdmins = new LinkedHashSet<String>(ozwilloDefaultModelAdmins); // "ozwillo-meta-admins" i.e. mainly project admins
   public static LinkedHashSet<String> ozwilloMainAdmins = new LinkedHashSet<String>(ozwilloDefaultModelAdmins); // "ozwillo-main-admins"
   public static LinkedHashSet<String> ozwilloSandboxAdmins = new LinkedHashSet<String>(ozwilloMainAdmins); // main's
   public static LinkedHashSet<String> ozwilloSampleAdmins = new LinkedHashSet<String>(ozwilloDefaultModelAdmins);
   public static LinkedHashSet<String> ozwilloGeoAdmins = new LinkedHashSet<String>(ozwilloDefaultModelAdmins); // "ozwillo-geo-admins"
   public static LinkedHashSet<String> ozwilloOrgAdmins = new LinkedHashSet<String>(ozwilloDefaultModelAdmins); // "ozwillo-org-admins"
   public static LinkedHashSet<String> ozwilloCitizenkinAdmins = new LinkedHashSet<String>(ozwilloDefaultModelAdmins); // "ozwillo-citizenkin-admins"
   */
   
   @PostConstruct
   public void init() {
      getMetamodelProject();
      getMainProject();
      getSampleProject();
      getSandboxProject();
   }
   
   /**
    * 
    * @param projectName
    * @return owners from project-named prop in project owners prop file
    * or else from its oasis.meta prop
    */
   public LinkedHashSet<String> buildDefaultProjectOwners(String projectName) {
      return new LinkedHashSet<String>(StringUtils
            .commaDelimitedListToSet(projectOwners.getProperty(projectName,
                  projectOwners.getProperty(DCProject.OASIS_META))));
   }

   
   ////////////////////////////////////////////////
   // GENERIC PROJECTS
   
   public final DCProject getMetamodelProject() {
      DCProject project = modelAdminService.getProject(DCProject.OASIS_META);
      if (project == null) {
         project = new DCProject(DCProject.OASIS_META);
         modelAdminService.addProject(project);
      }

      // override rights policy (though projects not restored from persistence for now) :
      setDefaultGlobalPublicSecurity(project, buildDefaultProjectOwners(project.getName()));
      
      return project;
   }
   
   public final DCProject getSampleProject() {
      DCProject project = modelAdminService.getProject(DCProject.OASIS_SAMPLE);
      if (project == null) {
         project = new DCProject(DCProject.OASIS_SAMPLE);

         // ONLY FOR OLD MODELS THAT DIDNT HAVE SECURITY override rights policy (though projects not restored from persistence for now) :
         setDefaultGlobalPublicSecurity(project, buildDefaultProjectOwners(project.getName()));
         
         project.setModelLevelSecurityEnabled(true); // let unit tests tweak model-level security as necessary
         project.setUseModelSecurity(true); // same
         project.getSecurityDefaults().setResourceCreators(null); // else DatacoreApiServerMixinTest.testAddress fails
         
         modelAdminService.addProject(project);
         ///dataForkMetamodelIn(project); // NO still store them all in oasis.meta, TODO LATER add projectName in their URI
         project.addLocalVisibleProject(getMetamodelProject()); // ... so for now rather this
      }
      return project;
   }
   
   public final DCProject getMainProject() {
      DCProject project = modelAdminService.getProject(DCProject.OASIS_MAIN);
      if (project == null) {
         project = buildProjectDefaultConf(DCProject.OASIS_MAIN,
               "(facade) Makes visible all published projects", null);
         // TODO ADD SECURITY TO CITIZEN KIN MODELS
         // TODO TODO add ALL other projects ; & should not have any local models
      }
      return project;
   }

   public DCProject getCitizenKinProject() {
      String projectName = "citizenkin_0";
      DCProject project = modelAdminService.getProject(projectName);
      if (project == null) {
         throw new RuntimeException("Create in playground project " + projectName
               + " with default dcms:modelLevelSecurityEnabled and dmcs:useModelSecurity set to true");
      }
      return project;
   }
   
   /**
    * i.e. main's sandbox
    * @return
    */
   public final DCProject getSandboxProject() { // TODO use it in playground
      DCProject project = modelAdminService.getProject(DCProject.OASIS_SANBOX);
      if (project == null) {
         project = new DCProject(DCProject.OASIS_SANBOX);
         project.setModelLevelSecurityEnabled(true); // to each sandbox user its own models
         project.setUseModelSecurity(true); // same
         modelAdminService.addProject(project);
         // NB. no need to fork metamodel, sandbox custom models will be stored
         // in multiProjectStorage oasis.meta.dcmi:mixin_0 collection
         // and their resources in oasis.sandbox.* collections
         // (not even hard forks i.e. copies, only soft forks i.e. inheriting overrides would be a problem)
         ///dataForkMetamodelIn(project); // TODO LATER still store them also in oasis.meta with projectName in their URI
         project.addLocalVisibleProject(getMainProject());
         project.addLocalVisibleProject(getSampleProject());
      }
      return project;
   }

   
   ////////////////////////////////////////////////
   // PROJECT HELPERS
   
   /**
    * Adds oasis.main
    * @param unversionedName
    * @param majorVersion 0, 1...
    * @param doc
    * @param owners default resourceOwners, therefore also of model resources
    * @param visibleProjects (oasis.meta is implicitly added)
    * @return
    */
   public DCProject buildContainerVersionedProjectDefaultConf(
         String unversionedName, long majorVersion,
         String doc, LinkedHashSet<String> owners,
         DCProject ... visibleProjects) {
      return buildProjectDefaultConf(unversionedName + '_' + majorVersion, // geo_1 // NB. in geo_1.0, 0 would be minorVersion
            doc, owners, visibleProjects);
      // NB. versioned projects aren't in main, rather their facade, to allow transparent migration
   }

   /**
    * Adds to oasis.main
    * @param containerVersionedProject
    * @return
    */
   public DCProject buildFacadeProjectDefaultConf(DCProject containerVersionedProject) {
      if (containerVersionedProject.getMajorVersion() < 0) {
         throw new RuntimeException("Not a versioned project " + containerVersionedProject);
      }
      DCProject orgProject = new DCProject(containerVersionedProject.getUnversionedName());
      orgProject.setDocumentation("(Unversioned facade) " + containerVersionedProject.getDocumentation());
      orgProject.addLocalVisibleProject(containerVersionedProject);
      
      // override rights policy (though projects not restored from persistence for now) :
      // HOWEVER SINCE THERE ARE NO LOCAL MODELS, MOST OF THIS IS USELESS
      // TODO LATER better : prevent from writing locally model resources (dcmo:model_0 modelType)
      setDefaultGlobalPublicSecurity(orgProject, containerVersionedProject.getSecurityDefaults().getResourceOwners());
      // XXX NB. PermissionEvaluator hack allows to write in visible container project, TODO better here
      
      modelAdminService.addProject(orgProject);
      
      getMainProject().addLocalVisibleProject(orgProject); // because facade, allows transparent migration
      return orgProject;
   }

   /**
    * Adds meta
    * @param unversionedName
    * @param majorVersion 0, 1...
    * @param doc
    * @param owners default resourceOwners, therefore also of model resources ;
    * if none, taken from project-named prop in project admins prop file
    * or else from its oasis.meta prop
    * @param visibleProjects (oasis.meta is implicitly added)
    * @return
    */
   public DCProject buildProjectDefaultConf(
         String nameWithVersionIfAny,
         String doc, LinkedHashSet<String> owners,
         DCProject ... visibleProjects) {
      DCProject org1Project = new DCProject(nameWithVersionIfAny); // geo_1 // NB. in geo_1.0, 0 would be minorVersion
      org1Project.setDocumentation(doc);
      org1Project.addLocalVisibleProject(getMetamodelProject()); // else metamodel not visible
      // TODO prevent local models
      
      for (DCProject visibleProject : visibleProjects) {
         org1Project.addLocalVisibleProject(visibleProject); // geo // ! could be rather geo1, but then could not be visible in org !
      }

      if (owners == null || owners.isEmpty()) {
         owners = buildDefaultProjectOwners(nameWithVersionIfAny);
      }
      // override rights policy (though projects not restored from persistence for now) :
      setDefaultGlobalPublicSecurity(org1Project, owners);
      
      modelAdminService.addProject(org1Project);
      // NB. versioned projects aren't in main, rather their facade, to allow transparent migration
      return org1Project;
   }
   
   
   ////////////////////////////////////////////////////////////
   // SECURITY HELPERS
   
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
