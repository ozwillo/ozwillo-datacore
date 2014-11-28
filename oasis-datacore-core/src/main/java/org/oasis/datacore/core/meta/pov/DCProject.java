package org.oasis.datacore.core.meta.pov;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.ModelException;
import org.oasis.datacore.core.meta.ModelNotFoundException;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;

public class DCProject extends DCPointOfViewBase {
   
   public static final String OASIS_MAIN = "oasis.main";
   public static final String OASIS_SAMPLE = "oasis.sample";

   /* TODO organization.project ex. oasis.main */
   /*
   private String name;

   private LinkedHashMap<String,DCModelBase> altModelMap = new LinkedHashMap<String, DCModelBase>();
   ///private LinkedHashMap<String,List<DCStorage>> altStoragesMap = new LinkedHashMap<String, List<DCStorage>>();
   ///private LinkedHashMap<String,DCStorage> altStorageMap = new LinkedHashMap<String, DCStorage>();
   */
   private String documentation;
   
   private LinkedHashMap<String,DCUseCasePointOfView> useCasePointOfViewMap = new LinkedHashMap<String, DCUseCasePointOfView>();
   
   /** LATER also reuse visible ones ? */
   private LinkedHashMap<String,DCProject> visibleProjectMap = new LinkedHashMap<String, DCProject>(); // TODO merge in visibleStorageProjectMap
   /** LATER can see model (unless alt model) but not data (by alt'ing model anonymously as not storage),
    * for this the local alt (storage) model must be created, optionally with some sample copied or rather imported data */
   private LinkedHashMap<String,DCProject> visibleDefProjectMap = new LinkedHashMap<String, DCProject>();
   /** OPT can see data (get & refer to it IF SECURITY OK) (unless alt storage) but not change (POST) (by alt'ing its DCSecurity to readonly),
    * for this the local alt (storage only) model must be created, probably with some sample copied data, with same security + data tester ;
    * modelization is not expected to change */
   private LinkedHashMap<String,DCProject> visibleDataProjectMap = new LinkedHashMap<String, DCProject>(); // TODO OPT
   /** (TODO or not ?) can change data (POST) (unless alt'd as not storage), i.e. allows oasis.main to redirect to others
    * AND DELEGATE SECURITY */
   private LinkedHashMap<String,DCProject> visibleStorageProjectMap = new LinkedHashMap<String, DCProject>(); // TODO or not ?

   /** TODO ordered cache, includes itself
    * to be invalidated each type (even indirectly) visible projects change
    * (including when backward compatible).
    * TODO not thread-safe, handle it the REST way
    * TODO minorVersion ??? */
   private LinkedHashMap<String,DCProject> allVisibleProjectMap = null;
   private LinkedHashMap<String,DCModelBase> allAltModelMap = null;

   /** for unmarshalling only */
   public DCProject() {
      super();
   }

   /** helper for tests */
   public DCProject(String name) {
      super(name);
   }

   public DCModelBase getModel(String type) { // TODO or in projectService ?!
      // TODO rather from cache allAltModelMap
      DCModelBase model = super.getModel(type); // existing local alt model
      if (model != null) {
         return model;
      }
      // else either reused as is (mixin or storage), or not alt'd yet,
      // in which case let's alt it by default :
      for (DCProject project : visibleDefProjectMap.values()) { // TODO cache allVisibleProjectMap
         model = project.getModel(type);
         if (model != null) {
            if (model.isStorage()) {
               // TODO new model without storage
               DCModelBase inheritedModel = model;
               model = new DCMixin(type, this); // DCModelOrMixin
               model.addMixin(inheritedModel);
               model.setStorage(false); // not storageOnly : allow to override modelization
               // data : none, must be imported (along with model probably)
            }
            return model;
         }
      }
      // TODO LATER readonly IN ADDITION to other rights (for now use storage) :
      for (DCProject project : visibleDataProjectMap.values()) { // TODO cache allVisibleProjectMap
         model = project.getModel(type);
         if (model != null) {
            /*if (model.isStorage()) { // && !model.getSecurity().isReadOnly()
               // TODO new model (with storage) with readonly (but not new data TODO POSSIBLE ???)
               DCModel inheritedModel = model;
               model = new DCModel(type); // DCAltModel
               model.addMixin(inheritedModel);
               model.setStorage(true);
               DCSecurity security = new DCSecurity();
               security.setReadOnly(true); // TODO LATER readonly IN ADDITION to other rights
               model.setSecurity(security);
               // data : reused
               model.setCollectionName(inheritedModel.getCollectionName());
            } else */
            if (model.isStorage()) {
               // TODO new model (with storage) with readonly (but not new data TODO POSSIBLE ???)
               DCModelBase inheritedModel = model;
               model = new DCModel(type, this); // DCAltModel
               model.addMixin(inheritedModel);
               model.setDefinition(false); // storageOnly : modelization shouldn't change
               // copy some data (according to rights) to new test project,
               // starting from business leaf model (according to security) !!!
            }
            return model;
         }
      }
      for (DCProject project : visibleStorageProjectMap.values()) { // TODO cache allVisibleProjectMap
         model = project.getModel(type);
         if (model != null) {
            /*???if (!model.isStorage()) {
               // TODO new model with storage
               DCModel inheritedModel = model;
               model = new DCModel(type); // DCModelOrMixin
               model.addMixin(inheritedModel);
               model.setStorageOnly(true); // & storage
            }*/
            return model;
         }
      }
      /*for (DCProject project : visibleProjectMap.values()) { // TODO cache allVisibleProjectMap
         model = project.getModel(type);
         if (model != null) {
            return model;
         }
      }*/
      return null;
   }

