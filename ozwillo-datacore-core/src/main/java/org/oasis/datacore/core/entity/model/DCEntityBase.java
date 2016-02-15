package org.oasis.datacore.core.entity.model;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;


/**
 * This class is the support for storing Datacore data in MongoDB.
 * Where it is stored (in which collection) is driven by the metamodel.
 * Tried to keep names short http://stackoverflow.com/questions/5916080/what-are-naming-conventions-for-mongodb
 * 
 * @author mdutoo
 *
 */
@Document
public abstract class DCEntityBase implements Comparable<DCEntityBase>, Serializable {
   private static final long serialVersionUID = -6529766074319438866L;

   public static final String KEY_URI = "_uri";
   public static final String KEY_V = "_v";
   public static final String KEY_T = "_t";
   public static final String KEY_B = "_b";
   public static final String KEY_P = "_p";

   public static final String KEY_AR = "_ar";
   public static final String KEY_R = "_r";
   public static final String KEY_W = "_w";
   public static final String KEY_O = "_o";

   /** creation date is retrieved from default mongo _id (seconds only) instead of @CreatedDate */
   public static final String KEY_ID = "_id"; // "_id.timestamp"; // "_crAt";
   public static final String KEY_CR_BY = "_crBy";
   public static final String KEY_CH_AT = "_chAt";
   public static final String KEY_CH_BY = "_chBy";

   public static final String KEY_ALIAS_OF = "_aliasOf";

   protected final static Logger LOG = LoggerFactory.getLogger(DCEntityBase.class.getCanonicalName());

   protected static final int COMPARE_LESS = -1;
   protected static final int COMPARE_EQUALS = 0;
   protected static final int COMPARE_GREATER = 1;

   /** mongo _id ; NB. CANT replace it by URI because not valid ObjectId
    * and interesting anyway : stores creation second, unique
    * (including across shards) save across versions / approvable / diffs */
   @Id // BEWARE this field should be removed before the uri field could be annotated
   // by @Id, because Spring has an inclination towards using any existing "id" field first !!
   private ObjectId id; // ObjectId rather than String to access its timestamp = createdAt
   /** for optimistic locking ; NB. for Spring 0 == new so no -1 !! ; default to null i.e. new ; NOT indexed */
   @Field(KEY_V)
   private Long version = null;
   /** NB. could not be a valid ObjectId because of its constraints (size...)
    * unique index in each shard, while mongo ensures uniqueness of
    * (indexInId-based) shard key http://docs.mongodb.org/manual/tutorial/enforce-unique-keys-for-sharded-collections/
    * BUT NOT UNIQUE in ex. history. NB. created in DatacoreSampleBase since not in a single collection
    * TODO Q not obligatory if embedded ? or then only sub-uri ??
    * TODO Q contains rdf:type, because collection = use case != rdf:type ? or even several types ???
    * TODO Q contains containerUrl, if we were to have local copies of remote federated datacores ???? */
   @Indexed(unique = true)
   @Field(KEY_URI)
   private String uri;
   /**
    * types : model type (first one) plus mixin types
    * index for (used to discriminate i.e.) polymorphic collection
    * TODO Q LATER rather direct references filled etc. ?
    */
   @Field(KEY_T)
   @Indexed // get on a given type in a wider storage model... NB. created in DatacoreSampleBase since not in a single collection
   private List<String> types;
   /** ("branch") its project, filled only when model.isMultiProjectStorage, used in unique index with _uri */
   @Field(KEY_B)
   @Indexed // get on a given project in a wider storage model... NB. created in DatacoreSampleBase since not in a single nor all collections
   private String projectName;

   // more auditing see
   // http://maciejwalkowiak.pl/blog/2013/05/24/auditing-entities-in-spring-data-mongodb/
   /** get first ones... cache only, creation date is retrieved from default
    * mongo _id (seconds only) instead of @CreatedDate */
   @Transient
   private DateTime created;
   /** index to get most recent... NB. created in DatacoreSampleBase since not in a single collection */
   @LastModifiedDate
   @Field(KEY_CH_AT)
   @Indexed
   private DateTime lastModified;

   /**
    * who did it : TODO required ? If Ozwillo Users are in same db, could be an
    * instance of User instead and let audited entities refer to it see
    * http://satishab.blogspot.fr/2013/03/part-2-persistence-layer-with-mongo-db.html
    */
   @CreatedBy
   @Field(KEY_CR_BY)
   // TODO Q index ?
   private String createdBy;
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

   // NB. also spring mongo :
   // @DBRef : annotated reference (asssociations / relationships) see
   // http://satishab.blogspot.fr/2013/03/part-2-persistence-layer-with-mongo-db.html
   // @Field : alias
   // @Indexed(unique=true)
   // inheritance...

   // TODO Q not in spring but in mongoid :
   // (previous)changes (history ?!) & reset, default values, types (binary,
   // range, regexp, symbol)
   // localization & fallbacks order
   // dynamic fields, security, readonly fields

   // TODO Q for datacore : _container_id/uri

   /** required to check query rights (which is read mass operation) in a single step.
    * Computed out of readers (only) + writers + readers. Only readers are not enough
    * because when removing a user's write permission, can't know if must also remove
    * his read permission or if he also independently had a read permission.
    * Alternative : allow several same values by storing as list, but there is still
    * as much requiring to be stored. */
   @Field(KEY_AR)
   @Indexed // NB. created in DatacoreSampleBase since not in a single collection
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


   @Field(KEY_ALIAS_OF)
   private String aliasOf;

