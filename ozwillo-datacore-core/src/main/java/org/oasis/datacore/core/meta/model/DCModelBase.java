package org.oasis.datacore.core.meta.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.oasis.datacore.core.meta.pov.DCPointOfView;

import com.google.common.collect.ImmutableList;


/**
 * TODO LATER inherits DCNamedBase .getName() common with DCField for fieldOrMixins ?
 * 
 * @author mdutoo
 *
 */
public abstract class DCModelBase {

   private String name;
   private String projectName;
   private List<String> pointOfViewNames;
   private String absoluteName; // i.e. collectionName
   private String pointOfViewAbsoluteName;

   // ID Field names: what they should be
   private List<String> idFieldNames;
   // ID Field names: is the naming scheme enforced by the data core? (default: no)
   private boolean enforceIdFieldNames = false;

   /** to be incremented each time there is a backward incompatible
    * (ex. field change, rather than new field) */
   private long majorVersion = 0;
   /** regular optimistic locking version (though NOT an explicit minor version of the major version) */
   private long version = -1;
   // TODO draft state & publish / life cycle
   
   /** can its modelization change from its inherited (first) mixin ? */
   private boolean isDefinition = true;
   /** Does it define a storage (collection & indexes) or not ? */
   private boolean isStorage = true; // default ? TODO rather in DCModel(Storage) ?? or getStorageModel() ex. for subResource ???
   /** Can Resources of this exact model type be created ? true = old DCModel */
   private boolean isInstanciable = true;
   /** can it also store resources of data-only forks ?
    * (then adds DCEntity._pv list of projects where it is visible, first being its actual project)
    * default is usually false for forked models, though true for the original oasis.meta.dcmi:mixin_0 */
   private boolean isMultiProjectStorage = false;
   
   /** doc TODO what (business), how (impl conf & instanciation), samples... */
   private String documentation; // TODO move in another collection for performance
   private LinkedHashMap<String,DCField> fieldMap = new LinkedHashMap<>();
   /** to maintain order ; persisted (json / mongo) as list */
   private LinkedHashSet<String> fieldAndMixinNames = new LinkedHashSet<>();
   /** NB. (concrete) models allowed */
   private LinkedHashMap<String,DCModelBase> mixinMap = new LinkedHashMap<>();
   // TODO allFieldNames, allFieldMap cached (?! beware versioning !!)
   /** Resource type event listeners (that use this DCModelBase's name as topic) */
   private List<Object> listeners = new ArrayList<>(); // TODO DC(Model/ResourceType)EventListener

   /** same as globalFieldMap */
   private LinkedHashMap<String,DCModelBase> globalMixinMap = null;
   /** cache, to be invalidated each type the model (or its mixins) change
    * (including when backward compatible).
    * TODO not thread-safe, handle it the REST way
    * TODO minorVersion ??? */
   private LinkedHashMap<String,DCField> globalFieldMap = null;
   
   // instanciable :
   private boolean isHistorizable;
   private boolean isContributable;
   private DCSecurity security = null; //  = new DCSecurity(); // TODO TODO TODO LATER or also storage i.e. inherited from polymorphism root ?
   
   // storage :
   /** Limits the specified number of documents to scan specified in DCField.queryLimit
    * when fulfilling a query on this Model's collection
    * http://docs.mongodb.org/manual/reference/operator/meta/maxScan/ */
   private int maxScan = 0;
   
   // features :
   /** Country / language specific : TODO LATER rather optional / candidate mixin (FR/IT...)CountryLanguageSpecific */
   private String countryLanguage = null;


   /** for unmarshalling only */
   public DCModelBase() {
      
   }
   /** requires project.addLocalModel(this model) to call setPointOfView() */
   public DCModelBase(String name) {
      this.name = name;
   }
   /** pointOfView won't be changed (but checked) when this model is added to its project */
   public DCModelBase(String name, DCPointOfView pointOfView) {
      this(name, pointOfView.getAbsoluteName());
   }
   /** pointOfView won't be changed (but checked) when this model is added to its project */
   public DCModelBase(String name, String ... pointOfViewNames) {
      this.name = name;
      this.pointOfViewNames = ImmutableList.copyOf(pointOfViewNames);
      buildAbsoluteNames();
   }
   /** if project not known  to be called when added to project */
   public void setPointOfView(DCPointOfView pointOfView) {
      this.setPointOfView(pointOfView.getName());
   }
   /** to be called when added to project */
   public void setPointOfView(String ... pointOfViewNames) {
      this.pointOfViewNames = ImmutableList.copyOf(pointOfViewNames);
      buildAbsoluteNames();
   }
   
   public String getDocumentation() {
      return documentation;
   }
   
   /** TODO rename getLocalField(), use rather getGlobalField (local fields only here) */
   public DCField getField(String name) {
      return fieldMap.get(name);
   }

