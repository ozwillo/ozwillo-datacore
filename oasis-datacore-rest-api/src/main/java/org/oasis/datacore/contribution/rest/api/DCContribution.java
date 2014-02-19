package org.oasis.datacore.contribution.rest.api;

import java.util.List;

import org.oasis.datacore.rest.api.DCResource;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DCContribution {

	@JsonProperty
	public String id;

	@JsonProperty
	public String title;

	@JsonProperty(required = true)
	public String comment;

	@JsonProperty
	public String validationComment;

	@JsonProperty
	public String userId;

	@JsonProperty
	public String status;

	@JsonProperty
	public String modelType;

	@JsonProperty
	public List<DCResource> listResources;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getValidationComment() {
		return validationComment;
	}

	public void setValidationComment(String validationComment) {
		this.validationComment = validationComment;
	}

	public List<DCResource> getListResources() {
		return listResources;
	}

	public void setListResources(List<DCResource> listResources) {
		this.listResources = listResources;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getModelType() {
		return modelType;
	}

	public void setModelType(String modelType) {
		this.modelType = modelType;
	}

}
