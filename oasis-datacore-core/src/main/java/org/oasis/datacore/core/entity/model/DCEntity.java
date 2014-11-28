package org.oasis.datacore.core.entity.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;


/**
 * This class is the support for storing Datacore data in MongoDB.
 * 
 * @author mdutoo
 *
 */
@Document
// HOWEVER where it is stored (in which collection) is driven by the metamodel
public class DCEntity implements Comparable<DCEntity>, Serializable {
   private static final long serialVersionUID = -6529766074319438866L;

   public static final String KEY_URI = "_uri";
   public static final String KEY_V = "_v";
   public static final String KEY_T = "_t";
   public static final String KEY_P = "_p";

   public static final String KEY_AR = "_ar";
   public static final String KEY_R = "_r";
   public static final String KEY_W = "_w";
   public static final String KEY_O = "_o";

   public static final String KEY_CR_AT = "_crAt";
   public static final String KEY_CR_BY = "_crBy";
   public static final String KEY_CH_AT = "_chAt";
   public static final String KEY_CH_BY = "_chBy";

   protected final static Logger LOG = LoggerFactory.getLogger(DCEntity.class
         .getCanonicalName());

   protected static final int COMPARE_LESS = -1;
   protected static final int COMPARE_EQUALS = 0;
   protected static final int COMPARE_GREATER = 1;

   /** (CANT replace it by URI because not valid ObjectId
    * (though interesting : unique save across versions / approvable / diffs) */
   @Id // _id ; BEWARE this field must be removed before the uri field can be annotated
   // by @Id, because Spring has an inclination towards using any existing id field first !!
   private String id; // TODO or ObjectId ??
   /** for optimistic locking ; NB. for Spring 0 == new so no -1 !! ; default to null i.e. new */
   @Version
   @Field(KEY_V)
   private Long version = null;
   @Indexed(unique = true) // NB. created in DatacoreSampleBase since Spring not in a single collection
   /** NB. could not be a valid ObjectId
    * TODO Q not obligatory if embedded ? or then only sub-uri ??
    * TODO Q contains rdf:type, because collection = use case != rdf:type ? or even several types ???
    * TODO Q contains containerUrl, if we were to have local copies of remote federated datacores ???? */
   @Field(KEY_URI)
   private String uri;
   /** TODO Q OR NOT because stays the same in a collection (see query uses) ?? (not indexed for the same reason)
    * and only @Transient and filled by service / dao or lifecycle event ??
    * TODO LATER rather direct reference filled etc. ? */
   @Field("_mdln") // TODO _t ? or it is rather the source / branch / responsible owner ??
   private String modelName;
   /*@Transient
   private DCResourceModel model;*/ // TODO have a transient reference to model in entity ?? fill it in lifecycle event ??? AND / OR baseType ?
   /**
    * types : type mixins plus model,
    * TODO must be indexed if (used to discriminate i.e.) polymorphic collection
    * TODO Q rather _ts, _a, _m ?? or only as key of submap ?!?
    * TODO LATER rather direct references filled etc. ?
    */
   @Field(KEY_T)
   private List<String> types;

   // more auditing see
   // http://maciejwalkowiak.pl/blog/2013/05/24/auditing-entities-in-spring-data-mongodb/
   // timestamps : TODO required ?
   @CreatedDate
   @Field(KEY_CR_AT)
   // TODO Q index ?
   private DateTime created;
   // keep names short
   // http://stackoverflow.com/questions/5916080/what-are-naming-conventions-for-mongodb
   @LastModifiedDate
   @Field(KEY_CH_AT)
   @Indexed // NB. created in DatacoreSampleBase since Spring not in a single collection
   private DateTime lastModified;

   /**
    * who did it : TODO required ? If Oasis Users are in same db, could be an
    * instance of User instead and let audited entities refer to it see
    * http://satishab
    * .blogspot.fr/2013/03/part-2-persistence-layer-with-mongo-db.html
    */
   @CreatedBy
   @Field(KEY_CR_BY)
   private String createdBy;
   // keep names short
   // http://stackoverflow.com/questions/5916080/what-are-naming-conventions-for-mongodb
   @LastModifiedBy
   @Field(KEY_CH_BY)
   // TODO Q index ?
   private String lastModifiedBy;
   