   public DCModelBase getLocalModel(String type) {
      // NB. other impls i.e. routing strategies are allowed by DCUseCasePointOfView
      return modelMap.get(type);
   }
   
   public DCModelBase getDefinitionModel(String type) {
      String inheritedType = type;
      DCModelBase model;
      // TODO cache :
      while ((model = this.getModel(inheritedType)) != null) {
         if (model.isDefinition()) {
            return model;
         }
         // get inherited type i.e. first mixin :
         // TODO or rather visit the full mixin hierarchy in order ??
         if (model.getMixinNames().isEmpty()) {
            throw new ModelNotFoundException(type, this, "Unable to find any definition model");
         }

         inheritedType = model.getMixinNames().iterator().next(); // TODO or from cache
      }
      throw new ModelNotFoundException(inheritedType, this, "Can't find model "
            + " while looking for definition of type " + type);
   }
   public DCModelBase getStorageModel(String type) {
      String inheritedType = type;
      DCModelBase model;
      // TODO cache :
      while ((model = this.getModel(inheritedType)) != null) {
         if (model.isStorage()) {
            return model;
         }
         // get inherited type i.e. first mixin :
         // TODO or rather visit the full mixin hierarchy in order ??
         if (model.getMixinNames().isEmpty()) {
            return null; // no inherited model, so storage might be in more generic types
         }
         
         inheritedType = model.getMixinNames().iterator().next(); // TODO or from cache
      }
      throw new ModelNotFoundException(inheritedType, this, "Can't find model "
            + " while looking for storage of type " + type);
   }

   /*public DCStorage getVisibleProject(String type) { // TODO or in projectService ?!
      // TODO rather from cache allAltStoragesMap
      DCStorage storage = getLocalStorage(type);
      if (storage != null) {
         return storage;
      }
      for (DCProject project : visibleProjectMap.values()) { // TODO cache allVisibleProjectMap
         storage = project.getLocalStorage(type);
         if (storage != null) {
            return storage;
         }
      }
      return null;
   }*/

   public DCProject getLocalVisibleProject(String projectName) {
      return visibleProjectMap.get(projectName);
   }

   public Collection<DCProject> getLocalVisibleProjects() {
      return visibleProjectMap.values();
   }
   
   public void addLocalVisibleProject(DCProject visibleProject) {
      this.visibleProjectMap.put(visibleProject.getName(), visibleProject);
   }


   /**
    * TODO or rather return Model & (not only) Storage
    * NB. requestContextProvider.getRequestContext() could be used to
    * answer depending on REST request i.e. GET or POST/PUT...
    * @param dataEntity
    * @return
    */
   public /*DCPointOfView*/DCModelBase getModel(DCEntity dataEntity) {
      String dataEntityModelType = dataEntity.getTypes().iterator().next(); // TODO dataEntity.getModelName() ?
      DCUseCasePointOfView pointOfView = useCasePointOfViewMap.get(dataEntityModelType);
      if (pointOfView != null) {
         return pointOfView.getModel(dataEntity);
      }
      return getModel(dataEntityModelType);
   }

   
   
   ///////////////////////////////////////
   // DCModelService impl :

