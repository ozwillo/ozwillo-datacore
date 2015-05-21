package org.oasis.datacore.core.meta.model;

import java.util.Collection;
import java.util.List;

import org.oasis.datacore.core.meta.pov.DCProject;


/**
 * NB. :
 * * getModel() methods return models from the current (contextual) project.
 * * to iterate over ALL models whatever the project, interate over projects
 * and their UseCasePointOfViews.
 * 
 * @author mdutoo
 *
 */
public interface DCModelService {
   
   /**
    * Returns the current project
    * @return
    */
   DCProject getProject();

   // POLY NEW
   DCProject getProject(String type);
   Collection<DCProject> getProjects();
   /**
    * 
    * @param localVisibleProject
    * @return including given project !
    */
   Collection<DCProject> getVisibleProjects(DCProject localVisibleProject);
   /**
    * 
    * @param project
    * @return including given project !
    */
   List<DCProject> getProjectsSeing(DCProject project);

   /**
    * POLY NEW replaces getModel/Mixin TODO migrate
    * @param type
    * @return any DCModelBase (where isStorage/Only can be tested)
    */
   DCModelBase getModelBase(String type);

   /**
    * i.e. in the current project's non visible projects, used when inheriting / hiding models
    * when type = the own model type of the model it is loaded in as mixin
    * @param type
    * @return
    */
   DCModelBase getNonLocalModel(String type);

   /**
    * NB. there can't be more than one inherited model being definition
    * (else they would be inherited by a non-definition model, which would
    * then be definition since it would define that both must be added).
    * @param model uses its project
    * @return
    */
   DCModelBase getDefinitionModel(DCModelBase model);

   /**
    * 
    * @param model uses its project
    * @return
    */
   DCModelBase getStorageModel(DCModelBase model);
   
   Collection<DCModelBase> getStoredModels(DCModelBase model);
   
   /**
    * reified model types
    * @param modelType
    * @return
    * @obsolete
    */
   DCModel getModel(String modelType);
   /**
    * shared mixin types ; includes models (?)
    * @param type
    * @return
    * @obsolete
    */
   DCModelBase getMixin(String type);

   /**
    * reified model types
    * @return
    * @obsolete
    */
   Collection<DCModel> getModels();
   /**
    * shared mixin types ; includes models TODO is it OK ?
    * @return
    * @obsolete
    */
   Collection<DCModelBase> getMixins();
   
   /** TODO not used for now */
   DCField getFieldByPath(DCModel dcModel, String fieldPath);

}
