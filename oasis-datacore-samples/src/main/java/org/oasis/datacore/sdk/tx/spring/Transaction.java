package org.oasis.datacore.sdk.tx.spring;

import java.util.List;

import org.joda.time.DateTime;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * NB. inheriting from this allows tx in separate collection & modeling
 * tx contents (source, destination, value)
 * 
 * TODO rather store diffs, better for allowing several simultaneous tx & also history ???
 * TODO (& to) allow to control tx from client side (& BEWARE optimistic locking) ??????
 * 
 * @author mdutoo
 *
 */
@Document(collection = "transactions") // stores type in _class through MappingMongoConverter
// (but could be prevented by conf see http://www.mkyong.com/mongodb/spring-data-mongodb-remove-_class-column/ http://stackoverflow.com/questions/6810488/spring-data-mongodb-mappingmongoconverter-remove-class )
public class Transaction {

   public static final String STATE_INITIAL = "initial";
   public static final String STATE_PENDING = "pending";
   public static final String STATE_COMMITTED = "committed";
   public static final String STATE_DONE = "done";

   @Id
   private String id;
   /** for optimistic locking */
   @Version
   private Long version;
   /** tx start time */
   @CreatedDate
   private DateTime createdAt;
   /** tx end time */
   @LastModifiedDate
   private DateTime lastModified;
   /** tx author. NB. no need for lastModifiedBy */
   @CreatedBy
   private String createdBy;
   private String state;
   /** OPT allows to reperform even if failed before perform
    * TODO rather diffs, better for allowing several simultaneous tx & also history ??? */
   private List<Object> newValues;

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
   public DateTime getCreatedAt() {
      return createdAt;
   }
   public void setCreatedAt(DateTime createdAt) {
      this.createdAt = createdAt;
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
   public String getState() {
      return state;
   }
   public void setState(String state) {
      this.state = state;
   }
   public List<Object> getNewValues() {
      return newValues;
   }
   public void setNewValues(List<Object> newValues) {
      this.newValues = newValues;
   }
   
   @Override
   public boolean equals(Object obj) {
      return !(obj instanceof Transaction)
            || this.getId().equals(((Transaction) obj).getId());
   }
   @Override
   public int hashCode() {
      return (this.getId() == null) ? super.hashCode() : this.getId().hashCode();
   }

}
