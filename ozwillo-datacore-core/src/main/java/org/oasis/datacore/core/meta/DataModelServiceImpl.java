package org.oasis.datacore.core.meta;

import org.oasis.datacore.common.context.DCRequestContextProvider;
import org.oasis.datacore.common.context.DCRequestContextProviderFactory;
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

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

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
   protected DCRequestContextProviderFactory requestContextProviderFactory;

   /** to inject caching... */
   @Resource(name="modelService")// or @PostConstruct ; @Autowired doesn't work http://stackoverflow.com/questions/5152686/self-injection-with-spring
   protected DCModelService dataModelService;
   
   private Map<String,DCProject> projectMap = new HashMap<>();

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
      return projects.stream()
              .map(DCPointOfView::getName).collect(Collectors.toCollection(LinkedHashSet::new));
   }
   @Override
   public LinkedHashSet<String> getForkedUriProjectNames(String uri) {
      if (forkedUriToProjectNames == null) {
         // NB. not required to be synchronized, because there's no problem if it's
         // done twice at the same time
         this.forkedUriToProjectNames = buildForkedUriToProjectNames(
               this.projectMap.values(), new HashMap<>(),
               new HashSet<>(this.projectMap.size()));
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

   @Override
   public DCProject getProject(String projectName) {
      return projectMap.get(projectName);
   }

   @Override
   public DCProject getProject() {
      String projectName = (String) requestContextProviderFactory.get(DCRequestContextProvider.PROJECT);
      if (projectName == null) {
         projectName = DCProject.OASIS_MAIN; // default TODO TODO rather oasis.test
      }
      DCProject project = getProject(projectName);
      if (project == null) {
         throw new ProjectException(projectName, "Unable to find project");
      }
      return project;
   }

   @Override
   public Collection<DCModelBase> getModels() {
      return getProject().getModels();
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
   
   /**  @deprecated  ONLY TO CREATE DERIVED MODELS ex. Contribution, TODO LATER rather change their name ?!? */
   public void addModel(DCModel dcModel, String name) {
      getModelProjectOrCurrent(dcModel).addModel(dcModel, name); // NB. project can't be null
   }

   public void removeModel(DCModelBase dcModel) {
      getModelProjectOrCurrent(dcModel).removeLocalModel(dcModel.getName()); // TODO LATER better : check, in context...
   }
   
   /** @deprecated rather use removeModel(model) to use the right project */
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
