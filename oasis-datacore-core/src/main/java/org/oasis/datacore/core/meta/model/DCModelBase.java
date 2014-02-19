package org.oasis.datacore.core.meta.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class DCModelBase {

   private String name;
   /** to be incremented each time there is a backward incompatible
    * (ex. field change, rather than new field) */
   private String majorVersion;
   /** regular optimistic locking version */
   private String version;
   /** TODO draft state & publish / life cycle */
   private String documentation; // TODO move in another collection for performance
   private Map<String,DCField> fieldMap = new HashMap<String,DCField>();
   //private List<String> fieldNames = new ArrayList<String>(); // to maintain order ; easiest to persist (json / mongo) than ordered map
   private List<Object> fieldAndMixins = new ArrayList<Object>(); // TODO TODO only names ??? ; to maintain order ; easiest to persist (json / mongo) than ordered map
   private List<DCModelBase> mixins = new ArrayList<DCModelBase>(); // allowing Models ; TODO or DCMixin ??
   // TODO allFieldNames, allFieldMap cached (?! beware versioning !!)
   /** Resource type event listeners (that use this DCModelBase's name as topic) */
   private List<Object> listeners = new ArrayList<Object>(); // TODO DC(Model/ResourceType)EventListener

   /** same as globalFieldMap */
   private Map<String,DCModelBase> globalMixinMap = null;
   private Set<String> globalMixinNameSet = null;
   /** cache, to be invalidated each type the model (or its mixins) change
    * (including when backward compatible).
    * TODO not thread-safe, handle it the REST way
    * TODO minorVersion ??? */
   private Map<String,DCField> globalFieldMap = null;
   
   private boolean isHistorizable;
   private boolean isContributable;
   
   /** for unmarshalling only */
   public DCModelBase() {
      
   }
   public DCModelBase(String name) {
      this.name = name;
   }

   public String getDocumentation() {
      return documentation;
   }
   
   public DCField getField(String name) {
      return fieldMap.get(name);
   }

   /** computed cache ; TODO LATER trigger reset, handle Model version (upgrade) */
   public Map<String,DCModelBase> getGlobalMixinMap() {
      checkGlobalCaches();
      return globalMixinMap;
   }
   /** computed cache ; TODO LATER trigger reset, handle Model version (upgrade) */
   public Set<String> getGlobalMixinNameSet() {
      checkGlobalCaches();
      return globalMixinNameSet;
   }
   /** computed cache ; TODO LATER trigger reset, handle Model version (upgrade) */
   public Map<String, DCField> getGlobalFieldMap() {
      checkGlobalCaches();
      return globalFieldMap;
   }

   /** works on computed cache ; TODO LATER trigger reset, handle Model version (upgrade) */
   public boolean hasMixin(String name) {
      return this.getGlobalMixinNameSet().contains(name);
   }
   /** works on computed cache ; TODO LATER trigger reset, handle Model version (upgrade) */
   public DCField getGlobalField(String name) {
      return getGlobalFieldMap().get(name);
   }

   public void resetGlobalCaches() {
      this.globalMixinMap = null;
      this.globalMixinNameSet = null;
      this.globalFieldMap = null;
   }
   /**
    * Builds the map of all fields including of mixins, in the order in which they were added
    * & same for mixins
    */
   private void checkGlobalCaches() {
      if (this.globalFieldMap == null) {
         HashMap<String, DCField> newGlobalFieldMap = new HashMap<String,DCField>();
         HashMap<String, DCModelBase> newGlobalMixinMap = new HashMap<String,DCModelBase>();
         Set<String> newGlobalMixinNameSet = new HashSet<String>();
         fillGlobalFieldMap(newGlobalMixinMap, newGlobalMixinNameSet, newGlobalFieldMap);
         // NB. not required to be synchronized, because there's no problem if it's
         // done twice at the same time
         this.globalMixinMap = newGlobalMixinMap;
         this.globalMixinNameSet = newGlobalMixinNameSet;
         this.globalFieldMap = newGlobalFieldMap;
      }
   }
   private void fillGlobalFieldMap(Map<String,DCModelBase> globalMixinMap, Set<String> globalMixinNameSet,
         Map<String,DCField> globalFieldMap) {
      for (Object fieldOrMixin : this.fieldAndMixins) {
         if (fieldOrMixin instanceof DCField) {
            DCField field = (DCField) fieldOrMixin;
            globalFieldMap.put(field.getName(), field);
         } else { // if (fieldOrMixin instanceof DCModelBase) {
            DCModelBase mixin = (DCModelBase) fieldOrMixin;
            globalMixinMap.put(mixin.getName(), mixin);
            globalMixinNameSet.add(mixin.getName());
            mixin.fillGlobalFieldMap(globalMixinMap, globalMixinNameSet, globalFieldMap);
         }
      }
   }
   
   public String getName() {
      return name;
   }

   /** TODO make it unmodifiable */
   public Map<String, DCField> getFieldMap() {
      return fieldMap;
   }

   /** TODO make it unmodifiable */
   /*public List<String> getFieldNames() {
      return fieldNames;
   }*/

   /** TODO make it unmodifiable */
   public List<DCModelBase> getMixins() {
      return mixins;
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
      this.fieldAndMixins.add(field);
      this.globalFieldMap = null;
      return this;
   }

   /** helper method to build new DCModel/Mixins FOR TESTING
    * TODO or in builder instance ? */
   public DCModelBase addMixin(DCModelBase mixin) {
      this.getMixins().add(mixin);
      this.fieldAndMixins.add(mixin);
      this.globalFieldMap = null;
      return this;
   }
   // update methods

   /** helper method to build new DCModel/Mixins FOR TESTING
    * TODO or in builder instance ? */
   public DCModelBase addListener(Object listener) { // TODO DCResourceEventListener & set its services
      // TODO clone if not for this type
      /*String resourceType = listener.getResourceType();
      if (resourceType == null) {
         listener.setTopic(resourceType);
      } else if (!resourceType.equals(name)) {
         listener = listener.clone(resourceType);
         listener.setTopic(resourceType);
      }
      listener.init();*/
      this.getListeners().add(listener);
      return this;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setDocumentation(String documentation) {
      this.documentation = documentation;
   }

   public void setFieldMap(Map<String, DCField> fieldMap) {
      this.fieldMap = fieldMap;
   }

   /*public void setFieldNames(List<String> fieldNames) {
      this.fieldNames = fieldNames;
   }*/
   
   public void setMixins(List<DCModelBase> mixins) {
      this.mixins = mixins;
   }
   
   public void setListeners(List<Object> listeners) {
      this.listeners = listeners;
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
   
}
