package org.oasis.datacore.core.meta.model;

import java.util.Collection;
import java.util.LinkedHashSet;
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
   
   /** to be able to build model URIs in -core */
   String MODEL_MODEL_NAME = "dcmo:model_0";

   /** gets model's (possible inherited) security */
   DCSecurity getSecurity(DCModelBase model);
   
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

   /** shortcut to build multiProjectStorage criteria, TODO request-level cache */
   LinkedHashSet<String> getVisibleProjectNames();
   /** (in memory for now) */
   LinkedHashSet<String> getForkedUriProjectNames(String uri);
   /** TODO cache */
   LinkedHashSet<String> getVisibleProjectNames(Collection<String> namesOfProjectsForkingUri);
   /** TODO cache */
   LinkedHashSet<String> getVisibleProjectNames(String projectName);

   /**
    * POLY NEW replaces getModel/Mixin TODO migrate.
    * Must be called within actual project else wrong if fork
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
    * Must be called within actual project else wrong if fork.
    * NB. there can't be more than one inherited model being definition
    * (else they would be inherited by a non-definition model, which would
    * then be definition since it would define that both must be added).
    * @param model uses its project
    * @return
    */
   DCModelBase getDefinitionModel(DCModelBase model);

   /**
    * Must be called within actual project else wrong if fork.
    * @param model uses its project
    * @return
    */
   DCModelBase getStorageModel(DCModelBase model);
   
   Collection<DCModelBase> getStoredModels(DCModelBase model);

   /**
    * @return
    * @obsolete
    */
   Collection<DCModelBase> getModels();

   /**
    * 
    * @param isInstanciable
    * @return instanciable models OR pure shared mixin types 
    */
   Collection<DCModelBase> getModels(boolean isInstanciable);
   
   /** TODO not used for now */
   DCField getFieldByPath(DCModelBase dcModel, String fieldPath);

}
