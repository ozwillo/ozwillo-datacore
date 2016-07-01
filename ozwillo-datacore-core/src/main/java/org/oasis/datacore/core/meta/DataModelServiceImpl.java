package org.oasis.datacore.core.meta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.oasis.datacore.common.context.DCRequestContextProvider;
import org.oasis.datacore.common.context.DCRequestContextProviderFactory;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
import org.oasis.datacore.core.meta.model.DCSecurity;
import org.oasis.datacore.core.meta.pov.DCPointOfView;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.core.meta.pov.ProjectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * TODO readonly : get & set map only for tests, LATER admin using another (inheriting) model ?!?
 * => LATER extract update methods in another admin interface
 * @author mdutoo
 *
 */
@Service("modelService")
public class DataModelServiceImpl implements DCModelService {
   
   /** to use default projects building */
   @Value("${datacore.devmode}")
   private boolean devmode;
   
   @Autowired
   ///@Qualifier("datacore.cxfJaxrsApiProvider")
   //protected DCRequestContextProvider requestContextProvider;
   protected DCRequestContextProviderFactory requestContextProviderFactory;
   /** to inject caching... */
   @Resource(name="modelService")// or @PostConstruct ; @Autowired doesn't work http://stackoverflow.com/questions/5152686/self-injection-with-spring
   protected DCModelService dataModelService;
   
   //TODO modelTo(ModelImplementation)CollectionMap ?
   //TODO (modelDefinitionMap) modelImplementationConfigurationMap, modelImplementationInstanciationMap, modelToImplementatioI(|C)Map
   //TODO ModelImplementationConfiguration MICCStrategy.checkAccepted(model, or Resource ex. if depends from contributor vs model owner ??)
   //TODO having MICCRootTypesStrategy.rootTypes = cofr, coit... or MICCModelOwnerStrategy.modelOwners = cofr_owners, coit_owners...
   
   private Map<String,DCProject> projectMap = new HashMap<String, DCProject>();

   /**
    * forkedUris are ordered with leaf projects being last.
    * not in project because more efficient without complex cache,
    * LATER maybe make it a cache */
   private HashMap<String,LinkedHashSet<String>> forkedUriToProjectNames = null;


   /** TODO cache */
   @Override
   public DCSecurity getSecurity(DCModelBase model) {
      DCSecurity security;
      while ((security = model.getSecurity()) == null) {
         if (model.getMixins().isEmpty()) {
            return null; // can't find primary-inherited security, TODO error
         }
         model = model.getMixins().iterator().next();
      }
      return security;
   }
   
   // NEW TODO migrate
   @Override
   public DCModelBase getModelBase(String type) {
      DCProject project = getProject(); // NB. can't be null
      // NB. devmode default alt models building is done in there :
      return project.getModel(type); // TODO if isStorageOnly, get inherited model ?
   }
   @Override
   public DCModelBase getNonLocalModel(String type) {
      DCProject project = getProject(); // NB. can't be null
      // NB. devmode default alt models building is done in there :
      return project.getNonLocalModel(type);
   }
   @Override
   public DCModelBase getDefinitionModel(DCModelBase model) {
      DCProject project = getProject(); // NB. can't be null
      // (and not getProject(model.getProjectName()) else dcmi:mixin_0 fork not returned for dcmo:model_0)
      return project.getDefinitionModel(model);
   }
   @Override
   public DCModelBase getStorageModel(DCModelBase model) {
      DCProject project = getProject(); // NB. can't be null
      // (and not getProject(model.getProjectName()) else dcmi:mixin_0 fork not returned for dcmo:model_0)
      return project.getStorageModel(model);
   }
   /**
    * TODO cache
    * @param model must be storage
    * @return models having given model as storage model, including itself (whether they are instanciable or not)
    */
   @Override
   public Collection<DCModelBase> getStoredModels(DCModelBase model) {
      ArrayList<DCModelBase> storedModels = new ArrayList<DCModelBase>();
      HashSet<String> alreadyDoneModelNames = new HashSet<String>();
      for (DCProject p : this.getProjectsSeeingModel(model)) {
         /*if (p.getName().equals(project.getName())) {
            continue;
         }*/
         for (DCModelBase m : p.getModels()) { // including visible projects'
            if (!alreadyDoneModelNames.add(m.getAbsoluteName())) {
               continue;
            }
            if (!m.getGlobalMixinNames().contains(model.getName())
                  && !model.getName().equals(m.getName())) { // NB. m.mixins do not contain m
               continue; // TODO is this check still required ??
            }
            DCModelBase storageModel = p.getStorageModel(m);
            if (storageModel != null && storageModel.getAbsoluteName().equals(model.getAbsoluteName())) {
               storedModels.add(m);
            }
         }
      }
      return storedModels;
   }


