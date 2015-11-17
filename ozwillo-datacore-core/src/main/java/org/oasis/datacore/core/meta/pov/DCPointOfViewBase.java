package org.oasis.datacore.core.meta.pov;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.oasis.datacore.core.meta.model.DCModelBase;

import com.google.common.collect.ImmutableList;

public abstract class DCPointOfViewBase implements DCPointOfView {

   private static final Pattern versionedNamePattern = Pattern.compile("(.+)_([0-9]+)");
   
   private String name;
   /** optimistic locking (implicitly minor) version */
   private long version = 0;
   /** end of name ex. geo_1, none of < 0 */
   private long majorVersion = -1;
   private String unversionedName;
   private List<String> parentPointOfViewNames;
   private String parentAbsoluteName;
   /** cache */
   private String absoluteName = null;
   ///protected LinkedHashMap<String,DCPointOfView> pointOfViewMap = new LinkedHashMap<String,DCPointOfView>();
   protected LinkedHashMap<String,DCModelBase> modelMap = new LinkedHashMap<String,DCModelBase>();

   /** for unmarshalling only */
   public DCPointOfViewBase() {
      
   }
   public DCPointOfViewBase(String name, String ... parentPointOfViewNames) {
      this.name = name;
      Matcher versionedNameMatcher = versionedNamePattern.matcher(name); // ex. geo_0
      if (!versionedNameMatcher.find()) {
         // not a versioned name
         this.majorVersion = -1;
         this.setUnversionedName(name);
      } else {
         this.setUnversionedName(versionedNameMatcher.group(1)); // NB. group 0 is the whole matched string
         String majorVersionString = versionedNameMatcher.group(2);
         this.majorVersion = Long.parseLong(majorVersionString); // TODO handle parsing pb
      }
      this.parentPointOfViewNames = ImmutableList.copyOf(parentPointOfViewNames);
      buildAbsoluteNames();
   }
   public DCPointOfViewBase(String unversionedName, long majorVersion, String ... parentPointOfViewNames) {
      this.name = unversionedName;
      if (majorVersion >= 0) {
         this.majorVersion = majorVersion;
         this.name += '_' + majorVersion;
      }
      this.setUnversionedName(unversionedName);
      this.parentPointOfViewNames = ImmutableList.copyOf(parentPointOfViewNames);
      buildAbsoluteNames();
   }
   
   @Override
   public boolean equals(Object o) {
      if (!(o instanceof DCPointOfView)) {
         return false;
      }
      DCPointOfView pov = (DCPointOfView) o;
      if (this.name == null) {
         return pov.getName() == null; // TODO or error ?
      }
      return this.name.equals(pov.getName());
   }
   @Override
   public int hashCode() {
      return this.name.hashCode();
   }

   public long getVersion() {
      return version;
   }
   public void setVersion(long version) {
      this.version = version;
   }
   public long getMajorVersion() {
      return majorVersion;
   }
   public void setMajorVersion(long majorVersion) {
      this.majorVersion = majorVersion;
   }
   public String getUnversionedName() {
      return unversionedName;
   }
   public void setUnversionedName(String unversionedName) {
      this.unversionedName = unversionedName;
   }
   
   @Override
   public String getParentAbsoluteName() {
      return parentAbsoluteName;
   }
   
   @Override
   public String getName() {
      return name;
   }
   
   @Override
   public String getAbsoluteName() {
      return absoluteName;
   }
   

   /** TO BE OVERRIDEN */
   @Override
   public DCModelBase getModel(String type) {
      return modelMap.get(type);
   }

   ///@Override
   public abstract Map<String, ? extends DCPointOfView> getPointOfViewMap();

   /*@Override
   public Map<String, DCModelBase> getLocalModelMap() {
      return modelMap;
   }*/

   @Override
   public Collection<DCModelBase> getLocalModels() {
      return modelMap.values();
   }

   @Override
   public DCModelBase getLocalModel(String type) {
      return modelMap.get(type);
   }

   @Override
   public void addLocalModel(DCModelBase model) {
      modelMap.put(model.getName(), model);
   }

   // TODO LATER check version
   @Override
   public void removeLocalModel(String type) {
      modelMap.remove(type);
   }

   public void setName(String name) {
      this.name = name;
   }
   
   public List<String> getParentPointOfViewNames() {
      return parentPointOfViewNames;
   }
   public void setParentPointOfViewNames(List<String> parentPointOfViewNames) {
      this.parentPointOfViewNames = parentPointOfViewNames;
      buildAbsoluteNames();
   }
   
   protected void buildAbsoluteNames() {
      if (name == null || name.length() == 0) {
         throw new RuntimeException("Empty name when creating new project " + name);
      }
      if (parentPointOfViewNames == null || parentPointOfViewNames.isEmpty()) {
         this.parentAbsoluteName = "";
         this.absoluteName = name;
         return;
      }
      this.parentAbsoluteName = this.parentPointOfViewNames.stream().collect(Collectors.joining("."));
      this.absoluteName = parentAbsoluteName + "." + name;
   }
   
}
