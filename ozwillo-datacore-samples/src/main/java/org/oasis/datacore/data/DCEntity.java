package org.oasis.datacore.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;


/**
 * TODO replace by -core's
 * 
 * @author mdutoo
 *
 */
@Document
// HOWEVER where it is stored (in which collection) is driven by the metamodel
public class DCEntity implements Comparable<DCEntity>, Serializable {
   private static final long serialVersionUID = -6529766074319438866L;

   protected final static Logger LOG = LoggerFactory.getLogger(DCEntity.class
         .getCanonicalName());

   protected static final int COMPARE_LESS = -1;
   protected static final int COMPARE_EQUALS = 0;
   protected static final int COMPARE_GREATER = 1;

   /** TODO replace it by URI (unique save across versions / approvable / diffs) */
   @Id // _id
   private String id; // TODO or ObjectId ??
   /** for optimistic locking */
   @Version
   @Field("_v")
   private Long version;
   @Indexed(unique = true)
   @Field("_uri")
   private String uri; // TODO Q not obligatory if embedded ? or then only sub-uri ??
   // TODO Q also rdf:type, because collection = use case != rdf:type ? or even several types ???
   /** TODO Q OR NOT because stays the same in a collection (see query uses) ?? (not indexed for the same reason)
    * and only @Transient and filled by service / dao or lifecycle event ??
    * TODO LATER rather direct reference filled etc. ? */
   @Field("_mdln") // TODO _t ?
   private String modelName;
   /*@Transient
   private DCResourceModel model;*/ // TODO have a transient reference to model in entity ?? fill it in lifecycle event ??? AND / OR baseType ?
   /**
    * TODO Q rather _ts, _a, _m ?? or only as key of submap ???
    * TODO LATER rather direct references filled etc. ?
    */
   @Field("_t")
   private List<String> types; 

   // more auditing see
   // http://maciejwalkowiak.pl/blog/2013/05/24/auditing-entities-in-spring-data-mongodb/
   // timestamps : TODO required ?
   @CreatedDate
   @Field("_crAt")
   private DateTime created;
   // keep names short
   // http://stackoverflow.com/questions/5916080/what-are-naming-conventions-for-mongodb
   @LastModifiedDate
   @Field("_chAt")
   private DateTime lastModified;

   /**
    * who did it : TODO required ? If Ozwillo Users are in same db, could be an
    * instance of User instead and let audited entities refer to it see
    * http://satishab
    * .blogspot.fr/2013/03/part-2-persistence-layer-with-mongo-db.html
    */
   @CreatedBy
   @Field("_crBy")
   private String createdBy;
   // keep names short
   // http://stackoverflow.com/questions/5916080/what-are-naming-conventions-for-mongodb
   @LastModifiedBy
   @Field("_chBy")
   private String lastModifiedBy;
   
   /** TODO Q rather store properties at root (using lifecycle event) ?
    * not really required for storage efficiency if renamed "_p", TODO Q maybe index size ?? */
   @Field("_p")
   private HashMap<String,Object> properties;
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

   
   public DCEntity() {
      
   }
   
   /**
    * Deep clone constructor
    * @param dcEntity to clone or copy (extended or embeded)
    */
   public DCEntity(DCEntity dcEntity) {
      this.setId(dcEntity.getId());
      this.setVersion(dcEntity.getVersion());
      this.setUri(dcEntity.getUri());
      this.setModelName(dcEntity.getModelName());
      this.setCreated(dcEntity.getCreated());
      this.setLastModified(dcEntity.getLastModified());
      this.setCreatedBy(dcEntity.getCreatedBy());
      this.setLastModifiedBy(dcEntity.getLastModifiedBy());
      this.properties = new HashMap<String, Object>(dcEntity.properties.size());
      for (String key : dcEntity.properties.keySet()) {
         Object value = dcEntity.properties.get(key);
         if (value instanceof DCEntity) {
            value = new DCEntity((DCEntity) value);
         } else if (value instanceof List<?>) {
            value = new ArrayList<Object>((List<?>) value);
         } else if (value instanceof Map<?,?>) {
            value = new HashMap<String,Object>((Map<? extends String,?>) value);
         }
         this.properties.put(key, value);
      }
   }

   public boolean isNew() {
      return this.getId() == null;
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

   public Long getVersion() {
      return version;
   }

   public void setVersion(Long version) {
      this.version = version;
   }

   public String getUri() {
      return uri;
   }

   public void setUri(String uri) {
      this.uri = uri;
   }

   public String getModelName() {
      return modelName;
   }

   public void setModelName(String modelName) {
      this.modelName = modelName;
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

   public HashMap<String,Object> getProperties() {
      if (this.properties == null) {
         this.properties = new  HashMap<String,Object>(); // TODO TODO does it take (too much) place in mongodb ???
      }
      return this.properties;
   }

   public void setProperties(HashMap<String,Object> properties) {
      this.properties = properties;
   }

}