   /** computed cache ; TODO LATER trigger reset, handle Model version (upgrade) */
   public Map<String,DCModelBase> getGlobalMixinMap() {
      checkGlobalCaches();
      return globalMixinMap;
   }
   /** computed cache ; TODO LATER trigger reset, handle Model version (upgrade) */
   public Set<String> getGlobalMixinNames() {
      return getGlobalMixinMap().keySet();
   }
   /** computed cache ; TODO LATER trigger reset, handle Model version (upgrade) */
   public Collection<DCModelBase> getGlobalMixins() {
      return getGlobalMixinMap().values();
   }
   /** computed cache ; TODO LATER trigger reset, handle Model version (upgrade) */
   public Collection<DCField> getGlobalFields() {
      return getGlobalFieldMap().values();
   }
   /** computed cache ; TODO LATER trigger reset, handle Model version (upgrade) */
   public Map<String, DCField> getGlobalFieldMap() {
      checkGlobalCaches();
      return globalFieldMap;
   }

   /** works on computed cache ; TODO LATER trigger reset, handle Model version (upgrade) */
   public boolean hasMixin(String name) {
      return this.getGlobalMixinNames().contains(name);
   }
   /** works on computed cache ; TODO LATER trigger reset, handle Model version (upgrade) */
   public DCField getGlobalField(String name) {
      return getGlobalFieldMap().get(name);
   }

   public void resetGlobalCaches() {
      this.globalMixinMap = null;
      this.globalFieldMap = null;
   }
   /**
    * Builds the map of all fields including of mixins, in the order in which they were added
    * & same for mixins
    */
   private void checkGlobalCaches() {
      if (this.globalFieldMap == null) {
         LinkedHashMap<String, DCField> newGlobalFieldMap = new LinkedHashMap<String,DCField>();
         LinkedHashMap<String, DCModelBase> newGlobalMixinMap = new LinkedHashMap<String,DCModelBase>();
         fillGlobalFieldMap(newGlobalMixinMap, newGlobalFieldMap);
         // NB. not required to be synchronized, because there's no problem if it's
         // done twice at the same time
         this.globalMixinMap = newGlobalMixinMap;
         this.globalFieldMap = newGlobalFieldMap;
      }
   }
   private void fillGlobalFieldMap(Map<String,DCModelBase> globalMixinMap,
         Map<String,DCField> globalFieldMap) {
      for (String fieldOrMixinName : this.fieldAndMixinNames) {
         DCField field = this.getField(fieldOrMixinName);
         if (field != null) {
            globalFieldMap.put(field.getName(), field);
         } else { // if (fieldOrMixin instanceof DCModelBase) {
            DCModelBase mixin = this.getMixinMap().get(fieldOrMixinName);
            globalMixinMap.put(mixin.getName(), mixin);
            mixin.fillGlobalFieldMap(globalMixinMap, globalFieldMap);
         }
      }
   }
   
   public String getName() {
      return name;
   }
   public long getMajorVersion() {
      return majorVersion;
   }
   public long getVersion() {
      return version;
   }

   /** TODO rename getLocalFieldNames(), TODO make it unmodifiable */
   public Set<String> getFieldNames() {
      return fieldMap.keySet();
   }
   /** TODO rename getLocalFields() */
   public Collection<DCField> getFields() {
      return fieldMap.values();
   }
   /** TODO rename getLocalFieldMap(), TODO make it unmodifiable */
   public Map<String, DCField> getFieldMap() {
      return fieldMap;
   }

   /** TODO rename getLocalMixinNames() */
   public Set<String> getMixinNames() {
      return mixinMap.keySet();
   }
   /** TODO rename getLocalMixins() */
   public Collection<DCModelBase> getMixins() {
      return mixinMap.values();
   }
   /** TODO rename getLocalMixinMap(), TODO make it unmodifiable */
   public Map<String,DCModelBase> getMixinMap() {
      return mixinMap;
   }
   
   /** ordered ; to be able to know the override order when storing it to Resources and back
    * NB. fields and mixins can't have the same name if following semantic rules (TODO LATER else handle it) */
   public Set<String> getFieldAndMixinNames() {
      return fieldAndMixinNames;
   }

   /** TODO make it unmodifiable */
   public List<Object> getListeners() {
      return listeners;
   }
   
   
   ///////////////////////////////////////
   // update methods

   /** helper method to build new DCModel/Mixins FOR TESTING
    * TODO or in builder instance ? */
   public DCModelBase addField(DCField field) {
      String fieldName = field.getName();
      this.fieldMap.put(fieldName , field);
      this.fieldAndMixinNames.add(field.getName()); // if already exists may change order
      this.globalFieldMap = null;
      return this;
   }

   /** helper method to build new DCModel/Mixins FOR TESTING
    * TODO or in builder instance ? */
   public DCModelBase addMixin(DCModelBase mixin) {
      this.getMixinMap().put(mixin.getName(), mixin);
      this.fieldAndMixinNames.add(mixin.getName()); // if already exists may change order
      this.globalFieldMap = null;
      return this;
   }

   public DCModelBase addMixins(DCMixin ... mixins) {
      for (DCMixin mixin : mixins) {
         addMixin(mixin);
      }
      return this;
   }

