package org.oasis.datacore.core.meta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
import org.springframework.stereotype.Component;

/**
 * TODO readonly : get & set map only for tests, LATER admin using another (inheriting) model ?!?
 * => LATER extract update methods in another admin interface
 * @author mdutoo
 *
 */
@Component
public class DataModelServiceImpl implements DCModelService {
   
   /** to use default projects building */
   @Value("${datacore.devmode}")
   private boolean devmode;
   @Autowired
   ///@Qualifier("datacore.cxfJaxrsApiProvider")
   //protected DCRequestContextProvider requestContextProvider;
   protected DCRequestContextProviderFactory requestContextProviderFactory;
   
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
   public DCModelBase getDefinitionModel(String type) {
      DCProject project = getProject(); // NB. can't be null
      return project.getDefinitionModel(type);
   }
   @Override
   public DCModelBase getStorageModel(String type) {
      DCProject project = getProject(); // NB. can't be null
      return project.getStorageModel(type);
   }
   @Override
   public Collection<DCProject> getProjects() {
      return this.projectMap.values();
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
      if (project == null && devmode) {
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
      if (project == null) {
         throw new ProjectException(projectName, "Unknown project");
      }
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
      getProject().addLocalModel(dcModel); // NB. project can't be null
   }
   
   /** ONLY TO CREATE DERIVED MODELS ex. Contribution, TODO LATER rather change their name ?!? */
   public void addModel(DCModel dcModel, String name) {
      getProject().addModel(dcModel, name); // NB. project can't be null
   }

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
   
   /**
    * TODO LATER check version
    * @param name
    */
   public void removeMixin(String name) {
      getProject().removeLocalModel(name);
   }
   
   public void addProject(DCProject project) {
      projectMap.put(project.getName(), project);
   }

}