   /** TODO Q rather store properties at root (using lifecycle event) ?
    * not really required for storage efficiency if renamed "_p", TODO Q maybe index size ??
    * TODO dates are loaded as Date and not Joda DateTime */
   @Field(KEY_P)
   private Map<String,Object> properties;
   //private Map<String,DCEntityValueBase> properties;
   //private ArrayList<HashMap<String,Object>> propertiesList; // TODO OPT alternate value properties ?

   // TODO also spring mongo :
   // @DBRef : annotated reference (asssociations / relationships) see
   // http://satishab.blogspot.fr/2013/03/part-2-persistence-layer-with-mongo-db.html
   // @Field : alias
   // @Indexed(unique=true)
   // inheritance...

   // TODO not in spring but in mongoid :
   // (previous)changes (history ?!) & reset, default values, types (binary,
   // range, regexp, symbol)
   // localization & fallbacks order
   // dynamic fields, security, readonly fields

   // TODO for datacore : _container_id/uri

   /** required to check query rights (which is read mass operation) in a single step.
    * Computed out of readers (only) + writers + readers. Only readers are not enough
    * because when removing a user's write permission, can't know if must also remove
    * his read permission or if he also independently had a read permission.
    * Alternative : allow several same values by storing as list, but there is still
    * as much requiring to be stored. */
   @Field(KEY_AR)
   @Indexed // NB. created in DatacoreSampleBase since Spring not in a single collection
   private Set<String> allReaders;
   /** ONLY REQUIRED IF MASS W & O OPERATIONS but otherwise still need to be stored somewhere */
   @Field(KEY_R)
   private Set<String> readers;
   /** ONLY REQUIRED IF MASS W & O OPERATIONS but otherwise still need to be stored somewhere */
   @Field(KEY_W)
   private Set<String> writers;
   /** ONLY REQUIRED IF MASS W & O OPERATIONS but otherwise still need to be stored somewhere */
   @Field(KEY_O)
   // For now NOT indexed, "my documents" has to be found using business / modeled DCFields
   private Set<String> owners;
   
   /** request-scoped cache built in resourceToEntity
    * or else getEntity & LDP service findDataInType()'s executeMongoDbQuery */
   @Transient
   private transient DCModelBase cachedModel;
   /** request-scoped cache built in resourceToEntity */
   @Transient
   private transient DCModelBase cachedStorageModel;
   /** request-scoped cache built in resourceToEntity */
   @Transient
   private transient DCModelBase cachedDefinitionModel;
   
   public DCEntity() {
      
   }
   
   /**
    * Deep clone constructor
    * @param dcEntity to clone or copy (extended or embeded)
    */
   public DCEntity(DCEntity dcEntity) {
      this.setId(dcEntity.getId()); // TODO rm
      this.setUri(dcEntity.getUri());
      this.setVersion(dcEntity.getVersion());
      this.setModelName(dcEntity.getModelName());
      this.setTypes(new ArrayList<String>(dcEntity.getTypes()));
      this.setCreated(dcEntity.getCreated());
      this.setLastModified(dcEntity.getLastModified());
      this.setCreatedBy(dcEntity.getCreatedBy());
      this.setLastModifiedBy(dcEntity.getLastModifiedBy());
      this.setAllReaders(dcEntity.getAllReaders());
      this.setReaders(dcEntity.getReaders());
      this.setWriters(dcEntity.getWriters());
      this.setOwners(dcEntity.getOwners());
      // NB. not copying model caches in case of derived model ex. historization
      this.properties = new HashMap<String, Object>(dcEntity.properties.size());
      //this.properties = new HashMap<String, DCEntityValueBase>(dcEntity.properties.size());
      for (String key : dcEntity.properties.keySet()) {
         Object value = dcEntity.properties.get(key);
         if (value instanceof DCEntity) {
            value = new DCEntity((DCEntity) value);
         } else if (value instanceof List<?>) {
            value = new ArrayList<Object>((List<?>) value);
         } else if (value instanceof Map<?,?>) {
            value = new HashMap<String,Object>((Map<? extends String,?>) value);
         }
         this.properties.put(key, value/*DCEntityValueBase.newValue(value)*/);
      }
   }

   public boolean isNew() {
      //return this.getId() == null;
      return this.getVersion() == null; // Spring's
   }

