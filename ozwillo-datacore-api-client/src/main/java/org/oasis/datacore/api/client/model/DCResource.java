package org.oasis.datacore.api.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

public class DCResource {
	private String modelType = null;
	/* URI */
	private String uri = null;
	private List<String> types = new ArrayList<String>();
	private DateTime created = null;
	private String createdBy = null;
	private String lastModifiedBy = null;
	private Map<String, Object> properties = null;
	private String id = null;
	private DateTime lastModified = null;
	/* version */
	private Long version = null;

	public String getModelType() {
		return modelType;
	}

	public void setModelType(String modelType) {
		this.modelType = modelType;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
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

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public DateTime getLastModified() {
		return lastModified;
	}

	public void setLastModified(DateTime lastModified) {
		this.lastModified = lastModified;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class DCResource {\n");
		sb.append("  modelType: ").append(modelType).append("\n");
		sb.append("  uri: ").append(uri).append("\n");
		sb.append("  types: ").append(types).append("\n");
		sb.append("  created: ").append(created).append("\n");
		sb.append("  createdBy: ").append(createdBy).append("\n");
		sb.append("  lastModifiedBy: ").append(lastModifiedBy).append("\n");
		sb.append("  properties: ").append(properties).append("\n");
		sb.append("  id: ").append(id).append("\n");
		sb.append("  lastModified: ").append(lastModified).append("\n");
		sb.append("  version: ").append(version).append("\n");
		sb.append("}\n");
		return sb.toString();
	}
}