   @Override
   public List<DCProject> getProjectsSeeingModel(DCModelBase modelOrMixin) {
      DCProject project = dataModelService.getProject(modelOrMixin.getProjectName());
      return this.getProjectsSeeing(project).stream()
            .filter(p -> doesProjectSeeModelOfSeenProject(p, modelOrMixin))
            .collect(Collectors.toList());
   }

   private boolean doesProjectSeeModelOfSeenProject(DCProject p, DCModelBase modelOrMixin) {
      DCModelBase modelOrMixinSeenFromP = p.getModel(modelOrMixin.getName());
      if (modelOrMixinSeenFromP == null) {
         // p sees project but not its modelOrMixin i.e. it has been
         // hard forked (but not yet created), therefore no impact
         return false;
      }
      if (modelOrMixinSeenFromP.getAbsoluteName().equals(modelOrMixin.getAbsoluteName())) {
         return true; // p sees exactly modelOrMixin
      }
      // checking among mixins rather than itself in the case of hidden / soft fork :
      for (DCModelBase mm : modelOrMixinSeenFromP.getMixins()) { // NB. m.mixins do not contain m
         if (mm.getAbsoluteName().equals(modelOrMixin.getAbsoluteName())) {
            // p sees a hidden / soft forked from another project version of modelOrMixin
            // and not another model with the same name in an orthogonal project, or a hard forked model
            return true;
         }
      }
      return false;
   }
   
   
   @Override
   public Collection<DCProject> getProjects() {
      return this.projectMap.values();
   }
   
   @Override
   public LinkedHashSet<DCProject> getVisibleProjects(DCProject fromProject) {
      LinkedHashSet<DCProject> allVisibleProjects = new LinkedHashSet<DCProject>();
      fillVisibleProjects(fromProject, allVisibleProjects);
      return allVisibleProjects;
   }
   public void fillVisibleProjects(DCProject fromProject, LinkedHashSet<DCProject> filledVisibleProjects) {
      filledVisibleProjects.add(fromProject);
      for (DCProject localVisibleProject : fromProject.getLocalVisibleProjects()) {
         // TODO cycle ??? TODO better order !!
         if (!filledVisibleProjects.contains(localVisibleProject)) {
            this.fillVisibleProjects(localVisibleProject, filledVisibleProjects);
         } // else ex. project seen by everybody
      }
   }
   @Override
   public List<DCProject> getProjectsSeeing(DCProject project) {
      return getProjectsSeeing(project, false);
   }
   @Override
   public List<DCProject> getProjectsSeeing(DCProject project, boolean outsideItself) {
      List<DCProject> projectsSeingIt = new ArrayList<DCProject>();
      p : for (DCProject p : dataModelService.getProjects()) {
         if (outsideItself && p.getName().equals(project.getName())) {
            continue;
         }
         for (DCProject vp : dataModelService.getVisibleProjects(p)) {
            if (vp.getName().equals(project.getName())) {
               projectsSeingIt.add(p);
               continue p;
            }
         }
      }
      return projectsSeingIt;
   }

   @Override
   public LinkedHashSet<String> toNames(Collection<? extends DCPointOfView> projects) {
      return new LinkedHashSet<String>(projects.stream()
            .map(p -> p.getName()).collect(Collectors.toList()));
   }
   @Override
   public LinkedHashSet<String> getForkedUriProjectNames(String uri) {
      if (forkedUriToProjectNames == null) {
         // NB. not required to be synchronized, because there's no problem if it's
         // done twice at the same time
         this.forkedUriToProjectNames = buildForkedUriToProjectNames(
               this.projectMap.values(), new HashMap<String,LinkedHashSet<String>>(),
               new HashSet<DCProject>(this.projectMap.size()));
      }
      return forkedUriToProjectNames.get(uri);
   }
   private HashMap<String, LinkedHashSet<String>> buildForkedUriToProjectNames(
         Collection<DCProject> projects, HashMap<String, LinkedHashSet<String>> res,
         HashSet<DCProject> alreadyHandledProjects) {
      for (DCProject project : projects) {
         if (alreadyHandledProjects.contains(project)) {
            continue;
         }
         // adding upper projects forkedUri first, so that forkedUris are ordered with leaf projects being last :
         buildForkedUriToProjectNames(project.getLocalVisibleProjects(), res, alreadyHandledProjects);
         // then adding this project's forkedUris :
         if (project.getForkedUris() != null) {
            for (String forkedUri : project.getForkedUris()) {
               LinkedHashSet<String> forkingProjects = res.get(forkedUri);
               if (forkingProjects == null) {
                  forkingProjects = new LinkedHashSet<String>();
                  res.put(forkedUri, forkingProjects);
               }
               forkingProjects.add(project.getName());
            }
         }
         alreadyHandledProjects.add(project);
      }
      return res;
   }