   public int compareTo(DCEntity o) {
      if (this.isNew()) {
         return COMPARE_LESS;
      }
      if (o.isNew()) {
         return COMPARE_GREATER;
      }
      int idCompareRes = this.getId().compareTo(o.getId());
      if (idCompareRes != COMPARE_EQUALS) {
         return idCompareRes;
      }
      return this.getVersion().compareTo(o.getVersion());
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }
      if (!this.getClass().isInstance(o)) {
         return false;
      }
      DCEntity oge = (DCEntity) o;
      if (this.isNew() || oge.isNew()) { // TODO TODO doesn't work once gone in
                                         // oasis datacore =>
                                         // setId(getDcObject().getUri()) ???
         return false;
      }
      return this.getId().equals(oge.getId());
   }

   @Override
   public int hashCode() {
      return (this.isNew()) ? super.hashCode() : this.getId().hashCode();
   }

   /**
    * TODO LATER using Jackson ?????
    */
   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("DCEntity[ ");
      sb.append(this.uri); // includes modelName (& iri)
      sb.append(" (");
      sb.append(this.version);
      sb.append(") +types: ");
      sb.append("" + this.types);
      sb.append(" - ");
      sb.append("" + this.properties);
      sb.append(" - ");
      sb.append(this.lastModified);
      sb.append(" ]");
      return sb.toString();
   }

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getUri() {
      return uri;
   }

   public void setUri(String uri) {
      this.uri = uri;
   }

   public Long getVersion() {
      return version;
   }

   /**
    * sets to null if < 0 (for Spring)
    * @param version
    */
   public void setVersion(Long version) {
      if (version != null && version < 0) {
         this.version = null;
      } else {
         this.version = version;
      }
   }

   public String getModelName() {
      return modelName;
   }

   public void setModelName(String modelName) {
      this.modelName = modelName;
   }

   public List<String> getTypes() {
      return types;
   }

   public void setTypes(List<String> types) {
      this.types = types;
   }

   public DateTime getCreated() {
      return created;
   }

   public void setCreated(DateTime created) {
      this.created = created;
   }

   public DateTime getLastModified() {
      return lastModified;
   }

   public void setLastModified(DateTime lastModified) {
      this.lastModified = lastModified;
   }

   public String getCreatedBy() {
      return createdBy;
   }

   public void setCreatedBy(String createdBy) {
      this.createdBy = createdBy;
   }

   public String getLastModifiedBy() {
      return lastModifiedBy;
   }

   public void setLastModifiedBy(String lastModifiedBy) {
      this.lastModifiedBy = lastModifiedBy;
   }

   public Map<String,Object> getProperties() {
      if (this.properties == null) {
         this.properties = new  HashMap<String,Object>(); // TODO TODO does it take (too much) place in mongodb ???
      }
      return this.properties;
   }

   public void setProperties(Map<String,Object> properties) {
      this.properties = properties;
   }

   public Set<String> getAllReaders() {
      return allReaders;
   }

   public void setAllReaders(Set<String> allReaders) {
      this.allReaders = allReaders;
   }

   public Set<String> getReaders() {
      return readers;
   }

   public void setReaders(Set<String> readers) {
      this.readers = readers;
   }

   public Set<String> getWriters() {
      return writers;
   }

   public void setWriters(Set<String> writers) {
      this.writers = writers;
   }

   public Set<String> getOwners() {
      return owners;
   }

   public void setOwners(Set<String> owners) {
      this.owners = owners;
   }

   public DCModelBase getCachedModel() {
      return cachedModel;
   }

   public void setCachedModel(DCModelBase cachedModel) {
      this.cachedModel = cachedModel;
   }

   /*public DCStorage getCachedStorage() {
      return cachedStorage;
   }

   public void setCachedStorage(DCStorage cachedStorage) {
      this.cachedStorage = cachedStorage;
   }*/

   public DCModelBase getCachedStorageModel() {
      return cachedStorageModel;
   }

   public void setCachedStorageModel(DCModelBase cachedStorageModel) {
      this.cachedStorageModel = cachedStorageModel;
   }

   public DCModelBase getCachedDefinitionModel() {
      return cachedDefinitionModel;
   }

   public void setCachedDefinitionModel(DCModelBase cachedDefinitionModel) {
      this.cachedDefinitionModel = cachedDefinitionModel;
   }

}