   /** request-scoped cache used in resourceToEntity,
    * getEntity & LDP service findDataInType()'s executeMongoDbQuery.
    * USE IT THROUGH EntityModelService.getModel() to allow LATER
    * to replace it by better archi & more generic impl. */
   @Transient
   private transient DCModelBase cachedModel;
   /** request-scoped cache built in resourceToEntity */
   @Transient
   private transient DCModelBase cachedStorageModel;
   /** request-scoped cache built in resourceToEntity */
   @Transient
   private transient DCModelBase cachedDefinitionModel;
   /** cache like, between ResourceService and EntityPermissionEvaluator */
   @Transient
   private transient DCEntityBase previousEntity;
   
   public DCEntityBase() {
      
   }
   
   /**
    * Deep clone constructor
    * @param dcEntity to clone or copy (extended or embeded)
    */
   @SuppressWarnings("unchecked")
   public DCEntityBase(DCEntityBase dcEntity) {
      this.copyNonResourceFieldsFrom(dcEntity);
      
      this.setUri(dcEntity.getUri());
      this.setVersion(dcEntity.getVersion());
      this.setTypes(new ArrayList<String>(dcEntity.getTypes()));
      if (dcEntity.getProjectName() != null) {
         this.setProjectName(dcEntity.getProjectName());
      } // else not multiStorageProject
      // (if new entity won't be in a multiStorageProject, will be erased in entityService)
      
      this.setLastModified(dcEntity.getLastModified());
      this.setCreatedBy(dcEntity.getCreatedBy());
      this.setLastModifiedBy(dcEntity.getLastModifiedBy());

      this.setAliasOf(dcEntity.getAliasOf());

      // NB. not copying model caches in case of derived model ex. historization
      
      this.properties = new HashMap<String, Object>(dcEntity.properties.size());
      //this.properties = new HashMap<String, DCEntityValueBase>(dcEntity.properties.size());
      for (String key : dcEntity.properties.keySet()) {
         Object value = dcEntity.properties.get(key);
         if (value instanceof DCEntityBase) {
            try {
               Constructor<? extends Object> copyCons = value.getClass().getConstructor(new Class[] { DCEntityBase.class });
               value = copyCons.newInstance((DCEntityBase) value);
            } catch (Exception ex) {
               throw new RuntimeException(ex);
            }
         } else if (value instanceof List<?>) {
            value = new ArrayList<Object>((List<?>) value);
         } else if (value instanceof Map<?,?>) {
            value = new HashMap<String,Object>((Map<? extends String,?>) value);
         }
         this.properties.put(key, value/*DCEntityValueBase.newValue(value)*/);
      }
   }

   public void copyNonResourceFieldsFrom(DCEntityBase existingDataEntity) {
//      this.setPreviousEntity(existingDataEntity);
      
      this.setId(existingDataEntity.getId()); // else OptimisticLockingFailureException !
//      this.setVersion(existingDataEntity.getVersion());
      this.setCreatedBy(existingDataEntity.getCreatedBy()); // else lost ! #157
      // NB. no setCreated() since stored (rounded down) in mongo-generated id
      this.setLastModified(existingDataEntity.getLastModified()); // (will be overriden anyway)
      this.setLastModifiedBy(existingDataEntity.getLastModifiedBy()); // (will be overriden anyway)
      
      this.setProjectName(existingDataEntity.getProjectName());
      
      this.setAllReaders(existingDataEntity.getAllReaders());
      this.setReaders(existingDataEntity.getReaders());
      this.setWriters(existingDataEntity.getWriters());
      this.setOwners(existingDataEntity.getOwners());

      this.setAliasOf(existingDataEntity.getAliasOf());
   }

   public boolean isNew() {
      //return this.getId() == null;
      return this.getVersion() == null; // Spring's
   }

   public boolean isAlias() {
      return this.aliasOf != null;
   }

   public int compareTo(DCEntityBase o) {
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
      DCEntityBase oge = (DCEntityBase) o;
      if (this.isNew() || oge.isNew()) { // TODO TODO doesn't work once gone in
                                         // Ozwillo datacore =>
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
      if (this.projectName != null) { // multiProjectStorage : log entity project / branch
         sb.append(this.projectName);
         sb.append('.');
      }
      sb.append(this.uri); // includes modelName (& iri)
      if (this.aliasOf != null) {
         sb.append(" [alias of ");
         sb.append(this.aliasOf);
         sb.append("]");
      }
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

   public ObjectId getId() {
      return id;
   }

   public void setId(ObjectId id) {
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

   public List<String> getTypes() {
      return types;
   }

   public void setTypes(List<String> types) {
      this.types = types;
   }
   
   public String getProjectName() {
      return projectName;
   }

   public void setProjectName(String projectName) {
      this.projectName = projectName;
   }

   public DateTime getCreated() {
      if (this.created == null // then cache
            && this.id != null) { // else new
         this.created = new DateTime(this.id.getDate()); // oid = timestamp + machine... see :
         // http://www.mongotips.com/b/another-objectid-trick/
         // http://stackoverflow.com/questions/26816734/spring-data-mongo-template-returning-timestamp-instead-of-plain-object-id
         // http://steveridout.github.io/mongo-object-time/
      }
      return this.created;
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

   public String getAliasOf() {
      return aliasOf;
   }

   public void setAliasOf(String aliasOf) {
      this.aliasOf = aliasOf;
   }

   public DCModelBase getCachedModel() {
      return cachedModel;
   }

   public void setCachedModel(DCModelBase cachedModel) {
      this.cachedModel = cachedModel;
   }

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
   
   public DCEntityBase getPreviousEntity() {
      return previousEntity;
   }

   public void setPreviousEntity(DCEntityBase previousEntity) {
      this.previousEntity = previousEntity;
   }

}
