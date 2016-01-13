package org.oasis.datacore.core.meta.pov;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.oasis.datacore.core.entity.model.DCEntity;
import org.oasis.datacore.core.meta.ModelException;
import org.oasis.datacore.core.meta.ModelNotFoundException;
import org.oasis.datacore.core.meta.SimpleUriService;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCSecurity;

public class DCProject extends DCPointOfViewBase {
   
   public static final String OASIS_MAIN = "oasis.main";
   public static final String OASIS_META = "oasis.meta"; // seen by every project in xxxxxxxxxxxxxxxxxxxxxxxxxx
   public static final String OASIS_SAMPLE = "oasis.sample"; // tech samples (also used in unit tests in datacore-test)
   public static final String OASIS_SANBOX = "oasis.sandbox"; // TODO sees (in readonly) meta, main, sample
   
   /** for frozen model names & allowed model prefixes */
   public static final String MODEL_NAMES_WILDCARD = "*";

   /* TODO organization.project ex. oasis.main */
   /*
   private String name;

   private LinkedHashMap<String,DCModelBase> altModelMap = new LinkedHashMap<String, DCModelBase>();
   ///private LinkedHashMap<String,List<DCStorage>> altStoragesMap = new LinkedHashMap<String, List<DCStorage>>();
   ///private LinkedHashMap<String,DCStorage> altStorageMap = new LinkedHashMap<String, DCStorage>();
   */
   private String documentation;
   
   private LinkedHashMap<String,DCUseCasePointOfView> useCasePointOfViewMap = new LinkedHashMap<String, DCUseCasePointOfView>();
   
   /** local visible projects ; LATER also reuse visible ones ? */
   private LinkedHashMap<String,DCProject> visibleProjectMap = new LinkedHashMap<String, DCProject>(); // TODO merge in visibleStorageProjectMap
   private LinkedHashSet<String> forkedUris = new LinkedHashSet<String>();
   private LinkedHashSet<String> frozenModelNames = new LinkedHashSet<String>();
   private LinkedHashSet<String> allowedModelPrefixes = new LinkedHashSet<String>();
   
   /** LATER can see outside model (unless alt model) but not data (by alt'ing model anonymously as not storage),
    * for this the local alt (storage) model must be created, optionally with some sample copied or rather imported data */
   private LinkedHashMap<String,DCProject> visibleDefProjectMap = new LinkedHashMap<String, DCProject>();
   /** OPT can see outside data (get & refer to it IF SECURITY OK) (unless alt storage) but not change (POST) (by alt'ing its DCSecurity to readonly),
    * for this the local alt (storage only) model must be created, probably with some sample copied data, with same security + data tester ;
    * modelization is not expected to change */
   private LinkedHashMap<String,DCProject> visibleDataProjectMap = new LinkedHashMap<String, DCProject>(); // TODO OPT
   /** (TODO or not ?) can change outside data (POST) (unless alt'd as not storage), i.e. allows oasis.main to redirect to others
    * AND DELEGATE SECURITY i.e. no fork at all */
   private LinkedHashMap<String,DCProject> visibleStorageProjectMap = new LinkedHashMap<String, DCProject>(); // TODO or not ?

   /** TODO ordered cache, includes itself
    * to be invalidated each type (even indirectly) visible projects change
    * (including when backward compatible).
    * TODO not thread-safe, handle it the REST way
    * TODO minorVersion ??? */
   private LinkedHashMap<String,DCProject> allVisibleProjectMap = null;
   private LinkedHashMap<String,DCModelBase> allAltModelMap = null;

   /** to be checked firsthand if any WHATEVER THE RESOURCE (null dataEntity)
    * when checking rights in a model ;
    * TODO resourceReaders should be in sync with project rights : readers OR OVERRIDEN
    * (but project resource writers then rather with owners) ;
    * TODO null resourceReaders means no check (and empty none ?) */
   private DCSecurity securityConstraints = null;
   /** used when !modelLevelSecurityEnabled or no security found in model hierarchy ;
    * null means global defaults (depends on devmode) */
   private DCSecurity securityDefaults = null;
   /** TODO LATER should rather be isRESOURCEModelLevelSecurityEnabled
    * allows different security levels in models/mixins (even in same resource),
    * with permissions that are defined at Resource-level allowing the most rights
    * (ex. owner on orgpr:PrivateOrganization_0 fields), and other permissions being all.
    * Defaults to false. */
   private boolean modelLevelSecurityEnabled = false;
   /** Use model security instead of project security as default security ;
    * if disabled, model security has not to be defined and is replaced by project securityDefaults
    * (even if modelLevelSecurityEnabled !) and is the same across all models of this project.
    * Defaults to false. */
   private boolean useModelSecurity = false;
   /** allows generic conf & constraints on the project to visible project relation
    * without having to manage & store a visibleProjectRelation object ; 
    * to be checked firsthand if any WHATEVER THE RESOURCE (null dataEntity)
    * when checking rights in a model from another current project ;
    * TODO resourceReaders should be in sync with project rights : readers OR OVERRIDEN
    * (but project resource writers then rather with owners) ;
    * TODO null resourceReaders means no check (and empty none ?),
    * !isAuthentifiedWritable no write from another project */
   private DCSecurity visibleSecurityConstraints = null;
   // NB. no forkStorageConstraint else won't know to store a in b or c if c sees b which sees a... 