   // update methods

   /** helper method to build new DCModel/Mixins FOR TESTING
    * TODO or in builder instance ? */
   public DCModelBase addListener(Object listener) { // TODO DCResourceEventListener & set its services
      this.getListeners().add(listener);
      return this;
   }

   public String getAbsoluteName() {
      return absoluteName;
   }
   public String getPointOfViewAbsoluteName() {
      return pointOfViewAbsoluteName;
   }
   public void setName(String name) {
      this.name = name;
   }

   public List<String> getIdFieldNames() {
      return idFieldNames;
   }

   public void setIdFieldNames(List<String> idFieldNames) {
      this.idFieldNames = idFieldNames;
   }
   public void setEnforceIdFieldNames(boolean enforceIdFieldNames) {
      this.enforceIdFieldNames = enforceIdFieldNames;
   }

   public boolean isEnforceIdFieldNames() {
      return enforceIdFieldNames;
   }

   public String getProjectName() {
      return projectName;
   }
   private void buildAbsoluteNames() {
      if (name == null || name.length() == 0) {
         throw new RuntimeException("Empty name when creating new model " + name);
      }
      if (pointOfViewNames == null || pointOfViewNames.isEmpty()) {
         throw new RuntimeException("There must be at least one pointOfView level (ex. project) "
               + "when creating new model " + name);
      }
      this.projectName = this.pointOfViewNames.get(0);
      this.pointOfViewAbsoluteName = this.pointOfViewNames.stream().collect(Collectors.joining("."));
      this.absoluteName = pointOfViewAbsoluteName + "." + name;
   }

   public void setMajorVersion(long majorVersion) {
      this.majorVersion = majorVersion;
   }
   public void setVersion(long version) {
      this.version = version;
   }

   public void setDocumentation(String documentation) {
      this.documentation = documentation;
   }

   public void setFieldMap(Map<String, DCField> fieldMap) {
      this.fieldMap = new LinkedHashMap<>(fieldMap);
   }
   
   public void setMixinMap(LinkedHashMap<String, DCModelBase> mixinMap) {
      this.mixinMap = new LinkedHashMap<>(mixinMap);
   }
   
   public void setListeners(List<Object> listeners) {
      this.listeners = listeners;
   }

   
   /////////////////////////////////////////////////////////////
   // POLY :

   public boolean isDefinition() {
      return isDefinition;
   }
   public void setDefinition(boolean isDefinition) {
      this.isDefinition = isDefinition;
   }
   public boolean isStorage() {
      return isStorage;
   }
   public void setStorage(boolean isStorage) {
      this.isStorage = isStorage;
   }
   public boolean isInstanciable() {
      return isInstanciable;
   }
   public void setInstanciable(boolean isInstanciable) {
      this.isInstanciable = isInstanciable;
   }
   public boolean isMultiProjectStorage() {
      return isMultiProjectStorage;
   }
   public void setMultiProjectStorage(boolean isMultiProjectStorage) {
      this.isMultiProjectStorage = isMultiProjectStorage;
   }

   
   /////////////////////////////////////////////////////////////
   // storage :

   /**
    * NB. to look up in mongo using special characters, do ex. with colon (':') :
    * db["my:model"].find()
    * @return projectName + "." + model.getName() IF isStorage(), else RuntimeException TODO better
    */
   public String getCollectionName() {
      if (!isStorage()) {
         throw new RuntimeException("Not a storage, use rather modelService.getStorageModel()");
      }
      return this.getAbsoluteName();
   }

   public int getMaxScan() {
      return maxScan;
   }

   public void setMaxScan(int maxScan) {
      this.maxScan = maxScan;
   }

   
   /////////////////////////////////////////////////////////////
   // instanciable :

   /** TODO LATER or also storage i.e. inherited from polymorphism root ?
    * TODO i.e. or in DCModelBase (i.e. mixins) ?? on fields ?????
    * LATER projectSecurityService to mutualize there as much as possible */
   public DCSecurity getSecurity() {
      return security;
   }

   public void setSecurity(DCSecurity security) {
      this.security = security;
   }

   public boolean isHistorizable() {
      return isHistorizable;
   }

   public void setHistorizable(boolean isHistorizable) {
      this.isHistorizable = isHistorizable;
   }

   public boolean isContributable() {
      return isContributable;
   }

   public void setContributable(boolean isContributable) {
      this.isContributable = isContributable;
   }

   public String getCountryLanguage() {
      return countryLanguage;
   }
   
   public void setCountryLanguage(String countryLanguage) {
      this.countryLanguage = countryLanguage;
   }

	/**
    * TODO better (ObjectMapper ??)
	 */
	@Override
	public String toString() {
	   return "Model[" + (this.absoluteName == null ? this.name : this.absoluteName)
	         + ";m:" + this.getGlobalMixinMap().keySet()
	         + "](f:" + this.getGlobalFieldMap().keySet() + ")";
	}
}
