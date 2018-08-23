package org.oasis.datacore.core.meta.model;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.oasis.datacore.core.meta.pov.DCPointOfView;
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
    */
   DCProject getProject();

   // POLY NEW
   DCProject getProject(String type);
   Collection<DCProject> getProjects();

   /**
    * @return including given project !
    */
   List<DCProject> getProjectsSeeing(DCProject project);
   List<DCProject> getProjectsSeeing(DCProject project, boolean outsideItself);

   /**
    * Because project seeing project can still hard fork one of its models
    */
   List<DCProject> getProjectsSeeingModel(DCModelBase modelOrMixin);

   /** (in memory for now) */
   LinkedHashSet<String> getForkedUriProjectNames(String uri);

   /**
    * Works on a new project ; use it to build multiProjectStorage criteria ;TODO cache
    * @return including given project !
    */
   LinkedHashSet<DCProject> getVisibleProjects(DCProject localVisibleProject);
   LinkedHashSet<String> toNames(Collection<? extends DCPointOfView> projects);

   /**
    * POLY NEW replaces getModel/Mixin TODO migrate.
    * Must be called within actual project else wrong if fork
    * @return any DCModelBase (where isStorage/Only can be tested)
    */
   DCModelBase getModelBase(String type);

   /**
    * Must be called within actual project else wrong if fork.
    * NB. there can't be more than one inherited model being definition
    * (else they would be inherited by a non-definition model, which would
    * then be definition since it would define that both must be added).
    * @param model uses its project
    */
   DCModelBase getDefinitionModel(DCModelBase model);

   /**
    * Must be called within actual project else wrong if fork.
    * @param model uses its project
    */
   DCModelBase getStorageModel(DCModelBase model);
   
   Collection<DCModelBase> getStoredModels(DCModelBase model);

   /**
    * @deprecated
    */
   Collection<DCModelBase> getModels();
}