   /** for unmarshalling only */
   public DCProject() {
      super();
   }

   /** helper for tests */
   public DCProject(String name) {
      super(name);
   }
   public DCProject(String unversionedName, long majorVersion) {
      super(unversionedName, majorVersion);
   }

   /**
    * 
    * @param type
    * @return null if its URI forked, else getNonLocalModel(type)
    */
   public DCModelBase getModel(String type) { // TODO or in projectService ?!
      // TODO rather from cache allAltModelMap
      DCModelBase model = super.getModel(type); // existing local alt model
      if (model != null) {
         return model;
      }
      DCModelBase nonLocalModel = getNonLocalModel(type);
      if (forkedUris.contains(SimpleUriService.buildModelUri(type))) {
         return null;
      }
      return nonLocalModel;
   }
   
   /**
    * i.e. in this project's visible projects only
    * @param type
    * @return
    */
   public DCModelBase getNonLocalModel(String type) { // TODO or in projectService ?!
      DCModelBase model;
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
      for (DCProject project : visibleProjectMap.values()) { // TODO cache allVisibleProjectMap
         model = project.getModel(type);
         if (model != null) {
            return model;
         }
      }
      return null;
   }

   public DCModelBase getLocalModel(String type) {
      // NB. other impls i.e. routing strategies are allowed by DCUseCasePointOfView
      return modelMap.get(type);
   }