   /*@Override
   public LinkedHashSet<String> getVisibleProjectNames(Collection<String> projectNames) {
      LinkedHashSet<DCProject> allVisibleProjects = new LinkedHashSet<DCProject>();
      for (String projectName : projectNames) {
         fillVisibleProjects(this.getProject(projectName), allVisibleProjects);
      }
      return new LinkedHashSet<String>(allVisibleProjects.stream()
            .map(p -> p.getName()).collect(Collectors.toList()));
   }*/
   
   @Override
   public DCProject getProject(String projectName) {
      DCProject project = projectMap.get(projectName);
      if (project == null) { // && devmode
         // devmode default projects building :
         /*if (projectName.endsWith(".test")) {
            String testedProjectName = projectName.substring(0,
                  projectName.length() - ".test".length());
            DCProject testedProject = projectMap.get(testedProjectName);
            if (testedProject != null) {
               project = new DCProject(projectName);
               project.addLocalVisibleProject(testedProject);
               this.addProject(project);
            }
         
         // auto created projects :
         // TODO move to init
         } else if (DCProject.OASIS_MAIN.equals(projectName)) {
            project = new DCProject(DCProject.OASIS_MAIN);
            this.addProject(project);
         } else if (DCProject.OASIS_SAMPLE.equals(projectName)) {
            project = new DCProject(DCProject.OASIS_SAMPLE);
            this.addProject(project);
         }*/
      }
      return project;
   }
   @Override
   public DCProject getProject() {
      String projectName = (String) requestContextProviderFactory.get(DCRequestContextProvider.PROJECT);
      if (projectName == null) {
         projectName = DCProject.OASIS_MAIN; // default TODO TODO rather oasis.test
      }
      if (projectName == null) { // TODO or default
         throw new ProjectException("[current]", "Unable to find current project");
      }
      DCProject project = getProject(projectName);
      if (project == null) {
         throw new ProjectException(projectName, "Unable to find project");
      }
      return project;
   }

   @Override
   public DCField getFieldByPath(DCModelBase dcModel, String fieldPath) {
      // see impl in DatacoreApiImpl BUT pb providing also lastHighestListField
      throw new UnsupportedOperationException();
   }

   @Override
   public Collection<DCModelBase> getModels() {
      return getProject().getModels();
   }

   @Override
   public Collection<DCModelBase> getModels(boolean isInstanciable) {
      ArrayList<DCModelBase> models = new ArrayList<DCModelBase>();
      for (DCModelBase model : getProject().getModels()) {
         if (model.isInstanciable() == isInstanciable) {
            models.add(model);
         }
      }
      return models;
   }
   
   
   ///////////////////////////////////////
   // admin / update methods
   // TODO TODO global write lock on those, & also when updating from Resource persistence

   /** also adds to mixin TODO is it OK ? */
   public void addModel(DCModelBase dcModel) {
      getModelProjectOrCurrent(dcModel).addLocalModel(dcModel); // TODO LATER better : check, in context...
   }
   
   private DCProject getModelProjectOrCurrent(DCModelBase dcModel) {
      String projectName = dcModel.getProjectName();
      DCProject project;
      if (projectName != null) {
         project = getProject(projectName);
      } else {
         project = getProject(); // current
      }
      return project;
   }
   
   /**  @obsolete ONLY TO CREATE DERIVED MODELS ex. Contribution, TODO LATER rather change their name ?!? */
   public void addModel(DCModel dcModel, String name) {
      getModelProjectOrCurrent(dcModel).addModel(dcModel, name); // NB. project can't be null
   }

   public void removeModel(DCModelBase dcModel) {
      getModelProjectOrCurrent(dcModel).removeLocalModel(dcModel.getName()); // TODO LATER better : check, in context...
   }
   
   /** @obsolete rather use removeModel(model) to use the right project */
   public void removeModel(String name) {
      getProject().removeLocalModel(name); // NB. project can't be null
   }
   
   public void addProject(DCProject project) {
      projectMap.put(project.getName(), project);
      this.forkedUriToProjectNames = null;
   }
   
   public void removeProject(DCProject project) {
      projectMap.remove(project.getName());
      this.forkedUriToProjectNames = null;
   }
   
   public void updateProject(DCProject project) {
      projectMap.put(project.getName(), project); // in case instance changed ; or copy fields
      this.forkedUriToProjectNames = null;
   }

   /** helper */
   public void addForkedUri(DCProject project, String forkedUri) {
      if (project.getForkedUris().add(forkedUri)) {
         this.updateProject(project);
      }
   }

   /** helper */
   public void removeForkedUri(DCProject project, String forkedUri) {
      if (project.getForkedUris().remove(forkedUri)) {
         this.updateProject(project);
      }
   }

   public void addLocalModel(DCProject project, DCModelBase localModel) {
      this.addLocalModel(project, localModel, false);
   }

   public void addLocalModel(DCProject project, DCModelBase localModel, boolean forkModel) {
      project.addLocalModel(localModel);
      if (forkModel) {
         // allowing fork of localModel :
         this.addForkedUri(project, SimpleUriService.buildModelUri(localModel.getName()));
      }
   }
   
}
