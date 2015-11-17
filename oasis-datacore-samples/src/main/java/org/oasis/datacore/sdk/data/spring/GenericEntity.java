package org.oasis.datacore.sdk.data.spring;

import java.io.Serializable;

import org.joda.time.DateTime;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Field;

//@Document // annotated @Persistent
public class GenericEntity<T>  implements Comparable<GenericEntity<T>>, Serializable {

	private static final long serialVersionUID = -9184935025288881172L;

	///protected final static Logger LOG = Logger.getLogger(GenericEntity.class.getCanonicalName());
	
	protected static final int COMPARE_LESS = -1;
	protected static final int COMPARE_EQUALS = 0;
	protected static final int COMPARE_GREATER = 1;
	
	@Id
	private String id; // TODO or ObjectId ??
	
	/** for optimistic locking */
    @Version
    private Long version;
    
    // more auditing see http://maciejwalkowiak.pl/blog/2013/05/24/auditing-entities-in-spring-data-mongodb/
    // timestamps : TODO required ?
    @CreatedDate
    private DateTime createdAt;
    @Field("changed") // keep names short http://stackoverflow.com/questions/5916080/what-are-naming-conventions-for-mongodb
    @LastModifiedDate
    private DateTime lastModified;
    
    /**
     * who did it : TODO required ?
	 * If Ozwillo Users are in same db, could be an instance of User instead and let audited entities refer to it
	 * see http://satishab.blogspot.fr/2013/03/part-2-persistence-layer-with-mongo-db.html
     */
    @CreatedBy
    private String createdBy;
    @Field("changedBy") // keep names short http://stackoverflow.com/questions/5916080/what-are-naming-conventions-for-mongodb
    @LastModifiedBy
    private String lastModifiedBy;
    
    // TODO also spring mongo :
    // @DBRef : annotated reference (asssociations / relationships) see http://satishab.blogspot.fr/2013/03/part-2-persistence-layer-with-mongo-db.html
    // @Field : alias
    // @Indexed(unique=true)
    // inheritance...
    
    // TODO not in spring but in mongoid :
    // (previous)changes (history ?!) & reset, default values, types (binary, range, regexp, symbol)
    // localization & fallbacks order
    // dynamic fields, security, readonly fields
    
    // TODO for datacore : _container_id/uri

	
	public boolean isNew() {
		return this.getId() == null;
	}

	public int compareTo(GenericEntity<T> o) {
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
		GenericEntity<?> oge = (GenericEntity<?>) o;
		if (this.isNew() || oge.isNew()) { // TODO TODO doesn't work once gone in ozwillo datacore => setId(getDcObject().getUri()) ???
			return false;
		}
		return this.getId().equals(oge.getId());
	}

	@Override
	public int hashCode() {
		return (this.isNew()) ? super.hashCode() : this.getId().hashCode();
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

	public String getLastModifiedBy() {
		return lastModifiedBy;
	}

	public void setLastModifiedBy(String lastModifiedBy) {
		this.lastModifiedBy = lastModifiedBy;
	}
	
}