   /**
    * Visits models in their order of inheritance,
    * using getModel(parentName) (allows definition forking by forking only definition models)
    * @param model
    * @return null if abstract model
    */
   // can't return null (explodes)
   public DCModelBase getDefinitionModel(DCModelBase model) {
      // TODO cache :
      if (model == null) {
         return null;
      }
      DCModelBase inheritanceParentModel = model;
      String inheritedType;
      do {
         if (inheritanceParentModel.isDefinition()) {
            return inheritanceParentModel;
         }
         // get inherited type i.e. first mixin :
         // TODO or rather visit the full mixin hierarchy in order ??
         if (inheritanceParentModel.getMixinNames().isEmpty()) {
            throw new ModelNotFoundException(inheritanceParentModel, this, "Unable to find any definition model");
         }

         // get its first ("inheritance parent") mixin
         inheritedType = inheritanceParentModel.getMixinNames().iterator().next(); // TODO or from cache
         inheritanceParentModel = this.getModel(inheritedType);
      } while (inheritanceParentModel != null);
      throw new ModelNotFoundException(inheritedType, this, "Can't find definition model "
            + "while looking for definition of type " + model.getName()
            + " in project " + this.getName());
   }
   /**
    * Visits models in their order of inheritance (depth-first),
    * using getModel(parentName) (allows data forking by forking only storage models)
    * @param model
    * @return null if abstract model
    */
   public DCModelBase getStorageModel(DCModelBase model) {
      // TODO cache :
      if (model == null) {
         return null;
      }
      if (model.isStorage()) {
         return model;
      }

      for (String parentMixinName : model.getMixinNames()) {
         DCModelBase parentMixinStorageModel = this.getStorageModel(this.getModel(parentMixinName));
         if (parentMixinStorageModel != null) {
            return parentMixinStorageModel;
         }
      }
      /*throw new ModelNotFoundException(inheritedType, this, "Can't find storage model "
            + "while looking for definition of type " + model.getName()
            + " in project " + this.getName());*/
      return null; // abstract model
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
   
   /** to allow single-operation changes (especially remove ex. on PUT / replace) */
   public void setLocalVisibleProjectMap(LinkedHashMap<String, DCProject> visibleProjectMap) {
      this.visibleProjectMap = visibleProjectMap;
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
         allAltModelMap.put(model.getName(),model); // TODO altName ??
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
      for (DCProject project : visibleProjectMap.values()) { // TODO cache allVisibleProjectMap
         for (DCModelBase model : project.getModels()) {
            if (allAltModelMap.containsKey(model.getName())) {
               break; // already overriden / alt'd
            }
            ///model = getModel(model.getName()); // does default alt'ing NOOO not here
            allAltModelMap.put(model.getName(), model); // TODO altName
         }
      }
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
   /*public void addUseCasePointOfView(DCModelBase dcModel, String name) {
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
    * @throws RuntimeException if unknown (non local if has same name mixin) or bad version mixin
    */
   @Override
   public void addLocalModel(DCModelBase dcModel) throws RuntimeException {
      List<DCModelBase> unknownMixins = dcModel.getMixinMap().values().stream().filter(mixin -> {
            String type = mixin.getName();
            DCModelBase existingMixin = (dcModel.getName().equals(type)) ?
                  getNonLocalModel(type) : getModel(type);
            return existingMixin == null;
      }).collect(Collectors.toList());
      List<DCModelBase> badVersionMixins = dcModel.getMixinMap().values().stream().filter(mixin -> {
         String type = mixin.getName();
         DCModelBase existingMixin = (dcModel.getName().equals(type)) ?
               getNonLocalModel(type) : getModel(type);
            return existingMixin != null && existingMixin.getVersion() != mixin.getVersion();
      }).collect(Collectors.toList());
      if (unknownMixins.size() != 0) {
         throw new ModelException(dcModel, this, "Unable to add model to project "
               + getName() + " with visible projects " + visibleProjectMap
               + ", this model has unknown mixins : "
               + unknownMixins.stream().map(mixin -> mixin.getName()).collect(Collectors.toList())); // TODO business exception
      }
      if (badVersionMixins.size() != 0) {
         throw new ModelException(dcModel, this, "Unable to add model to project "
               + getName() + " with visible projects " + visibleProjectMap
               + ", this model has obsolete version mixins : "
               + badVersionMixins.stream().map(mixin -> mixin.getName()).collect(Collectors.toList())); // TODO business exception
      }
      super.addLocalModel(dcModel);
      ///addMixin(dcModel); // TODO all alt mixin cache ??
   }
   
   /** ONLY TO CREATE DERIVED MODELS ex. Contribution, TODO LATER rather change their name ?!? */
   public void addModel(DCModelBase dcModel, String name) {
      modelMap.put(name, dcModel);
   }

   public LinkedHashSet<String> getForkedUris() {
      return forkedUris;
   }

   public void setForkedUris(LinkedHashSet<String> forkedUris) {
      this.forkedUris = forkedUris;
   }

   public LinkedHashSet<String> getFrozenModelNames() {
      return frozenModelNames;
   }

   public void setFrozenModelNames(LinkedHashSet<String> frozenModelNames) {
      this.frozenModelNames = frozenModelNames;
   }

   public LinkedHashSet<String> getAllowedModelPrefixes() {
      return allowedModelPrefixes;
   }

   public void setAllowedModelPrefixes(LinkedHashSet<String> allowedModelPrefixes) {
      this.allowedModelPrefixes = allowedModelPrefixes;
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
   
   
   public DCSecurity getSecurityConstraints() {
      return securityConstraints;
   }

   public void setSecurityConstraints(DCSecurity securityConstraints) {
      this.securityConstraints = securityConstraints;
   }

   public DCSecurity getSecurityDefaults() {
      return securityDefaults;
   }

   public void setSecurityDefaults(DCSecurity securityDefaults) {
      this.securityDefaults = securityDefaults;
   }

   public boolean isModelLevelSecurityEnabled() {
      return modelLevelSecurityEnabled;
   }

   public void setModelLevelSecurityEnabled(boolean modelLevelSecurityEnabled) {
      this.modelLevelSecurityEnabled = modelLevelSecurityEnabled;
   }

   public boolean isUseModelSecurity() {
      return useModelSecurity;
   }

   public void setUseModelSecurity(boolean useModelSecurity) {
      this.useModelSecurity = useModelSecurity;
   }

   public DCSecurity getVisibleSecurityConstraints() {
      return visibleSecurityConstraints;
   }

   public void setVisibleSecurityConstraints(DCSecurity visibleSecurityConstraints) {
      this.visibleSecurityConstraints = visibleSecurityConstraints;
   }
   
   
   @Override
   public boolean equals(Object o) {
      if (!(o instanceof DCProject)) {
         return false;
      }
      DCProject p = ((DCProject) o);
      return this.getName().equals(p.getName());
   }

   @Override
   public int hashCode() {
      return this.getName().hashCode();
   }
   

   public String toString() {
      return "project " + this.getName()
            + "; " + this.getLocalVisibleProjects().stream()
            .map(p -> p.getName()).collect(Collectors.toList())
            + "\n   " + this.getLocalModels().stream()
            .map(m -> m.getName()).collect(Collectors.toList())
            ;
   }
   
}
