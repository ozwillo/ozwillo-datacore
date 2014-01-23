package org.oasis.datacore.rights.rest.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DCRights {

	@JsonProperty
	private List<String> owners;

	@JsonProperty
	private List<String> writers;

	@JsonProperty
	private List<String> readers;

	public List<String> getOwners() {
		return owners;
	}

	public void setOwners(List<String> owners) {
		this.owners = owners;
	}

	public List<String> getWriters() {
		return writers;
	}

	public void setWriters(List<String> writers) {
		this.writers = writers;
	}

	public List<String> getReaders() {
		return readers;
	}

	public void setReaders(List<String> readers) {
		this.readers = readers;
	}

}