   public Collection<DCModelBase> getModels() { // TODO rather "getSampleModels to persist"
      // TODO LATER cache BUT AFTER obsolete on update
      /*if (allAltModelMap != null) {
         return allAltModelMap.values();
      }*/
      LinkedHashMap<String, DCModelBase> allAltModelMap = new LinkedHashMap<String, DCModelBase>();
      for (DCModelBase model : modelMap.values()) {
         allAltModelMap.put(model.getName(),model); // TODO altName
      }
      for (DCProject project : visibleDefProjectMap.values()) { // TODO cache allVisibleProjectMap
         for (DCModelBase model : project.getModels()) {
            if (allAltModelMap.containsKey(model.getName())) {
               break; // already overriden / alt'd
            }
            model = getModel(model.getName()); // does default alt'ing
            allAltModelMap.put(model.getName(), model); // TODO altName
         }
      }
      for (DCProject project : visibleDataProjectMap.values()) { // TODO cache allVisibleProjectMap
         for (DCModelBase model : project.getModels()) {
            if (allAltModelMap.containsKey(model.getName())) {
               break; // already overriden / alt'd
            }
            model = getModel(model.getName()); // does default alt'ing
            allAltModelMap.put(model.getName(), model); // TODO altName
         }
      }
      for (DCProject project : visibleStorageProjectMap.values()) { // TODO cache allVisibleProjectMap
         for (DCModelBase model : project.getModels()) {
            if (allAltModelMap.containsKey(model.getName())) {
               break; // already overriden / alt'd
            }
            model = getModel(model.getName()); // does default alt'ing
            allAltModelMap.put(model.getName(), model); // TODO altName
         }
      }
      /*for (DCProject project : visibleProjectMap.values()) { // TODO cache allVisibleProjectMap
         model = project.getModel(type);
         if (model != null) {
            return model;
         }
      }*/
      //this.allAltModelMap = allAltModelMap; // // TODO LATER cache BUT AFTER obsolete on update
      return allAltModelMap.values();
   }


   ///@Override
   public Map<String, ? extends DCPointOfView> getPointOfViewMap() {
      return useCasePointOfViewMap;
   }
   @Override
   public Collection<? extends DCPointOfView> getPointOfViews() {
      return useCasePointOfViewMap.values(); // TODO TODO and ((local)) visible projects ?? 
   }
   
   public void addUseCasePointOfView(DCUseCasePointOfView pointOfView) {
      useCasePointOfViewMap.put(pointOfView.getName(), pointOfView);
   }

   public DCUseCasePointOfView getUseCasePointOfView(String name) {
      return useCasePointOfViewMap.get(name);
   }

   public Collection<DCUseCasePointOfView> getUseCasePointOfViews() {
      return useCasePointOfViewMap.values();
   }
   
   /** TODO ?? ONLY TO CREATE DERIVED MODELS ex. Contribution, TODO LATER rather change their name ?!? */
  /* public void addUseCasePointOfView(DCModelBase dcModel, String name) {
      altModelMap.put(name, dcModel);
   }*/

   public void removePointOfView(String name) {
      useCasePointOfViewMap.remove(name);
   }
   
   
   
   ///////////////////////////////////////
   // COPIED FROM DataAdminServiceImpl
   // admin / update methods
   // TODO TODO also update persistence if not done in the other way i.e. at startup
   // TODO TODO global write lock on those, & also when updating from Resource persistence

   /**
    * also adds to mixin TODO is it OK ?
    * Checks whether its used mixin models are already known (TODO in same version)
    * @param dcModel
    */
   @Override
   public void addLocalModel(DCModelBase dcModel) {
      List<DCModelBase> unknownMixins = dcModel.getMixinMap().values().stream().filter(
            mixin -> { DCModelBase existingMixin = getModel(mixin.getName());
            return existingMixin == null || existingMixin.getVersion() != mixin.getVersion(); })
            .collect(Collectors.toList());
      if (unknownMixins.size() != 0) { // TDOO check version
         throw new ModelException(dcModel, this, "Unable to add model "
               + " to project " + getName() + " with visible projects "
               + visibleProjectMap + ", this model has unknown mixins : "
               + unknownMixins.stream().map(mixin -> mixin.getName()).collect(Collectors.toList())); // TODO business exception
      }
      super.addLocalModel(dcModel);
      ///addMixin(dcModel); // TODO all alt mixin cache ??
   }
   
   /** ONLY TO CREATE DERIVED MODELS ex. Contribution, TODO LATER rather change their name ?!? */
   public void addModel(DCModelBase dcModel, String name) {
      modelMap.put(name, dcModel);
   }

   /*
    * TODO LATER also check version
    * @param name
    */
   /*public void removeModel(String name) {
      altModelMap.remove(name);
      ///removeMixin(name); // TODO all alt mixin cache ??
   }*/
   
   /*public Map<String, DCModelBase> getModelMap() {
      return this.altModelMap;
   }

   public void setModelMap(LinkedHashMap<String, DCModelBase> modelMap) {
      this.altModelMap = modelMap;
   }

   public void addMixin(DCModelBase mixin) { // TODO or addAsMixin i.e. alt'ing without storage ??
      mixinMap.put(mixin.getName(), mixin);
   }
   
   public void removeMixin(String name) {
      mixinMap.remove(name);
   }
   
   public Map<String, DCModelBase> getMixinMap() {
      return this.mixinMap;
   }

   public void setMixinMap(Map<String, DCModelBase> mixinMap) {
      this.mixinMap = mixinMap;
   }*/

   public String getDocumentation() {
      return documentation;
   }

   public void setDocumentation(String documentation) {
      this.documentation = documentation;
   }
   
}
