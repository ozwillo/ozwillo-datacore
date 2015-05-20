package org.oasis.datacore.core.meta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.oasis.datacore.common.context.DCRequestContextProvider;
import org.oasis.datacore.common.context.DCRequestContextProviderFactory;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCModelService;
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
      DCProject project = getProject(model.getProjectName()); // NB. can't be null
      return project.getDefinitionModel(model);
   }
   @Override
   public DCModelBase getStorageModel(DCModelBase model) {
      DCProject project = getProject(model.getProjectName()); // NB. can't be null
      return project.getStorageModel(model);
   }
   /**
    * TODO cache
    * @param model must be storage
    * @return models having given model as storage model
    */
   @Override
   public Collection<DCModelBase> getStoredModels(DCModelBase model) {
      ArrayList<DCModelBase> storedModels = new ArrayList<DCModelBase>();
      HashSet<String> alreadyDoneModelNames = new HashSet<String>();
      for (DCProject p : this.getProjects()) {
         /*if (p.getName().equals(project.getName())) {
            continue;
         }*/
         for (DCModelBase m : p.getModels()) { // including visible projects'
            if (!m.getMixinNames().contains(model.getName())
                  && !model.getName().equals(m.getName()) // NB. m.mixins do not contain m
                  || alreadyDoneModelNames.contains(m.getName())) {
               continue;
            }
            alreadyDoneModelNames.add(m.getName());
            DCModelBase storageModel = p.getStorageModel(m);
            if (storageModel == null || !storageModel.getAbsoluteName().equals(model.getAbsoluteName())) {
               continue;
            }
            storedModels.add(m);
         }
      }
      return storedModels;
   }
   
   
   @Override
   public Collection<DCProject> getProjects() {
      return this.projectMap.values();
   }
   
   // TODO cache
   public List<DCProject> getVisibleProjects(DCProject fromProject) {
      List<DCProject> allVisibleProjects = new ArrayList<DCProject>();
      for (DCProject localVisibleProject : fromProject.getLocalVisibleProjects()) {
         // TODO cycle ???
         allVisibleProjects.addAll(dataModelService.getVisibleProjects(localVisibleProject));
      }
      return allVisibleProjects;
   }
   public List<DCProject> getProjectsSeing(DCProject project) {
      List<DCProject> projectsSeingIt = new ArrayList<DCProject>();
      p : for (DCProject p : dataModelService.getProjects()) {
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
   public DCModel getModel(String type) {
      DCProject project = getProject(); // NB. can't be null
      // NB. devmode default alt models building is done in there :
      DCModelBase model = project.getModel(type);
      if (model == null) {
         return null;
      }
      if (!model.isStorage()) {
         return null; // actually a mixin NOOOOOOOOOOOOOO TODO Instanciable
      }
      return (DCModel) model; // TODO handle changes of isStorage
   }
   // includes models (?)
   @Override
   public DCModelBase getMixin(String type) {
      DCProject project = getProject(); // NB. can't be null
      // NB. devmode default alt models building is done in there :
      return project.getModel(type); // TODO if isStorageOnly, get inherited model
   }
   public DCProject getProject(String projectName) {
      DCProject project = projectMap.get(projectName);
      if (project == null) { // && devmode
         // devmode default projects building :
         if (projectName.endsWith(".test")) {
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
         }
      }
      /*if (project == null) {
         throw new ProjectException(projectName, "Unknown project");
      }*/
      return project;
   }
   public DCProject getProject() {
      String projectName = (String) requestContextProviderFactory.get(DCRequestContextProvider.PROJECT);
      if (projectName == null) {
         projectName = DCProject.OASIS_MAIN; // default TODO TODO rather oasis.test
      }
      if (projectName == null) { // TODO or default
         throw new ProjectException("current", "Unable to find current project");
      }
      DCProject project = getProject(projectName);
      if (project == null) {
         throw new ProjectException(projectName, "Unable to find project");
      }
      return project;
   }
   public DCProject getMainProject() {
      return getProject(DCProject.OASIS_MAIN);
   }
   public DCProject getSampleProject() {
      return getProject(DCProject.OASIS_SAMPLE);
   }

   @Override
   public DCField getFieldByPath(DCModel dcModel, String fieldPath) {
      // see impl in DatacoreApiImpl BUT pb providing also lastHighestListField
      throw new UnsupportedOperationException();
   }

   @Override
   public Collection<DCModel> getModels() {
      ArrayList<DCModel> models = new ArrayList<DCModel>();
      for (DCModelBase model : getProject().getModels()) {
         if (model.isStorage()) {
            models.add((DCModel) model); // TODO handle changes to isStorage
         }
      }
      return models;
   }

   // also contains models TODO is it OK ?
   @Override
   public Collection<DCModelBase> getMixins() {
      return getProject().getModels(); // TODO TODO // NB. project can't be null
   }
   
   
   ///////////////////////////////////////
   // admin / update methods
   // TODO TODO global write lock on those, & also when updating from Resource persistence

   /** also adds to mixin TODO is it OK ? */
   public void addModel(DCModelBase dcModel) {
      getProject(dcModel.getProjectName()).addLocalModel(dcModel); // TODO LATER better : check, in context...
   }
   
   /**  @obsolete ONLY TO CREATE DERIVED MODELS ex. Contribution, TODO LATER rather change their name ?!? */
   public void addModel(DCModel dcModel, String name) {
      getProject().addModel(dcModel, name); // NB. project can't be null
   }

   public void removeModel(DCModelBase dcModel) {
      getProject(dcModel.getProjectName()).removeLocalModel(dcModel.getName()); // TODO LATER better : check, in context...
   }
   
   /** @obsolete rather use removeModel(model) to use the right project */
   public void removeModel(String name) {
      getProject().removeLocalModel(name); // NB. project can't be null
   }

   /**
    * Checks whether its used mixin models are already known (in same version)
    * LATER do the same for fields ?
    * @param mixin
    */
   public void addMixin(DCModelBase mixin) {
      getProject().addLocalModel(mixin); // NB. project can't be null
   }
   
   /** @obsolete ather use removeModel(model) to use the right project */
   public void removeMixin(String name) {
      getProject().removeLocalModel(name);
   }
   
   public void addProject(DCProject project) {
      projectMap.put(project.getName(), project);
   }
   
}
