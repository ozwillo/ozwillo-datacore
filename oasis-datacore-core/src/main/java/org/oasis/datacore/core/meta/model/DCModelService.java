package org.oasis.datacore.core.meta.model;

import java.util.Collection;

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

   // POLY NEW
   DCProject getProject(String type);
   Collection<DCProject> getProjects();
   /**
    * Returns the current project
    * @return
    */
   DCProject getProject();

   /**
    * POLY NEW replaces getModel/Mixin TODO migrate
    * @param type
    * @return any DCModelBase (where isStorage/Only can be tested)
    */
   DCModelBase getModelBase(String type);

   /**
    * NB. there can't be more than one inherited model being definition
    * (else they would be inherited by a non-definition model, which would
    * then be definition since it would define that both must be added)
    * @param type
    * @return
    */
   DCModelBase getDefinitionModel(String type);

   /**
    * 
    * @param type
    * @return
    */
   DCModelBase getStorageModel(String type);
   
   /**
    * reified model types
    * @param modelType
    * @return
    */
   DCModel getModel(String modelType);
   /**
    * shared mixin types ; includes models (?)
    * @param type
    * @return
    */
   DCModelBase getMixin(String type);

   /**
    * reified model types
    * @return
    */
   Collection<DCModel> getModels();
   /**
    * shared mixin types ; includes models TODO is it OK ?
    * @return
    */
   Collection<DCModelBase> getMixins();
   
   /** TODO not used for now */
   DCField getFieldByPath(DCModel dcModel, String fieldPath);

}
